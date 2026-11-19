package br.com.smarttarget.android.airdeck

import br.com.smarttarget.android.airdeck.domain.MarkdownBlock
import br.com.smarttarget.android.airdeck.domain.MarkdownParser
import br.com.smarttarget.android.airdeck.domain.MermaidDiagram
import br.com.smarttarget.android.airdeck.domain.MermaidDiagramParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownAndMermaidParserTest {

    @Test
    fun `parses markdown headings tables lists and all three mermaid diagram kinds`() {
        val sample = """
            # Air Deck – Virtual Hardware

            > **Aplicativo:** `Air Deck`

            | Col A | Col B |
            | :--- | :---: |
            | `1` | Valor |

            - Item um
            1. Passo um

            ```mermaid
            flowchart TB
                subgraph UI["Camada UI"]
                    Header["StatusHeaderBar<br/>(Ajuda & Ajustes)"]
                end
                Host(("Host"))
                Header -->|clique| Host
            ```

            ```mermaid
            stateDiagram-v2
                [*] --> Registering: start()
                state AutoReconnecting {
                    [*] --> WaitingBackoff: 2s
                }
                AutoReconnecting --> Connected: ok
            ```

            ```mermaid
            sequenceDiagram
                actor User as Usuário
                participant UI as Compose UI
                Note over User,UI: Teste
                User->>UI: Toque Ajuda
                UI-->>User: Abre README.md
            ```
        """.trimIndent()

        val blocks = MarkdownParser.parse(sample)
        assertTrue(blocks.any { it is MarkdownBlock.Heading && it.level == 1 && it.text == "Air Deck – Virtual Hardware" })
        assertTrue(blocks.any { it is MarkdownBlock.Blockquote })
        assertTrue(blocks.any { it is MarkdownBlock.Table && it.headers == listOf("Col A", "Col B") })
        assertTrue(blocks.any { it is MarkdownBlock.ListItem && it.orderedNumber == 1 })

        val mermaidDiagrams = blocks.filterIsInstance<MarkdownBlock.MermaidBlock>().map { it.diagram }
        assertEquals(3, mermaidDiagrams.size)

        val flowchart = mermaidDiagrams[0] as MermaidDiagram.Flowchart
        assertEquals("TB", flowchart.direction)
        assertEquals(1, flowchart.subgraphs.size)
        assertTrue(flowchart.nodes.any { it.id == "Header" && it.label.contains("\n") })
        assertTrue(flowchart.edges.any { it.fromId == "Header" && it.toId == "Host" && it.label == "clique" })

        val stateDiagram = mermaidDiagrams[1] as MermaidDiagram.StateDiagram
        assertEquals(1, stateDiagram.groups.size)
        assertTrue(stateDiagram.transitions.any { it.toId == "Connected" && it.label == "ok" })

        val sequenceDiagram = mermaidDiagrams[2] as MermaidDiagram.SequenceDiagram
        assertEquals(2, sequenceDiagram.participants.size)
        assertEquals(3, sequenceDiagram.items.size)
    }

    @Test
    fun `parses multi-source ampersand edges in flowchart`() {
        val diagram = MermaidDiagramParser.parse(
            """
            flowchart TB
                Trackpad & Actions --> InputVM
                InputVM & RemoteVM <--> Store
            """.trimIndent()
        ) as MermaidDiagram.Flowchart

        assertEquals(4, diagram.edges.size)
        assertTrue(diagram.edges.any { it.fromId == "Trackpad" && it.toId == "InputVM" })
        assertTrue(diagram.edges.any { it.fromId == "RemoteVM" && it.toId == "Store" && it.bidirectional })
    }
}
