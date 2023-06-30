package com.ramitsuri.notificationjournal

import android.app.Application
import com.google.android.gms.wearable.Wearable
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.data.DataSharingClient
import com.ramitsuri.notificationjournal.core.data.DataSharingClientImpl
import com.ramitsuri.notificationjournal.presentation.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MainApplication : Application() {
    private val coroutineScope = CoroutineScope(SupervisorJob())

    fun getViewModelFactory(): MainViewModel.Factory {
        return MainViewModel.Factory(
            AppDatabase.getDao(applicationContext),
            getDataClient(),
            coroutineScope
        )
    }

    private fun getDataClient(): DataSharingClient {
        val dataClient = Wearable.getDataClient(this)
        return DataSharingClientImpl(dataClient)
    }
}