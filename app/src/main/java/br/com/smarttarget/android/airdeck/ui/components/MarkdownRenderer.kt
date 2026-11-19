package br.com.smarttarget.android.airdeck.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.smarttarget.android.airdeck.domain.MarkdownBlock
import br.com.smarttarget.android.airdeck.domain.TableAlignment

@Composable
fun MarkdownRenderer(
    blocks: List<MarkdownBlock>,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(16.dp)
) {
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        itemsIndexed(
            items = blocks,
            key = { index, _ -> index }
        ) { _, block ->
            MarkdownBlockItem(block = block)
        }
    }
}

@Composable
private fun MarkdownBlockItem(block: MarkdownBlock) {
    val colors = MaterialTheme.colorScheme
    when (block) {
        is MarkdownBlock.Heading -> {
            Column(modifier = Modifier.fillMaxWidth().padding(top = if (block.level <= 2) 6.dp else 2.dp)) {
                val style = when (block.level) {
                    1 -> MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                    2 -> MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    else -> MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (block.level == 2) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(18.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(colors.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = rememberInlineMarkdown(block.text, colors),
                        style = style,
                        color = if (block.level == 1) colors.primary else colors.onSurface
                    )
                }
                if (block.level == 1) {
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = colors.primary.copy(alpha = 0.45f))
                }
            }
        }

        is MarkdownBlock.Paragraph -> {
            Text(
                text = rememberInlineMarkdown(block.text, colors),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 21.sp),
                color = colors.onSurface
            )
        }

        is MarkdownBlock.Blockquote -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceContainer,
                border = BorderStroke(1.dp, colors.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .fillMaxHeight()
                            .background(colors.primary)
                    )
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        block.lines.forEach { line ->
                            Text(
                                text = rememberInlineMarkdown(line, colors),
                                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
                                color = colors.onSurface
                            )
                        }
                    }
                }
            }
        }

        is MarkdownBlock.ListItem -> {
            val startPad = (block.indentLevel * 14).dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = startPad),
                verticalAlignment = Alignment.Top
            ) {
                val marker = if (block.orderedNumber != null) {
                    "${block.orderedNumber}."
                } else {
                    "•"
                }
                Text(
                    text = marker,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = colors.primary,
                    modifier = Modifier.width(if (block.orderedNumber != null) 24.dp else 14.dp)
                )
                Text(
                    text = rememberInlineMarkdown(block.text, colors),
                    style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 20.sp),
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        is MarkdownBlock.Table -> {
            MarkdownTableCard(block = block)
        }

        is MarkdownBlock.CodeBlock -> {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surfaceContainerLowest,
                border = BorderStroke(1.dp, colors.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (block.language.isNotBlank()) {
                        Text(
                            text = block.language.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.secondary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(colors.surfaceContainer)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(12.dp)
                    ) {
                        Text(
                            text = block.code,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                lineHeight = 18.sp
                            ),
                            color = colors.onSurface
                        )
                    }
                }
            }
        }

        is MarkdownBlock.MermaidBlock -> {
            MermaidDiagramCard(
                rawCode = block.rawCode,
                diagram = block.diagram
            )
        }

        MarkdownBlock.HorizontalRule -> {
            HorizontalDivider(
                color = colors.outline,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun MarkdownTableCard(block: MarkdownBlock.Table) {
    val colors = MaterialTheme.colorScheme
    val colCount = maxOf(
        block.headers.size,
        block.rows.maxOfOrNull { it.size } ?: 0
    ).coerceAtLeast(1)
    val colWidth = if (colCount <= 2) 170.dp else 148.dp

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surfaceContainer,
        border = BorderStroke(1.dp, colors.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.horizontalScroll(rememberScrollState())) {
            // Header row
            Row(
                modifier = Modifier
                    .background(colors.surfaceContainerHigh)
                    .padding(vertical = 8.dp)
            ) {
                for (c in 0 until colCount) {
                    val text = block.headers.getOrElse(c) { "" }
                    val align = toTextAlign(block.alignments.getOrElse(c) { TableAlignment.START })
                    Text(
                        text = rememberInlineMarkdown(text, colors),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = colors.primary,
                        textAlign = align,
                        modifier = Modifier
                            .width(colWidth)
                            .padding(horizontal = 10.dp)
                    )
                }
            }

            HorizontalDivider(color = colors.outline)

            block.rows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .background(
                            if (rowIndex % 2 == 0) colors.surface else colors.surfaceContainer
                        )
                        .padding(vertical = 8.dp)
                ) {
                    for (c in 0 until colCount) {
                        val cell = row.getOrElse(c) { "" }
                        val align = toTextAlign(block.alignments.getOrElse(c) { TableAlignment.START })
                        Text(
                            text = rememberInlineMarkdown(cell, colors),
                            style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
                            color = colors.onSurface,
                            textAlign = align,
                            modifier = Modifier
                                .width(colWidth)
                                .padding(horizontal = 10.dp)
                        )
                    }
                }
                if (rowIndex < block.rows.lastIndex) {
                    HorizontalDivider(color = colors.outline.copy(alpha = 0.5f))
                }
            }
        }
    }
}

private fun toTextAlign(alignment: TableAlignment): TextAlign = when (alignment) {
    TableAlignment.START -> TextAlign.Start
    TableAlignment.CENTER -> TextAlign.Center
    TableAlignment.END -> TextAlign.End
}

@Composable
private fun rememberInlineMarkdown(
    text: String,
    colors: ColorScheme
): AnnotatedString = remember(text, colors) {
    buildInlineMarkdownString(text, colors)
}

fun buildInlineMarkdownString(
    text: String,
    colors: ColorScheme
): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        when {
            text.startsWith("**", startIndex = i) -> {
                val close = text.indexOf("**", startIndex = i + 2)
                if (close > i + 2) {
                    val inner = text.substring(i + 2, close)
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = colors.onSurface))
                    appendInlineCodeOnly(inner, colors)
                    pop()
                    i = close + 2
                } else {
                    append("**")
                    i += 2
                }
            }

            text[i] == '`' -> {
                val close = text.indexOf('`', startIndex = i + 1)
                if (close > i + 1) {
                    val code = text.substring(i + 1, close)
                    pushStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            color = colors.primary,
                            background = colors.surfaceContainerHigh
                        )
                    )
                    append(" $code ")
                    pop()
                    i = close + 1
                } else {
                    append('`')
                    i++
                }
            }

            text[i] == '*' -> {
                val close = text.indexOf('*', startIndex = i + 1)
                if (close > i + 1) {
                    val inner = text.substring(i + 1, close)
                    pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    append(inner)
                    pop()
                    i = close + 1
                } else {
                    append('*')
                    i++
                }
            }

            else -> {
                append(text[i])
                i++
            }
        }
    }
}

private fun AnnotatedString.Builder.appendInlineCodeOnly(
    text: String,
    colors: ColorScheme
) {
    var i = 0
    while (i < text.length) {
        if (text[i] == '`') {
            val close = text.indexOf('`', startIndex = i + 1)
            if (close > i + 1) {
                pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        color = colors.primary,
                        background = colors.surfaceContainerHigh
                    )
                )
                append(" ${text.substring(i + 1, close)} ")
                pop()
                i = close + 1
                continue
            }
        }
        append(text[i])
        i++
    }
}
