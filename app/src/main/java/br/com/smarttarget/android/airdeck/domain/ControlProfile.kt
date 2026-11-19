package br.com.smarttarget.android.airdeck.domain

import br.com.smarttarget.android.airdeck.hid.HidConstants
import br.com.smarttarget.android.airdeck.hid.KeyboardLayout
import br.com.smarttarget.android.airdeck.ui.ControlMode

enum class ProfileIconType(val label: String) {
    WORK("Trabalho"),
    GAMING("Jogos"),
    MEDIA("Mídia / TV"),
    PRESENTATION("Apresentação"),
    CUSTOM("Personalizado")
}

enum class ButtonActionType(val label: String) {
    KEY_COMBO("Atalho de Teclado"),
    CONSUMER_MEDIA("Mídia / TV"),
    MOUSE_CLICK("Clique de Mouse")
}

data class CustomActionButton(
    val id: String,
    val label: String,
    val actionType: ButtonActionType,
    val modifiers: Int = HidConstants.MOD_NONE,
    val keyCode: Int = HidConstants.KEY_NONE,
    val consumerMask: Int = HidConstants.CONSUMER_NONE,
    val mouseMask: Int = HidConstants.MOUSE_NONE
)

data class ControlProfile(
    val id: String,
    val name: String,
    val iconType: ProfileIconType = ProfileIconType.CUSTOM,
    val defaultControlMode: ControlMode = ControlMode.TRACKPAD,
    val keyboardLayout: KeyboardLayout = KeyboardLayout.ABNT2,
    val pointerSensitivity: Float = 1.4f,
    val scrollSensitivity: Float = 1.2f,
    val airMouseSensitivity: Float = 1.5f,
    val naturalScroll: Boolean = true,
    val tapToClick: Boolean = true,
    val twoFingerRightClick: Boolean = true,
    val hapticFeedback: Boolean = true,
    val showMouseButtons: Boolean = true,
    val swapMouseButtons: Boolean = false,
    val customButtons: List<CustomActionButton> = emptyList(),
    val swipeGestures: Map<SwipeGesturePattern, NavigationAction> = emptyMap()
)

data class ActionTemplate(
    val id: String,
    val label: String,
    val description: String,
    val actionType: ButtonActionType,
    val modifiers: Int = HidConstants.MOD_NONE,
    val keyCode: Int = HidConstants.KEY_NONE,
    val consumerMask: Int = HidConstants.CONSUMER_NONE,
    val mouseMask: Int = HidConstants.MOUSE_NONE
) {
    fun toButton(buttonId: String, customLabel: String = label): CustomActionButton =
        CustomActionButton(
            id = buttonId,
            label = customLabel.ifBlank { label },
            actionType = actionType,
            modifiers = modifiers,
            keyCode = keyCode,
            consumerMask = consumerMask,
            mouseMask = mouseMask
        )
}

object ProfilePresets {

    val availableActionTemplates: List<ActionTemplate> = listOf(
        ActionTemplate("copy", "Copiar", "Ctrl+C", ButtonActionType.KEY_COMBO, HidConstants.MOD_LEFT_CTRL, HidConstants.KEY_C),
        ActionTemplate("paste", "Colar", "Ctrl+V", ButtonActionType.KEY_COMBO, HidConstants.MOD_LEFT_CTRL, HidConstants.KEY_V),
        ActionTemplate("undo", "Desfazer", "Ctrl+Z", ButtonActionType.KEY_COMBO, HidConstants.MOD_LEFT_CTRL, HidConstants.KEY_Z),
        ActionTemplate("alt_tab", "Alt+Tab", "Alternar janela", ButtonActionType.KEY_COMBO, HidConstants.MOD_LEFT_ALT, HidConstants.KEY_TAB),
        ActionTemplate("space", "Espaço", "Tecla Espaço / Pular", ButtonActionType.KEY_COMBO, HidConstants.MOD_NONE, HidConstants.KEY_SPACE),
        ActionTemplate("enter", "Enter", "Confirmar", ButtonActionType.KEY_COMBO, HidConstants.MOD_NONE, HidConstants.KEY_ENTER),
        ActionTemplate("esc", "Esc", "Menu / Sair", ButtonActionType.KEY_COMBO, HidConstants.MOD_NONE, HidConstants.KEY_ESCAPE),
        ActionTemplate("f5", "F5", "Iniciar / Atualizar", ButtonActionType.KEY_COMBO, HidConstants.MOD_NONE, HidConstants.KEY_F5),
        ActionTemplate("reload_r", "R", "Recarregar (Jogos)", ButtonActionType.KEY_COMBO, HidConstants.MOD_NONE, HidConstants.KEY_R),
        ActionTemplate("interact_e", "E", "Interagir (Jogos)", ButtonActionType.KEY_COMBO, HidConstants.MOD_NONE, HidConstants.KEY_E),
        ActionTemplate("crouch_ctrl", "Ctrl", "Agachar / Modificador", ButtonActionType.KEY_COMBO, HidConstants.MOD_LEFT_CTRL, HidConstants.KEY_NONE),
        ActionTemplate("play_pause", "Play/Pause", "Mídia Play/Pause", ButtonActionType.CONSUMER_MEDIA, consumerMask = HidConstants.CONSUMER_PLAY_PAUSE),
        ActionTemplate("vol_up", "Vol +", "Aumentar Volume", ButtonActionType.CONSUMER_MEDIA, consumerMask = HidConstants.CONSUMER_VOL_UP),
        ActionTemplate("vol_down", "Vol -", "Diminuir Volume", ButtonActionType.CONSUMER_MEDIA, consumerMask = HidConstants.CONSUMER_VOL_DOWN),
        ActionTemplate("mute", "Mudo", "Silenciar Áudio", ButtonActionType.CONSUMER_MEDIA, consumerMask = HidConstants.CONSUMER_MUTE),
        ActionTemplate("middle_click", "Botão Meio", "Clique do Meio", ButtonActionType.MOUSE_CLICK, mouseMask = HidConstants.MOUSE_MIDDLE)
    )

