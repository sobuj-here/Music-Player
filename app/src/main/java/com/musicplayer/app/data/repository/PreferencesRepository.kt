package com.musicplayer.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.musicplayer.app.model.RepeatMode
import com.musicplayer.app.model.SortOrder
import kotlinx.coroutines.flow.Flow
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
        val LAST_PLAYED_SONG_ID = longPreferencesKey("last_played_song_id")
        val LAST_PLAYED_POSITION = longPreferencesKey("last_played_position")
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
        val name = preferences[PreferencesKeys.SORT_ORDER] ?: SortOrder.TITLE_ASC.name
        try {
            SortOrder.valueOf(name)
        } catch (e: Exception) {
            SortOrder.TITLE_ASC
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

    suspend fun saveLastPlayed(songId: Long, position: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.LAST_PLAYED_SONG_ID] = songId
            preferences[PreferencesKeys.LAST_PLAYED_POSITION] = position
        }
    }
}
