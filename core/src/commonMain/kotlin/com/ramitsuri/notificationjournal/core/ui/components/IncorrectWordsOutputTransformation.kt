package com.ramitsuri.notificationjournal.core.ui.components

import androidx.compose.foundation.text.input.OutputTransformation
import androidx.compose.foundation.text.input.TextFieldBuffer
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.style.TextDecoration

class IncorrectWordsOutputTransformation(
    private val incorrectWords: List<String>,
) : OutputTransformation {
    override fun TextFieldBuffer.transformOutput() {
        incorrectWords.forEach { word ->
            var start = originalText.indexOf(word)
            while (start != -1) {
                val end = start + word.length
                addStyle(SpanStyle(textDecoration = TextDecoration.Underline), start, end)
                start = originalText.indexOf(word, end)
            }
        }
    }
}
