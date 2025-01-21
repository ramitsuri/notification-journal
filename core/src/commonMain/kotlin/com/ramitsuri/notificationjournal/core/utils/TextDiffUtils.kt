package com.ramitsuri.notificationjournal.core.utils

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.github.difflib.text.DiffRow
import com.github.difflib.text.DiffRowGenerator

fun getDiffAsAnnotatedText(
    left: String,
    right: String,
    leftColor: Color,
    rightColor: Color,
): AnnotatedString {
    val rows: List<DiffRow> =
        diffGenerator
            .generateDiffRows(listOf(left), listOf(right))
    return rows[0]
        .oldLine
        .splitByType()
        .annotated(leftColor, rightColor)
}

private val diffGenerator by lazy {
    DiffRowGenerator.create()
        .showInlineDiffs(true)
        .mergeOriginalRevised(true)
        .inlineDiffByWord(true)
        .oldTag { _ -> "${TextType.LEFT.char}" }
        .newTag { _ -> "${TextType.RIGHT.char}" }
        .build()
}

private fun String.splitByType(): DiffText {
    val splitByType = mutableListOf<Text>()
    var textType: TextType? = null
    var textTracked = ""
    forEachIndexed { index, char ->
        if (char != TextType.LEFT.char && char != TextType.RIGHT.char) {
            textTracked += char
        }
        if (index == lastIndex) {
            // Last index, add with whatever type we're tracking or as common if not tracking anything
            splitByType.add(Text(textType ?: TextType.COMMON, textTracked))
            return@forEachIndexed
        }
        val type = textType
        if (type == null) {
            textType = TextType.fromChar(char)
            return@forEachIndexed
        }
        if (char == TextType.LEFT.char) {
            splitByType.add(Text(type, textTracked))
            textType = if (textType == TextType.COMMON) TextType.LEFT else null
            textTracked = ""
        } else if (char == TextType.RIGHT.char) {
            splitByType.add(Text(type, textTracked))
            textType = if (textType == TextType.COMMON) TextType.RIGHT else null
            textTracked = ""
        }
    }
    return DiffText(splitByType)
}

private fun DiffText.annotated(
    leftColor: Color,
    rightColor: Color,
): AnnotatedString {
    return buildAnnotatedString {
        texts.forEach { type ->
            when (type.type) {
                TextType.LEFT -> {
                    withStyle(SpanStyle(background = leftColor)) {
                        append(type.text)
                    }
                }

                TextType.RIGHT -> {
                    withStyle(SpanStyle(background = rightColor)) {
                        append(type.text)
                    }
                }

                TextType.COMMON -> {
                    append(type.text)
                }
            }
        }
    }
}

private data class DiffText(val texts: List<Text>)

private data class Text(val type: TextType, val text: String)

private enum class TextType(val char: Char) {
    COMMON(' '),
    LEFT('\u00AB'),
    RIGHT('\u00BB'),
    ;

    companion object {
        fun fromChar(char: Char): TextType {
            return when (char) {
                LEFT.char -> {
                    LEFT
                }

                RIGHT.char -> {
                    RIGHT
                }

                else -> {
                    COMMON
                }
            }
        }
    }
}
