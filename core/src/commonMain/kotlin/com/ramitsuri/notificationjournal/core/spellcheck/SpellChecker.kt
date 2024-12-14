package com.ramitsuri.notificationjournal.core.spellcheck

import com.darkrockstudios.symspell.fdic.loadFdicFile
import com.darkrockstudios.symspellkt.common.Verbosity
import com.darkrockstudios.symspellkt.impl.SymSpell
import com.ramitsuri.notificationjournal.core.data.dictionary.DictionaryDao
import com.ramitsuri.notificationjournal.core.data.dictionary.DictionaryDaoImpl
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.consumeEach
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import notificationjournal.core.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import java.util.Locale

class SpellChecker(
    initializationScope: CoroutineScope,
    ioDispatcher: CoroutineDispatcher,
    private val defaultDispatcher: CoroutineDispatcher,
    private val dictionaryDao: DictionaryDao = DictionaryDaoImpl(),
    private val locale: Locale = Locale.US,
) {
    private val _corrections = MutableStateFlow<Map<String, List<String>>>(mapOf())
    val corrections = _corrections.asStateFlow()

    private val findCorrectionsChannel = Channel<String>()
    private val encounteredWords = mutableSetOf<String>()
    private val mutex = Mutex()

    private val checker = SymSpell()

    init {
        initializationScope.launch(ioDispatcher) {
            loadDictionary()
            findCorrectionsChannel.consumeEach {
                findCorrections(it)
            }
        }
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadDictionary() {
        checker.dictionary.loadFdicFile(Res.readBytes("files/en-80k.fdic"))
        checker.dictionary.loadFdicFile(Res.readBytes("files/frequency_bigramdictionary_en_243_342.fdic"))

        dictionaryDao
            .getItems()
            .forEach { dictionaryItem ->
                checker.createDictionaryEntry(dictionaryItem.word, 1)
            }
    }

    suspend fun onTextUpdated(text: String) = coroutineScope {
        withContext(defaultDispatcher) {
            val wordsInText = getWordsInText(text)

            wordsInText.forEach { word ->
                // Storing in a separate list so that it can be immediately updated with encountered
                // words rather than updating the state flow with the encountered word and no
                // corrections first because the state flow is setup to only hold entries which have
                // any corrections
                if (addToEncounteredWords(word)) {
                    findCorrectionsChannel.send(word)
                }
            }

            _corrections.update { existing ->
                existing.filter {
                    wordsInText.contains(it.key)
                }
            }
        }
    }

    fun reset() {
        encounteredWords.clear()
        _corrections.update { mapOf() }
    }

    private suspend fun findCorrections(word: String) = coroutineScope {
        withContext(defaultDispatcher) {
            checker.lookup(
                word,
                Verbosity.Closest,
                2.0
            )
                .filter {
                    it.term.lowercase() != word.lowercase()
                }
                .map { suggestion ->
                    suggestion.term.replaceFirstChar {
                        if (word.first().isUpperCase()) {
                            it.titlecase(locale)
                        } else if (word.first().isLowerCase()) {
                            it.lowercase(locale)
                        } else {
                            it.toString()
                        }
                    }
                }
                .takeIf { it.isNotEmpty() }
                ?.let { suggestions ->
                    _corrections.update { existing ->
                        existing + (word to suggestions)
                    }
                }
        }
    }

    private fun getWordsInText(text: String): List<String> {
        val regex = """(?<!\w)'(?!\w)|[.,!?;:"-]""".toRegex()
        return text
            .replace(regex, "")
            .split("\n")
            .flatMap { it.split(" ") }
    }

    private suspend fun addToEncounteredWords(word: String): Boolean {
        mutex.withLock {
            return encounteredWords.add(word)
        }
    }
}
