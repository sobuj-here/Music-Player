package com.musicplayer.app.settings

import android.content.Context
import android.os.Environment
import com.musicplayer.app.data.local.MusicDatabase
import com.musicplayer.app.data.local.entity.PlaylistEntity
import com.musicplayer.app.data.local.entity.PlaylistSongCrossRef
import com.musicplayer.app.model.Playlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object BackupManager {

    suspend fun exportPlaylists(context: Context, database: MusicDatabase): File = withContext(Dispatchers.IO) {
        val playlists = database.playlistDao().getAllPlaylists().first()
        val rootArray = JSONArray()

        for (playlist in playlists) {
            val songIds = database.playlistDao().getSongIdsForPlaylist(playlist.id).first()
            val pObj = JSONObject().apply {
                put("name", playlist.name)
                put("createdAt", playlist.createdAt)
                val songArray = JSONArray()
                songIds.forEach { songId -> songArray.put(songId) }
                put("songIds", songArray)
            }
            rootArray.put(pObj)
        }

        val exportDir = File(context.getExternalFilesDir(null), "backups").apply { mkdirs() }
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val file = File(exportDir, "playlists_backup_$timeStamp.json")
        file.writeText(rootArray.toString(2))
        file
    }

    suspend fun importPlaylists(database: MusicDatabase, jsonContent: String): Int = withContext(Dispatchers.IO) {
        val rootArray = JSONArray(jsonContent)
        var importedCount = 0

        for (i in 0 until rootArray.length()) {
            val pObj = rootArray.getJSONObject(i)
            val name = pObj.getString("name")
            val createdAt = pObj.optLong("createdAt", System.currentTimeMillis())

            val playlistId = database.playlistDao().insertPlaylist(
                PlaylistEntity(name = name, createdAt = createdAt)
            )

            val songArray = pObj.optJSONArray("songIds")
            if (songArray != null) {
                for (j in 0 until songArray.length()) {
                    val songId = songArray.getLong(j)
                    database.playlistDao().insertSongToPlaylist(
                        PlaylistSongCrossRef(playlistId = playlistId, songId = songId)
                    )
                }
            }
            importedCount++
        }
        importedCount
    }
}
