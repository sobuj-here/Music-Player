package com.musicplayer.app.data.mediastore

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.musicplayer.app.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreScanner(private val context: Context) {

    suspend fun scanSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songsList = mutableListOf<Song>()

        val collection: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 5000"
        val sortOrder = "${MediaStore.Audio.Media.DATE_ADDED} DESC"

        try {
            val cursor: Cursor? = context.contentResolver.query(
                collection,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndex(MediaStore.Audio.Media._ID)
                val titleCol = it.getColumnIndex(MediaStore.Audio.Media.TITLE)
                val artistCol = it.getColumnIndex(MediaStore.Audio.Media.ARTIST)
                val albumCol = it.getColumnIndex(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = it.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = it.getColumnIndex(MediaStore.Audio.Media.DURATION)
                val dataCol = it.getColumnIndex(MediaStore.Audio.Media.DATA)
                val dateAddedCol = it.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED)
                val sizeCol = it.getColumnIndex(MediaStore.Audio.Media.SIZE)
                val trackCol = it.getColumnIndex(MediaStore.Audio.Media.TRACK)
                val yearCol = it.getColumnIndex(MediaStore.Audio.Media.YEAR)

                while (it.moveToNext()) {
                    if (idCol < 0) continue
                    val id = it.getLong(idCol)
                    val rawTitle = if (titleCol >= 0) it.getString(titleCol) else null
                    val rawArtist = if (artistCol >= 0) it.getString(artistCol) else null
                    val rawAlbum = if (albumCol >= 0) it.getString(albumCol) else null
                    val albumId = if (albumIdCol >= 0) it.getLong(albumIdCol) else 0L
                    val duration = if (durationCol >= 0) it.getLong(durationCol) else 0L
                    val filePath = if (dataCol >= 0) (it.getString(dataCol) ?: "") else ""
                    val dateAdded = if (dateAddedCol >= 0) it.getLong(dateAddedCol) else 0L
                    val size = if (sizeCol >= 0) it.getLong(sizeCol) else 0L
                    val track = if (trackCol >= 0) it.getInt(trackCol) else 0
                    val year = if (yearCol >= 0) it.getInt(yearCol) else 0

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val cleanTitle = when {
                        !rawTitle.isNullOrBlank() -> rawTitle.trim()
                        filePath.isNotBlank() -> File(filePath).nameWithoutExtension
                        else -> "Unknown Track"
                    }

                    val cleanArtist = if (!rawArtist.isNullOrBlank() && rawArtist != "<unknown>") {
                        rawArtist.trim()
                    } else {
                        "Unknown Artist"
                    }

                    val cleanAlbum = if (!rawAlbum.isNullOrBlank()) {
                        rawAlbum.trim()
                    } else {
                        "Unknown Album"
                    }

                    songsList.add(
                        Song(
                            id = id,
                            title = cleanTitle,
                            artist = cleanArtist,
                            album = cleanAlbum,
                            albumId = albumId,
                            duration = duration,
                            contentUri = contentUri,
                            data = filePath,
                            dateAdded = dateAdded,
                            size = size,
                            trackNumber = track,
                            year = year
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        songsList
    }
}