    fun defaultProfiles(): List<ControlProfile> = listOf(
        ControlProfile(
            id = "preset_productivity",
            name = "Geral",
            iconType = ProfileIconType.WORK,
            defaultControlMode = ControlMode.TRACKPAD,
            keyboardLayout = KeyboardLayout.ABNT2,
            pointerSensitivity = 1.4f,
            scrollSensitivity = 1.2f,
            airMouseSensitivity = 1.4f,
            naturalScroll = true,
            tapToClick = true,
            twoFingerRightClick = true,
            hapticFeedback = true,
            showMouseButtons = true,
            swapMouseButtons = false,
            customButtons = listOf(
                availableActionTemplates[0].toButton("btn_copy"),
                availableActionTemplates[1].toButton("btn_paste"),
                availableActionTemplates[2].toButton("btn_undo"),
                availableActionTemplates[3].toButton("btn_alttab")
            ),
            swipeGestures = SwipeGesturePresets.defaultProductivityGestures()
        ),
        ControlProfile(
            id = "preset_gaming",
            name = "Jogos",
            iconType = ProfileIconType.GAMING,
            defaultControlMode = ControlMode.TRACKPAD,
            keyboardLayout = KeyboardLayout.US_INTL,
            pointerSensitivity = 2.1f,
            scrollSensitivity = 1.5f,
            airMouseSensitivity = 2.2f,
            naturalScroll = false,
            tapToClick = true,
            twoFingerRightClick = false,
            hapticFeedback = true,
            showMouseButtons = true,
            swapMouseButtons = false,
            customButtons = listOf(
                availableActionTemplates[4].toButton("btn_space", "Pular"),
                availableActionTemplates[8].toButton("btn_reload", "Recarregar"),
                availableActionTemplates[9].toButton("btn_interact", "Usar (E)"),
                availableActionTemplates[6].toButton("btn_esc", "Pause")
            ),
            swipeGestures = SwipeGesturePresets.defaultGamingGestures()
        ),
        ControlProfile(
            id = "preset_tv",
            name = "TV",
            iconType = ProfileIconType.MEDIA,
            defaultControlMode = ControlMode.MEDIA_REMOTE,
            keyboardLayout = KeyboardLayout.ABNT2,
            pointerSensitivity = 1.2f,
            scrollSensitivity = 1.0f,
            airMouseSensitivity = 1.3f,
            naturalScroll = true,
            tapToClick = true,
            twoFingerRightClick = true,
            hapticFeedback = true,
            showMouseButtons = false,
            swapMouseButtons = false,
            customButtons = listOf(
                availableActionTemplates[11].toButton("btn_play"),
                availableActionTemplates[13].toButton("btn_voldown"),
                availableActionTemplates[12].toButton("btn_volup"),
                availableActionTemplates[14].toButton("btn_mute")
            ),
            swipeGestures = SwipeGesturePresets.defaultTvGestures()
        )
    )
}

object ControlProfileCodec {
    private const val FIELD_SEP = "\u001F"
    private const val BTN_SEP = "\u001E"
    private const val BTN_FIELD_SEP = "\u001D"
    private const val PROFILE_SEP = "\u001C"
    private const val GESTURE_SEP = "\u001B"
    private const val GESTURE_KV_SEP = "\u001A"

