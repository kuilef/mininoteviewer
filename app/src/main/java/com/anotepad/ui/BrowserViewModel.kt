package com.anotepad.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anotepad.data.PreferencesRepository
import com.anotepad.file.DocumentNode
import com.anotepad.file.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BrowserState(
    val rootUri: Uri? = null,
    val currentDirUri: Uri? = null,
    val dirStack: List<Uri> = emptyList(),
    val entries: List<DocumentNode> = emptyList(),
    val isLoading: Boolean = false
)

class BrowserViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val fileRepository: FileRepository
) : ViewModel() {
    private val _state = MutableStateFlow(BrowserState())
    val state: StateFlow<BrowserState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferencesRepository.preferencesFlow.collectLatest { prefs ->
                val root = prefs.rootTreeUri?.let(Uri::parse)
                val prevRoot = _state.value.rootUri
                _state.update { it.copy(rootUri = root) }
                val currentDir = _state.value.currentDirUri
                if (root == null) {
                    _state.update { state -> state.copy(currentDirUri = null, dirStack = emptyList(), entries = emptyList()) }
                } else if (root != prevRoot || currentDir == null) {
                    setRoot(root)
                }
            }
        }
    }

    fun setRoot(root: Uri) {
        _state.update { it.copy(rootUri = root, currentDirUri = root, dirStack = listOf(root)) }
        refresh()
    }

    fun navigateInto(dirUri: Uri) {
        _state.update { it.copy(currentDirUri = dirUri, dirStack = it.dirStack + dirUri) }
        refresh()
    }

    fun navigateUp() {
        val stack = _state.value.dirStack
        if (stack.size <= 1) return
        val newStack = stack.dropLast(1)
        _state.update { it.copy(currentDirUri = newStack.last(), dirStack = newStack) }
        refresh()
    }

    fun refresh() {
        val dirUri = _state.value.currentDirUri ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val entries = fileRepository.listChildren(dirUri)
            _state.update { it.copy(entries = entries, isLoading = false) }
        }
    }
}
