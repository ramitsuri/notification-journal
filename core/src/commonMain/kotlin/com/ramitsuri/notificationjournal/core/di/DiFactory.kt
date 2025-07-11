package com.ramitsuri.notificationjournal.core.di

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDeepLink
import androidx.room.RoomDatabase
import com.ramitsuri.notificationjournal.core.data.AppDatabase
import com.ramitsuri.notificationjournal.core.data.WearDataSharingClient
import com.ramitsuri.notificationjournal.core.repository.ExportRepository
import com.ramitsuri.notificationjournal.core.repository.ImportRepository
import com.ramitsuri.notificationjournal.core.ui.addjournal.AddJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.editjournal.EditJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.journalentryday.ViewJournalEntryDayViewModel
import com.ramitsuri.notificationjournal.core.utils.NotificationHandler
import com.russhwolf.settings.Settings
import kotlinx.coroutines.CoroutineDispatcher
import java.nio.file.Path

expect class DiFactory {
    val allowJournalImport: Boolean

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

    // Because Android AbstractSavedStateViewModelFactory expects Class as a parameter but JVM
    // AbstractSavedStateViewModelFactory expects KClass as a parameter.
    fun viewJournalEntryDayVMFactory(
        navBackStackEntry: NavBackStackEntry,
        getVMInstance: (SavedStateHandle) -> ViewJournalEntryDayViewModel,
    ): AbstractSavedStateViewModelFactory

    fun getDataStorePath(): Path

    fun getImportRepository(ioDispatcher: CoroutineDispatcher): ImportRepository

    fun getExportRepository(ioDispatcher: CoroutineDispatcher): ExportRepository?

    fun getJournalEntryScreenDeepLinks(): List<NavDeepLink>

    fun getAddEntryScreenDeepLinks(): List<NavDeepLink>
}
