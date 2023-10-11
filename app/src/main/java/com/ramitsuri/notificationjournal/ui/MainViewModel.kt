package com.ramitsuri.notificationjournal.ui

import android.util.Log
import android.webkit.URLUtil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ramitsuri.notificationjournal.core.data.JournalEntry
import com.ramitsuri.notificationjournal.core.model.SortOrder
import com.ramitsuri.notificationjournal.core.repository.JournalRepository
import com.ramitsuri.notificationjournal.core.utils.Constants
import com.ramitsuri.notificationjournal.core.utils.KeyValueStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.time.Instant
import java.time.temporal.ChronoUnit

class MainViewModel(
    private val keyValueStore: KeyValueStore,
    private val repository: JournalRepository
) : ViewModel() {

    private var collectionJob: Job? = null

    private val _state = MutableStateFlow(
        ViewState(
            journalEntries = mapOf(),
            loading = false,
            serverText = getApiUrl()
        )
    )
    val state: StateFlow<ViewState> = _state

    init {
        restartCollection()
    }

    fun setReceivedText(text: String?) {
        _state.update {
            it.copy(receivedText = text)
        }
        loadAdditionalDataIfUrl(text)
    }

    fun upload() {
        _state.update {
            it.copy(loading = true)
        }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val error = repository.upload()
                _state.update {
                    it.copy(loading = false, error = error)
                }
            }
        }
    }

    fun delete(journalEntry: JournalEntry) {
        viewModelScope.launch {
            repository.delete(journalEntry)
        }
    }

    fun add(text: String) {
        viewModelScope.launch {
            repository.insert(
                text = text
            )
        }
    }

    fun edit(id: Int, text: String) {
        viewModelScope.launch {
            repository.edit(id, text)
        }
    }

    fun setApiUrl(url: String) {
        keyValueStore.putString(Constants.PREF_KEY_API_URL, url)
        _state.update {
            it.copy(serverText = url)
        }
    }

    fun reverseSortOrder() {
        val currentSortOrder = getSortOrder()
        val newSortOrder = if (currentSortOrder == SortOrder.ASC) {
            SortOrder.DESC
        } else {
            SortOrder.ASC
        }
        setSortOrder(newSortOrder)
        restartCollection()
    }

    private fun setSortOrder(sortOrder: SortOrder) {
        keyValueStore.putInt(Constants.PREF_KEY_SORT_ORDER, sortOrder.key)
    }

    private fun getSortOrder(): SortOrder {
        val preferredSortOrderKey = keyValueStore.getInt(Constants.PREF_KEY_SORT_ORDER, 0)
        return SortOrder.fromKey(preferredSortOrderKey)
    }

    fun onErrorAcknowledged() {
        _state.update {
            it.copy(error = null)
        }
    }

    fun resetReceivedText() {
        _state.update {
            it.copy(receivedText = null, suggestedTextFromReceivedText = null)
        }
    }

    private fun restartCollection() {
        collectionJob?.cancel()
        collectionJob = viewModelScope.launch {
            repository.getFlow(getSortOrder()).collect { entries ->
                val groupedByDay = entries.groupBy { it.entryTime.truncatedTo(ChronoUnit.DAYS) }
                _state.update {
                    it.copy(journalEntries = groupedByDay)
                }
            }
        }
    }

    private fun loadAdditionalDataIfUrl(url: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            if (url == null) {
                Log.d(TAG, "receivedText null")
                return@launch
            }
            if (!(URLUtil.isHttpUrl(url) || URLUtil.isHttpsUrl(url))) {
                Log.d(TAG, "receivedText not url")
                return@launch
            }
            try {
                val response = Jsoup.connect(url)
                    .ignoreContentType(true)
                    .userAgent("Mozilla")
                    .referrer("http://www.google.com")
                    .timeout(10000)
                    .followRedirects(true)
                    .execute()

                val doc = response.parse()

                val ogTags = doc.select("meta[property^=og:]")
                if (ogTags.isEmpty()) {
                    Log.d(TAG, "Tags empty")
                    return@launch
                }
                ogTags.forEach { tag ->
                    when (tag.attr("property")) {
                        "og:title" -> {
                            val pageTitle = tag.attr("content")
                            Log.d(TAG, "Page title: $pageTitle")
                            if (!pageTitle.isNullOrEmpty()) {
                                _state.update {
                                    it.copy(suggestedTextFromReceivedText = pageTitle)
                                }
                                return@launch
                            }
                        }

                        else -> {
                            // Do nothing
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun getApiUrl() = keyValueStore.getString(Constants.PREF_KEY_API_URL, "") ?: ""

    companion object {
        private const val TAG = "PhoneViewModel"

        fun factory(
            keyValueStore: KeyValueStore,
            repository: JournalRepository
        ) = object : ViewModelProvider.Factory {

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(keyValueStore, repository) as T
            }
        }
    }
}

data class ViewState(
    val journalEntries: Map<Instant, List<JournalEntry>> = mapOf(),
    val receivedText: String? = null,
    val suggestedTextFromReceivedText: String? = null,
    val serverText: String = "",
    val loading: Boolean = false,
    val error: String? = null
)