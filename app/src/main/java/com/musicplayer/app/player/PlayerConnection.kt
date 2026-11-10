package com.musicplayer.app.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.annotation.OptIn
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
import com.musicplayer.app.model.PlaybackState
import com.musicplayer.app.model.RepeatMode
import com.musicplayer.app.model.Song
import com.musicplayer.app.service.MusicService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    val equalizerController = EqualizerController()
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
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackState()
            if (isPlaying) {
                startPositionTicker()
                val currentSong = _playbackState.value.currentSong
                if (currentSong != null) {
                    scope.launch(Dispatchers.IO) {
                        musicRepository.recordPlayback(currentSong.id)
                    }
                }
            } else {
                stopPositionTicker()
                saveCurrentPosition()
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updatePlaybackState()
            saveCurrentPosition()
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
                    updatePlaybackState()

                    // Attach audio session to equalizer (0 attaches to default mix session)
                    equalizerController.attachAudioSession(0)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            MoreExecutors.directExecutor()
        )
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
            duration = player.duration.coerceAtLeast(0L),
            bufferedPosition = player.bufferedPosition.coerceAtLeast(0L),
            shuffleMode = player.shuffleModeEnabled,
            repeatMode = repeat,
            playbackSpeed = player.playbackParameters.speed,
            queue = currentSongList.toList(),
            currentQueueIndex = queueIndex,
            isServiceConnected = true,
            audioSessionId = 0
        )
    }

    private fun startPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = scope.launch {
            while (isActive) {
                val player = controller
                if (player != null && player.isPlaying) {
                    _playbackState.value = _playbackState.value.copy(
                        currentPosition = player.currentPosition.coerceAtLeast(0L),
                        bufferedPosition = player.bufferedPosition.coerceAtLeast(0L)
                    )
                }
                delay(200L)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    private fun saveCurrentPosition() {
        val player = controller ?: return
        val currentSong = _playbackState.value.currentSong ?: return
        val pos = player.currentPosition
        scope.launch(Dispatchers.IO) {
            preferencesRepository.saveLastPlayed(currentSong.id, pos)
        }
    }

    fun play() {
        controller?.play()
    }

    fun pause() {
        controller?.pause()
    }

    fun togglePlayPause() {
        val player = controller ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            player.play()
        }
    }

    fun seekTo(positionMs: Long) {
        val player = controller ?: return
        player.seekTo(positionMs)
        _playbackState.value = _playbackState.value.copy(currentPosition = positionMs)
    }

    fun skipNext() {
        val player = controller ?: return
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        }
    }

    fun skipPrevious() {
        val player = controller ?: return
        if (player.currentPosition > 3000 || !player.hasPreviousMediaItem()) {
            player.seekTo(0)
        } else {
            player.seekToPreviousMediaItem()
        }
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
        val next = when (_playbackState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }
        setRepeatMode(next)
    }

    fun setShuffleMode(enabled: Boolean) {
        val player = controller ?: return
        player.shuffleModeEnabled = enabled
        scope.launch(Dispatchers.IO) {
            preferencesRepository.setShuffleEnabled(enabled)
        }
        updatePlaybackState()
    }

    fun toggleShuffleMode() {
        setShuffleMode(!_playbackState.value.shuffleMode)
    }

    fun setPlaybackSpeed(speed: Float) {
        val player = controller ?: return
        player.playbackParameters = PlaybackParameters(speed)
        scope.launch(Dispatchers.IO) {
            preferencesRepository.setPlaybackSpeed(speed)
        }
        updatePlaybackState()
    }

    fun playSongs(songs: List<Song>, startIndex: Int = 0, startPositionMs: Long = 0L) {
        val player = controller ?: return
        if (songs.isEmpty()) return

        currentSongList.clear()
        currentSongList.addAll(songs)

        val mediaItems = songs.map { it.toMediaItem() }
        val safeIndex = startIndex.coerceIn(0, songs.lastIndex)

        player.setMediaItems(mediaItems, safeIndex, startPositionMs)
        player.prepare()
        player.play()
        updatePlaybackState()
    }

    fun playNext(song: Song) {
        val player = controller ?: return
        val currentIdx = player.currentMediaItemIndex
        val targetIdx = (currentIdx + 1).coerceAtMost(currentSongList.size)

        currentSongList.add(targetIdx, song)
        player.addMediaItem(targetIdx, song.toMediaItem())
        updatePlaybackState()
    }

    fun addToQueue(song: Song) {
        val player = controller ?: return
        currentSongList.add(song)
        player.addMediaItem(song.toMediaItem())
        updatePlaybackState()
    }

    fun removeFromQueue(index: Int) {
        val player = controller ?: return
        if (index in 0 until currentSongList.size) {
            currentSongList.removeAt(index)
            player.removeMediaItem(index)
            updatePlaybackState()
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val player = controller ?: return
        if (fromIndex in 0 until currentSongList.size && toIndex in 0 until currentSongList.size) {
            val item = currentSongList.removeAt(fromIndex)
            currentSongList.add(toIndex, item)
            player.moveMediaItem(fromIndex, toIndex)
            updatePlaybackState()
        }
    }

    fun playQueueIndex(index: Int) {
        val player = controller ?: return
        if (index in 0 until currentSongList.size) {
            player.seekTo(index, 0L)
            player.play()
            updatePlaybackState()
        }
    }

    fun clearQueue() {
        val player = controller ?: return
        currentSongList.clear()
        player.clearMediaItems()
        updatePlaybackState()
    }

    @OptIn(UnstableApi::class)
    private fun Song.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setArtworkUri(artworkUri)
            .build()

        return MediaItem.Builder()
            .setMediaId(id.toString())
            .setUri(contentUri)
            .setMediaMetadata(metadata)
            .build()
    }

    private fun mediaItemToSong(mediaItem: MediaItem): Song {
        val metadata = mediaItem.mediaMetadata
        val id = mediaItem.mediaId.toLongOrNull() ?: 0L
        return Song(
            id = id,
            title = metadata.title?.toString() ?: "Unknown Track",
            artist = metadata.artist?.toString() ?: "Unknown Artist",
            album = metadata.albumTitle?.toString() ?: "Unknown Album",
            albumId = 0L,
            duration = 0L,
            contentUri = mediaItem.requestMetadata.mediaUri ?: Uri.EMPTY,
            data = "",
            dateAdded = 0L,
            size = 0L
        )
    }

    fun release() {
        stopPositionTicker()
        controller?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        controller = null
        equalizerController.release()
    }
}
