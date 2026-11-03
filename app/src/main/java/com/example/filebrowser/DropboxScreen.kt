package com.example.filebrowser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.Metadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DropboxScreen(repo: DropboxRepository) {
    var loggedIn by remember { mutableStateOf(repo.isLoggedIn) }
    var path by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<Metadata>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    // Dropbox লগইন থেকে ফিরে এলে টোকেন নিন
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (repo.completeLoginIfPending()) loggedIn = true
    }

    LaunchedEffect(loggedIn, path) {
        val client: DbxClientV2 = repo.client() ?: return@LaunchedEffect
        try {
            items = withContext(Dispatchers.IO) { repo.list(client, path) }
            error = null
        } catch (e: Exception) {
            error = "ত্রুটি: ${e.message}"
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (!loggedIn) {
            Text("Dropbox-এ সাইন-ইন করুন")
            if (!repo.hasAppKey) {
                Text(
                    "local.properties-এ DROPBOX_APP_KEY=আপনার_key যোগ করে আবার বিল্ড করুন।",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Button(onClick = { repo.startLogin() }, enabled = repo.hasAppKey) {
                Text("Dropbox-এ সাইন-ইন")
            }
            return@Column
        }

        Row {
            if (path.isNotEmpty()) {
                TextButton(onClick = { path = path.substringBeforeLast('/') }) { Text("⬆ উপরে") }
            }
            TextButton(onClick = { repo.logout(); loggedIn = false; items = emptyList() }) {
                Text("লগআউট")
            }
        }
        Text(if (path.isEmpty()) "/" else path)
        error?.let { Text(it) }

        LazyColumn(Modifier.fillMaxSize()) {
            items(items, key = { it.pathLower ?: it.name }) { m ->
                val isFolder = repo.isFolder(m)
                Text(
                    text = (if (isFolder) "📁 " else "📄 ") + m.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                        .clickable(enabled = isFolder) { path = m.pathLower ?: "" }
                )
            }
        }
    }
}
