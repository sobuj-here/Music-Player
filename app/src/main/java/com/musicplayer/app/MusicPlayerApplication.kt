package com.musicplayer.app

import android.app.Application
import com.musicplayer.app.data.local.MusicDatabase
import com.musicplayer.app.data.mediastore.MediaStoreScanner
import com.musicplayer.app.data.repository.MusicRepository
import com.musicplayer.app.data.repository.PreferencesRepository
import com.musicplayer.app.player.PlayerConnection

class MusicPlayerApplication : Application() {

    lateinit var database: MusicDatabase
        private set

    lateinit var scanner: MediaStoreScanner
        private set

    lateinit var musicRepository: MusicRepository
        private set

    lateinit var preferencesRepository: PreferencesRepository
        private set

    lateinit var playerConnection: PlayerConnection
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = MusicDatabase.getInstance(this)
        scanner = MediaStoreScanner(this)
        preferencesRepository = PreferencesRepository(this)
        musicRepository = MusicRepository(scanner, database, preferencesRepository)
        playerConnection = PlayerConnection(this, musicRepository, preferencesRepository)
    }

    companion object {
        lateinit var instance: MusicPlayerApplication
            private set
    }
}
