package com.ramitsuri.notificationjournal.core

import co.touchlab.kermit.Logger
import com.ramitsuri.notificationjournal.core.model.entry.JournalEntry
import com.ramitsuri.notificationjournal.core.repository.ImportRepository
import com.ramitsuri.notificationjournal.core.utils.asImportFileName
import com.ramitsuri.notificationjournal.core.utils.plus
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.withContext
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import java.io.File
import kotlin.time.Duration.Companion.seconds

class ImportRepositoryImpl(
    private val ioDispatcher: CoroutineDispatcher,
) : ImportRepository {

    private val channel = Channel<List<JournalEntry>>()

    override val importedEntriesFlow: Flow<List<JournalEntry>> = channel.consumeAsFlow()

    /**
     * Reads Markdown files named in the following format in the fromDir directory:
     * - fromDir
     *    - 2023
     *      - 11
     *        - 01.md
     *        - 02.md
     *      - 12
     *        - 01.md
     *        - 02.md
     *    - 2024
     *      - 01
     *        - 01.md
     *        - 02.md
     *      - 02
     *        - 01.md
     *        - 02.md
     *
     * The Markdown files themselves are formatted like
     *
     * // 2024/01/01.md
     *
     * # Tuesday, January 1, 2024
     *
     * ## Tag1
     * - Tag1 Entry1
     * - Tag1 Entry2
     *
     * ## Tag2
     * - Tag2 Entry1
     * - Tag2 Entry2
     *
     * NOTE: startDate and endDate are inclusive
     *
     */
    override suspend fun import(
        fromDir: String,
        startDate: LocalDate,
        endDate: LocalDate,
        lastImportDate: Instant,
    ) {
        withContext(ioDispatcher) {
            var date = startDate
            do {
                val filePath = buildString {
                    append(fromDir)
                    append("/")
                    append(date.asImportFileName())
                }
                val contentLines = readFileContents(
                    filePath = filePath,
                    lastImportDate = lastImportDate,
                )

                parseFileContent(date = date, contentLines = contentLines)
                    .let { entries ->
                        channel.send(entries)
                    }

                date = date.plus(DatePeriod(days = 1))
            } while (date <= endDate)
            channel.close()
        }
    }

    private fun readFileContents(filePath: String, lastImportDate: Instant): List<String> {
        val file = File(filePath)
        if (!file.exists()) {
            return emptyList()
        }
        return file
            .takeIf {
                val fileLastModifiedAt = Instant.fromEpochMilliseconds(it.lastModified())
                val fileModifiedSinceLastImport = fileLastModifiedAt >= lastImportDate
                if (!fileModifiedSinceLastImport) {
                    Logger.i(TAG) { "File not changed since last import, ignoring: $filePath" }
                }
                fileModifiedSinceLastImport
            }
            ?.readLines()
            ?.let { lines ->
                // Add an empty line at the end if not present
                if (lines.lastOrNull()?.isBlank() == true) {
                    lines
                } else {
                    lines.plus("")
                }
            }
            ?: emptyList()
    }

    private fun parseFileContent(
        date: LocalDate,
        contentLines: List<String>,
    ): List<JournalEntry> {
        var tag: String? = null
        var text: String? = null
        var time = date.atTime(LocalTime(0, 0, 0, 0))

        fun MutableList<JournalEntry>.addEntryIfPossible() {
            // There's already some text being tracked so should add that as entry first
            if (tag?.isNotBlank() == true && text?.isNotBlank() == true) {
                val entryTag = tag?.trim() ?: return // Should not be null at this point
                val entryText = text?.trim() ?: return // Should not be null at this point
                add(
                    JournalEntry(
                        entryTime = time,
                        text = entryText,
                        tag = entryTag,
                        reconciled = true,
                    )
                )
                time = time.plus(1.seconds)
                text = null
            }
        }

        return buildList {
            contentLines.forEachIndexed { index, line ->
                // Ignore the date line
                if (line.startsWith("# ")) {
                    return@forEachIndexed
                }

                // Ignore empty lines
                if (line.isBlank() && index != contentLines.lastIndex) {
                    return@forEachIndexed
                }

                if (line.startsWith("## ")) { // New tag line encountered
                    addEntryIfPossible()
                    tag = line.removePrefix("## ")
                } else if (line.startsWith("-")) { // A new entry found
                    addEntryIfPossible()
                    text = (text ?: "").plus(line.removePrefix("-"))
                } else if (index == contentLines.lastIndex) {
                    addEntryIfPossible()
                } else {
                    text = (text ?: "").plus("\n").plus(line)
                }
            }
        }
    }

    companion object {
        private const val TAG = "ImportRepository"
    }
}
