package br.com.smarttarget.android.airdeck.domain

import br.com.smarttarget.android.airdeck.hid.HidConstants

enum class SwipeGesturePattern(
    val label: String,
    val shortHint: String
) {
    THREE_FINGER_LEFT("Deslize 3 dedos ←", "3 dedos ←"),
    THREE_FINGER_RIGHT("Deslize 3 dedos →", "3 dedos →"),
    THREE_FINGER_UP("Deslize 3 dedos ↑", "3 dedos ↑"),
    THREE_FINGER_DOWN("Deslize 3 dedos ↓", "3 dedos ↓"),
    TWO_FINGER_SWIPE_LEFT("Deslize rápido 2 dedos ←", "2 dedos ←"),
    TWO_FINGER_SWIPE_RIGHT("Deslize rápido 2 dedos →", "2 dedos →")
}

enum class NavigationAction(
    val label: String,
    val badge: String,
    val modifiers: Int = HidConstants.MOD_NONE,
    val keyCode: Int = HidConstants.KEY_NONE,
    val consumerMask: Int = HidConstants.CONSUMER_NONE
) {
    NONE("Nenhuma ação", "—"),
    BROWSER_BACK("Voltar (Alt + ←)", "Voltar", HidConstants.MOD_LEFT_ALT, HidConstants.KEY_LEFT),
    BROWSER_FORWARD("Avançar (Alt + →)", "Avançar", HidConstants.MOD_LEFT_ALT, HidConstants.KEY_RIGHT),
    SWITCH_WINDOW_NEXT("Alternar Janela (Alt + Tab)", "Alt+Tab", HidConstants.MOD_LEFT_ALT, HidConstants.KEY_TAB),
    SWITCH_WINDOW_PREV(
        "Janela Anterior (Alt + Shift + Tab)",
        "Alt+Shift+Tab",
        HidConstants.MOD_LEFT_ALT or HidConstants.MOD_LEFT_SHIFT,
        HidConstants.KEY_TAB
    ),
    TASK_VIEW("Visão de Tarefas (Win + Tab)", "Win+Tab", HidConstants.MOD_LEFT_GUI, HidConstants.KEY_TAB),
    SHOW_DESKTOP("Área de Trabalho (Win + D)", "Win+D", HidConstants.MOD_LEFT_GUI, HidConstants.KEY_D),
    PAGE_UP("Página Acima (Page Up)", "PgUp", HidConstants.MOD_NONE, HidConstants.KEY_PAGE_UP),
    PAGE_DOWN("Página Abaixo (Page Down)", "PgDn", HidConstants.MOD_NONE, HidConstants.KEY_PAGE_DOWN),
    CLOSE_TAB("Fechar Aba (Ctrl + W)", "Ctrl+W", HidConstants.MOD_LEFT_CTRL, HidConstants.KEY_W),
    TV_HOME("Início / Home (TV)", "Home", consumerMask = HidConstants.CONSUMER_AC_HOME),
    TV_BACK("Voltar (TV / Android)", "Voltar TV", consumerMask = HidConstants.CONSUMER_AC_BACK),
    MEDIA_NEXT("Próxima Faixa", "Próx.", consumerMask = HidConstants.CONSUMER_NEXT),
    MEDIA_PREV("Faixa Anterior", "Ant.", consumerMask = HidConstants.CONSUMER_PREV)
}

data class GestureProfileDraft(
    val profileId: String = "",
    val profileName: String = "",
    val bindings: Map<SwipeGesturePattern, NavigationAction> = emptyMap(),
    val isDirty: Boolean = false
)

object SwipeGesturePresets {
    fun defaultProductivityGestures(): Map<SwipeGesturePattern, NavigationAction> = mapOf(
        SwipeGesturePattern.THREE_FINGER_LEFT to NavigationAction.BROWSER_BACK,
        SwipeGesturePattern.THREE_FINGER_RIGHT to NavigationAction.BROWSER_FORWARD,
        SwipeGesturePattern.THREE_FINGER_UP to NavigationAction.TASK_VIEW,
        SwipeGesturePattern.THREE_FINGER_DOWN to NavigationAction.SHOW_DESKTOP
    )

    fun defaultGamingGestures(): Map<SwipeGesturePattern, NavigationAction> = mapOf(
        SwipeGesturePattern.THREE_FINGER_UP to NavigationAction.SWITCH_WINDOW_NEXT,
        SwipeGesturePattern.THREE_FINGER_DOWN to NavigationAction.SHOW_DESKTOP
    )

    fun defaultTvGestures(): Map<SwipeGesturePattern, NavigationAction> = mapOf(
        SwipeGesturePattern.THREE_FINGER_LEFT to NavigationAction.TV_BACK,
        SwipeGesturePattern.THREE_FINGER_RIGHT to NavigationAction.MEDIA_NEXT,
        SwipeGesturePattern.THREE_FINGER_UP to NavigationAction.TV_HOME,
        SwipeGesturePattern.TWO_FINGER_SWIPE_LEFT to NavigationAction.MEDIA_PREV,
        SwipeGesturePattern.TWO_FINGER_SWIPE_RIGHT to NavigationAction.MEDIA_NEXT
    )

    fun defaultsForProfileId(profileId: String): Map<SwipeGesturePattern, NavigationAction> = when (profileId) {
        "preset_gaming" -> defaultGamingGestures()
        "preset_tv" -> defaultTvGestures()
        else -> defaultProductivityGestures()
    }
}