    fun encodeList(profiles: List<ControlProfile>): String {
        return profiles.joinToString(PROFILE_SEP) { encodeProfile(it) }
    }

    fun decodeList(raw: String): List<ControlProfile> {
        if (raw.isBlank()) return emptyList()
        return raw.split(PROFILE_SEP).mapNotNull { decodeProfile(it) }
    }

    private fun encodeProfile(p: ControlProfile): String {
        val buttonsEncoded = p.customButtons.joinToString(BTN_SEP) { b ->
            listOf(
                sanitize(b.id),
                sanitize(b.label),
                b.actionType.name,
                b.modifiers.toString(),
                b.keyCode.toString(),
                b.consumerMask.toString(),
                b.mouseMask.toString()
            ).joinToString(BTN_FIELD_SEP)
        }
        val gesturesEncoded = p.swipeGestures.entries
            .filter { it.value != NavigationAction.NONE }
            .joinToString(GESTURE_SEP) { (pattern, action) ->
                "${pattern.name}$GESTURE_KV_SEP${action.name}"
            }
        return listOf(
            sanitize(p.id),
            sanitize(p.name),
            p.iconType.name,
            p.defaultControlMode.name,
            p.keyboardLayout.name,
            p.pointerSensitivity.toString(),
            p.scrollSensitivity.toString(),
            p.airMouseSensitivity.toString(),
            p.naturalScroll.toString(),
            p.tapToClick.toString(),
            p.twoFingerRightClick.toString(),
            p.hapticFeedback.toString(),
            p.showMouseButtons.toString(),
            p.swapMouseButtons.toString(),
            buttonsEncoded,
            gesturesEncoded
        ).joinToString(FIELD_SEP)
    }

    private fun decodeProfile(raw: String): ControlProfile? {
        val parts = raw.split(FIELD_SEP)
        if (parts.size < 15) return null
        return runCatching {
            val buttons = if (parts[14].isBlank()) {
                emptyList()
            } else {
                parts[14].split(BTN_SEP).mapNotNull { bRaw ->
                    val bf = bRaw.split(BTN_FIELD_SEP)
                    if (bf.size < 7) null
                    else CustomActionButton(
                        id = bf[0],
                        label = bf[1],
                        actionType = ButtonActionType.valueOf(bf[2]),
                        modifiers = bf[3].toInt(),
                        keyCode = bf[4].toInt(),
                        consumerMask = bf[5].toInt(),
                        mouseMask = bf[6].toInt()
                    )
                }
            }
            val gestures = if (parts.size >= 16 && parts[15].isNotBlank()) {
                parts[15].split(GESTURE_SEP).mapNotNull { entryRaw ->
                    val kv = entryRaw.split(GESTURE_KV_SEP)
                    if (kv.size < 2) null
                    else {
                        val pattern = runCatching { SwipeGesturePattern.valueOf(kv[0]) }.getOrNull()
                        val action = runCatching { NavigationAction.valueOf(kv[1]) }.getOrNull()
                        if (pattern != null && action != null && action != NavigationAction.NONE) {
                            pattern to action
                        } else {
                            null
                        }
                    }
                }.toMap()
            } else {
                emptyMap()
            }
            ControlProfile(
                id = parts[0],
                name = parts[1],
                iconType = ProfileIconType.valueOf(parts[2]),
                defaultControlMode = ControlMode.valueOf(parts[3]),
                keyboardLayout = KeyboardLayout.valueOf(parts[4]),
                pointerSensitivity = parts[5].toFloat().coerceIn(0.4f, 3.5f),
                scrollSensitivity = parts[6].toFloat().coerceIn(0.4f, 3.5f),
                airMouseSensitivity = parts[7].toFloat().coerceIn(0.4f, 3.5f),
                naturalScroll = parts[8].toBoolean(),
                tapToClick = parts[9].toBoolean(),
                twoFingerRightClick = parts[10].toBoolean(),
                hapticFeedback = parts[11].toBoolean(),
                showMouseButtons = parts[12].toBoolean(),
                swapMouseButtons = parts[13].toBoolean(),
                customButtons = buttons,
                swipeGestures = gestures
            )
        }.getOrNull()
    }

    private fun sanitize(input: String): String {
        return input
            .replace(FIELD_SEP, " ")
            .replace(BTN_SEP, " ")
            .replace(BTN_FIELD_SEP, " ")
            .replace(PROFILE_SEP, " ")
            .replace(GESTURE_SEP, " ")
            .replace(GESTURE_KV_SEP, " ")
    }
}
