package com.ramitsuri.notificationjournal.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.app.RemoteInput
import com.ramitsuri.notificationjournal.MainApplication
import com.ramitsuri.notificationjournal.data.AppDatabase
import com.ramitsuri.notificationjournal.data.JournalEntry
import com.ramitsuri.notificationjournal.utils.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId

class NotificationContentReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val remoteInputBundle = RemoteInput.getResultsFromIntent(intent)

        if (remoteInputBundle != null) {
            processRemoteInput(context, remoteInputBundle)
            (context.applicationContext as MainApplication).showJournalNotification()
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
        val database = AppDatabase.getInstance(context = context)
        CoroutineScope(SupervisorJob() ).launch {
            withContext(Dispatchers.IO) {
                database.journalEntryDao().insert(journalEntry)
            }
        }
    }
}