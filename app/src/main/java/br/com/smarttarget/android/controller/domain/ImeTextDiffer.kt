package br.com.smarttarget.android.controller.domain

import br.com.smarttarget.android.controller.hid.HidConstants
import br.com.smarttarget.android.controller.hid.HidKeyMapper
import br.com.smarttarget.android.controller.hid.HidKeyStroke
import br.com.smarttarget.android.controller.hid.KeyboardLayout

data class ImeDiffResult(
    val strokes: List<HidKeyStroke>,
    val deletions: Int,
    val insertions: String,
    val consumedActiveModifier: Boolean
)

object ImeTextDiffer {

    fun computeDiff(
        oldText: String,
        newText: String,
        layout: KeyboardLayout,
        activeModifierMask: Int
    ): ImeDiffResult {
        if (oldText == newText) {
            return ImeDiffResult(emptyList(), 0, "", false)
        }

        val minLen = minOf(oldText.length, newText.length)
        var common = 0
        while (common < minLen && oldText[common] == newText[common]) {
            common++
        }

        val deletions = oldText.length - common
        val insertions = newText.substring(common)

        val strokes = mutableListOf<HidKeyStroke>()
        repeat(deletions) {
            strokes.add(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_BACKSPACE))
        }

        var consumedModifier = false
        if (insertions.isNotEmpty()) {
            if (activeModifierMask != 0 && insertions.length == 1) {
                val mapped = HidKeyMapper.mapCharToKeyStrokes(insertions[0], layout)
                mapped.forEach { stroke ->
                    strokes.add(HidKeyStroke(stroke.modifiers or activeModifierMask, stroke.keyCode))
                }
                consumedModifier = true
            } else {
                strokes.addAll(HidKeyMapper.mapTextToKeyStrokes(insertions, layout))
            }
        }

        return ImeDiffResult(
            strokes = strokes,
            deletions = deletions,
            insertions = insertions,
            consumedActiveModifier = consumedModifier
        )
    }
}
