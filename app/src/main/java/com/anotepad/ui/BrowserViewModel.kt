package com.anotepad.ui

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.anotepad.data.BrowserViewMode
import com.anotepad.data.FileSortOrder
import com.anotepad.data.PreferencesRepository
import com.anotepad.file.DocumentNode
import com.anotepad.file.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FeedItem(
    val node: DocumentNode,
    val text: String
)

data class BrowserState(
    val rootUri: Uri? = null,
    val currentDirUri: Uri? = null,
    val currentDirLabel: String? = null,
    val dirStack: List<Uri> = emptyList(),
    val entries: List<DocumentNode> = emptyList(),
    val isLoading: Boolean = false,
    val fileListFontSizeSp: Float = 14f,
    val fileSortOrder: FileSortOrder = FileSortOrder.NAME_DESC,
    val defaultFileExtension: String = "txt",
    val viewMode: BrowserViewMode = BrowserViewMode.LIST,
    val feedItems: List<FeedItem> = emptyList(),
    val feedHasMore: Boolean = false,
    val feedLoading: Boolean = false,
    val feedScrollIndex: Int = 0,
    val feedScrollOffset: Int = 0,
    val feedResetSignal: Int = 0
)

class BrowserViewModel(
    private val preferencesRepository: PreferencesRepository,
    private val fileRepository: FileRepository
) : ViewModel() {
    private val _state = MutableStateFlow(BrowserState())
    val state: StateFlow<BrowserState> = _state.asStateFlow()
    private var feedFiles: List<DocumentNode> = emptyList()
    private val feedPageSize = 10
    private var feedGeneration = 0

    init {
        viewModelScope.launch {
            preferencesRepository.preferencesFlow.collectLatest { prefs ->
                val root = prefs.rootTreeUri?.let(Uri::parse)
                val prevRoot = _state.value.rootUri
                val prevSortOrder = _state.value.fileSortOrder
                _state.update {
                    it.copy(
                        rootUri = root,
                        fileListFontSizeSp = prefs.browserFontSizeSp,
                        fileSortOrder = prefs.fileSortOrder,
                        defaultFileExtension = prefs.defaultFileExtension,
                        viewMode = prefs.browserViewMode
                    )
                }
                val currentDir = _state.value.currentDirUri
                if (root == null) {
                    _state.update { state ->
                        state.copy(
                            currentDirUri = null,
                            currentDirLabel = null,
                            dirStack = emptyList(),
                            entries = emptyList(),
                            feedItems = emptyList(),
                            feedHasMore = false,
                            feedLoading = false,
                            feedScrollIndex = 0,
                            feedScrollOffset = 0,
                            feedResetSignal = state.feedResetSignal + 1
                        )
                    }
                    feedFiles = emptyList()
                } else if (root != prevRoot || currentDir == null) {
                    setRoot(root)
                } else if (prevSortOrder != prefs.fileSortOrder) {
                    refresh()
                }
            }
        }
    }

    fun setRoot(root: Uri) {
        updateCurrentDir(root, listOf(root))
        refresh()
    }

    fun navigateInto(dirUri: Uri) {
        val newStack = _state.value.dirStack + dirUri
        updateCurrentDir(dirUri, newStack)
        refresh()
    }

    fun navigateUp() {
        val stack = _state.value.dirStack
        if (stack.size <= 1) return
        val newStack = stack.dropLast(1)
        updateCurrentDir(newStack.last(), newStack)
        refresh()
    }

    fun refresh() {
        val dirUri = _state.value.currentDirUri ?: return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val entries = fileRepository.listChildren(dirUri, _state.value.fileSortOrder)
            _state.update { it.copy(entries = entries, isLoading = false) }
            updateFeedSource(entries)
        }
    }

    fun createDirectory(name: String) {
        val dirUri = _state.value.currentDirUri ?: return
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            fileRepository.createDirectory(dirUri, trimmed)
            refresh()
        }
    }

    private fun updateCurrentDir(dirUri: Uri, stack: List<Uri>) {
        _state.update {
            it.copy(
                currentDirUri = dirUri,
                currentDirLabel = fileRepository.getTreeDisplayPath(dirUri),
                dirStack = stack
            )
        }
    }

    fun toggleViewMode() {
        val next = if (_state.value.viewMode == BrowserViewMode.LIST) {
            BrowserViewMode.FEED
        } else {
            BrowserViewMode.LIST
        }
        _state.update { it.copy(viewMode = next) }
        viewModelScope.launch { preferencesRepository.setBrowserViewMode(next) }
        if (next == BrowserViewMode.FEED) {
            ensureFeedLoaded()
        }
    }

    fun ensureFeedLoaded() {
        if (_state.value.feedItems.isEmpty() && feedFiles.isNotEmpty() && !_state.value.feedLoading) {
            loadMoreFeed()
        }
    }

    fun loadMoreFeed() {
        if (_state.value.feedLoading) return
        val start = _state.value.feedItems.size
        if (start >= feedFiles.size) {
            _state.update { it.copy(feedHasMore = false) }
            return
        }
        val end = (start + feedPageSize).coerceAtMost(feedFiles.size)
        val batch = feedFiles.subList(start, end)
        val generation = feedGeneration
        viewModelScope.launch {
            _state.update { it.copy(feedLoading = true) }
            val items = batch.map { node ->
                FeedItem(node = node, text = fileRepository.readText(node.uri))
            }
            _state.update { state ->
                if (generation != feedGeneration) {
                    state
                } else {
                    val combined = state.feedItems + items
                    state.copy(
                        feedItems = combined,
                        feedHasMore = combined.size < feedFiles.size,
                        feedLoading = false
                    )
                }
            }
        }
    }

    fun updateFeedScroll(index: Int, offset: Int) {
        _state.update { it.copy(feedScrollIndex = index, feedScrollOffset = offset) }
    }

    private fun updateFeedSource(entries: List<DocumentNode>) {
        feedGeneration += 1
        feedFiles = entries.filterNot { it.isDirectory }
        _state.update { state ->
            state.copy(
                feedItems = emptyList(),
                feedHasMore = feedFiles.isNotEmpty(),
                feedLoading = false,
                feedScrollIndex = 0,
                feedScrollOffset = 0,
                feedResetSignal = state.feedResetSignal + 1
            )
        }
        if (_state.value.viewMode == BrowserViewMode.FEED) {
            ensureFeedLoaded()
        }
    }
}
