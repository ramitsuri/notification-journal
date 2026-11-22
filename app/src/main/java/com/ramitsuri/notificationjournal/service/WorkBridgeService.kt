package com.ramitsuri.notificationjournal.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.R
import com.ramitsuri.notificationjournal.core.di.AppScope
import com.ramitsuri.notificationjournal.core.di.IoDispatcher
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.KeyValueStoreV2
import com.ramitsuri.notificationjournal.core.utils.WorkProfileManager
import com.ramitsuri.notificationjournal.core.work.WorkBridgeController
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import dev.zacsweers.metrox.android.ServiceKey
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Inject
@ContributesIntoMap(AppScope::class, binding = binding<Service>())
@ServiceKey
class WorkBridgeService(
    private val repository: JournalRepository,
    private val workProfileManager: WorkProfileManager,
    private val keyValueStore: KeyValueStoreV2,
    private val json: Json,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : LifecycleService() {
    private var controller: WorkBridgeController? = null
    private var bridgeJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForegroundImmediately()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_STOP) {
            Logger.withTag(TAG).i { "Received ACTION_STOP, terminating service" }
            stopServiceInternal()
            return START_NOT_STICKY
        }

        startForegroundImmediately()

        if (controller == null) {
            val newController =
                WorkBridgeController(
                    repository = repository,
                    workProfileManager = workProfileManager,
                    keyValueStore = keyValueStore,
                    json = json,
                    ioDispatcher = ioDispatcher,
                    onStopRequested = {
                        stopServiceInternal()
                    },
                )
            controller = newController

            lifecycleScope.launch(ioDispatcher) {
                newController.state.collect { state ->
                    when (state) {
                        is WorkBridgeController.State.Connecting -> {
                            updateNotification(
                                getString(
                                    R.string.work_bridge_connecting,
                                    state.attempt,
                                    state.maxAttempts,
                                ),
                            )
                        }

                        is WorkBridgeController.State.Connected -> {
                            updateNotification(getString(R.string.work_bridge_connected))
                        }

                        is WorkBridgeController.State.Stopped -> {
                            stopServiceInternal()
                        }

                        is WorkBridgeController.State.Idle -> {
                            // Keep initial notification
                        }
                    }
                }
            }

            bridgeJob =
                lifecycleScope.launch {
                    newController.start()
                }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    override fun onDestroy() {
        stopServiceInternal()
        super.onDestroy()
    }

    private fun startForegroundImmediately() {
        val notification = buildNotification(getString(R.string.work_bridge_connecting, 1, 3))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.work_bridge_notification_title),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = "Silent foreground notification for work capture sync"
                setShowBadge(false)
                setSound(null, null)
                enableVibration(false)
            }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }

    private fun buildNotification(contentText: String): Notification {
        val stopIntent =
            Intent(this, WorkBridgeService::class.java).apply {
                action = ACTION_STOP
            }
        val stopPendingIntent =
            PendingIntent.getService(
                this,
                REQUEST_CODE_STOP,
                stopIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )

        return NotificationCompat
            .Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.work_bridge_notification_title))
            .setContentText(contentText)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                0,
                getString(R.string.work_bridge_action_stop),
                stopPendingIntent,
            ).build()
    }

    private fun updateNotification(contentText: String) {
        val notification = buildNotification(contentText)
        val notificationManager = NotificationManagerCompat.from(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun stopServiceInternal() {
        bridgeJob?.cancel()
        bridgeJob = null
        controller = null
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        const val CHANNEL_ID = "work_bridge"
        const val ACTION_START = "com.ramitsuri.notificationjournal.action.WORK_BRIDGE_START"
        const val ACTION_STOP = "com.ramitsuri.notificationjournal.action.WORK_BRIDGE_STOP"
        const val NOTIFICATION_ID = 2001
        const val REQUEST_CODE_STOP = 2002
        private const val TAG = "WorkBridgeService"
    }
}
