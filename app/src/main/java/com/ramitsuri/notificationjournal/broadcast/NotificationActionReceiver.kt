package com.ramitsuri.notificationjournal.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.core.app.RemoteInput
import com.ramitsuri.notificationjournal.MainApplication
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.Constants.ACTION_JOURNAL
import com.ramitsuri.notificationjournal.core.utils.Constants.ACTION_UPLOAD
import com.ramitsuri.notificationjournal.di.ServiceLocator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val action = intent.action ?: return
        when (action) {
            ACTION_JOURNAL -> {
                val remoteInputBundle = RemoteInput.getResultsFromIntent(intent)
                if (remoteInputBundle != null) {
                    processRemoteInput(remoteInputBundle)
                    (context.applicationContext as MainApplication).showJournalNotification()
                }
            }

            ACTION_UPLOAD -> {
                upload()
            }
        }

    }

    private fun processRemoteInput(remoteInput: Bundle) {
        val text = remoteInput.getCharSequence(Constants.REMOTE_INPUT_JOURNAL_KEY).toString()
        val repository = ServiceLocator.repository
        CoroutineScope(SupervisorJob()).launch {
            withContext(Dispatchers.IO) {
                repository.insert(text = text)
            }
        }
    }

    private fun upload() {
        CoroutineScope(SupervisorJob()).launch {
            val error = ServiceLocator.repository.upload() ?: return@launch
            Log.d(TAG, "Failed to upload: $error")
        }
    }

    companion object {
        private const val TAG = "NotificationActionReceiver"
    }
}