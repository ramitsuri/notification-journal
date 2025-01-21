package com.ramitsuri.notificationjournal.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.RemoteInput
import com.ramitsuri.notificationjournal.MainApplication
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.work.UploadWorker
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val action = intent.action ?: return

        if (action == Constants.ACTION_UPLOAD) {
            upload(context)
            return
        }

        if (action == Constants.ACTION_JOURNAL) {
            addEntry(context, intent)
            return
        }
    }

    private fun addEntry(
        context: Context,
        intent: Intent,
    ) {
        val remoteInputBundle = RemoteInput.getResultsFromIntent(intent)
        if (remoteInputBundle == null) {
            Log.d(TAG, "RemoteInputBundle is null")
            return
        }

        val text = remoteInputBundle.getCharSequence(Constants.REMOTE_INPUT_JOURNAL_KEY)?.toString()
        if (text == null) {
            Log.d(TAG, "Text in RemoteInputBundle is null")
            return
        }

        val pendingResult = goAsync()
        val repository = ServiceLocator.repository
        ServiceLocator.coroutineScope.launch {
            repository.insert(text = text)
            pendingResult.finish()
        }
        (context.applicationContext as MainApplication).showJournalNotification()
    }

    private fun upload(context: Context) {
        UploadWorker.enqueueImmediate(context)
    }

    companion object {
        private const val TAG = "NotificationActionReceiver"
    }
}
