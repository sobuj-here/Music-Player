package com.musicplayer.app.ui.screens.tabs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musicplayer.app.model.Song
import com.musicplayer.app.ui.components.EmptyStateView
import com.musicplayer.app.ui.components.SongItem

@Composable
fun RecentlyAddedTab(
    songs: List<Song>,
    currentSongId: Long?,
    isPlaying: Boolean,
    onSongClick: (Song) -> Unit,
    onFavoriteToggle: (Song) -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onShowDetails: (Song) -> Unit,
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) {
        EmptyStateView(
            title = "No Recent Audio",
            subtitle = "Songs added to your device will appear here sorted by newest date.",
            onRefresh = null,
            modifier = modifier
        )
    } else {
        Column(modifier = modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
            ) {
                items(
                    items = songs,
                    key = { it.id }
                ) { song ->
                    SongItem(
                        song = song,
                        isPlaying = isPlaying,
                        isCurrentTrack = (song.id == currentSongId),
                        onClick = { onSongClick(song) },
                        onFavoriteToggle = { onFavoriteToggle(song) },
                        onPlayNext = { onPlayNext(song) },
                        onAddToQueue = { onAddToQueue(song) },
                        onAddToPlaylist = { onAddToPlaylist(song) },
                        onShowDetails = { onShowDetails(song) }
                    )
                }
            }
        }
    }
}
