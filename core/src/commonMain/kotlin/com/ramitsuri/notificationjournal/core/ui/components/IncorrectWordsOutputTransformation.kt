package com.ramitsuri.notificationjournal.core.ui.components

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle

class IncorrectWordsOutputTransformation(
    private val color: Color,
    private val incorrectWords: List<String>,
) : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        incorrectWords.forEach { word ->
            val start = originalText.indexOf(word)
            if (start == -1) {
                return@forEach
            }
            val end = start + word.length
            addStyle(SpanStyle(color = color), start, end)
        }
    }
}
