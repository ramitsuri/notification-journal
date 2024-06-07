package com.ramitsuri.notificationjournal.core.ui.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ramitsuri.notificationjournal.core.data.DataSharingClient
import com.ramitsuri.notificationjournal.core.data.JournalEntryTemplateDao
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class TemplatesViewModel(
    private val dao: JournalEntryTemplateDao,
    private val tagsDao: TagsDao,
    private val dataSharingClient: DataSharingClient,
) : ViewModel() {

    private val _state = MutableStateFlow(TemplatesViewState())
    val state: StateFlow<TemplatesViewState> = _state

    private var idBeingEdited: String? = null

    init {
        viewModelScope.launch {
            dao.getAllFlow().collect { templates ->
                _state.update { currentState ->
                    currentState.copy(
                        templates = templates,
                        canAddMore = templates.size < MAX_TEMPLATES_ALLOWED
                    )
                }
            }
        }
        viewModelScope.launch {
            _state.update { currentState ->
                currentState.copy(tags = tagsDao.getAll())
            }
        }
    }

    fun editClicked(journalEntryTemplate: JournalEntryTemplate) {
        idBeingEdited = journalEntryTemplate.id
        viewModelScope.launch {
            _state.update {
                it.copy(text = journalEntryTemplate.text, selectedTag = journalEntryTemplate.tag)
            }
        }
    }

    fun addClicked() {
        idBeingEdited = null
        _state.update {
            it.copy(text = "")
        }
    }

    fun textUpdated(text: String) {
        _state.update {
            it.copy(text = text)
        }
    }

    fun tagClicked(tag: String) {
        _state.update {
            it.copy(selectedTag = tag)
        }
    }

    fun save() {
        val currentState = _state.value
        if (!currentState.canSave) {
            return
        }
        val text = currentState.text
        val tag = currentState.selectedTag
        if (text.isEmpty() || tag.isNullOrEmpty()) {
            return
        }

        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            dao.insertOrUpdate(idBeingEdited, text, tag)
            idBeingEdited = null
            _state.update {
                it.copy(isLoading = false, text = "", selectedTag = null, showSync = true)
            }
        }
    }

    fun onAddOrEditCanceled() {
        idBeingEdited = null
        _state.update {
            it.copy(text = "", selectedTag = null)
        }
    }

    fun delete(journalEntryTemplate: JournalEntryTemplate) {
        viewModelScope.launch {
            dao.delete(listOf(journalEntryTemplate))
        }
    }

    fun syncWithWear() {
        viewModelScope.launch {
            val templates = _state.value.templates
            templates.forEach { template ->
                dataSharingClient.postTemplate(
                    id = template.id,
                    value = template.text,
                    tag = template.tag
                )
            }
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun factory() = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                return TemplatesViewModel(
                    dao = ServiceLocator.templatesDao,
                    tagsDao = ServiceLocator.tagsDao,
                    dataSharingClient = ServiceLocator.dataSharingClient,
                ) as T
            }
        }

        private const val MAX_TEMPLATES_ALLOWED = 8
    }
}

data class TemplatesViewState(
    val isLoading: Boolean = false,
    val text: String = "",
    val tags: List<Tag> = listOf(),
    val selectedTag: String? = null,
    val templates: List<JournalEntryTemplate> = listOf(),
    val canAddMore: Boolean = false,
    val showSync: Boolean = false,
) {
    val canSave: Boolean
        get() {
            return text.isNotEmpty() && !selectedTag.isNullOrEmpty()
        }
}