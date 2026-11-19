package br.com.smarttarget.android.airdeck.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.com.smarttarget.android.airdeck.domain.MermaidDiagram
import br.com.smarttarget.android.airdeck.domain.MermaidNode
import br.com.smarttarget.android.airdeck.domain.MermaidNodeShape
import br.com.smarttarget.android.airdeck.domain.MermaidSequenceItem
import br.com.smarttarget.android.airdeck.domain.MermaidStateNode
import br.com.smarttarget.android.airdeck.ui.theme.AmberWarning
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun MermaidDiagramCard(
    rawCode: String,
    diagram: MermaidDiagram,
    modifier: Modifier = Modifier
) {
    var showRawSource by remember { mutableStateOf(false) }
    var zoomScale by remember { mutableFloatStateOf(1f) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxWidth()
            .testTag("mermaid_diagram_card")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccountTree,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Mermaid · ${diagram.kindLabel}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!showRawSource) {
                        IconButton(
                            onClick = { zoomScale = (zoomScale - 0.15f).coerceAtLeast(0.75f) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ZoomOut,
                                contentDescription = "Reduzir zoom do diagrama",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Text(
                            text = "${(zoomScale * 100).roundToInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        IconButton(
                            onClick = { zoomScale = (zoomScale + 0.15f).coerceAtMost(1.45f) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ZoomIn,
                                contentDescription = "Ampliar zoom do diagrama",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Surface(
                        onClick = { showRawSource = !showRawSource },
                        shape = RoundedCornerShape(8.dp),
                        color = if (showRawSource) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            1.dp,
                            if (showRawSource) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("mermaid_toggle_source")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (showRawSource) Icons.Rounded.AccountTree else Icons.Rounded.Code,
                                contentDescription = null,
                                tint = if (showRawSource) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (showRawSource) "Visual" else "Código",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (showRawSource) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            }

            if (showRawSource) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    Text(
                        text = rawCode,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = FontFamily.Monospace,
                            lineHeight = 18.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(12.dp)
                ) {
                    when (diagram) {
                        is MermaidDiagram.Flowchart -> FlowchartDiagramCanvas(diagram, zoomScale)
                        is MermaidDiagram.StateDiagram -> StateDiagramCanvas(diagram, zoomScale)
                        is MermaidDiagram.SequenceDiagram -> SequenceDiagramCanvas(diagram, zoomScale)
                        is MermaidDiagram.Generic -> {
                            Text(
                                text = diagram.lines.joinToString("\n"),
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FlowchartDiagramCanvas(
    diagram: MermaidDiagram.Flowchart,
    zoomScale: Float
) {
    var rootCoords by remember(diagram) { mutableStateOf<LayoutCoordinates?>(null) }
    val nodeBounds = remember(diagram) { mutableStateMapOf<String, Rect>() }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val labelBgColor = MaterialTheme.colorScheme.surfaceContainerHigh

    val subgraphColors = listOf(primaryColor, secondaryColor, tertiaryColor, AmberWarning)
    val standaloneNodes = remember(diagram) {
        val groupedIds = diagram.subgraphs.map { it.id }.toSet()
        diagram.nodes.filter { it.subgraphId == null || it.subgraphId !in groupedIds }
    }

    Box(
        modifier = Modifier
            .widthIn(min = (520 * zoomScale).dp, max = (760 * zoomScale).dp)
            .onGloballyPositioned { rootCoords = it }
    ) {
        // Vector connectors underlay/overlay
        Canvas(modifier = Modifier.matchParentSize()) {
            diagram.edges.forEach { edge ->
                val fromRect = nodeBounds[edge.fromId] ?: return@forEach
                val toRect = nodeBounds[edge.toId] ?: return@forEach
                val strokeColor = if (edge.bidirectional) tertiaryColor else primaryColor.copy(alpha = 0.72f)
                drawFlowConnection(
                    fromRect = fromRect,
                    toRect = toRect,
                    color = strokeColor,
                    bidirectional = edge.bidirectional,
                    dashed = edge.dashed
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy((18 * zoomScale).dp)
        ) {
            diagram.subgraphs.forEachIndexed { idx, subgraph ->
                val accent = subgraphColors[idx % subgraphColors.size]
                val groupNodes = diagram.nodes.filter { it.subgraphId == subgraph.id }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                    border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding((12 * zoomScale).dp),
                        verticalArrangement = Arrangement.spacedBy((10 * zoomScale).dp)
                    ) {
                        Text(
                            text = subgraph.title,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = (12 * zoomScale).sp
                            ),
                            color = accent
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy((12 * zoomScale).dp),
                            verticalArrangement = Arrangement.spacedBy((12 * zoomScale).dp),
                            maxItemsInEachRow = 3
                        ) {
                            groupNodes.forEach { node ->
                                FlowchartNodeBox(
                                    node = node,
                                    accent = accent,
                                    zoomScale = zoomScale,
                                    onPositioned = { coords ->
                                        rootCoords?.let { root ->
                                            if (root.isAttached && coords.isAttached) {
                                                nodeBounds[node.id] = root.localBoundingBoxOf(coords, false)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (standaloneNodes.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy((14 * zoomScale).dp),
                    verticalArrangement = Arrangement.spacedBy((12 * zoomScale).dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    standaloneNodes.forEach { node ->
                        FlowchartNodeBox(
                            node = node,
                            accent = tertiaryColor,
                            zoomScale = zoomScale,
                            onPositioned = { coords ->
                                rootCoords?.let { root ->
                                    if (root.isAttached && coords.isAttached) {
                                        nodeBounds[node.id] = root.localBoundingBoxOf(coords, false)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }

        // Labeled edge pills positioned at curve midpoints
        diagram.edges.filter { it.label.isNotBlank() }.forEach { edge ->
            val fromRect = nodeBounds[edge.fromId]
            val toRect = nodeBounds[edge.toId]
            if (fromRect != null && toRect != null) {
                val midX = ((fromRect.center.x + toRect.center.x) / 2f).roundToInt()
                val midY = ((fromRect.center.y + toRect.center.y) / 2f).roundToInt()
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = labelBgColor.copy(alpha = 0.94f),
                    border = BorderStroke(1.dp, primaryColor.copy(alpha = 0.4f)),
                    modifier = Modifier.offset { IntOffset((midX - 58).coerceAtLeast(0), (midY - 12).coerceAtLeast(0)) }
                ) {
                    Text(
                        text = edge.label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = (10 * zoomScale).sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FlowchartNodeBox(
    node: MermaidNode,
    accent: Color,
    zoomScale: Float,
    onPositioned: (LayoutCoordinates) -> Unit
) {
    val shape = when (node.shape) {
        MermaidNodeShape.CIRCLE -> CircleShape
        MermaidNodeShape.ROUNDED -> RoundedCornerShape(20.dp)
        else -> RoundedCornerShape(10.dp)
    }
    val lines = node.label.lines()
    val title = lines.firstOrNull().orEmpty()
    val subtitle = lines.drop(1).joinToString("\n")

    Surface(
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = BorderStroke(
            width = if (node.shape == MermaidNodeShape.CIRCLE) 2.dp else 1.dp,
            color = accent.copy(alpha = 0.65f)
        ),
        modifier = Modifier
            .widthIn(min = (118 * zoomScale).dp, max = (205 * zoomScale).dp)
            .onGloballyPositioned(onPositioned)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = (10 * zoomScale).dp, vertical = (8 * zoomScale).dp),
            horizontalAlignment = if (node.shape == MermaidNodeShape.CIRCLE) Alignment.CenterHorizontally else Alignment.Start
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (11.5f * zoomScale).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = if (node.shape == MermaidNodeShape.CIRCLE) TextAlign.Center else TextAlign.Start
            )
            if (subtitle.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = (10 * zoomScale).sp,
                        lineHeight = (13 * zoomScale).sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = if (node.shape == MermaidNodeShape.CIRCLE) TextAlign.Center else TextAlign.Start
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StateDiagramCanvas(
    diagram: MermaidDiagram.StateDiagram,
    zoomScale: Float
) {
    var rootCoords by remember(diagram) { mutableStateOf<LayoutCoordinates?>(null) }
    val stateBounds = remember(diagram) { mutableStateMapOf<String, Rect>() }

    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error

    val groupIds = remember(diagram) { diagram.groups.map { it.id }.toSet() }
    val topStates = remember(diagram) { diagram.states.filter { it.parentGroupId == null || it.parentGroupId !in groupIds } }

    Box(
        modifier = Modifier
            .widthIn(min = (460 * zoomScale).dp, max = (680 * zoomScale).dp)
            .onGloballyPositioned { rootCoords = it }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            diagram.transitions.forEach { tr ->
                val fromRect = stateBounds[tr.fromId] ?: return@forEach
                val toRect = stateBounds[tr.toId] ?: return@forEach
                val color = when {
                    tr.toId.contains("Error", ignoreCase = true) || tr.toId.contains("Unsupported", ignoreCase = true) ->
                        errorColor.copy(alpha = 0.8f)
                    tr.toId.contains("Connected", ignoreCase = true) -> tertiaryColor
                    else -> primaryColor.copy(alpha = 0.8f)
                }
                drawFlowConnection(fromRect, toRect, color = color, bidirectional = false, dashed = false)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy((16 * zoomScale).dp)
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy((14 * zoomScale).dp),
                verticalArrangement = Arrangement.spacedBy((14 * zoomScale).dp)
            ) {
                topStates.forEach { stateNode ->
                    StateNodePill(
                        stateNode = stateNode,
                        zoomScale = zoomScale,
                        onPositioned = { coords ->
                            rootCoords?.let { root ->
                                if (root.isAttached && coords.isAttached) {
                                    stateBounds[stateNode.id] = root.localBoundingBoxOf(coords, false)
                                }
                            }
                        }
                    )
                }
            }

            diagram.groups.forEach { group ->
                val childStates = diagram.states.filter { it.parentGroupId == group.id }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.5.dp, AmberWarning.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coords ->
                            rootCoords?.let { root ->
                                if (root.isAttached && coords.isAttached) {
                                    stateBounds[group.id] = root.localBoundingBoxOf(coords, false)
                                }
                            }
                        }
                ) {
                    Column(
                        modifier = Modifier.padding((12 * zoomScale).dp),
                        verticalArrangement = Arrangement.spacedBy((10 * zoomScale).dp)
                    ) {
                        Text(
                            text = "state ${group.title}",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = (12 * zoomScale).sp
                            ),
                            color = AmberWarning
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy((12 * zoomScale).dp),
                            verticalArrangement = Arrangement.spacedBy((10 * zoomScale).dp)
                        ) {
                            childStates.forEach { child ->
                                StateNodePill(
                                    stateNode = child,
                                    zoomScale = zoomScale,
                                    accentOverride = AmberWarning,
                                    onPositioned = { coords ->
                                        rootCoords?.let { root ->
                                            if (root.isAttached && coords.isAttached) {
                                                stateBounds[child.id] = root.localBoundingBoxOf(coords, false)
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Concise transition legend below state blocks for full readability
            if (diagram.transitions.any { it.label.isNotBlank() }) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        diagram.transitions.filter { it.label.isNotBlank() }.forEach { tr ->
                            val fromName = if (tr.fromId.endsWith("start__") || tr.fromId.endsWith("__start")) "[*]" else tr.fromId
                            Text(
                                text = "$fromName → ${tr.toId}: ${tr.label}",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = (10.5f * zoomScale).sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StateNodePill(
    stateNode: MermaidStateNode,
    zoomScale: Float,
    accentOverride: Color? = null,
    onPositioned: (LayoutCoordinates) -> Unit
) {
    val accent = accentOverride ?: when {
        stateNode.isInitialOrTerminal -> MaterialTheme.colorScheme.primary
        stateNode.id.contains("Connected", ignoreCase = true) -> MaterialTheme.colorScheme.tertiary
        stateNode.id.contains("Error", ignoreCase = true) ||
            stateNode.id.contains("Unsupported", ignoreCase = true) -> MaterialTheme.colorScheme.error
        stateNode.id.contains("Permission", ignoreCase = true) ||
            stateNode.id.contains("Disabled", ignoreCase = true) -> AmberWarning
        else -> MaterialTheme.colorScheme.primary
    }

    if (stateNode.isInitialOrTerminal) {
        Box(
            modifier = Modifier
                .size((24 * zoomScale).dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.25f))
                .border(2.dp, accent, CircleShape)
                .onGloballyPositioned(onPositioned),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size((10 * zoomScale).dp)
                    .clip(CircleShape)
                    .background(accent)
            )
        }
    } else {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            border = BorderStroke(1.dp, accent.copy(alpha = 0.7f)),
            modifier = Modifier.onGloballyPositioned(onPositioned)
        ) {
            Text(
                text = stateNode.label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (11.5f * zoomScale).sp
                ),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = (12 * zoomScale).dp, vertical = (8 * zoomScale).dp)
            )
        }
    }
}

@Composable
private fun SequenceDiagramCanvas(
    diagram: MermaidDiagram.SequenceDiagram,
    zoomScale: Float
) {
    val participants = diagram.participants
    if (participants.isEmpty()) return

    val colWidth = (118 * zoomScale).dp
    val totalWidth = colWidth * participants.size
    val indexById = remember(participants) { participants.mapIndexed { i, p -> p.id to i }.toMap() }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val errorColor = MaterialTheme.colorScheme.error
    val lifelineColor = MaterialTheme.colorScheme.outlineVariant

    Box(modifier = Modifier.width(totalWidth)) {
        // Dashed vertical lifelines across columns
        Canvas(modifier = Modifier.matchParentSize()) {
            val colPx = size.width / participants.size
            val topStart = 52.dp.toPx()
            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            participants.indices.forEach { idx ->
                val x = colPx * idx + colPx / 2f
                drawLine(
                    color = lifelineColor,
                    start = Offset(x, topStart),
                    end = Offset(x, size.height),
                    strokeWidth = 1.5f,
                    pathEffect = dashEffect
                )
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy((8 * zoomScale).dp)
        ) {
            // Participant / Actor headers
            Row(modifier = Modifier.fillMaxWidth()) {
                participants.forEach { participant ->
                    Box(
                        modifier = Modifier.width(colWidth).padding(horizontal = 3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (participant.isActor) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            border = BorderStroke(
                                1.dp,
                                if (participant.isActor) primaryColor else MaterialTheme.colorScheme.outline
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (participant.isActor) {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = primaryColor,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                }
                                Text(
                                    text = participant.label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = (10 * zoomScale).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            diagram.items.forEach { item ->
                when (item) {
                    is MermaidSequenceItem.Note -> {
                        val startIdx = indexById[item.startParticipantId] ?: 0
                        val endIdx = indexById[item.endParticipantId] ?: startIdx
                        val minIdx = minOf(startIdx, endIdx)
                        val maxIdx = maxOf(startIdx, endIdx)
                        val spanCols = (maxIdx - minIdx + 1).coerceAtLeast(1)
                        Row(modifier = Modifier.fillMaxWidth()) {
                            if (minIdx > 0) {
                                Spacer(modifier = Modifier.width(colWidth * minIdx))
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                                border = BorderStroke(1.dp, secondaryColor.copy(alpha = 0.65f)),
                                modifier = Modifier
                                    .width(colWidth * spanCols)
                                    .padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    text = item.text,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Medium,
                                        fontSize = (10.5f * zoomScale).sp
                                    ),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    is MermaidSequenceItem.BlockHeader -> {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = AmberWarning.copy(alpha = 0.14f),
                            border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.6f)),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = "[${item.kind.lowercase()}] ${item.condition}",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = (10.5f * zoomScale).sp
                                ),
                                color = AmberWarning,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    MermaidSequenceItem.BlockEnd -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp)
                                .height(1.dp)
                                .background(AmberWarning.copy(alpha = 0.45f))
                        )
                    }

                    is MermaidSequenceItem.Message -> {
                        val fromIdx = indexById[item.fromId] ?: 0
                        val toIdx = indexById[item.toId] ?: 0
                        val arrowColor = when {
                            item.isLost -> errorColor
                            item.dashed -> tertiaryColor
                            else -> primaryColor
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((38 * zoomScale).dp)
                        ) {
                            Canvas(modifier = Modifier.matchParentSize()) {
                                val colPx = size.width / participants.size
                                val startX = colPx * fromIdx + colPx / 2f
                                val endX = colPx * toIdx + colPx / 2f
                                val y = size.height * 0.72f
                                val dash = if (item.dashed) {
                                    PathEffect.dashPathEffect(floatArrayOf(9f, 7f), 0f)
                                } else {
                                    null
                                }
                                if (fromIdx == toIdx) {
                                    val loopRect = Path().apply {
                                        moveTo(startX, y - 14f)
                                        lineTo(startX + colPx * 0.34f, y - 14f)
                                        lineTo(startX + colPx * 0.34f, y)
                                        lineTo(startX, y)
                                    }
                                    drawPath(
                                        path = loopRect,
                                        color = arrowColor,
                                        style = Stroke(width = 2.4f, pathEffect = dash)
                                    )
                                    drawArrowHead(Offset(startX + 10f, y), Offset(startX, y), arrowColor)
                                } else {
                                    drawLine(
                                        color = arrowColor,
                                        start = Offset(startX, y),
                                        end = Offset(endX, y),
                                        strokeWidth = 2.5f,
                                        pathEffect = dash,
                                        cap = StrokeCap.Round
                                    )
                                    if (item.isLost) {
                                        val r = 6f
                                        drawLine(arrowColor, Offset(endX - r, y - r), Offset(endX + r, y + r), 3f)
                                        drawLine(arrowColor, Offset(endX - r, y + r), Offset(endX + r, y - r), 3f)
                                    } else {
                                        drawArrowHead(Offset(startX, y), Offset(endX, y), arrowColor)
                                    }
                                }
                            }

                            val minC = minOf(fromIdx, toIdx)
                            val maxC = maxOf(fromIdx, toIdx)
                            val span = (maxC - minC + 1).coerceAtLeast(1)
                            Row(modifier = Modifier.fillMaxWidth()) {
                                if (minC > 0) Spacer(modifier = Modifier.width(colWidth * minC))
                                Box(
                                    modifier = Modifier
                                        .width(colWidth * span)
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.TopCenter
                                ) {
                                    Text(
                                        text = item.text,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontSize = (9.5f * zoomScale).sp
                                        ),
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawFlowConnection(
    fromRect: Rect,
    toRect: Rect,
    color: Color,
    bidirectional: Boolean,
    dashed: Boolean
) {
    val start = edgeAnchor(fromRect, toRect.center)
    val end = edgeAnchor(toRect, fromRect.center)
    val midY = (start.y + end.y) / 2f
    val path = Path().apply {
        moveTo(start.x, start.y)
        cubicTo(
            start.x, midY,
            end.x, midY,
            end.x, end.y
        )
    }
    val effect = if (dashed) PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f) else null
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 2.5f, cap = StrokeCap.Round, pathEffect = effect)
    )
    drawArrowHead(Offset(end.x, midY), end, color)
    if (bidirectional) {
        drawArrowHead(Offset(start.x, midY), start, color)
    }
}

private fun edgeAnchor(rect: Rect, towards: Offset): Offset {
    val dx = towards.x - rect.center.x
    val dy = towards.y - rect.center.y
    return if (kotlin.math.abs(dy) >= kotlin.math.abs(dx) * 0.65f) {
        if (dy >= 0f) Offset(rect.center.x, rect.bottom) else Offset(rect.center.x, rect.top)
    } else {
        if (dx >= 0f) Offset(rect.right, rect.center.y) else Offset(rect.left, rect.center.y)
    }
}

private fun DrawScope.drawArrowHead(from: Offset, tip: Offset, color: Color) {
    val angle = atan2(tip.y - from.y, tip.x - from.x).toDouble()
    val len = 12f
    val spread = PI / 7.0
    val p1 = Offset(
        x = tip.x - (len * cos(angle - spread)).toFloat(),
        y = tip.y - (len * sin(angle - spread)).toFloat()
    )
    val p2 = Offset(
        x = tip.x - (len * cos(angle + spread)).toFloat(),
        y = tip.y - (len * sin(angle + spread)).toFloat()
    )
    val path = Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        close()
    }
    drawPath(path = path, color = color)
}
