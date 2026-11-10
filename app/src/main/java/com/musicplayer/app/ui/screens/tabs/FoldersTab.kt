package com.musicplayer.app.ui.screens.tabs

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.musicplayer.app.model.Folder
import com.musicplayer.app.ui.components.EmptyStateView
import com.musicplayer.app.ui.components.FolderItem

@Composable
fun FoldersTab(
    folders: List<Folder>,
    onFolderClick: (Folder) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (folders.isEmpty()) {
        EmptyStateView(
            title = "No Music Folders",
            subtitle = "No directories containing audio files were discovered.",
            onRefresh = onRefresh,
            modifier = modifier
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 8.dp, bottom = 80.dp)
        ) {
            items(
                items = folders,
                key = { it.path }
            ) { folder ->
                FolderItem(
                    folder = folder,
                    onClick = { onFolderClick(folder) }
                )
            }
        }
    }
}
