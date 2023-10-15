package com.ramitsuri.notificationjournal.ui.tags

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.model.Tag
import com.ramitsuri.notificationjournal.core.model.TagTextUpdate
import com.ramitsuri.notificationjournal.di.ServiceLocator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TagsViewModel(private val dao: TagsDao) : ViewModel() {

    private val _state = MutableStateFlow(
        TagsViewState(tags = listOf())
    )
    val state: StateFlow<TagsViewState> = _state

    init {
        viewModelScope.launch {
            dao.getAllFlow().collect { tags ->
                _state.update { currentState ->
                    currentState.copy(tags = tags)
                }
            }
        }
    }

    fun add(text: String) {
        val currentState = _state.value
        viewModelScope.launch {
            val maxOrder = currentState.tags.maxByOrNull { it.order }?.order ?: 0
            val success = dao.insertIfPossible(Tag(id = 0, order = maxOrder + 1, value = text))
            if (!success) {
                _state.update {
                    it.copy(error = TagError.INSERT_FAIL)
                }
            }
        }
    }

    fun editValue(id: Int, text: String) {
        viewModelScope.launch {
            val success = dao.updateTextIfPossible(TagTextUpdate(id = id, value = text))
            if (!success) {
                _state.update {
                    it.copy(error = TagError.INSERT_FAIL)
                }
            }
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
    val tags: List<Tag>,
    val error: TagError? = null
)

enum class TagError {
    DELETE_FAIL,
    INSERT_FAIL
}