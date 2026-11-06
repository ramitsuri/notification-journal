package com.ramitsuri.notificationjournal

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.google.android.material.color.DynamicColors
import com.ramitsuri.notificationjournal.broadcast.NotificationActionReceiver
import com.ramitsuri.notificationjournal.core.deeplink.homeDeepLink
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.NotificationActionInfo
import com.ramitsuri.notificationjournal.core.utils.NotificationChannelType
import com.ramitsuri.notificationjournal.core.utils.NotificationInfo
import com.ramitsuri.notificationjournal.di.AppGraph
import dev.zacsweers.metro.createGraphFactory
import dev.zacsweers.metrox.android.MetroAppComponentProviders
import dev.zacsweers.metrox.android.MetroApplication

class MainApplication :
    Application(),
    DefaultLifecycleObserver,
    Configuration.Provider,
    MetroApplication {
    lateinit var appGraph: AppGraph
        private set

    override val appComponentProviders: MetroAppComponentProviders
        get() = appGraph

    override val workManagerConfiguration: Configuration
        get() =
            Configuration
                .Builder()
                .setWorkerFactory(appGraph.workerFactory)
                .build()

    override fun onCreate() {
        super<Application>.onCreate()

        DynamicColors.applyToActivitiesIfAvailable(this)
        appGraph = createGraphFactory<AppGraph.Factory>().create(this)
        appGraph.initLogger()
        showJournalNotification()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        appGraph.lifecycleCoordinator.onAppStart()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        appGraph.lifecycleCoordinator.onAppStop()
    }

    fun showJournalNotification() {
        val notificationInfo =
            NotificationInfo(
                channel = NotificationChannelType.MAIN,
                title = "",
                body = "",
                iconResId = R.drawable.ic_notification,
                isVisibilityPublic = true,
                cancelOnTouch = false,
                isForegroundServiceImmediate = false,
                isOngoing = true,
                clickDeepLinkUri = homeDeepLink,
                actions =
                    listOf(
                        NotificationActionInfo(
                            action = Constants.ACTION_JOURNAL,
                            text = getString(R.string.add_new_journal_content),
                            intentReceiverClass = NotificationActionReceiver::class.java,
                            remoteInputKey = Constants.REMOTE_INPUT_JOURNAL_KEY,
                        ),
                    ),
                actionExtras = mapOf(),
            )
        appGraph.notificationHandler.showNotification(notificationInfo)
    }
}
