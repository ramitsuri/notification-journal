package com.ramitsuri.notificationjournal.core.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.TagTextUpdate
import com.ramitsuri.notificationjournal.core.network.DataSendHelper
import com.ramitsuri.notificationjournal.core.utils.PrefManager
import com.ramitsuri.notificationjournal.core.utils.combine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

class TagsViewModel(
    private val dao: TagsDao,
    private val dataSendHelper: DataSendHelper?,
    private val prefManager: PrefManager,
) :
    ViewModel() {
    private val _state =
        MutableStateFlow(
            TagsViewState(text = "", tags = listOf()),
        )
    val state: StateFlow<TagsViewState> = _state

    private var idBeingEdited: String? = null

    init {
        viewModelScope.launch {
            combine(
                dao.getAllFlow(),
                prefManager.getDefaultTagFlow(),
            ) { tags, defaultTag ->
                tags to defaultTag
            }.collect { (tags, defaultTag) ->
                _state.update { currentState ->
                    currentState.copy(tags = tags, defaultTag = defaultTag)
                }
            }
        }
    }

    fun editClicked(tag: Tag) {
        idBeingEdited = tag.id
        viewModelScope.launch {
            _state.update {
                it.copy(text = tag.value)
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

    fun save() {
        val currentState = _state.value

        val text = currentState.text
        if (text.isEmpty()) {
            return
        }

        viewModelScope.launch {
            val id = idBeingEdited
            if (id == null) {
                val maxOrder = currentState.tags.maxByOrNull { it.order }?.order ?: 0
                val success = dao.insertIfPossible(Tag(order = maxOrder + 1, value = text))
                if (!success) {
                    _state.update {
                        it.copy(error = TagError.INSERT_FAIL)
                    }
                }
            } else {
                val success = dao.updateTextIfPossible(TagTextUpdate(id = id, value = text))
                if (!success) {
                    _state.update {
                        it.copy(error = TagError.INSERT_FAIL)
                    }
                }
            }
        }
    }

    fun onAddOrEditCanceled() {
        idBeingEdited = null
        _state.update {
            it.copy(text = "")
        }
    }

    fun delete(tag: Tag) {
        viewModelScope.launch {
            val success = dao.deleteIfPossible(tag)
            if (!success) {
                _state.update {
                    it.copy(error = TagError.DELETE_FAIL)
                }
            }
        }
    }

    fun editOrder(
        fromOrder: Int,
        toOrder: Int,
    ) {
        val currentTags = _state.value.tags
        viewModelScope.launch {
            val currentTagAtFromIndex =
                currentTags
                    .getOrNull(fromOrder)
                    ?: return@launch
            val currentAtToOrder =
                currentTags
                    .getOrNull(toOrder)
                    ?: return@launch
            val newTags =
                listOf(
                    currentTagAtFromIndex.copy(order = toOrder),
                    currentAtToOrder.copy(order = fromOrder),
                )
            dao.updateOrder(newTags)
        }
    }

    fun onErrorAcknowledged() {
        _state.update {
            it.copy(error = null)
        }
    }

    fun sync() {
        viewModelScope.launch {
            val tags = _state.value.tags
            dataSendHelper?.sendTags(tags)
        }
    }

    fun setDefaultTag(tag: Tag?) {
        viewModelScope.launch {
            prefManager.setDefaultTag((tag ?: Tag.NO_TAG).value)
        }
    }

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun factory() =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(
                    modelClass: KClass<T>,
                    extras: CreationExtras,
                ): T {
                    return TagsViewModel(
                        ServiceLocator.tagsDao,
                        dataSendHelper = ServiceLocator.dataSendHelper,
                        prefManager = ServiceLocator.prefManager,
                    ) as T
                }
            }
    }
}

data class TagsViewState(
    val text: String,
    val tags: List<Tag>,
    val defaultTag: String = Tag.NO_TAG.value,
    val error: TagError? = null,
)

enum class TagError {
    DELETE_FAIL,
    INSERT_FAIL,
}
