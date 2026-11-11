package com.musicplayer.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.musicplayer.app.model.RepeatMode
import com.musicplayer.app.model.SortOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "music_player_preferences")

class PreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val REPEAT_MODE = stringPreferencesKey("repeat_mode")
        val SHUFFLE_ENABLED = booleanPreferencesKey("shuffle_enabled")
        val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
        val SORT_ORDER = stringPreferencesKey("sort_order")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_COLOR_INDEX = stringPreferencesKey("accent_color_index")

        // Playback state persistence
        val LAST_PLAYED_SONG_ID = longPreferencesKey("last_played_song_id")
        val LAST_PLAYED_POSITION = longPreferencesKey("last_played_position")
        val SAVED_QUEUE_IDS = stringPreferencesKey("saved_queue_ids")
        val SAVED_QUEUE_INDEX = intPreferencesKey("saved_queue_index")

        // Playback options
        val RESUME_PLAYBACK = booleanPreferencesKey("resume_playback")
        val GAPLESS_PLAYBACK = booleanPreferencesKey("gapless_playback")
        val CROSSFADE_ENABLED = booleanPreferencesKey("crossfade_enabled")
        val CROSSFADE_DURATION = intPreferencesKey("crossfade_duration")
        val PAUSE_ON_DISCONNECT = booleanPreferencesKey("pause_on_disconnect")
        val REMEMBER_POSITION = booleanPreferencesKey("remember_position")

        // Library options
        val HIDE_SHORT_AUDIO = booleanPreferencesKey("hide_short_audio")
        val NOTIFICATION_ARTWORK = booleanPreferencesKey("notification_artwork")
        val EXCLUDED_FOLDERS = stringSetPreferencesKey("excluded_folders")
    }

    val repeatModeFlow: Flow<RepeatMode> = context.dataStore.data.map { preferences ->
        val name = preferences[PreferencesKeys.REPEAT_MODE] ?: RepeatMode.OFF.name
        try {
            RepeatMode.valueOf(name)
        } catch (e: Exception) {
            RepeatMode.OFF
        }
    }

    val shuffleEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SHUFFLE_ENABLED] ?: false
    }

    val playbackSpeedFlow: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PLAYBACK_SPEED] ?: 1.0f
    }

    val sortOrderFlow: Flow<SortOrder> = context.dataStore.data.map { preferences ->
        val name = preferences[PreferencesKeys.SORT_ORDER] ?: SortOrder.DATE_ADDED_DESC.name
        try {
            SortOrder.valueOf(name)
        } catch (e: Exception) {
            SortOrder.DATE_ADDED_DESC
        }
    }

    val themeModeFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.THEME_MODE] ?: "SYSTEM"
    }

    val accentColorFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.ACCENT_COLOR_INDEX]?.toIntOrNull() ?: 0
    }

    val lastPlayedSongIdFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_PLAYED_SONG_ID] ?: -1L
    }

    val lastPlayedPositionFlow: Flow<Long> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.LAST_PLAYED_POSITION] ?: 0L
    }

    val savedQueueIdsFlow: Flow<List<Long>> = context.dataStore.data.map { preferences ->
        val raw = preferences[PreferencesKeys.SAVED_QUEUE_IDS] ?: ""
        if (raw.isBlank()) emptyList()
        else raw.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    val savedQueueIndexFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.SAVED_QUEUE_INDEX] ?: 0
    }

    val resumePlaybackFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.RESUME_PLAYBACK] ?: true
    }

    val gaplessPlaybackFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.GAPLESS_PLAYBACK] ?: true
    }

    val crossfadeEnabledFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CROSSFADE_ENABLED] ?: false
    }

    val crossfadeDurationFlow: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CROSSFADE_DURATION] ?: 3
    }

    val pauseOnDisconnectFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.PAUSE_ON_DISCONNECT] ?: true
    }

    val rememberPositionFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.REMEMBER_POSITION] ?: true
    }

    val hideShortAudioFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.HIDE_SHORT_AUDIO] ?: true
    }

    val notificationArtworkFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.NOTIFICATION_ARTWORK] ?: true
    }

    val excludedFoldersFlow: Flow<Set<String>> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.EXCLUDED_FOLDERS] ?: emptySet()
    }

    suspend fun setRepeatMode(repeatMode: RepeatMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REPEAT_MODE] = repeatMode.name
        }
    }

    suspend fun setShuffleEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SHUFFLE_ENABLED] = enabled
        }
    }

    suspend fun setPlaybackSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PLAYBACK_SPEED] = speed
        }
    }

    suspend fun setSortOrder(sortOrder: SortOrder) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SORT_ORDER] = sortOrder.name
        }
    }

    suspend fun setThemeMode(themeMode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = themeMode
        }
    }

    suspend fun setAccentColorIndex(index: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACCENT_COLOR_INDEX] = index.toString()
        }
    }

    suspend fun savePlaybackState(
        songId: Long,
        position: Long,
        queueIds: List<Long>,
        queueIndex: Int
    ) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_PLAYED_SONG_ID] = songId
            preferences[PreferencesKeys.LAST_PLAYED_POSITION] = position
            preferences[PreferencesKeys.SAVED_QUEUE_IDS] = queueIds.joinToString(",")
            preferences[PreferencesKeys.SAVED_QUEUE_INDEX] = queueIndex
        }
    }

    suspend fun saveLastPosition(position: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_PLAYED_POSITION] = position
        }
    }

    suspend fun setResumePlayback(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RESUME_PLAYBACK] = enabled
        }
    }

    suspend fun setGaplessPlayback(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.GAPLESS_PLAYBACK] = enabled
        }
    }

    suspend fun setCrossfadeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CROSSFADE_ENABLED] = enabled
        }
    }

    suspend fun setCrossfadeDuration(seconds: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CROSSFADE_DURATION] = seconds
        }
    }

    suspend fun setPauseOnDisconnect(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.PAUSE_ON_DISCONNECT] = enabled
        }
    }

    suspend fun setRememberPosition(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.REMEMBER_POSITION] = enabled
        }
    }

    suspend fun setHideShortAudio(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.HIDE_SHORT_AUDIO] = enabled
        }
    }

    suspend fun setNotificationArtwork(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.NOTIFICATION_ARTWORK] = enabled
        }
    }

    suspend fun setExcludedFolders(folders: Set<String>) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.EXCLUDED_FOLDERS] = folders
        }
    }

    // Direct synchronous snapshot read for quick startup initialization
    suspend fun getLastPlaybackSnapshot(): Pair<Long, Long> {
        val prefs = context.dataStore.data.first()
        val songId = prefs[PreferencesKeys.LAST_PLAYED_SONG_ID] ?: -1L
        val pos = prefs[PreferencesKeys.LAST_PLAYED_POSITION] ?: 0L
        return Pair(songId, pos)
    }
}
