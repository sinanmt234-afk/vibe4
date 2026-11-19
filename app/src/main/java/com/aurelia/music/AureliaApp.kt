package com.aurelia.music

import android.app.Application
import com.aurelia.music.data.prefs.SettingsRepository
import com.aurelia.music.data.ytmusic.InnerTubeClient
import com.aurelia.music.player.PlayerController

class AureliaApp : Application() {
    lateinit var settingsRepository: SettingsRepository
        private set
    lateinit var innerTubeClient: InnerTubeClient
        private set
    lateinit var playerController: PlayerController
        private set

    override fun onCreate() {
        super.onCreate()
        settingsRepository = SettingsRepository(this)
        innerTubeClient = InnerTubeClient()
        playerController = PlayerController(this, settingsRepository, innerTubeClient)
    }
}
