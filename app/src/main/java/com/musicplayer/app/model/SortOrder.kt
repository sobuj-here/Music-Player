package com.musicplayer.app.model

enum class SortOrder(val displayName: String) {
    TITLE_ASC("Title (A to Z)"),
    TITLE_DESC("Title (Z to A)"),
    ARTIST_ASC("Artist (A to Z)"),
    ARTIST_DESC("Artist (Z to A)"),
    ALBUM_ASC("Album (A to Z)"),
    DATE_ADDED_DESC("Date Added (Newest first)"),
    DATE_ADDED_ASC("Date Added (Oldest first)"),
    DURATION_DESC("Duration (Longest first)")
}
