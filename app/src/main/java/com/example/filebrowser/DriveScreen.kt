package com.example.filebrowser

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.api.services.drive.model.File as DriveFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun DriveScreen(repo: DriveRepository) {
    var account by remember { mutableStateOf<GoogleSignInAccount?>(null) }
    var files by remember { mutableStateOf<List<DriveFile>>(emptyList()) }
    var status by remember { mutableStateOf("Not signed in") }

    val signInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        GoogleSignIn.getSignedInAccountFromIntent(result.data)
            .addOnSuccessListener { acc ->
                account = acc
                status = "Signed in: ${acc.email}"
            }
            .addOnFailureListener { status = "Sign-in failed: ${it.message}" }
    }

    LaunchedEffect(account) {
        val acc = account ?: return@LaunchedEffect
        try {
            files = withContext(Dispatchers.IO) { repo.list(repo.service(acc)) }
        } catch (e: Exception) {
            status = "Could not load files: ${e.message}"
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(status)
        Button(onClick = { signInLauncher.launch(repo.signInClient().signInIntent) }) {
            Text("Sign in to Google Drive")
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(files, key = { it.id ?: it.name ?: "" }) { f ->
                Text(
                    text = (if (repo.isFolder(f)) "📁 " else "📄 ") + (f.name ?: ""),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)
                )
            }
        }
    }
}
