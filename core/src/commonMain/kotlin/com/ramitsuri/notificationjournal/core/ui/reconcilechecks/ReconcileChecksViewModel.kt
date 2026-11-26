package com.ramitsuri.notificationjournal.core.ui.reconcilechecks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.core.data.ReconcileCheckDao
import com.ramitsuri.notificationjournal.core.data.TagsDao
import com.ramitsuri.notificationjournal.core.model.ReconcileCheck
import com.ramitsuri.notificationjournal.core.model.ReconcileCheckMatchType
import com.ramitsuri.notificationjournal.core.model.Tag
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import java.util.UUID

/** Manages state and persistence for configuring [ReconcileCheck]s in Settings. */
class ReconcileChecksViewModel
    @Inject
    constructor(
        private val dao: ReconcileCheckDao,
        private val tagsDao: TagsDao,
    ) : ViewModel() {
        private val _state = MutableStateFlow(ReconcileChecksViewState())
        val state: StateFlow<ReconcileChecksViewState> = _state

        init {
            viewModelScope.launch {
                combine(
                    dao.getAllFlow(),
                    tagsDao.getAllFlow(),
                ) { checks, tags ->
                    val availableTags = tags.filterNot { Tag.isNoTag(it.value) || it.id == Tag.NO_TAG.id }
                    val tagValues = availableTags.map { it.value }.toSet()
                    val items =
                        checks.map { check ->
                            ReconcileCheckItemState(
                                check = check,
                                summary = check.summary(),
                                isMissingTag = check.tag !in tagValues,
                            )
                        }
                    items to availableTags
                }.collect { (items, availableTags) ->
                    _state.update { currentState ->
                        currentState.copy(
                            checks = items,
                            availableTags = availableTags,
                        )
                    }
                }
            }
        }

        fun addClicked() {
            _state.update {
                it.copy(editorState = ReconcileCheckEditorState())
            }
        }

        fun editClicked(check: ReconcileCheck) {
            _state.update {
                it.copy(editorState = ReconcileCheckEditorState(check))
            }
        }

        fun tagSelected(tag: String) {
            if (Tag.isNoTag(tag)) {
                return
            }
            _state.update { currentState ->
                val editor = currentState.editorState ?: return@update currentState
                currentState.copy(editorState = editor.copy(selectedTag = tag))
            }
        }

        fun dayToggled(day: DayOfWeek) {
            _state.update { currentState ->
                val editor = currentState.editorState ?: return@update currentState
                val updatedDays =
                    if (day in editor.daysOfWeek) {
                        editor.daysOfWeek - day
                    } else {
                        editor.daysOfWeek + day
                    }
                currentState.copy(editorState = editor.copy(daysOfWeek = updatedDays))
            }
        }

        fun matchTypeSelected(matchType: ReconcileCheckMatchType) {
            _state.update { currentState ->
                val editor = currentState.editorState ?: return@update currentState
                currentState.copy(editorState = editor.copy(matchType = matchType))
            }
        }

        fun queryUpdated(query: String) {
            _state.update { currentState ->
                val editor = currentState.editorState ?: return@update currentState
                currentState.copy(editorState = editor.copy(query = query))
            }
        }

        fun save() {
            val currentState = _state.value
            val editor = currentState.editorState ?: return
            if (!currentState.canSave) {
                return
            }
            val selectedTag = editor.selectedTag ?: return
            val normalizedQuery =
                if (editor.matchType == ReconcileCheckMatchType.ANY_ENTRY) {
                    ""
                } else {
                    editor.query.trim()
                }
            val check =
                ReconcileCheck(
                    id = editor.editingCheckId ?: UUID.randomUUID().toString(),
                    tag = selectedTag,
                    daysOfWeek = editor.daysOfWeek,
                    matchType = editor.matchType,
                    query = normalizedQuery,
                    enabled = editor.enabled,
                )
            _state.update { it.copy(editorState = null) }
            viewModelScope.launch {
                dao.upsert(check)
            }
        }

        fun onAddOrEditCanceled() {
            _state.update { it.copy(editorState = null) }
        }

        fun toggleEnabled(
            check: ReconcileCheck,
            enabled: Boolean,
        ) {
            viewModelScope.launch {
                dao.update(check.copy(enabled = enabled))
            }
        }

        fun delete(check: ReconcileCheck) {
            viewModelScope.launch {
                dao.delete(check)
            }
        }
    }

data class ReconcileChecksViewState(
    val checks: List<ReconcileCheckItemState> = emptyList(),
    val availableTags: List<Tag> = emptyList(),
    val editorState: ReconcileCheckEditorState? = null,
) {
    val canSave: Boolean
        get() {
            val editor = editorState ?: return false
            val tag = editor.selectedTag ?: return false
            if (Tag.isNoTag(tag) || availableTags.none { it.value == tag }) {
                return false
            }
            if (editor.daysOfWeek.isEmpty()) {
                return false
            }
            return when (editor.matchType) {
                ReconcileCheckMatchType.ANY_ENTRY -> true
                ReconcileCheckMatchType.STARTS_WITH -> editor.query.isNotBlank()
                ReconcileCheckMatchType.CONTAINS_ALL_WORDS ->
                    ReconcileCheck(
                        tag = tag,
                        matchType = editor.matchType,
                        query = editor.query,
                    ).queryWords().isNotEmpty()
            }
        }
}

data class ReconcileCheckItemState(
    val check: ReconcileCheck,
    val summary: String,
    val isMissingTag: Boolean,
)

data class ReconcileCheckEditorState(
    val editingCheckId: String? = null,
    val selectedTag: String? = null,
    val daysOfWeek: Set<DayOfWeek> = DayOfWeek.entries.toSet(),
    val matchType: ReconcileCheckMatchType = ReconcileCheckMatchType.ANY_ENTRY,
    val query: String = "",
    val enabled: Boolean = true,
) {
    val isEditing: Boolean
        get() = editingCheckId != null

    constructor(check: ReconcileCheck) : this(
        editingCheckId = check.id,
        selectedTag = check.tag,
        daysOfWeek = check.daysOfWeek,
        matchType = check.matchType,
        query = check.query,
        enabled = check.enabled,
    )
}
