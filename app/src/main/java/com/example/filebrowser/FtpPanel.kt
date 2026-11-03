package com.example.filebrowser

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private fun randomPassword(): String {
    val chars = "abcdefghjkmnpqrstuvwxyz23456789"
    return (1..10).map { chars.random() }.joinToString("")
}

@Composable
fun FtpPanel() {
    val context = LocalContext.current
    var running by remember { mutableStateOf(FtpServerManager.isRunning) }
    var password by remember { mutableStateOf(randomPassword()) }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("FTP সার্ভার", style = MaterialTheme.typography.titleLarge)
        Text(
            if (running)
                "চালু: ftp://${FtpServerManager.localIp()}:${FtpServerManager.PORT}\n" +
                    "ইউজার: user   পাসওয়ার্ড: $password"
            else "বন্ধ আছে। ফোন ও কম্পিউটার একই Wi-Fi-তে থাকতে হবে।"
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("পাসওয়ার্ড") },
            enabled = !running,
            singleLine = true
        )
        Button(onClick = {
            if (running) FtpService.stop(context) else FtpService.start(context, password)
            running = !running
        }) {
            Text(if (running) "বন্ধ করুন" else "চালু করুন")
        }
    }
}
