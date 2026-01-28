package com.anotepad.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anotepad.data.AppPreferences
import com.anotepad.data.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsViewModel(private val preferencesRepository: PreferencesRepository) : ViewModel() {
    private val _state = MutableStateFlow(AppPreferences())
    val state: StateFlow<AppPreferences> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.preferencesFlow.collectLatest { prefs ->
                _state.value = prefs
            }
        }
    }

    fun setAutoLinkWeb(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoLinkWeb(enabled) }
    }

    fun setAutoLinkEmail(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoLinkEmail(enabled) }
    }

    fun setAutoLinkTel(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setAutoLinkTel(enabled) }
    }

    fun setSyncTitle(enabled: Boolean) {
        viewModelScope.launch { preferencesRepository.setSyncTitle(enabled) }
    }
}
