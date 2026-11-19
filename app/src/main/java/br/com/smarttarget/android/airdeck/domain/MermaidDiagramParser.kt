package br.com.smarttarget.android.airdeck.domain

object MermaidDiagramParser {

    fun parse(rawCode: String): MermaidDiagram {
        val lines = rawCode.lines()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() && !it.trimStart().startsWith("%%") }

        val firstLine = lines.firstOrNull()?.trim().orEmpty()
        return when {
            firstLine.startsWith("flowchart", ignoreCase = true) ||
                firstLine.startsWith("graph", ignoreCase = true) ->
                parseFlowchart(firstLine, lines.drop(1))

            firstLine.startsWith("stateDiagram", ignoreCase = true) ->
                parseStateDiagram(lines.drop(1))

            firstLine.startsWith("sequenceDiagram", ignoreCase = true) ->
                parseSequenceDiagram(lines.drop(1))

            else -> MermaidDiagram.Generic(
                header = firstLine.ifBlank { "Mermaid" },
                lines = lines
            )
        }
    }

    private fun parseFlowchart(header: String, bodyLines: List<String>): MermaidDiagram.Flowchart {
        val direction = header.split(Regex("\\s+")).getOrNull(1)?.uppercase() ?: "TB"
        val subgraphs = mutableListOf<MermaidSubgraph>()
        val nodeMap = linkedMapOf<String, MermaidNode>()
        val edges = mutableListOf<MermaidEdge>()
        val subgraphStack = ArrayDeque<String>()

        fun registerNodeToken(token: String): String {
            val parsed = parseFlowchartNodeToken(token, subgraphStack.lastOrNull())
            val existing = nodeMap[parsed.id]
            if (existing == null) {
                nodeMap[parsed.id] = parsed
            } else {
                val upgradedLabel = if (existing.label == existing.id && parsed.label != parsed.id) {
                    parsed.label
                } else {
                    existing.label
                }
                val upgradedShape = if (existing.shape == MermaidNodeShape.RECTANGLE) parsed.shape else existing.shape
                val upgradedSubgraph = existing.subgraphId ?: parsed.subgraphId
                nodeMap[parsed.id] = existing.copy(
                    label = upgradedLabel,
                    shape = upgradedShape,
                    subgraphId = upgradedSubgraph
                )
            }
            return parsed.id
        }

        for (raw in bodyLines) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line == "end") {
                if (subgraphStack.isNotEmpty()) subgraphStack.removeLast()
                continue
            }
            if (line.startsWith("subgraph ", ignoreCase = true)) {
                val rest = line.removePrefix("subgraph ").trim()
                val sub = parseSubgraphHeader(rest)
                subgraphs.add(sub)
                subgraphStack.addLast(sub.id)
                continue
            }

            val edgeMatch = findFlowchartEdgeOperator(line)
            if (edgeMatch != null) {
                val leftExpr = line.substring(0, edgeMatch.startIndex).trim()
                var rightExpr = line.substring(edgeMatch.endIndex).trim()
                var edgeLabel = ""
                if (rightExpr.startsWith("|")) {
                    val closingPipe = rightExpr.indexOf('|', startIndex = 1)
                    if (closingPipe > 1) {
                        edgeLabel = normalizeLineBreaks(rightExpr.substring(1, closingPipe).trim())
                        rightExpr = rightExpr.substring(closingPipe + 1).trim()
                    }
                }
                val sourceIds = splitAmpersandOutsideBrackets(leftExpr).map(::registerNodeToken)
                val targetIds = splitAmpersandOutsideBrackets(rightExpr).map(::registerNodeToken)
                for (src in sourceIds) {
                    for (dst in targetIds) {
                        edges.add(
                            MermaidEdge(
                                fromId = src,
                                toId = dst,
                                label = edgeLabel,
                                bidirectional = edgeMatch.bidirectional,
                                dashed = edgeMatch.dashed
                            )
                        )
                    }
                }
            } else if (!line.startsWith("classDef", ignoreCase = true) &&
                !line.startsWith("class ", ignoreCase = true) &&
                !line.startsWith("style ", ignoreCase = true) &&
                !line.startsWith("linkStyle ", ignoreCase = true)
            ) {
                registerNodeToken(line)
            }
        }

        return MermaidDiagram.Flowchart(
            direction = direction,
            subgraphs = subgraphs,
            nodes = nodeMap.values.toList(),
            edges = edges
        )
    }

    private fun parseSubgraphHeader(rest: String): MermaidSubgraph {
        val bracketIdx = rest.indexOf('[')
        return if (bracketIdx > 0 && rest.endsWith("]")) {
            val id = rest.substring(0, bracketIdx).trim()
            val rawTitle = rest.substring(bracketIdx + 1, rest.length - 1).trim().trim('"')
            MermaidSubgraph(id = id, title = normalizeLineBreaks(rawTitle))
        } else {
            val clean = rest.trim('"')
            MermaidSubgraph(id = clean.replace(" ", "_"), title = clean)
        }
    }

    private fun parseFlowchartNodeToken(token: String, subgraphId: String?): MermaidNode {
        val trimmed = token.trim()
        // Double-circle ((...))
        val doubleCircleIdx = trimmed.indexOf("((")
        if (doubleCircleIdx > 0 && trimmed.endsWith("))")) {
            val id = trimmed.substring(0, doubleCircleIdx).trim()
            val label = trimmed.substring(doubleCircleIdx + 2, trimmed.length - 2).trim().trim('"')
            return MermaidNode(id, normalizeLineBreaks(label), MermaidNodeShape.CIRCLE, subgraphId)
        }
        // Bracket [...]
        val bracketIdx = trimmed.indexOf('[')
        if (bracketIdx > 0 && trimmed.endsWith("]")) {
            val id = trimmed.substring(0, bracketIdx).trim()
            val inner = trimmed.substring(bracketIdx + 1, trimmed.length - 1).trim()
            val label = inner.removePrefix("(").removeSuffix(")").trim().trim('"')
            return MermaidNode(id, normalizeLineBreaks(label), MermaidNodeShape.RECTANGLE, subgraphId)
        }
        // Rounded (...)
        val parenIdx = trimmed.indexOf('(')
        if (parenIdx > 0 && trimmed.endsWith(")")) {
            val id = trimmed.substring(0, parenIdx).trim()
            val label = trimmed.substring(parenIdx + 1, trimmed.length - 1).trim().trim('"')
            return MermaidNode(id, normalizeLineBreaks(label), MermaidNodeShape.ROUNDED, subgraphId)
        }
        // Diamond {...}
        val braceIdx = trimmed.indexOf('{')
        if (braceIdx > 0 && trimmed.endsWith("}")) {
            val id = trimmed.substring(0, braceIdx).trim()
            val label = trimmed.substring(braceIdx + 1, trimmed.length - 1).trim().trim('"')
            return MermaidNode(id, normalizeLineBreaks(label), MermaidNodeShape.DIAMOND, subgraphId)
        }
        return MermaidNode(trimmed, trimmed, MermaidNodeShape.RECTANGLE, subgraphId)
    }

    private data class EdgeOpMatch(
        val startIndex: Int,
        val endIndex: Int,
        val bidirectional: Boolean,
        val dashed: Boolean
    )

    private fun findFlowchartEdgeOperator(line: String): EdgeOpMatch? {
        val operators = listOf(
            "<-->" to (true to false),
            "-.->" to (false to true),
            "==>" to (false to false),
            "-->" to (false to false),
            "---" to (false to false)
        )
        var depthBracket = 0
        var depthParen = 0
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val ch = line[i]
            if (ch == '"') inQuotes = !inQuotes
            if (!inQuotes) {
                when (ch) {
                    '[' -> depthBracket++
                    ']' -> if (depthBracket > 0) depthBracket--
                    '(' -> depthParen++
                    ')' -> if (depthParen > 0) depthParen--
                }
                if (depthBracket == 0 && depthParen == 0) {
                    for ((op, flags) in operators) {
                        if (line.startsWith(op, startIndex = i)) {
                            return EdgeOpMatch(
                                startIndex = i,
                                endIndex = i + op.length,
                                bidirectional = flags.first,
                                dashed = flags.second
                            )
                        }
                    }
                }
            }
            i++
        }
        return null
    }

    private fun splitAmpersandOutsideBrackets(expr: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var depthBracket = 0
        var depthParen = 0
        var inQuotes = false
        for (ch in expr) {
            if (ch == '"') inQuotes = !inQuotes
            if (!inQuotes) {
                when (ch) {
                    '[' -> depthBracket++
                    ']' -> if (depthBracket > 0) depthBracket--
                    '(' -> depthParen++
                    ')' -> if (depthParen > 0) depthParen--
                }
            }
            if (!inQuotes && depthBracket == 0 && depthParen == 0 && ch == '&') {
                val part = current.toString().trim()
                if (part.isNotEmpty()) result.add(part)
                current.clear()
            } else {
                current.append(ch)
            }
        }
        val tail = current.toString().trim()
        if (tail.isNotEmpty()) result.add(tail)
        return result
    }

    private fun parseStateDiagram(bodyLines: List<String>): MermaidDiagram.StateDiagram {
        val groups = mutableListOf<MermaidStateGroup>()
        val stateMap = linkedMapOf<String, MermaidStateNode>()
        val transitions = mutableListOf<MermaidTransition>()
        val groupStack = ArrayDeque<String>()

        fun ensureState(rawId: String): String {
            val groupId = groupStack.lastOrNull()
            val normalizedId = if (rawId == "[*]") {
                if (groupId != null) "${groupId}__start" else "__root_start__"
            } else {
                rawId
            }
            if (!stateMap.containsKey(normalizedId)) {
                stateMap[normalizedId] = MermaidStateNode(
                    id = normalizedId,
                    label = if (rawId == "[*]") "Início" else rawId,
                    isInitialOrTerminal = rawId == "[*]",
                    parentGroupId = groupId
                )
            } else if (groupId != null && stateMap[normalizedId]?.parentGroupId == null) {
                stateMap[normalizedId] = stateMap[normalizedId]!!.copy(parentGroupId = groupId)
            }
            return normalizedId
        }

        for (raw in bodyLines) {
            val line = raw.trim()
            if (line.isEmpty()) continue
            if (line == "}") {
                if (groupStack.isNotEmpty()) groupStack.removeLast()
                continue
            }
            if (line.startsWith("state ", ignoreCase = true) && line.endsWith("{")) {
                val groupName = line.removePrefix("state ").removeSuffix("{").trim().trim('"')
                groups.add(MermaidStateGroup(id = groupName, title = groupName))
                groupStack.addLast(groupName)
                continue
            }
            val arrowIndex = line.indexOf("-->")
            if (arrowIndex > 0) {
                val rawFrom = line.substring(0, arrowIndex).trim()
                val rest = line.substring(arrowIndex + 3).trim()
                val colonIndex = rest.indexOf(':')
                val rawTo: String
                val label: String
                if (colonIndex > 0) {
                    rawTo = rest.substring(0, colonIndex).trim()
                    label = rest.substring(colonIndex + 1).trim()
                } else {
                    rawTo = rest
                    label = ""
                }
                val fromId = ensureState(rawFrom)
                val toId = ensureState(rawTo)
                transitions.add(MermaidTransition(fromId = fromId, toId = toId, label = label))
            }
        }

        return MermaidDiagram.StateDiagram(
            groups = groups,
            states = stateMap.values.toList(),
            transitions = transitions
        )
    }

    private fun parseSequenceDiagram(bodyLines: List<String>): MermaidDiagram.SequenceDiagram {
        val participants = linkedMapOf<String, MermaidParticipant>()
        val items = mutableListOf<MermaidSequenceItem>()

        fun ensureParticipant(id: String, label: String = id, isActor: Boolean = false) {
            val existing = participants[id]
            if (existing == null) {
                participants[id] = MermaidParticipant(id = id, label = label, isActor = isActor)
            } else if (label != id && existing.label == existing.id) {
                participants[id] = existing.copy(label = label, isActor = isActor || existing.isActor)
            }
        }

        val arrowPatterns = listOf("-->>", "->>", "--x", "-x", "-->", "->")

        for (raw in bodyLines) {
            val line = raw.trim()
            if (line.isEmpty()) continue

            when {
                line.startsWith("actor ", ignoreCase = true) ||
                    line.startsWith("participant ", ignoreCase = true) -> {
                    val isActor = line.startsWith("actor ", ignoreCase = true)
                    val decl = line.substringAfter(' ').trim()
                    val asIdx = decl.indexOf(" as ", ignoreCase = true)
                    if (asIdx > 0) {
                        val id = decl.substring(0, asIdx).trim()
                        val label = decl.substring(asIdx + 4).trim()
                        ensureParticipant(id, label, isActor)
                    } else {
                        ensureParticipant(decl, decl, isActor)
                    }
                }

                line.startsWith("Note ", ignoreCase = true) -> {
                    val colonIdx = line.indexOf(':')
                    if (colonIdx > 0) {
                        val header = line.substring(5, colonIdx).trim()
                        val text = line.substring(colonIdx + 1).trim()
                        val targetExpr = header
                            .removePrefix("over ")
                            .removePrefix("left of ")
                            .removePrefix("right of ")
                            .trim()
                        val parts = targetExpr.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        val firstId = parts.firstOrNull() ?: participants.keys.firstOrNull().orEmpty()
                        val lastId = parts.lastOrNull() ?: firstId
                        if (firstId.isNotEmpty()) ensureParticipant(firstId)
                        if (lastId.isNotEmpty()) ensureParticipant(lastId)
                        items.add(MermaidSequenceItem.Note(firstId, lastId, text))
                    }
                }

                line.startsWith("alt ", ignoreCase = true) -> {
                    items.add(MermaidSequenceItem.BlockHeader("ALT", line.substring(4).trim()))
                }

                line.startsWith("else ", ignoreCase = true) || line.equals("else", ignoreCase = true) -> {
                    items.add(MermaidSequenceItem.BlockHeader("ELSE", line.drop(4).trim()))
                }

                line.startsWith("opt ", ignoreCase = true) -> {
                    items.add(MermaidSequenceItem.BlockHeader("OPT", line.substring(4).trim()))
                }

                line.startsWith("loop ", ignoreCase = true) -> {
                    items.add(MermaidSequenceItem.BlockHeader("LOOP", line.substring(5).trim()))
                }

                line.equals("end", ignoreCase = true) -> {
                    items.add(MermaidSequenceItem.BlockEnd)
                }

                else -> {
                    val colonIdx = line.indexOf(':')
                    val msgHeader = if (colonIdx > 0) line.substring(0, colonIdx).trim() else line
                    val msgText = if (colonIdx > 0) line.substring(colonIdx + 1).trim() else ""
                    val matchedOp = arrowPatterns.firstOrNull { msgHeader.contains(it) }
                    if (matchedOp != null) {
                        val opIndex = msgHeader.indexOf(matchedOp)
                        val fromId = msgHeader.substring(0, opIndex).trim()
                        val toId = msgHeader.substring(opIndex + matchedOp.length).trim()
                        ensureParticipant(fromId)
                        ensureParticipant(toId)
                        items.add(
                            MermaidSequenceItem.Message(
                                fromId = fromId,
                                toId = toId,
                                text = msgText,
                                dashed = matchedOp.startsWith("--"),
                                isLost = matchedOp.endsWith("x")
                            )
                        )
                    }
                }
            }
        }

        return MermaidDiagram.SequenceDiagram(
            participants = participants.values.toList(),
            items = items
        )
    }

    private fun normalizeLineBreaks(text: String): String =
        text.replace("<br/>", "\n")
            .replace("<br />", "\n")
            .replace("<br>", "\n")
}
