package com.anotepad.data

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "anotepad_settings")

class PreferencesRepository(private val context: Context) {
    private object Keys {
        val ROOT_URI = stringPreferencesKey("root_tree_uri")
        val AUTO_LINK_WEB = booleanPreferencesKey("auto_link_web")
        val AUTO_LINK_EMAIL = booleanPreferencesKey("auto_link_email")
        val AUTO_LINK_TEL = booleanPreferencesKey("auto_link_tel")
        val SYNC_TITLE = booleanPreferencesKey("sync_title")
        val AUTO_SAVE_DEBOUNCE = longPreferencesKey("autosave_debounce_ms")
    }

    val preferencesFlow: Flow<AppPreferences> = context.dataStore.data.map { prefs ->
        AppPreferences(
            rootTreeUri = prefs[Keys.ROOT_URI],
            autoLinkWeb = prefs[Keys.AUTO_LINK_WEB] ?: false,
            autoLinkEmail = prefs[Keys.AUTO_LINK_EMAIL] ?: false,
            autoLinkTel = prefs[Keys.AUTO_LINK_TEL] ?: false,
            syncTitle = prefs[Keys.SYNC_TITLE] ?: false,
            autoSaveDebounceMs = prefs[Keys.AUTO_SAVE_DEBOUNCE] ?: 1200L
        )
    }

    suspend fun setRootTreeUri(uri: Uri?) {
        context.dataStore.edit { prefs ->
            if (uri == null) {
                prefs.remove(Keys.ROOT_URI)
            } else {
                prefs[Keys.ROOT_URI] = uri.toString()
            }
        }
    }

    suspend fun setAutoLinkWeb(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_LINK_WEB] = enabled }
    }

    suspend fun setAutoLinkEmail(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_LINK_EMAIL] = enabled }
    }

    suspend fun setAutoLinkTel(enabled: Boolean) {
        context.dataStore.edit { it[Keys.AUTO_LINK_TEL] = enabled }
    }

    suspend fun setSyncTitle(enabled: Boolean) {
        context.dataStore.edit { it[Keys.SYNC_TITLE] = enabled }
    }

    suspend fun setAutoSaveDebounce(ms: Long) {
        context.dataStore.edit { it[Keys.AUTO_SAVE_DEBOUNCE] = ms }
    }

    internal val dataStore = context.dataStore
}
