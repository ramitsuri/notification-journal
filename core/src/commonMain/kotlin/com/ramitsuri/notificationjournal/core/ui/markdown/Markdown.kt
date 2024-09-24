package com.ramitsuri.notificationjournal.core.ui.markdown

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode

@Composable
fun Markdown(
    text: String,
    markdown: ASTNode,
    style: TextStyle = TextStyle.Default,
    boldStyleOverride: SpanStyle? = null,
    italicStyleOverride: SpanStyle? = null,
) {
    val builder = AnnotatedString.Builder()
    val images = mutableListOf<Pair<Int, String>>()
    val links = mutableListOf<Pair<IntRange, String>>()
    val treeStringBuilder = StringBuilder()
    fun showTree(node: ASTNode, depth: Int) {
        treeStringBuilder.appendLine(
            " ".repeat(depth) + node.type + " " + node.getTextInNode(text).toString()
                .take(10) + "..."
        )
        node.children.forEach {
            showTree(it, depth = depth + 1)
        }
    }
    showTree(markdown, 0)
    builder.appendMarkdown(
        markdownText = text,
        node = markdown,
        onInlineContents = { position, link ->
            images.add(position to link)
        },
        onLinkContents = { positionRange, url ->
            links.add(positionRange to url)
        },
        boldStyleOverride = boldStyleOverride,
        italicStyleOverride = italicStyleOverride,
    )
    ClickableText(
        text = builder.toAnnotatedString(),
        style = style,
    )
}

@Composable
private fun ClickableText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Clip,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    maxLines: Int = Int.MAX_VALUE,
) {

    Text(
        text = text,
        modifier = modifier,
        style = style,
        softWrap = softWrap,
        overflow = overflow,
        maxLines = maxLines,
        inlineContent = inlineContent,
    )
}

private fun AnnotatedString.Builder.appendMarkdown(
    markdownText: String,
    node: ASTNode,
    depth: Int = 0,
    onInlineContents: (position: Int, link: String) -> Unit,
    onLinkContents: (positionRange: IntRange, url: String) -> Unit,
    boldStyleOverride: SpanStyle? = null,
    italicStyleOverride: SpanStyle? = null,
): AnnotatedString.Builder {
    when (node.type) {
        MarkdownElementTypes.MARKDOWN_FILE, MarkdownElementTypes.PARAGRAPH -> {
            node.children.forEach { childNode ->
                appendMarkdown(
                    markdownText = markdownText,
                    node = childNode,
                    depth = depth + 1,
                    onInlineContents = onInlineContents,
                    onLinkContents = onLinkContents,
                    boldStyleOverride = boldStyleOverride,
                    italicStyleOverride = italicStyleOverride,
                )
            }
        }

        MarkdownElementTypes.SETEXT_1, MarkdownElementTypes.ATX_1 -> {
            withStyle(SpanStyle(fontSize = 24.sp)) {
                node.children.subList(1, node.children.size).forEach { childNode ->
                    appendMarkdown(
                        markdownText = markdownText,
                        node = childNode,
                        depth = depth + 1,
                        onInlineContents = onInlineContents,
                        onLinkContents = onLinkContents,
                        boldStyleOverride = boldStyleOverride,
                        italicStyleOverride = italicStyleOverride,
                    )
                }
            }
        }

        MarkdownElementTypes.SETEXT_2, MarkdownElementTypes.ATX_2 -> {
            withStyle(SpanStyle(fontSize = 20.sp)) {
                node.children.subList(1, node.children.size).forEach { childNode ->
                    appendMarkdown(
                        markdownText = markdownText,
                        node = childNode,
                        depth = depth + 1,
                        onInlineContents = onInlineContents,
                        onLinkContents = onLinkContents,
                        boldStyleOverride = boldStyleOverride,
                        italicStyleOverride = italicStyleOverride,
                    )
                }
            }
        }

        MarkdownElementTypes.CODE_SPAN -> {
            withStyle(SpanStyle(background = Color.LightGray)) {
                node.children.subList(1, node.children.size - 1)
                    .forEach { childNode ->
                        appendMarkdown(
                            markdownText = markdownText,
                            node = childNode,
                            depth = depth + 1,
                            onInlineContents = onInlineContents,
                            onLinkContents = onLinkContents,
                            boldStyleOverride = boldStyleOverride,
                            italicStyleOverride = italicStyleOverride,
                        )
                    }
            }
        }

        MarkdownElementTypes.STRONG -> {
            withStyle(boldStyleOverride ?: SpanStyle(fontWeight = FontWeight.Bold)) {
                node.children
                    .drop(2)
                    .dropLast(2)
                    .forEach { childNode ->
                        appendMarkdown(
                            markdownText = markdownText.trim(),
                            node = childNode,
                            depth = depth + 1,
                            onInlineContents = onInlineContents,
                            onLinkContents = onLinkContents,
                            boldStyleOverride = boldStyleOverride,
                            italicStyleOverride = italicStyleOverride,
                        )
                    }
            }
        }

        MarkdownElementTypes.EMPH -> {
            withStyle(italicStyleOverride ?: SpanStyle(fontStyle = FontStyle.Italic)) {
                node.children
                    .drop(1)
                    .dropLast(1)
                    .forEach { childNode ->
                        appendMarkdown(
                            markdownText = markdownText,
                            node = childNode,
                            depth = depth + 1,
                            onInlineContents = onInlineContents,
                            onLinkContents = onLinkContents,
                            boldStyleOverride = boldStyleOverride,
                            italicStyleOverride = italicStyleOverride,
                        )
                    }
            }
        }

        MarkdownElementTypes.CODE_FENCE -> {
            withStyle(SpanStyle(background = Color.Gray)) {
                node.children
                    .drop(1)
                    .dropLast(1)
                    .forEach { childNode ->
                        appendMarkdown(
                            markdownText = markdownText,
                            node = childNode,
                            depth = depth + 1,
                            onInlineContents = onInlineContents,
                            onLinkContents = onLinkContents,
                            boldStyleOverride = boldStyleOverride,
                            italicStyleOverride = italicStyleOverride,
                        )
                    }
            }
        }

        MarkdownElementTypes.IMAGE -> {
            val linkNode = node.children[node.children.size - 1]
            if (linkNode.children.size > 2) {
                val link =
                    linkNode.children[linkNode.children.size - 2].getTextInNode(markdownText)
                onInlineContents(node.startOffset, link.toString())
                appendInlineContent(link.toString(), link.toString())
            }
        }

        MarkdownElementTypes.INLINE_LINK -> {
            val linkDestination =
                node.children.findLast { it.type == MarkdownElementTypes.LINK_DESTINATION }
                    ?: return this
            val linkText = node.children.find { it.type == MarkdownElementTypes.LINK_TEXT }!!
                .children[1]
            if (linkDestination.children.size > 2) {
                val link =
                    linkDestination.getTextInNode(markdownText).toString()
                val start = this.length
                withStyle(SpanStyle(color = Color.Blue)) {
                    appendMarkdown(
                        markdownText = markdownText,
                        node = linkText,
                        depth = depth + 1,
                        onInlineContents = onInlineContents,
                        onLinkContents = onLinkContents,
                        boldStyleOverride = boldStyleOverride,
                        italicStyleOverride = italicStyleOverride,
                    )
                    val end = this.length
                    onLinkContents(start..end, link)
                }
            }
        }

        else -> {
            append(
                text = node.getTextInNode(markdownText).toString()
            )
        }
    }
    return this
}