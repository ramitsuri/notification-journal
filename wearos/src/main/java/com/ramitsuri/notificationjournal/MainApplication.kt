package com.ramitsuri.notificationjournal

import android.app.Application
import com.ramitsuri.notificationjournal.di.WearGraph
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.android.MetroAppComponentProviders
import dev.zacsweers.metrox.android.MetroApplication

class MainApplication :
    Application(),
    MetroApplication {
    lateinit var wearGraph: WearGraph
        private set

    override val appComponentProviders: MetroAppComponentProviders
        get() = wearGraph

    override fun onCreate() {
        super.onCreate()
        wearGraph = createGraphFactory<WearGraph.Factory>().create(this)
    }
}
