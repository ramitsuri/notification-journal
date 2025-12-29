package com.ramitsuri.notificationjournal

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.material.color.DynamicColors
import com.ramitsuri.notificationjournal.broadcast.NotificationActionReceiver
import com.ramitsuri.notificationjournal.core.deeplink.homeDeepLink
import com.ramitsuri.notificationjournal.core.di.DiFactory
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.JournalEntryNotificationHelper
import com.ramitsuri.notificationjournal.core.utils.NotificationActionInfo
import com.ramitsuri.notificationjournal.core.utils.NotificationChannelType
import com.ramitsuri.notificationjournal.core.utils.NotificationInfo
import com.ramitsuri.notificationjournal.work.ShowNotificationWorker
import kotlin.time.Duration

class MainApplication : Application(), DefaultLifecycleObserver {
    override fun onCreate() {
        super<Application>.onCreate()

        DynamicColors.applyToActivitiesIfAvailable(this)
        val factory = DiFactory(this)
        ServiceLocator.init(
            factory,
            object : JournalEntryNotificationHelper {
                override fun show(
                    journalEntry: JournalEntry,
                    showIn: Duration,
                ) {
                    ShowNotificationWorker.enqueue(this@MainApplication, journalEntry.id, showIn)
                }
            },
        )
        showJournalNotification()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        ServiceLocator.onAppStart()
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        ServiceLocator.onAppStop()
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
        ServiceLocator.notificationHandler.showNotification(notificationInfo)
    }
}
