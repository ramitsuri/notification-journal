package com.ramitsuri.notificationjournal

import android.app.Application
import com.ramitsuri.notificationjournal.core.di.Factory
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.presentation.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob

class MainApplication : Application() {
    private val coroutineScope = CoroutineScope(SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        val factory = Factory(this)
        ServiceLocator.init(factory)
    }

    fun getViewModelFactory(): MainViewModel.Factory {
        return MainViewModel.Factory(
            ServiceLocator.journalEntryDao,
            ServiceLocator.templatesDao,
            ServiceLocator.dataSharingClient,
            coroutineScope
        )
    }
}