package com.ramitsuri.notificationjournal.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.app.RemoteInput
import com.ramitsuri.notificationjournal.MainApplication
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.model.JournalEntry
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.Constants.ACTION_JOURNAL
import com.ramitsuri.notificationjournal.core.utils.Constants.ACTION_UPLOAD
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val action = intent.action ?: return
        when (action) {
            ACTION_JOURNAL -> {
                val remoteInputBundle = RemoteInput.getResultsFromIntent(intent)
                if (remoteInputBundle != null) {
                    processRemoteInput(context, remoteInputBundle)
                    (context.applicationContext as MainApplication).showJournalNotification()
                }
            }

            ACTION_UPLOAD -> {
                upload(context)
            }
        }

    }

    private fun processRemoteInput(context: Context, remoteInput: Bundle) {
        val text = remoteInput.getCharSequence(Constants.REMOTE_INPUT_JOURNAL_KEY).toString()
        val journalEntry = JournalEntry(
            id = 0,
            entryTime = Instant.now(),
            timeZone = ZoneId.systemDefault(),
            text = text
        )
        val dao = AppDatabase.getDao(context = context)
        CoroutineScope(SupervisorJob()).launch {
            withContext(Dispatchers.IO) {
                dao.insert(journalEntry)
            }
        }
    }

    private fun upload(context: Context) {
        CoroutineScope(SupervisorJob()).launch {
            val error =
                (context.applicationContext as? MainApplication)?.getRepository()?.upload()
                    ?: return@launch

            Log.d(TAG, "Failed to upload: $error")
        }
    }

    companion object {
        private const val TAG = "NotificationActionReceiver"
    }
}