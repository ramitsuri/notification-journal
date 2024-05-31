package com.ramitsuri.notificationjournal.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.TagTextUpdate
import com.ramitsuri.notificationjournal.core.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TagsViewModel(private val dao: TagsDao) : ViewModel() {

    private val _state = MutableStateFlow(
        TagsViewState(text = "", tags = listOf())
    )
    val state: StateFlow<TagsViewState> = _state

    private var idBeingEdited: Int? = null

    init {
        viewModelScope.launch {
            dao.getAllFlow().collect { tags ->
                _state.update { currentState ->
                    currentState.copy(tags = tags)
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
                val success = dao.insertIfPossible(Tag(id = 0, order = maxOrder + 1, value = text))
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

    fun editOrder(fromOrder: Int, toOrder: Int) {
        val currentTags = _state.value.tags
        viewModelScope.launch {
            val currentTagAtFromIndex = currentTags
                .getOrNull(fromOrder)
                ?: return@launch
            val currentAtToOrder = currentTags
                .getOrNull(toOrder)
                ?: return@launch
            val newTags = listOf(
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

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun factory() = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TagsViewModel(ServiceLocator.tagsDao) as T
            }
        }
    }
}

data class TagsViewState(
    val text: String,
    val tags: List<Tag>,
    val error: TagError? = null
)

enum class TagError {
    DELETE_FAIL,
    INSERT_FAIL
}