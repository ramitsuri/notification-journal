package com.ramitsuri.notificationjournal.core.spellcheck

import app.cash.turbine.test
import com.ramitsuri.notificationjournal.core.data.dictionary.DictionaryDao
import com.ramitsuri.notificationjournal.core.data.dictionary.DictionaryItem
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SpellCheckerTest {
    private lateinit var spellChecker: SpellChecker

    @Test
    fun `get suggestions`() =
        runTest {
            setup()

            spellChecker.corrections.test {
                // Initial state
                awaitItem()

                spellChecker.onTextUpdated("This is a very simpl test")
                val incorrectWord = "simpl"
                val suggestions = awaitItem()[incorrectWord]
                assert(!suggestions.isNullOrEmpty())
            }
        }

    @Test
    fun `suggestions respect lower case`() =
        runTest {
            setup()

            spellChecker.corrections.test {
                // Initial state
                awaitItem()

                spellChecker.onTextUpdated("This is a very simpl test")
                val incorrectWord = "simpl"
                val suggestions = awaitItem()[incorrectWord]
                assert(suggestions!!.all { it.first().isLowerCase() })
            }
        }

    @Test
    fun `suggestions respect upper case`() =
        runTest {
            setup()

            spellChecker.corrections.test {
                // Initial state
                awaitItem()

                spellChecker.onTextUpdated("This is a very Simpl test")
                val incorrectWord = "Simpl"
                val suggestions = awaitItem()[incorrectWord]
                assert(suggestions!!.all { it.first().isUpperCase() })
            }
        }

    private fun TestScope.setup() {
        val dispatcher = StandardTestDispatcher(testScheduler)
        spellChecker =
            SpellChecker(
                backgroundScope, dispatcher, dispatcher,
                object :
                    DictionaryDao() {
                    override suspend fun getItems(): List<DictionaryItem> {
                        return listOf()
                    }

                    override suspend fun insert(item: DictionaryItem) {
                    }
                },
            )
    }
}
