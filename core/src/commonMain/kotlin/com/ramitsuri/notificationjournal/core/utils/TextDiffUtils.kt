package com.ramitsuri.notificationjournal.core.utils

import com.github.difflib.text.DiffRow
import com.github.difflib.text.DiffRowGenerator

private val diffGenerator by lazy {
    DiffRowGenerator.create()
        .showInlineDiffs(true)
        .mergeOriginalRevised(true)
        .inlineDiffByWord(true)
        .oldTag { _ -> "_" }
        .newTag { _ -> "**" }
        .build()
}

fun getDiffTextAsMarkdown(first: String, second: String): String {
    val rows: List<DiffRow> = diffGenerator
        .generateDiffRows(listOf(first), listOf(second))
    return rows[0].oldLine
}