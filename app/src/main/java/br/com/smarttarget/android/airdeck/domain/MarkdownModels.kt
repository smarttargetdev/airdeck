package br.com.smarttarget.android.airdeck.domain

enum class TableAlignment {
    START,
    CENTER,
    END
}

sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class Blockquote(val lines: List<String>) : MarkdownBlock
    data class ListItem(
        val indentLevel: Int,
        val text: String,
        val orderedNumber: Int? = null
    ) : MarkdownBlock
    data class Table(
        val headers: List<String>,
        val alignments: List<TableAlignment>,
        val rows: List<List<String>>
    ) : MarkdownBlock
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock
    data class MermaidBlock(val rawCode: String, val diagram: MermaidDiagram) : MarkdownBlock
    data object HorizontalRule : MarkdownBlock
}

enum class MermaidNodeShape {
    RECTANGLE,
    ROUNDED,
    CIRCLE,
    DIAMOND
}

data class MermaidNode(
    val id: String,
    val label: String,
    val shape: MermaidNodeShape = MermaidNodeShape.RECTANGLE,
    val subgraphId: String? = null
)

data class MermaidSubgraph(
    val id: String,
    val title: String
)

data class MermaidEdge(
    val fromId: String,
    val toId: String,
    val label: String = "",
    val bidirectional: Boolean = false,
    val dashed: Boolean = false
)

data class MermaidStateNode(
    val id: String,
    val label: String,
    val isInitialOrTerminal: Boolean = false,
    val parentGroupId: String? = null
)

data class MermaidStateGroup(
    val id: String,
    val title: String
)

data class MermaidTransition(
    val fromId: String,
    val toId: String,
    val label: String = ""
)

data class MermaidParticipant(
    val id: String,
    val label: String,
    val isActor: Boolean = false
)

sealed interface MermaidSequenceItem {
    data class Note(
        val startParticipantId: String,
        val endParticipantId: String,
        val text: String
    ) : MermaidSequenceItem

    data class Message(
        val fromId: String,
        val toId: String,
        val text: String,
        val dashed: Boolean,
        val isLost: Boolean
    ) : MermaidSequenceItem

    data class BlockHeader(
        val kind: String,
        val condition: String
    ) : MermaidSequenceItem

    data object BlockEnd : MermaidSequenceItem
}

sealed interface MermaidDiagram {
    val kindLabel: String

    data class Flowchart(
        val direction: String,
        val subgraphs: List<MermaidSubgraph>,
        val nodes: List<MermaidNode>,
        val edges: List<MermaidEdge>
    ) : MermaidDiagram {
        override val kindLabel: String = "Fluxograma ($direction)"
    }

    data class StateDiagram(
        val groups: List<MermaidStateGroup>,
        val states: List<MermaidStateNode>,
        val transitions: List<MermaidTransition>
    ) : MermaidDiagram {
        override val kindLabel: String = "Diagrama de Estados"
    }

    data class SequenceDiagram(
        val participants: List<MermaidParticipant>,
        val items: List<MermaidSequenceItem>
    ) : MermaidDiagram {
        override val kindLabel: String = "Diagrama de Sequência"
    }

    data class Generic(
        val header: String,
        val lines: List<String>
    ) : MermaidDiagram {
        override val kindLabel: String = header.ifBlank { "Mermaid" }
    }
}
