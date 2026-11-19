package br.com.smarttarget.android.controller.hid

object HidConstants {
    const val REPORT_ID_KEYBOARD = 1
    const val REPORT_ID_MOUSE = 2
    const val REPORT_ID_CONSUMER = 3

    // Keyboard Modifier Masks
    const val MOD_NONE = 0x00
    const val MOD_LEFT_CTRL = 0x01
    const val MOD_LEFT_SHIFT = 0x02
    const val MOD_LEFT_ALT = 0x04
    const val MOD_LEFT_GUI = 0x08
    const val MOD_RIGHT_CTRL = 0x10
    const val MOD_RIGHT_SHIFT = 0x20
    const val MOD_RIGHT_ALT = 0x40
    const val MOD_RIGHT_GUI = 0x80

    // Mouse Button Masks
    const val MOUSE_NONE = 0x00
    const val MOUSE_LEFT = 0x01
    const val MOUSE_RIGHT = 0x02
    const val MOUSE_MIDDLE = 0x04

    // Consumer Control 16-bit Masks (Report ID 3, little-endian 2 bytes)
    const val CONSUMER_NONE = 0x0000
    const val CONSUMER_PLAY_PAUSE = 0x0001
    const val CONSUMER_NEXT = 0x0002
    const val CONSUMER_PREV = 0x0004
    const val CONSUMER_STOP = 0x0008
    const val CONSUMER_VOL_UP = 0x0010
    const val CONSUMER_VOL_DOWN = 0x0020
    const val CONSUMER_MUTE = 0x0040
    const val CONSUMER_AC_HOME = 0x0080
    const val CONSUMER_AC_BACK = 0x0100
    const val CONSUMER_AC_SEARCH = 0x0200

    // Standard USB HID Usage IDs (Usage Page 0x07 - Keyboard/Keypad)
    const val KEY_NONE = 0x00
    const val KEY_A = 0x04
    const val KEY_B = 0x05
    const val KEY_C = 0x06
    const val KEY_D = 0x07
    const val KEY_E = 0x08
    const val KEY_F = 0x09
    const val KEY_G = 0x0A
    const val KEY_H = 0x0B
    const val KEY_I = 0x0C
    const val KEY_J = 0x0D
    const val KEY_K = 0x0E
    const val KEY_L = 0x0F
    const val KEY_M = 0x10
    const val KEY_N = 0x11
    const val KEY_O = 0x12
    const val KEY_P = 0x13
    const val KEY_Q = 0x14
    const val KEY_R = 0x15
    const val KEY_S = 0x16
    const val KEY_T = 0x17
    const val KEY_U = 0x18
    const val KEY_V = 0x19
    const val KEY_W = 0x1A
    const val KEY_X = 0x1B
    const val KEY_Y = 0x1C
    const val KEY_Z = 0x1D

    const val KEY_1 = 0x1E
    const val KEY_2 = 0x1F
    const val KEY_3 = 0x20
    const val KEY_4 = 0x21
    const val KEY_5 = 0x22
    const val KEY_6 = 0x23
    const val KEY_7 = 0x24
    const val KEY_8 = 0x25
    const val KEY_9 = 0x26
    const val KEY_0 = 0x27

    const val KEY_ENTER = 0x28
    const val KEY_ESCAPE = 0x29
    const val KEY_BACKSPACE = 0x2A
    const val KEY_TAB = 0x2B
    const val KEY_SPACE = 0x2C
    const val KEY_MINUS = 0x2D
    const val KEY_EQUAL = 0x2E
    const val KEY_LEFT_BRACKET = 0x2F
    const val KEY_RIGHT_BRACKET = 0x30
    const val KEY_BACKSLASH = 0x31
    const val KEY_NON_US_HASH = 0x32
    const val KEY_SEMICOLON = 0x33
    const val KEY_APOSTROPHE = 0x34
    const val KEY_GRAVE = 0x35
    const val KEY_COMMA = 0x36
    const val KEY_PERIOD = 0x37
    const val KEY_SLASH = 0x38
    const val KEY_CAPS_LOCK = 0x39

