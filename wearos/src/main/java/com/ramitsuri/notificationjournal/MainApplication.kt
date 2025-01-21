package com.ramitsuri.notificationjournal

import android.app.Application
import com.ramitsuri.notificationjournal.core.di.Factory
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.presentation.MainViewModel

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        val factory = Factory(this)
        ServiceLocator.init(factory)
    }

    fun getViewModelFactory(): MainViewModel.Factory {
        return MainViewModel.Factory(
            ServiceLocator.repository,
            ServiceLocator.templatesDao,
            ServiceLocator.wearDataSharingClient,
            ServiceLocator.coroutineScope,
        )
    }
}
