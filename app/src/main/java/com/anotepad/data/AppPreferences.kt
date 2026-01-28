package com.anotepad.data

data class AppPreferences(
    val rootTreeUri: String? = null,
    val autoLinkWeb: Boolean = false,
    val autoLinkEmail: Boolean = false,
    val autoLinkTel: Boolean = false,
    val syncTitle: Boolean = false,
    val autoSaveDebounceMs: Long = 1200L
)
