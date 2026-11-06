package com.ramitsuri.notificationjournal.core.ui.nav

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.ramitsuri.notificationjournal.core.ui.addjournal.AddJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.editjournal.EditJournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.import.ImportViewModel
import com.ramitsuri.notificationjournal.core.ui.journalentry.JournalEntryViewModel
import com.ramitsuri.notificationjournal.core.ui.journalentryday.ViewJournalEntryDayViewModel
import com.ramitsuri.notificationjournal.core.ui.journalentrylist.JournalEntryDaysViewModel
import com.ramitsuri.notificationjournal.core.ui.log.LogScreenViewModel
import com.ramitsuri.notificationjournal.core.ui.search.SearchViewModel
import com.ramitsuri.notificationjournal.core.ui.settings.SettingsViewModel
import com.ramitsuri.notificationjournal.core.ui.tags.TagsViewModel
import com.ramitsuri.notificationjournal.core.ui.templates.TemplatesViewModel
import kotlin.reflect.KClass

interface NavViewModelFactories {
    val addJournalEntryViewModelFactory: AddJournalEntryViewModel.Factory
    val editJournalEntryViewModelFactory: EditJournalEntryViewModel.Factory
    val viewJournalEntryDayViewModelFactory: ViewJournalEntryDayViewModel.Factory
    val journalEntryViewModelFactory: JournalEntryViewModel.Factory
    val journalEntryDaysViewModel: JournalEntryDaysViewModel
    val tagsViewModel: TagsViewModel
    val templatesViewModel: TemplatesViewModel
    val logScreenViewModel: LogScreenViewModel
    val searchViewModel: SearchViewModel
    val importViewModel: ImportViewModel
    val settingsViewModel: SettingsViewModel
}

inline fun <reified T : ViewModel> viewModelFactory(crossinline create: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <VM : ViewModel> create(
            modelClass: KClass<VM>,
            extras: CreationExtras,
        ): VM = create() as VM
    }
