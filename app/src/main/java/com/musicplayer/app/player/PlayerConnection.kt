package com.musicplayer.app.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.musicplayer.app.data.repository.MusicRepository
import com.musicplayer.app.data.repository.PreferencesRepository
import com.musicplayer.app.model.PlaybackContext
import com.musicplayer.app.model.PlaybackContextType
import com.musicplayer.app.model.PlaybackState
import com.musicplayer.app.model.RepeatMode
import com.musicplayer.app.model.Song
import com.musicplayer.app.service.MusicService
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PlayerConnection(
    private val context: Context,
    private val musicRepository: MusicRepository,
    private val preferencesRepository: PreferencesRepository
) {
    private val scope = CoroutineScope(Dispatchers.Main)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    private val _playbackState = MutableStateFlow(PlaybackState())
    val playbackState = _playbackState.asStateFlow()

    private var currentSongList = mutableListOf<Song>()
    private var positionTickerJob: Job? = null
    private var hasRestoredState = false
    private var currentAudioSessionId: Int = 0
    private var currentPlaybackContext: PlaybackContext? = null

    val equalizerController = EqualizerController(preferencesRepository)
    val sleepTimerManager = SleepTimerManager(scope) {
        pause()
    }

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            updatePlaybackState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_ENDED) {
                sleepTimerManager.onTrackEnded()
                val player = controller
                if (player != null && player.repeatMode == Player.REPEAT_MODE_OFF) {
                    val advanced = advanceToNextCollection()
                    if (advanced) {
                        return
                    }
                }
            }
            if (playbackState == Player.STATE_READY) {
                if (currentAudioSessionId > 0) {
                    equalizerController.reapplyIfEnabled(currentAudioSessionId)
                }
            }
            updatePlaybackState()
            saveCurrentPosition()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackState()
            if (isPlaying) {
                startPositionTicker()
                if (currentAudioSessionId > 0) {
                    equalizerController.reapplyIfEnabled(currentAudioSessionId)
                }
            } else {
                stopPositionTicker()
                saveCurrentPosition()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updatePlaybackState()
            saveCurrentPosition()
            if (currentAudioSessionId > 0) {
                equalizerController.reapplyIfEnabled(currentAudioSessionId)
            }
            val currentSong = _playbackState.value.currentSong
            if (currentSong != null && controller?.isPlaying == true) {
                scope.launch(Dispatchers.IO) {
                    musicRepository.recordPlayback(currentSong.id)
                }
            }
        }
    }

    init {
        connectToService()
        observeLibraryForRestoration()
    }

    fun onAudioSessionCreated(audioSessionId: Int) {
        currentAudioSessionId = audioSessionId
        equalizerController.attachAudioSession(audioSessionId)
    }

    fun onAudioSessionReleased() {
        currentAudioSessionId = 0
        equalizerController.release()
    }

    private fun connectToService() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, MusicService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture = future

        future.addListener(
            {
                try {
                    val mediaController = future.get()
                    controller = mediaController
                    mediaController.addListener(playerListener)

                    // Sync preference states
                    applySavedPreferences()

                    // If player is already running, sync state immediately
                    if (mediaController.currentMediaItem != null) {
                        updatePlaybackState()
                        if (mediaController.isPlaying) {
                            startPositionTicker()
                        }
                    } else {
                        restoreSavedPlaybackState()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    private fun observeLibraryForRestoration() {
        scope.launch {
            musicRepository.allSongs.collect { songs ->
                if (songs.isNotEmpty() && !hasRestoredState && controller != null) {
                    restoreSavedPlaybackState()
                }
            }
        }
    }

    private fun applySavedPreferences() {
        scope.launch(Dispatchers.IO) {
            val repeatMode = preferencesRepository.repeatModeFlow.first()
            val shuffleEnabled = preferencesRepository.shuffleEnabledFlow.first()
            val speed = preferencesRepository.playbackSpeedFlow.first()

            withContext(Dispatchers.Main) {
                val player = controller ?: return@withContext
                val exoMode = when (repeatMode) {
                    RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                    RepeatMode.ALL -> Player.REPEAT_MODE_ALL
                    RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                }
                player.repeatMode = exoMode
                player.shuffleModeEnabled = shuffleEnabled
                player.setPlaybackSpeed(speed)
            }
        }
    }

    private fun restoreSavedPlaybackState() {
        val player = controller ?: return
        if (player.currentMediaItem != null) return // Already playing or loaded

        scope.launch(Dispatchers.IO) {
            val (lastSongId, lastPos) = preferencesRepository.getLastPlaybackSnapshot()
            val queueIds = preferencesRepository.savedQueueIdsFlow.first()
            val savedIndex = preferencesRepository.savedQueueIndexFlow.first()
            val rememberPos = preferencesRepository.rememberPositionFlow.first()
            val allSongs = musicRepository.allSongs.value

            if (allSongs.isEmpty()) return@launch

            val songMap = allSongs.associateBy { it.id }
            val restoredSongs = if (queueIds.isNotEmpty()) {
                queueIds.mapNotNull { songMap[it] }
            } else {
                // Fallback to all songs if no queue was saved
                allSongs.take(50)
            }

            if (restoredSongs.isNotEmpty()) {
                val targetIndex = if (lastSongId > 0) {
                    val idx = restoredSongs.indexOfFirst { it.id == lastSongId }
                    if (idx >= 0) idx else savedIndex.coerceIn(0, restoredSongs.size - 1)
                } else {
                    savedIndex.coerceIn(0, restoredSongs.size - 1)
                }

                val targetSong = restoredSongs[targetIndex]
                val safePosition = if (rememberPos && lastPos > 0) lastPos else 0L

                withContext(Dispatchers.Main) {
                    currentSongList.clear()
                    currentSongList.addAll(restoredSongs)

                    val mediaItems = restoredSongs.map { songToMediaItem(it) }
                    player.setMediaItems(mediaItems, targetIndex, safePosition)
                    player.prepare()

                    _playbackState.value = PlaybackState(
                        currentSong = targetSong,
                        isPlaying = false,
                        currentPosition = safePosition,
                        duration = targetSong.duration,
                        queue = restoredSongs,
                        currentQueueIndex = targetIndex,
                        repeatMode = when (player.repeatMode) {
                            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
                            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
                            else -> RepeatMode.OFF
                        },
                        shuffleMode = player.shuffleModeEnabled,
                        playbackSpeed = player.playbackParameters.speed
                    )

                    hasRestoredState = true
                }
            }

            // Restore saved playback context if available
            val savedContext = preferencesRepository.getSavedPlaybackContext()
            if (savedContext != null) {
                try {
                    val type = PlaybackContextType.valueOf(savedContext.first)
                    currentPlaybackContext = PlaybackContext(type, savedContext.second)
                } catch (e: Exception) {
                    // Ignore malformed context
                }
            }
        }
    }

    private fun updatePlaybackState() {
        val player = controller ?: return

        val mediaItem = player.currentMediaItem
        val currentSong = if (mediaItem != null) {
            val id = mediaItem.mediaId.toLongOrNull() ?: -1L
            currentSongList.find { it.id == id } ?: mediaItemToSong(mediaItem)
        } else null

        val repeat = when (player.repeatMode) {
            Player.REPEAT_MODE_ONE -> RepeatMode.ONE
            Player.REPEAT_MODE_ALL -> RepeatMode.ALL
            else -> RepeatMode.OFF
        }

        val queueIndex = player.currentMediaItemIndex

        _playbackState.value = PlaybackState(
            currentSong = currentSong,
            isPlaying = player.isPlaying,
            currentPosition = player.currentPosition.coerceAtLeast(0L),
            duration = if (player.duration > 0) player.duration else (currentSong?.duration ?: 0L),
            repeatMode = repeat,
            shuffleMode = player.shuffleModeEnabled,
            playbackSpeed = player.playbackParameters.speed,
            queue = currentSongList.toList(),
            currentQueueIndex = queueIndex.coerceAtLeast(0)
        )
    }

    private fun startPositionTicker() {
        positionTickerJob?.cancel()
        var tickCounter = 0
        positionTickerJob = scope.launch {
            while (isActive) {
                val player = controller
                if (player != null && player.isPlaying) {
                    val pos = player.currentPosition
                    _playbackState.value = _playbackState.value.copy(currentPosition = pos)
                    // Periodic persistence every 4 seconds without excessive writes
                    tickCounter++
                    if (tickCounter % 8 == 0) {
                        saveCurrentPosition()
                    }
                }
                delay(500)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    fun saveCurrentPosition() {
        val player = controller ?: return
        val currentSong = _playbackState.value.currentSong ?: return
        val pos = player.currentPosition
        val queueIds = currentSongList.map { it.id }
        val queueIndex = player.currentMediaItemIndex

        scope.launch(Dispatchers.IO) {
            preferencesRepository.savePlaybackState(
                songId = currentSong.id,
                position = pos,
                queueIds = queueIds,
                queueIndex = queueIndex
            )
        }
    }

    fun play() {
        controller?.play()
    }

    fun pause() {
        controller?.pause()
        saveCurrentPosition()
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
            saveCurrentPosition()
        } else {
            // If prepared, simply play
            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            }
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val player = controller ?: return
        player.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(currentPosition = positionMs)
        saveCurrentPosition()
    }

    fun skipNext() {
        val player = controller ?: return
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        } else {
            advanceToNextCollection()
        }
    }

    fun skipPrevious() {
        val player = controller ?: return
        if (player.currentPosition > 3000) {
            player.seekTo(0)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            advanceToPreviousCollection()
        }
    }

    fun advanceToNextCollection(): Boolean {
        val context = currentPlaybackContext ?: return false
        val allSongs = musicRepository.allSongs.value
        if (allSongs.isEmpty()) return false

        when (context.type) {
            PlaybackContextType.ALBUM -> {
                val albums = musicRepository.allAlbums.value
                if (albums.isEmpty()) return false
                val currentIndex = albums.indexOfFirst { it.id.toString() == context.identifier }
                val nextAlbum = if (currentIndex >= 0 && currentIndex + 1 < albums.size) {
                    albums[currentIndex + 1]
                } else {
                    albums.firstOrNull()
                } ?: return false

                val nextSongs = allSongs.filter { it.albumId == nextAlbum.id }
                if (nextSongs.isNotEmpty()) {
                    val newContext = PlaybackContext(PlaybackContextType.ALBUM, nextAlbum.id.toString())
                    playAll(nextSongs, startIndex = 0, context = newContext)
                    return true
                }
            }
            PlaybackContextType.ARTIST -> {
                val artists = musicRepository.allArtists.value
                if (artists.isEmpty()) return false
                val currentIndex = artists.indexOfFirst { it.name.equals(context.identifier, ignoreCase = true) }
                val nextArtist = if (currentIndex >= 0 && currentIndex + 1 < artists.size) {
                    artists[currentIndex + 1]
                } else {
                    artists.firstOrNull()
                } ?: return false

                val nextSongs = allSongs.filter { it.artist.equals(nextArtist.name, ignoreCase = true) }
                if (nextSongs.isNotEmpty()) {
                    val newContext = PlaybackContext(PlaybackContextType.ARTIST, nextArtist.name)
                    playAll(nextSongs, startIndex = 0, context = newContext)
                    return true
                }
            }
            PlaybackContextType.FOLDER -> {
                val folders = musicRepository.allFolders.value
                if (folders.isEmpty()) return false
                val currentIndex = folders.indexOfFirst { it.path == context.identifier }
                val nextFolder = if (currentIndex >= 0 && currentIndex + 1 < folders.size) {
                    folders[currentIndex + 1]
                } else {
                    folders.firstOrNull()
                } ?: return false

                val nextSongs = allSongs.filter { File(it.data).parent == nextFolder.path }
                if (nextSongs.isNotEmpty()) {
                    val newContext = PlaybackContext(PlaybackContextType.FOLDER, nextFolder.path)
                    playAll(nextSongs, startIndex = 0, context = newContext)
                    return true
                }
            }
            else -> return false
        }
        return false
    }

    fun advanceToPreviousCollection(): Boolean {
        val context = currentPlaybackContext ?: return false
        val allSongs = musicRepository.allSongs.value
        if (allSongs.isEmpty()) return false

        when (context.type) {
            PlaybackContextType.ALBUM -> {
                val albums = musicRepository.allAlbums.value
                if (albums.isEmpty()) return false
                val currentIndex = albums.indexOfFirst { it.id.toString() == context.identifier }
                val prevAlbum = if (currentIndex > 0) {
                    albums[currentIndex - 1]
                } else {
                    albums.lastOrNull()
                } ?: return false

                val prevSongs = allSongs.filter { it.albumId == prevAlbum.id }
                if (prevSongs.isNotEmpty()) {
                    val newContext = PlaybackContext(PlaybackContextType.ALBUM, prevAlbum.id.toString())
                    playAll(prevSongs, startIndex = 0, context = newContext)
                    return true
                }
            }
            PlaybackContextType.ARTIST -> {
                val artists = musicRepository.allArtists.value
                if (artists.isEmpty()) return false
                val currentIndex = artists.indexOfFirst { it.name.equals(context.identifier, ignoreCase = true) }
                val prevArtist = if (currentIndex > 0) {
                    artists[currentIndex - 1]
                } else {
                    artists.lastOrNull()
                } ?: return false

                val prevSongs = allSongs.filter { it.artist.equals(prevArtist.name, ignoreCase = true) }
                if (prevSongs.isNotEmpty()) {
                    val newContext = PlaybackContext(PlaybackContextType.ARTIST, prevArtist.name)
                    playAll(prevSongs, startIndex = 0, context = newContext)
                    return true
                }
            }
            PlaybackContextType.FOLDER -> {
                val folders = musicRepository.allFolders.value
                if (folders.isEmpty()) return false
                val currentIndex = folders.indexOfFirst { it.path == context.identifier }
                val prevFolder = if (currentIndex > 0) {
                    folders[currentIndex - 1]
                } else {
                    folders.lastOrNull()
                } ?: return false

                val prevSongs = allSongs.filter { File(it.data).parent == prevFolder.path }
                if (prevSongs.isNotEmpty()) {
                    val newContext = PlaybackContext(PlaybackContextType.FOLDER, prevFolder.path)
                    playAll(prevSongs, startIndex = 0, context = newContext)
                    return true
                }
            }
            else -> return false
        }
        return false
    }

    fun setRepeatMode(repeatMode: RepeatMode) {
        val player = controller ?: return
        val exoMode = when (repeatMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }
        player.repeatMode = exoMode
        scope.launch(Dispatchers.IO) {
            preferencesRepository.setRepeatMode(repeatMode)
        }
        updatePlaybackState()
    }

    fun toggleRepeatMode() {
        val nextMode = when (_playbackState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        setRepeatMode(nextMode)
    }

    fun setShuffleEnabled(enabled: Boolean) {
        val player = controller ?: return
        player.shuffleModeEnabled = enabled
        scope.launch(Dispatchers.IO) {
            preferencesRepository.setShuffleEnabled(enabled)
        }
        updatePlaybackState()
    }

    fun toggleShuffle() {
        setShuffleEnabled(!_playbackState.value.shuffleMode)
    }

    fun setPlaybackSpeed(speed: Float) {
        val player = controller ?: return
        player.setPlaybackSpeed(speed)
        scope.launch(Dispatchers.IO) {
            preferencesRepository.setPlaybackSpeed(speed)
        }
        updatePlaybackState()
    }

    fun playSong(song: Song, queue: List<Song>, context: PlaybackContext? = null) {
        val player = controller ?: return

        if (context != null) {
            currentPlaybackContext = context
            scope.launch(Dispatchers.IO) {
                preferencesRepository.savePlaybackContext(context.type.name, context.identifier)
            }
        }

        currentSongList.clear()
        currentSongList.addAll(queue)

        val mediaItems = queue.map { songToMediaItem(it) }
        val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)

        // Check if this song was the saved last played song
        scope.launch {
            val (lastSongId, lastPos) = preferencesRepository.getLastPlaybackSnapshot()
            val startPos = if (lastSongId == song.id && lastPos > 0) lastPos else 0L

            withContext(Dispatchers.Main) {
                player.setMediaItems(mediaItems, index, startPos)
                player.prepare()
                player.play()
                saveCurrentPosition()
                if (currentAudioSessionId > 0) {
                    equalizerController.reapplyIfEnabled(currentAudioSessionId)
                }
            }
        }
    }

    fun playAll(songs: List<Song>, startIndex: Int = 0, context: PlaybackContext? = null) {
        if (songs.isEmpty()) return
        val startSong = songs.getOrElse(startIndex) { songs.first() }
        playSong(startSong, songs, context)
    }

    fun shuffleAll(songs: List<Song>, context: PlaybackContext? = null) {
        if (songs.isEmpty()) return
        val shuffled = songs.shuffled()
        playSong(shuffled.first(), shuffled, context)
        setShuffleEnabled(true)
    }

    fun playNext(song: Song) {
        val player = controller ?: return
        val currentIndex = player.currentMediaItemIndex
        val insertIndex = (currentIndex + 1).coerceAtMost(currentSongList.size)

        currentSongList.add(insertIndex, song)
        player.addMediaItem(insertIndex, songToMediaItem(song))
        updatePlaybackState()
        saveCurrentPosition()
    }

    fun addToQueue(song: Song) {
        val player = controller ?: return
        currentSongList.add(song)
        player.addMediaItem(songToMediaItem(song))
        updatePlaybackState()
        saveCurrentPosition()
    }

    fun removeQueueItem(index: Int) {
        val player = controller ?: return
        if (index in 0 until currentSongList.size) {
            currentSongList.removeAt(index)
            player.removeMediaItem(index)
            updatePlaybackState()
            saveCurrentPosition()
        }
    }

    fun playQueueItem(index: Int) {
        val player = controller ?: return
        if (index in 0 until currentSongList.size) {
            player.seekTo(index, 0L)
            player.play()
        }
    }

    fun clearQueue() {
        val player = controller ?: return
        currentSongList.clear()
        player.clearMediaItems()
        updatePlaybackState()
        saveCurrentPosition()
    }

    fun toggleShuffleMode() {
        toggleShuffle()
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0, context: PlaybackContext? = null) {
        playAll(songs, startIndex, context)
    }

    fun removeFromQueue(index: Int) {
        removeQueueItem(index)
    }

    fun playQueueIndex(index: Int) {
        playQueueItem(index)
    }

    fun moveQueueItem(from: Int, to: Int) {
        val player = controller ?: return
        if (from in 0 until currentSongList.size && to in 0 until currentSongList.size) {
            val item = currentSongList.removeAt(from)
            currentSongList.add(to, item)
            player.moveMediaItem(from, to)
            updatePlaybackState()
            saveCurrentPosition()
        }
    }

    @OptIn(UnstableApi::class)
    private fun songToMediaItem(song: Song): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(song.artworkUri)
            .setExtras(Bundle().apply {
                putLong("song_id", song.id)
                putLong("duration", song.duration)
            })
            .build()

        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.contentUri)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun mediaItemToSong(mediaItem: MediaItem): Song {
        val id = mediaItem.mediaId.toLongOrNull() ?: 0L
        val metadata = mediaItem.mediaMetadata
        val title = metadata.title?.toString() ?: "Unknown Track"
        val artist = metadata.artist?.toString() ?: "Unknown Artist"
        val album = metadata.albumTitle?.toString() ?: "Unknown Album"
        val duration = metadata.extras?.getLong("duration") ?: 0L

        return Song(
            id = id,
            title = title,
            artist = artist,
            album = album,
            albumId = 0L,
            duration = duration,
            contentUri = mediaItem.requestMetadata.mediaUri ?: Uri.EMPTY,
            data = "",
            dateAdded = 0L,
            size = 0L
        )
    }

    fun release() {
        stopPositionTicker()
        saveCurrentPosition()
        equalizerController.release()
        sleepTimerManager.cancelTimer()
        controller?.removeListener(playerListener)
        controller?.release()
        controller = null
        controllerFuture = null
    }
}
