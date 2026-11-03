package com.example.filebrowser

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val vm: FileExplorerViewModel by viewModels()
    private val dropbox by lazy { DropboxRepository(this, BuildConfig.DROPBOX_APP_KEY) }
    private val drive by lazy { DriveRepository(this) }

    private val legacyPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }
    private val allFilesPermission =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }
    private val notifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Android 13+: FTP চলাকালীন নোটিফিকেশনের জন্য অনুমতি
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    val context = LocalContext.current
                    var granted by remember { mutableStateOf(hasStoragePermission(context)) }
                    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
                        granted = hasStoragePermission(context)   // সেটিং থেকে ফিরলে আবার চেক
                    }
                    if (granted) {
                        AppRoot(vm, dropbox, drive)
                    } else {
                        PermissionScreen(onRequest = { requestStoragePermission() })
                    }
                }
            }
        }
    }

    private fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            allFilesPermission.launch(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        } else {
            legacyPermission.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
}

// ---------- অনুমতি ----------
fun hasStoragePermission(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        Environment.isExternalStorageManager()
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED
    }

@Composable
fun PermissionScreen(onRequest: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ফাইল অ্যাক্সেসের অনুমতি দরকার", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text("ফোনের ফাইল দেখতে ও পরিচালনা করতে অনুমতি দিন।", textAlign = TextAlign.Center)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequest) { Text("অনুমতি দিন") }
    }
}

// ---------- নিচের ট্যাব ----------
enum class MainTab(val label: String, val icon: ImageVector) {
    FILES("ফাইল", Icons.Default.Folder),
    FTP("FTP", Icons.Default.Wifi),
    DRIVE("Drive", Icons.Default.Cloud),
    DROPBOX("Dropbox", Icons.Default.CloudQueue)
}

