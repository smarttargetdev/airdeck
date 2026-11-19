package br.com.smarttarget.android.airdeck.hid

enum class KeyboardLayout(val label: String) {
    ABNT2("Português"),
    US_INTL("Internacional")
}

data class HidKeyStroke(
    val modifiers: Int,
    val keyCode: Int
)

data class ShortcutPreset(
    val id: String,
    val title: String,
    val badge: String,
    val modifiers: Int,
    val keyCode: Int
)

object HidKeyMapper {

    val shortcutPresets: List<ShortcutPreset> = listOf(
        ShortcutPreset("copy", "Copiar", "Ctrl+C", HidConstants.MOD_LEFT_CTRL, HidConstants.KEY_C),
        ShortcutPreset("paste", "Colar", "Ctrl+V", HidConstants.MOD_LEFT_CTRL, HidConstants.KEY_V),
        ShortcutPreset("cut", "Recortar", "Ctrl+X", HidConstants.MOD_LEFT_CTRL, HidConstants.KEY_X),
        ShortcutPreset("undo", "Desfazer", "Ctrl+Z", HidConstants.MOD_LEFT_CTRL, HidConstants.KEY_Z),
        ShortcutPreset("redo", "Refazer", "Ctrl+Y", HidConstants.MOD_LEFT_CTRL, HidConstants.KEY_Y),
        ShortcutPreset("select_all", "Tudo", "Ctrl+A", HidConstants.MOD_LEFT_CTRL, HidConstants.KEY_A),
        ShortcutPreset("alt_tab", "Alternar", "Alt+Tab", HidConstants.MOD_LEFT_ALT, HidConstants.KEY_TAB),
        ShortcutPreset("desktop", "Desktop", "Win+D", HidConstants.MOD_LEFT_GUI, HidConstants.KEY_D),
        ShortcutPreset("find", "Buscar", "Ctrl+F", HidConstants.MOD_LEFT_CTRL, HidConstants.KEY_F),
        ShortcutPreset("close_tab", "Fechar Aba", "Ctrl+W", HidConstants.MOD_LEFT_CTRL, HidConstants.KEY_W),
        ShortcutPreset("new_tab", "Nova Aba", "Ctrl+T", HidConstants.MOD_LEFT_CTRL, HidConstants.KEY_T),
        ShortcutPreset(
            "task_mgr",
            "Gerenciador",
            "Ctrl+Shift+Esc",
            HidConstants.MOD_LEFT_CTRL or HidConstants.MOD_LEFT_SHIFT,
            HidConstants.KEY_ESCAPE
        )
    )

    fun mapTextToKeyStrokes(text: String, layout: KeyboardLayout): List<HidKeyStroke> {
        val result = ArrayList<HidKeyStroke>(text.length * 2)
        for (ch in text) {
            result.addAll(mapCharToKeyStrokes(ch, layout))
        }
        return result
    }

