package com.ramitsuri.notificationjournal

import android.app.Application
import com.google.android.gms.wearable.Wearable
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.data.DataSharingClient
import com.ramitsuri.notificationjournal.core.data.DataSharingClientImpl
import com.ramitsuri.notificationjournal.core.di.Factory
import com.ramitsuri.notificationjournal.presentation.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MainApplication : Application() {
    private val coroutineScope = CoroutineScope(SupervisorJob())

    lateinit var factory: Factory

    override fun onCreate() {
        super.onCreate()
        factory = Factory(this)
    }

    fun getViewModelFactory(): MainViewModel.Factory {
        return MainViewModel.Factory(
            AppDatabase.getJournalEntryDao(factory),
            AppDatabase.getJournalEntryTemplateDao(factory),
            getDataClient(),
            coroutineScope
        )
    }

    private fun getDataClient(): DataSharingClient {
        val dataClient = Wearable.getDataClient(this)
        return DataSharingClientImpl(dataClient)
    }
}