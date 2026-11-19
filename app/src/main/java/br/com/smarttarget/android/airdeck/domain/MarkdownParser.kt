package br.com.smarttarget.android.airdeck.domain

object MarkdownParser {

    private val orderedListRegex = Regex("^(\\s*)(\\d+)\\.\\s+(.*)$")
    private val unorderedListRegex = Regex("^(\\s*)[-*+]\\s+(.*)$")
    private val headingRegex = Regex("^(#{1,6})\\s+(.*)$")
    private val tableDividerRegex = Regex("^\\|?\\s*:?-+:?\\s*(\\|\\s*:?-+:?\\s*)+\\|?$")

    fun parse(markdown: String): List<MarkdownBlock> {
        val lines = markdown.replace("\r\n", "\n").lines()
        val blocks = mutableListOf<MarkdownBlock>()
        var i = 0

        while (i < lines.size) {
            val rawLine = lines[i]
            val trimmed = rawLine.trim()

            if (trimmed.isEmpty()) {
                i++
                continue
            }

            // Fenced code / Mermaid block
            if (trimmed.startsWith("```")) {
                val language = trimmed.removePrefix("```").trim().lowercase()
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                if (i < lines.size) i++ // consume closing ```
                val codeContent = codeLines.joinToString("\n")
                if (language == "mermaid") {
                    blocks.add(
                        MarkdownBlock.MermaidBlock(
                            rawCode = codeContent,
                            diagram = MermaidDiagramParser.parse(codeContent)
                        )
                    )
                } else {
                    blocks.add(MarkdownBlock.CodeBlock(language = language, code = codeContent))
                }
                continue
            }

            // Horizontal rule
            if (trimmed == "---" || trimmed == "***" || trimmed == "___") {
                blocks.add(MarkdownBlock.HorizontalRule)
                i++
                continue
            }

            // Heading
            val headingMatch = headingRegex.matchEntire(trimmed)
            if (headingMatch != null) {
                blocks.add(
                    MarkdownBlock.Heading(
                        level = headingMatch.groupValues[1].length,
                        text = headingMatch.groupValues[2].trim()
                    )
                )
                i++
                continue
            }

            // Blockquote
            if (trimmed.startsWith(">")) {
                val quoteLines = mutableListOf<String>()
                while (i < lines.size && lines[i].trim().startsWith(">")) {
                    quoteLines.add(lines[i].trim().removePrefix(">").trimStart())
                    i++
                }
                blocks.add(MarkdownBlock.Blockquote(quoteLines))
                continue
            }

            // Markdown Table
            if (trimmed.startsWith("|") && i + 1 < lines.size && tableDividerRegex.matches(lines[i + 1].trim())) {
                val headers = splitTableRow(trimmed)
                val alignments = splitTableRow(lines[i + 1].trim()).map(::parseAlignment)
                i += 2
                val rows = mutableListOf<List<String>>()
                while (i < lines.size) {
                    val rowLine = lines[i].trim()
                    if (!rowLine.startsWith("|")) break
                    rows.add(splitTableRow(rowLine))
                    i++
                }
                blocks.add(MarkdownBlock.Table(headers = headers, alignments = alignments, rows = rows))
                continue
            }

            // Ordered List Item
            val orderedMatch = orderedListRegex.matchEntire(rawLine)
            if (orderedMatch != null) {
                val indent = orderedMatch.groupValues[1].length / 2
                val number = orderedMatch.groupValues[2].toIntOrNull()
                val itemText = orderedMatch.groupValues[3].trim()
                blocks.add(MarkdownBlock.ListItem(indentLevel = indent, text = itemText, orderedNumber = number))
                i++
                continue
            }

            // Unordered List Item
            val unorderedMatch = unorderedListRegex.matchEntire(rawLine)
            if (unorderedMatch != null) {
                val indent = unorderedMatch.groupValues[1].length / 2
                val itemText = unorderedMatch.groupValues[2].trim()
                blocks.add(MarkdownBlock.ListItem(indentLevel = indent, text = itemText, orderedNumber = null))
                i++
                continue
            }

            // Paragraph (merge consecutive plain lines)
            val paragraphLines = mutableListOf<String>()
            while (i < lines.size) {
                val candidate = lines[i].trim()
                if (candidate.isEmpty() ||
                    candidate.startsWith("#") ||
                    candidate.startsWith(">") ||
                    candidate.startsWith("```") ||
                    candidate == "---" ||
                    candidate.startsWith("|") ||
                    orderedListRegex.matches(lines[i]) ||
                    unorderedListRegex.matches(lines[i])
                ) {
                    break
                }
                paragraphLines.add(candidate)
                i++
            }
            if (paragraphLines.isNotEmpty()) {
                blocks.add(MarkdownBlock.Paragraph(paragraphLines.joinToString(" ")))
            } else {
                i++
            }
        }

        return blocks
    }

    private fun splitTableRow(rowLine: String): List<String> =
        rowLine.trim()
            .removePrefix("|")
            .removeSuffix("|")
            .split("|")
            .map { it.trim() }

    private fun parseAlignment(cell: String): TableAlignment {
        val clean = cell.trim()
        return when {
            clean.startsWith(":") && clean.endsWith(":") -> TableAlignment.CENTER
            clean.endsWith(":") -> TableAlignment.END
            else -> TableAlignment.START
        }
    }
}
