package com.musicplayer.app.ui.screens.tabs

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musicplayer.app.model.Artist
import com.musicplayer.app.ui.components.ArtistCard
import com.musicplayer.app.ui.components.EmptyStateView

@Composable
fun ArtistsTab(
    artists: List<Artist>,
    onArtistClick: (Artist) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (artists.isEmpty()) {
        EmptyStateView(
            title = "No Artists Found",
            subtitle = "Your audio library does not contain any detected artists.",
            onRefresh = onRefresh,
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {
            items(
                items = artists,
                key = { it.name }
            ) { artist ->
                ArtistCard(
                    artist = artist,
                    onClick = { onArtistClick(artist) }
                )
            }
        }
    }
}