@Composable
fun AppRoot(vm: FileExplorerViewModel, dropbox: DropboxRepository, drive: DriveRepository) {
    var tab by rememberSaveable { mutableStateOf(MainTab.FILES) }
    Scaffold(
        bottomBar = {
            NavigationBar {
                MainTab.entries.forEach { t ->
                    NavigationBarItem(
                        selected = tab == t,
                        onClick = { tab = t },
                        icon = { Icon(t.icon, contentDescription = t.label) },
                        label = { Text(t.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when (tab) {
                MainTab.FILES -> FileExplorerScreen(vm)
                MainTab.FTP -> FtpPanel()
                MainTab.DRIVE -> DriveScreen(drive)
                MainTab.DROPBOX -> DropboxScreen(dropbox)
            }
        }
    }
}

// ---------- ফাইল এক্সপ্লোরার ----------
sealed interface DialogType {
    data object NewFolder : DialogType
    data class Rename(val file: File) : DialogType
    data object Delete : DialogType
    data object Search : DialogType
    data object Zip : DialogType
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FileExplorerScreen(vm: FileExplorerViewModel) {
    val ui by vm.state.collectAsState()
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var dialog by remember { mutableStateOf<DialogType?>(null) }
    var sortMenu by remember { mutableStateOf(false) }

    LaunchedEffect(ui.message) {
        ui.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    BackHandler {
        when {
            ui.selected.isNotEmpty() -> vm.clearSelection()
            ui.searchQuery != null -> vm.clearSearch()
            !vm.goUp() -> (context as? Activity)?.finish()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = ui.searchQuery?.let { "খোঁজা: $it" }
                            ?: ui.currentDir.absolutePath
                                .removePrefix(FileOps.root.absolutePath)
                                .ifEmpty { "/" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                actions = {
                    IconButton(onClick = { dialog = DialogType.Search }) {
                        Icon(Icons.Default.Search, "খুঁজুন")
                    }
                    IconButton(onClick = { vm.toggleHidden() }) {
                        Icon(
                            if (ui.showHidden) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            "লুকানো ফাইল"
                        )
                    }
                    Box {
                        IconButton(onClick = { sortMenu = true }) {
                            Icon(Icons.Default.MoreVert, "মেনু")
                        }
                        DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                            SortBy.entries.forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text("${sort.label}${if (ui.sort == sort) " ✓" else ""}") },
                                    onClick = { vm.setSort(sort); sortMenu = false }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("নিজের APK ব্যাকআপ") },
                                onClick = { vm.backupApk(context); sortMenu = false }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomBar(
                ui = ui,
                onPaste = { vm.paste() },
                onClearClipboard = { vm.clearClipboard() },
                onCopy = { vm.copySelected() },
                onCut = { vm.cutSelected() },
                onSelectAll = { vm.selectAll() },
                onRename = { dialog = DialogType.Rename(it) },
                onUnzip = { vm.unzip(it) },
                onZip = { dialog = DialogType.Zip },
                onDelete = { dialog = DialogType.Delete }
            )
        },
        floatingActionButton = {
            if (ui.selected.isEmpty() && ui.searchQuery == null) {
                FloatingActionButton(onClick = { dialog = DialogType.NewFolder }) {
                    Icon(Icons.Default.Add, "নতুন ফোল্ডার")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            QuickAccessRow(onOpen = { vm.open(it) })
            if (ui.busy) LinearProgressIndicator(Modifier.fillMaxWidth())

            if (ui.items.isEmpty() && !ui.busy) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("এখানে কোনো ফাইল নেই")
                }
            }

            LazyColumn(Modifier.fillMaxSize()) {
                items(ui.items, key = { it.file.absolutePath }) { item ->
                    FileRow(
                        item = item,
                        selected = item.file in ui.selected,
                        onClick = {
                            when {
                                ui.selected.isNotEmpty() -> vm.toggleSelect(item.file)
                                item.isDirectory -> vm.open(item.file)
                                else -> openFile(context, item.file)
                            }
                        },
                        onLongClick = { vm.toggleSelect(item.file) }
                    )
                }
            }
        }
    }

    // ---------- ডায়ালগ ----------
    when (val d = dialog) {
        DialogType.NewFolder -> InputDialog(
            title = "নতুন ফোল্ডার",
            initial = "",
            onDismiss = { dialog = null },
            onConfirm = { vm.createFolder(it); dialog = null }
        )
        is DialogType.Rename -> InputDialog(
            title = "নাম পরিবর্তন",
            initial = d.file.name,
            onDismiss = { dialog = null },
            onConfirm = { vm.rename(d.file, it); dialog = null }
        )
        DialogType.Search -> InputDialog(
            title = "ফাইল খুঁজুন",
            initial = "",
            onDismiss = { dialog = null },
            onConfirm = { vm.search(it); dialog = null }
        )
        DialogType.Zip -> InputDialog(
            title = "ZIP ফাইলের নাম",
            initial = "archive",
            onDismiss = { dialog = null },
            onConfirm = { vm.zipSelected(it); dialog = null }
        )
        DialogType.Delete -> AlertDialog(
            onDismissRequest = { dialog = null },
            title = { Text("মুছে ফেলবেন?") },
            text = { Text("নির্বাচিত ${ui.selected.size}টি আইটেম স্থায়ীভাবে মুছে যাবে।") },
            confirmButton = {
                TextButton(onClick = { vm.deleteSelected(); dialog = null }) { Text("মুছুন") }
            },
            dismissButton = {
                TextButton(onClick = { dialog = null }) { Text("বাতিল") }
            }
        )
        null -> Unit
    }
}

@Composable
fun InputDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(value = text, onValueChange = { text = it }, singleLine = true)
        },
        confirmButton = {
            TextButton(onClick = { if (text.isNotBlank()) onConfirm(text.trim()) }) { Text("ঠিক আছে") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("বাতিল") }
        }
    )
}

@Composable
fun BottomBar(
    ui: UiState,
    onPaste: () -> Unit,
    onClearClipboard: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onSelectAll: () -> Unit,
    onRename: (File) -> Unit,
    onUnzip: (File) -> Unit,
    onZip: () -> Unit,
    onDelete: () -> Unit
) {
    when {
        ui.selected.isNotEmpty() -> Surface(tonalElevation = 3.dp) {
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onCopy) { Text("কপি") }
                TextButton(onClick = onCut) { Text("কাট") }
                TextButton(onClick = onSelectAll) { Text("সব") }
                TextButton(onClick = onZip) { Text("ZIP") }
                if (ui.selected.size == 1) {
                    val only = ui.selected.first()
                    TextButton(onClick = { onRename(only) }) { Text("নাম") }
                    if (only.extension.equals("zip", ignoreCase = true)) {
                        TextButton(onClick = { onUnzip(only) }) { Text("আনজিপ") }
                    }
                }
                TextButton(onClick = onDelete) { Text("মুছুন") }
            }
        }

        ui.clipboard.isNotEmpty() -> Surface(tonalElevation = 3.dp) {
            Row(
                Modifier.fillMaxWidth().padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${ui.clipboard.size}টি আইটেম (${if (ui.clipMode == ClipMode.CUT) "কাট" else "কপি"})",
                    Modifier.weight(1f)
                )
                TextButton(onClick = onClearClipboard) { Text("বাতিল") }
                Button(onClick = onPaste) { Text("এখানে পেস্ট") }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileRow(
    item: FileItem,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val bg = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent

    Row(
        Modifier
            .fillMaxWidth()
            .background(bg)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = if (item.isDirectory) Icons.Default.Folder else Icons.Default.Description,
            contentDescription = null,
            tint = if (item.isDirectory) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                item.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = buildString {
                    append(if (item.isDirectory) "ফোল্ডার" else formatSize(item.size))
                    append(" • ")
                    append(formatDate(item.lastModified))
                },
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (selected) Icon(Icons.Default.Check, contentDescription = "নির্বাচিত")
    }
}

// ---------- সহায়ক ফাংশন ----------
fun openFile(context: Context, file: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
    val mime = MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(file.extension.lowercase()) ?: "*/*"
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mime)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "এই ফাইল খোলার মতো অ্যাপ পাওয়া যায়নি", Toast.LENGTH_SHORT).show()
    }
}

fun formatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
}

private val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.ENGLISH)
fun formatDate(millis: Long): String = dateFormat.format(millis)
