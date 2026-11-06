package com.ramitsuri.notificationjournal.di

import android.app.Application
import com.ramitsuri.notificationjournal.ai.JournalAssistantViewModel
import com.ramitsuri.notificationjournal.ai.diagnostics.AiDiagnosticsViewModel
import com.ramitsuri.notificationjournal.ai.ui.VoiceNoteReviewViewModel
import com.ramitsuri.notificationjournal.core.di.AppScope
import com.ramitsuri.notificationjournal.core.di.CoreGraph
import com.ramitsuri.notificationjournal.work.AppWorkerFactory
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metrox.android.MetroAppComponentProviders

@SingleIn(AppScope::class)
@DependencyGraph(AppScope::class)
interface AppGraph :
    CoreGraph,
    MetroAppComponentProviders {
    val workerFactory: AppWorkerFactory
    val journalAssistantViewModel: JournalAssistantViewModel
    val voiceNoteReviewViewModel: VoiceNoteReviewViewModel
    val aiDiagnosticsViewModel: AiDiagnosticsViewModel

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(
            @Provides application: Application,
        ): AppGraph
    }
}
