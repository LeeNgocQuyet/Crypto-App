package com.project.cryptoapp.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AppSettingsState(
    val defaultEncoding: String = "Text",
    val saveHistory: Boolean = true,
)

class AppSettingsStore {
    private val _state = MutableStateFlow(AppSettingsState())
    val state: StateFlow<AppSettingsState> = _state.asStateFlow()

    fun setDefaultEncoding(encoding: String) {
        _state.update { it.copy(defaultEncoding = encoding) }
    }

    fun setSaveHistory(enabled: Boolean) {
        _state.update { it.copy(saveHistory = enabled) }
    }
}
