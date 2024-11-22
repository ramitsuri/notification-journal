package com.ramitsuri.notificationjournal.core.ui.templates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ramitsuri.notificationjournal.core.data.JournalEntryTemplateDao
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.data.WearDataSharingClient
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.template.JournalEntryTemplate
import com.ramitsuri.notificationjournal.core.network.DataSendHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class TemplatesViewModel(
    private val dao: JournalEntryTemplateDao,
    private val tagsDao: TagsDao,
    private val wearDataSharingClient: WearDataSharingClient,
    private val dataSendHelper: DataSendHelper?,
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
                it.copy(templateBeingAddedOrEdited = TemplateValues(journalEntryTemplate))
            }
        }
    }

    fun addClicked() {
        idBeingEdited = null
        _state.update {
            it.copy(templateBeingAddedOrEdited = TemplateValues())
        }
    }

    fun textUpdated(text: String) {
        _state.update {
            it.updateText(text)
        }
    }

    fun displayTextUpdated(text: String) {
        _state.update {
            it.updateDisplayText(text)
        }
    }

    fun shortDisplayTextUpdated(text: String) {
        _state.update {
            it.updateShortDisplayText(text)
        }
    }

    fun tagClicked(tag: String) {
        _state.update {
            it.updateTag(tag)
        }
    }

    fun save() {
        val currentState = _state.value
        if (!currentState.canSave) {
            return
        }

        _state.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            dao.insertOrUpdate(
                id = idBeingEdited,
                text = currentState.templateBeingAddedOrEdited.text,
                tag = currentState.templateBeingAddedOrEdited.tag ?: Tag.NO_TAG.value,
                displayText = currentState.templateBeingAddedOrEdited.displayText,
                shortDisplayText = currentState.templateBeingAddedOrEdited.shortDisplayText,
            )
            idBeingEdited = null
            _state.update {
                it.copy(isLoading = false, templateBeingAddedOrEdited = TemplateValues())
            }
        }
    }

    fun onAddOrEditCanceled() {
        idBeingEdited = null
        _state.update {
            it.copy(templateBeingAddedOrEdited = TemplateValues())
        }
    }

    fun delete(journalEntryTemplate: JournalEntryTemplate) {
        viewModelScope.launch {
            dao.delete(listOf(journalEntryTemplate))
        }
    }

    fun sync() {
        viewModelScope.launch {
            val templates = _state.value.templates
            templates.forEach { template ->
                wearDataSharingClient.postTemplate(template)
            }
            dataSendHelper?.sendTemplates(templates)
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun factory() = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T {
                return TemplatesViewModel(
                    dao = ServiceLocator.templatesDao,
                    tagsDao = ServiceLocator.tagsDao,
                    wearDataSharingClient = ServiceLocator.wearDataSharingClient,
                    dataSendHelper = ServiceLocator.dataSendHelper,
                ) as T
            }
        }

        private const val MAX_TEMPLATES_ALLOWED = 10
    }
}

data class TemplatesViewState(
    val isLoading: Boolean = false,
    val templateBeingAddedOrEdited: TemplateValues = TemplateValues(),
    val tags: List<Tag> = listOf(),
    val templates: List<JournalEntryTemplate> = listOf(),
    val canAddMore: Boolean = false,
) {
    val canSave: Boolean
        get() {
            return templateBeingAddedOrEdited.isValid
        }

    fun updateText(text: String) =
        copy(templateBeingAddedOrEdited = templateBeingAddedOrEdited.copy(text = text))

    fun updateTag(tag: String) = if (templateBeingAddedOrEdited.tag == tag) {
        // Unselect if already selected
        copy(templateBeingAddedOrEdited = templateBeingAddedOrEdited.copy(tag = null))
    } else {
        copy(templateBeingAddedOrEdited = templateBeingAddedOrEdited.copy(tag = tag))
    }

    fun updateDisplayText(text: String) =
        copy(templateBeingAddedOrEdited = templateBeingAddedOrEdited.copy(displayText = text))

    fun updateShortDisplayText(text: String) =
        copy(templateBeingAddedOrEdited = templateBeingAddedOrEdited.copy(shortDisplayText = text))

}

data class TemplateValues(
    val text: String = "",
    val displayText: String = "",
    val shortDisplayText: String = "",
    val tag: String? = null,
) {
    val isValid: Boolean
        get() {
            return text.isNotEmpty() && displayText.isNotEmpty()
        }

    constructor(template: JournalEntryTemplate) : this(
        text = template.text,
        displayText = template.displayText,
        shortDisplayText = template.shortDisplayText,
        tag = template.tag,
    )
}