    const val KEY_F1 = 0x3A
    const val KEY_F2 = 0x3B
    const val KEY_F3 = 0x3C
    const val KEY_F4 = 0x3D
    const val KEY_F5 = 0x3E
    const val KEY_F6 = 0x3F
    const val KEY_F7 = 0x40
    const val KEY_F8 = 0x41
    const val KEY_F9 = 0x42
    const val KEY_F10 = 0x43
    const val KEY_F11 = 0x44
    const val KEY_F12 = 0x45

    const val KEY_PRINT_SCREEN = 0x46
    const val KEY_INSERT = 0x49
    const val KEY_HOME = 0x4A
    const val KEY_PAGE_UP = 0x4B
    const val KEY_DELETE = 0x4C
    const val KEY_END = 0x4D
    const val KEY_PAGE_DOWN = 0x4E
    const val KEY_RIGHT = 0x4F
    const val KEY_LEFT = 0x50
    const val KEY_DOWN = 0x51
    const val KEY_UP = 0x52

    // Keypad / Numpad
    const val KEY_NUM_LOCK = 0x53
    const val KEY_KP_DIVIDE = 0x54
    const val KEY_KP_MULTIPLY = 0x55
    const val KEY_KP_MINUS = 0x56
    const val KEY_KP_PLUS = 0x57
    const val KEY_KP_ENTER = 0x58
    const val KEY_KP_1 = 0x59
    const val KEY_KP_2 = 0x5A
    const val KEY_KP_3 = 0x5B
    const val KEY_KP_4 = 0x5C
    const val KEY_KP_5 = 0x5D
    const val KEY_KP_6 = 0x5E
    const val KEY_KP_7 = 0x5F
    const val KEY_KP_8 = 0x60
    const val KEY_KP_9 = 0x61
    const val KEY_KP_0 = 0x62
    const val KEY_KP_DOT = 0x63

    const val KEY_NON_US_BACKSLASH = 0x64
    const val KEY_INTL_1 = 0x87

