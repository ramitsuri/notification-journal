package com.ramitsuri.notificationjournal.di

import android.app.Application
import com.ramitsuri.notificationjournal.core.di.AppScope
import com.ramitsuri.notificationjournal.core.di.CoreGraph
import com.ramitsuri.notificationjournal.presentation.MainViewModel
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.android.MetroAppComponentProviders

@SingleIn(AppScope::class)
@DependencyGraph(AppScope::class)
interface WearGraph :
    CoreGraph,
    MetroAppComponentProviders {
    val mainViewModel: MainViewModel

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides application: Application,
        ): WearGraph
    }
}
