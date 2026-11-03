package com.example.filebrowser

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

enum class ClipMode { COPY, CUT }

data class UiState(
    val currentDir: File = FileOps.root,
    val items: List<FileItem> = emptyList(),
    val selected: Set<File> = emptySet(),
    val clipboard: List<File> = emptyList(),
    val clipMode: ClipMode? = null,
    val sort: SortBy = SortBy.NAME,
    val showHidden: Boolean = false,
    val searchQuery: String? = null,
    val message: String? = null,
    val busy: Boolean = false
)

class FileExplorerViewModel : ViewModel() {

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    init { refresh() }

    // ---------- নেভিগেশন ----------
    fun open(dir: File) {
        _state.update { it.copy(currentDir = dir, selected = emptySet(), searchQuery = null) }
        refresh()
    }

    /** উপরের ফোল্ডারে যাবে। রুটে থাকলে false ফেরত দেবে */
    fun goUp(): Boolean {
        val dir = _state.value.currentDir
        if (dir == FileOps.root) return false
        open(dir.parentFile ?: return false)
        return true
    }

    /** সেটিংস থেকে পাওয়া ডিফল্ট (হিডেন ফাইল ও সর্ট) প্রয়োগ করে */
    fun applyDefaults(showHidden: Boolean, sort: SortBy) {
        _state.update { it.copy(showHidden = showHidden, sort = sort) }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val s = _state.value
            val items = withContext(Dispatchers.IO) {
                FileOps.list(s.currentDir, s.showHidden, s.sort)
            }
            _state.update { it.copy(items = items) }
        }
    }

    fun search(query: String) {
        val root = _state.value.currentDir
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val results = withContext(Dispatchers.IO) { FileOps.search(root, query) }
            _state.update {
                it.copy(busy = false, items = results, searchQuery = query, selected = emptySet())
            }
        }
    }

    fun clearSearch() {
        _state.update { it.copy(searchQuery = null) }
        refresh()
    }

    // ---------- সর্ট ও হিডেন ফাইল ----------
    fun setSort(sort: SortBy) {
        _state.update { it.copy(sort = sort) }
        refresh()
    }

    fun toggleHidden() {
        _state.update { it.copy(showHidden = !it.showHidden) }
        refresh()
    }

    // ---------- সিলেকশন ----------
    fun toggleSelect(file: File) = _state.update { s ->
        s.copy(selected = if (file in s.selected) s.selected - file else s.selected + file)
    }

    fun selectAll() = _state.update { s -> s.copy(selected = s.items.map { it.file }.toSet()) }

    fun clearSelection() = _state.update { it.copy(selected = emptySet()) }

    // ---------- কপি / কাট / পেস্ট ----------
    fun copySelected() = stageClipboard(ClipMode.COPY)
    fun cutSelected() = stageClipboard(ClipMode.CUT)

    private fun stageClipboard(mode: ClipMode) = _state.update { s ->
        s.copy(clipboard = s.selected.toList(), clipMode = mode, selected = emptySet())
    }

    fun clearClipboard() = _state.update { it.copy(clipboard = emptyList(), clipMode = null) }

    fun paste() {
        val s = _state.value
        val mode = s.clipMode ?: return
        val items = s.clipboard
        val dest = s.currentDir
        runOp("${items.size} item(s) ${if (mode == ClipMode.CUT) "moved" else "copied"}") {
            items.forEach { file ->
                if (mode == ClipMode.COPY) FileOps.copy(file, dest) else FileOps.move(file, dest)
            }
        }
        if (mode == ClipMode.CUT) clearClipboard()
    }

    // ---------- মুছে ফেলা, নাম, ফোল্ডার, ZIP ----------
    fun deleteSelected() {
        val targets = _state.value.selected.toList()
        runOp("${targets.size} item(s) deleted") {
            targets.forEach { FileOps.delete(it) }
        }
    }

    fun rename(file: File, newName: String) =
        runOp("Renamed") { FileOps.rename(file, newName) }

    fun createFolder(name: String) {
        val dir = _state.value.currentDir
        runOp("Folder created") { FileOps.createFolder(dir, name) }
    }

    fun unzip(zip: File) {
        val dir = _state.value.currentDir
        runOp("Unzipped") {
            val dest = FileOps.uniqueTarget(dir, zip.nameWithoutExtension).apply { mkdirs() }
            FileOps.unzip(zip, dest)
        }
    }

    fun zipSelected(name: String) {
        val s = _state.value
        val sources = s.selected.toList()
        val out = FileOps.uniqueTarget(s.currentDir, "$name.zip")
        runOp("ZIP created: ${out.name}") { FileOps.zip(sources, out) }
    }

    fun backupApk(context: Context) {
        runOp("APK backup saved to Downloads") { ApkBackup.backupOwnApk(context) }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    /** সব ফাইল অপারেশন এখানে; ব্যাকগ্রাউন্ডে চলে, শেষে UI আপডেট হয় */
    private fun runOp(successMsg: String, block: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val msg = try {
                withContext(Dispatchers.IO) { block() }
                successMsg
            } catch (e: Exception) {
                "Error: ${e.message ?: "Unknown error"}"
            }
            _state.update { it.copy(busy = false, message = msg, selected = emptySet()) }
            refresh()
        }
    }
}
