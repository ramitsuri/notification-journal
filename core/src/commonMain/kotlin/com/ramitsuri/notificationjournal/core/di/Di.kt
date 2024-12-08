package com.ramitsuri.notificationjournal.core.di

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavBackStackEntry
import androidx.room.RoomDatabase
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.data.WearDataSharingClient
import com.ramitsuri.notificationjournal.core.spellcheck.SpellChecker
import com.ramitsuri.notificationjournal.core.ui.addjournal.AddJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.editjournal.EditJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.utils.NotificationHandler
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

expect class Factory {
    fun getSettings(): Settings

    fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase>

    fun getWearDataSharingClient(): WearDataSharingClient

    fun getNotificationHandler(): NotificationHandler

    // Because Android AbstractSavedStateViewModelFactory expects Class as a parameter but JVM
    // AbstractSavedStateViewModelFactory expects KClass as a parameter.
    fun addJournalEntryVMFactory(
        navBackStackEntry: NavBackStackEntry,
        getVMInstance: (SavedStateHandle) -> AddJournalEntryViewModel,
    ): AbstractSavedStateViewModelFactory

    // Because Android AbstractSavedStateViewModelFactory expects Class as a parameter but JVM
    // AbstractSavedStateViewModelFactory expects KClass as a parameter.
    fun editJournalEntryVMFactory(
        navBackStackEntry: NavBackStackEntry,
        getVMInstance: (SavedStateHandle) -> EditJournalEntryViewModel,
    ): AbstractSavedStateViewModelFactory

    fun getDataStorePath(): String

    fun getSpellChecker(
        initializationScope: CoroutineScope,
        ioDispatcher: CoroutineDispatcher,
        defaultDispatcher: CoroutineDispatcher,
    ): SpellChecker?
}