    fun mapCharToKeyStrokes(ch: Char, layout: KeyboardLayout): List<HidKeyStroke> {
        if (ch in 'a'..'z') {
            return listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_A + (ch - 'a')))
        }
        if (ch in 'A'..'Z') {
            return listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_A + (ch - 'A')))
        }
        if (ch in '1'..'9') {
            return listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_1 + (ch - '1')))
        }
        if (ch == '0') {
            return listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_0))
        }
        when (ch) {
            ' ' -> return listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_SPACE))
            '\n', '\r' -> return listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_ENTER))
            '\t' -> return listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_TAB))
        }

        return when (layout) {
            KeyboardLayout.ABNT2 -> mapAbnt2SpecialChar(ch)
            KeyboardLayout.US_INTL -> mapUsIntlSpecialChar(ch)
        }
    }

    private fun mapAbnt2SpecialChar(ch: Char): List<HidKeyStroke> {
        return when (ch) {
            '!' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_1))
            '@' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_2))
            '#' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_3))
            '$' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_4))
            '%' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_5))
            '&' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_7))
            '*' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_8))
            '(' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_9))
            ')' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_0))
            '-' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_MINUS))
            '_' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_MINUS))
            '=' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_EQUAL))
            '+' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_EQUAL))
            '[' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_RIGHT_BRACKET))
            '{' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_RIGHT_BRACKET))
            ']' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_BACKSLASH))
            '}' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_BACKSLASH))
            'ç' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_SEMICOLON))
            'Ç' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_SEMICOLON))
            '\'' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_GRAVE))
            '"' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_GRAVE))
            ',' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_COMMA))
            '<' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_COMMA))
            '.' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_PERIOD))
            '>' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_PERIOD))
            ';' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_SLASH))
            ':' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_SLASH))
            '\\' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_NON_US_BACKSLASH))
            '|' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_NON_US_BACKSLASH))
            '/' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_INTL_1))
            '?' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_INTL_1))
            '~' -> listOf(
                HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_APOSTROPHE),
                HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_SPACE)
            )
            '^' -> listOf(
                HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_APOSTROPHE),
                HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_SPACE)
            )
            '´' -> listOf(
                HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_LEFT_BRACKET),
                HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_SPACE)
            )
            '`' -> listOf(
                HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_LEFT_BRACKET),
                HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_SPACE)
            )
            'á' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_LEFT_BRACKET, HidConstants.KEY_A, false)
            'Á' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_LEFT_BRACKET, HidConstants.KEY_A, true)
            'é' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_LEFT_BRACKET, HidConstants.KEY_E, false)
            'É' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_LEFT_BRACKET, HidConstants.KEY_E, true)
            'í' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_LEFT_BRACKET, HidConstants.KEY_I, false)
            'Í' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_LEFT_BRACKET, HidConstants.KEY_I, true)
            'ó' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_LEFT_BRACKET, HidConstants.KEY_O, false)
            'Ó' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_LEFT_BRACKET, HidConstants.KEY_O, true)
            'ú' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_LEFT_BRACKET, HidConstants.KEY_U, false)
            'Ú' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_LEFT_BRACKET, HidConstants.KEY_U, true)
            'à' -> deadKeyVowel(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_LEFT_BRACKET, HidConstants.KEY_A, false)
            'À' -> deadKeyVowel(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_LEFT_BRACKET, HidConstants.KEY_A, true)
            'ã' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_APOSTROPHE, HidConstants.KEY_A, false)
            'Ã' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_APOSTROPHE, HidConstants.KEY_A, true)
            'õ' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_APOSTROPHE, HidConstants.KEY_O, false)
            'Õ' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_APOSTROPHE, HidConstants.KEY_O, true)
            'â' -> deadKeyVowel(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_APOSTROPHE, HidConstants.KEY_A, false)
            'Â' -> deadKeyVowel(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_APOSTROPHE, HidConstants.KEY_A, true)
            'ê' -> deadKeyVowel(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_APOSTROPHE, HidConstants.KEY_E, false)
            'Ê' -> deadKeyVowel(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_APOSTROPHE, HidConstants.KEY_E, true)
            'ô' -> deadKeyVowel(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_APOSTROPHE, HidConstants.KEY_O, false)
            'Ô' -> deadKeyVowel(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_APOSTROPHE, HidConstants.KEY_O, true)
            else -> emptyList()
        }
    }

    private fun mapUsIntlSpecialChar(ch: Char): List<HidKeyStroke> {
        return when (ch) {
            '!' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_1))
            '@' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_2))
            '#' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_3))
            '$' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_4))
            '%' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_5))
            '^' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_6))
            '&' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_7))
            '*' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_8))
            '(' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_9))
            ')' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_0))
            '-' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_MINUS))
            '_' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_MINUS))
            '=' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_EQUAL))
            '+' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_EQUAL))
            '[' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_LEFT_BRACKET))
            '{' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_LEFT_BRACKET))
            ']' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_RIGHT_BRACKET))
            '}' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_RIGHT_BRACKET))
            '\\' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_BACKSLASH))
            '|' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_BACKSLASH))
            ';' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_SEMICOLON))
            ':' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_SEMICOLON))
            '\'' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_APOSTROPHE))
            '"' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_APOSTROPHE))
            '`' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_GRAVE))
            '~' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_GRAVE))
            ',' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_COMMA))
            '<' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_COMMA))
            '.' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_PERIOD))
            '>' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_PERIOD))
            '/' -> listOf(HidKeyStroke(HidConstants.MOD_NONE, HidConstants.KEY_SLASH))
            '?' -> listOf(HidKeyStroke(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_SLASH))
            'ç' -> listOf(HidKeyStroke(HidConstants.MOD_RIGHT_ALT, HidConstants.KEY_COMMA))
            'Ç' -> listOf(HidKeyStroke(HidConstants.MOD_RIGHT_ALT or HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_COMMA))
            'á' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_APOSTROPHE, HidConstants.KEY_A, false)
            'Á' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_APOSTROPHE, HidConstants.KEY_A, true)
            'é' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_APOSTROPHE, HidConstants.KEY_E, false)
            'É' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_APOSTROPHE, HidConstants.KEY_E, true)
            'í' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_APOSTROPHE, HidConstants.KEY_I, false)
            'Í' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_APOSTROPHE, HidConstants.KEY_I, true)
            'ó' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_APOSTROPHE, HidConstants.KEY_O, false)
            'Ó' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_APOSTROPHE, HidConstants.KEY_O, true)
            'ú' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_APOSTROPHE, HidConstants.KEY_U, false)
            'Ú' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_APOSTROPHE, HidConstants.KEY_U, true)
            'ã' -> deadKeyVowel(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_GRAVE, HidConstants.KEY_A, false)
            'Ã' -> deadKeyVowel(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_GRAVE, HidConstants.KEY_A, true)
            'õ' -> deadKeyVowel(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_GRAVE, HidConstants.KEY_O, false)
            'Õ' -> deadKeyVowel(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_GRAVE, HidConstants.KEY_O, true)
            'â' -> deadKeyVowel(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_6, HidConstants.KEY_A, false)
            'Â' -> deadKeyVowel(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_6, HidConstants.KEY_A, true)
            'ê' -> deadKeyVowel(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_6, HidConstants.KEY_E, false)
            'Ê' -> deadKeyVowel(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_6, HidConstants.KEY_E, true)
            'ô' -> deadKeyVowel(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_6, HidConstants.KEY_O, false)
            'Ô' -> deadKeyVowel(HidConstants.MOD_LEFT_SHIFT, HidConstants.KEY_6, HidConstants.KEY_O, true)
            'à' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_GRAVE, HidConstants.KEY_A, false)
            'À' -> deadKeyVowel(HidConstants.MOD_NONE, HidConstants.KEY_GRAVE, HidConstants.KEY_A, true)
            else -> emptyList()
        }
    }

    private fun deadKeyVowel(
        deadMod: Int,
        deadKey: Int,
        vowelKey: Int,
        uppercase: Boolean
    ): List<HidKeyStroke> = listOf(
        HidKeyStroke(deadMod, deadKey),
        HidKeyStroke(if (uppercase) HidConstants.MOD_LEFT_SHIFT else HidConstants.MOD_NONE, vowelKey)
    )
}