    /**
     * Composite USB HID Report Descriptor:
     * - Report ID 1: Standard 8-byte Keyboard (supports ABNT2 + US Extended up to 0xFF)
     * - Report ID 2: Standard 4-byte Relative Mouse (3 buttons, X, Y, Vertical Wheel)
     * - Report ID 3: Consumer Control 2-byte bitmask (Media & TV navigation)
     */
    val HID_REPORT_DESCRIPTOR: ByteArray = byteArrayOf(
        // Report ID 1: Keyboard
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x06.toByte(), // Usage (Keyboard)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), REPORT_ID_KEYBOARD.toByte(), // Report ID (1)
        0x05.toByte(), 0x07.toByte(), //   Usage Page (Key Codes)
        0x19.toByte(), 0xE0.toByte(), //   Usage Minimum (224)
        0x29.toByte(), 0xE7.toByte(), //   Usage Maximum (231)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(), //   Report Size (1)
        0x95.toByte(), 0x08.toByte(), //   Report Count (8)
        0x81.toByte(), 0x02.toByte(), //   Input (Data, Variable, Absolute)
        0x95.toByte(), 0x01.toByte(), //   Report Count (1)
        0x75.toByte(), 0x08.toByte(), //   Report Size (8)
        0x81.toByte(), 0x01.toByte(), //   Input (Constant)
        0x95.toByte(), 0x06.toByte(), //   Report Count (6)
        0x75.toByte(), 0x08.toByte(), //   Report Size (8)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x26.toByte(), 0xFF.toByte(), 0x00.toByte(), // Logical Maximum (255)
        0x05.toByte(), 0x07.toByte(), //   Usage Page (Key Codes)
        0x19.toByte(), 0x00.toByte(), //   Usage Minimum (0)
        0x2A.toByte(), 0xFF.toByte(), 0x00.toByte(), // Usage Maximum (255)
        0x81.toByte(), 0x00.toByte(), //   Input (Data, Array)
        0xC0.toByte(),                // End Collection

        // Report ID 2: Mouse
        0x05.toByte(), 0x01.toByte(), // Usage Page (Generic Desktop)
        0x09.toByte(), 0x02.toByte(), // Usage (Mouse)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), REPORT_ID_MOUSE.toByte(), // Report ID (2)
        0x09.toByte(), 0x01.toByte(), //   Usage (Pointer)
        0xA1.toByte(), 0x00.toByte(), //   Collection (Physical)
        0x05.toByte(), 0x09.toByte(), //     Usage Page (Buttons)
        0x19.toByte(), 0x01.toByte(), //     Usage Minimum (1)
        0x29.toByte(), 0x03.toByte(), //     Usage Maximum (3)
        0x15.toByte(), 0x00.toByte(), //     Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), //     Logical Maximum (1)
        0x95.toByte(), 0x03.toByte(), //     Report Count (3)
        0x75.toByte(), 0x01.toByte(), //     Report Size (1)
        0x81.toByte(), 0x02.toByte(), //     Input (Data, Variable, Absolute)
        0x95.toByte(), 0x01.toByte(), //     Report Count (1)
        0x75.toByte(), 0x05.toByte(), //     Report Size (5)
        0x81.toByte(), 0x01.toByte(), //     Input (Constant)
        0x05.toByte(), 0x01.toByte(), //     Usage Page (Generic Desktop)
        0x09.toByte(), 0x30.toByte(), //     Usage (X)
        0x09.toByte(), 0x31.toByte(), //     Usage (Y)
        0x09.toByte(), 0x38.toByte(), //     Usage (Wheel)
        0x15.toByte(), 0x81.toByte(), //     Logical Minimum (-127)
        0x25.toByte(), 0x7F.toByte(), //     Logical Maximum (127)
        0x75.toByte(), 0x08.toByte(), //     Report Size (8)
        0x95.toByte(), 0x03.toByte(), //     Report Count (3)
        0x81.toByte(), 0x06.toByte(), //     Input (Data, Variable, Relative)
        0xC0.toByte(),                //   End Collection (Physical)
        0xC0.toByte(),                // End Collection (Application)

        // Report ID 3: Consumer Control (Media & Smart TV)
        0x05.toByte(), 0x0C.toByte(), // Usage Page (Consumer)
        0x09.toByte(), 0x01.toByte(), // Usage (Consumer Control)
        0xA1.toByte(), 0x01.toByte(), // Collection (Application)
        0x85.toByte(), REPORT_ID_CONSUMER.toByte(), // Report ID (3)
        0x15.toByte(), 0x00.toByte(), //   Logical Minimum (0)
        0x25.toByte(), 0x01.toByte(), //   Logical Maximum (1)
        0x75.toByte(), 0x01.toByte(), //   Report Size (1)
        0x95.toByte(), 0x0A.toByte(), //   Report Count (10)
        0x09.toByte(), 0xCD.toByte(), //   Bit 0: Play/Pause
        0x09.toByte(), 0xB5.toByte(), //   Bit 1: Scan Next Track
        0x09.toByte(), 0xB6.toByte(), //   Bit 2: Scan Previous Track
        0x09.toByte(), 0xB7.toByte(), //   Bit 3: Stop
        0x09.toByte(), 0xE9.toByte(), //   Bit 4: Volume Increment
        0x09.toByte(), 0xEA.toByte(), //   Bit 5: Volume Decrement
        0x09.toByte(), 0xE2.toByte(), //   Bit 6: Mute
        0x0A.toByte(), 0x23.toByte(), 0x02.toByte(), // Bit 7: AC Home
        0x0A.toByte(), 0x24.toByte(), 0x02.toByte(), // Bit 8: AC Back
        0x0A.toByte(), 0x21.toByte(), 0x02.toByte(), // Bit 9: AC Search
        0x81.toByte(), 0x02.toByte(), //   Input (Data, Variable, Absolute)
        0x75.toByte(), 0x06.toByte(), //   Report Size (6)
        0x95.toByte(), 0x01.toByte(), //   Report Count (1)
        0x81.toByte(), 0x01.toByte(), //   Input (Constant)
        0xC0.toByte()                 // End Collection
    )
}
