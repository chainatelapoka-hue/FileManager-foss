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
        Text("FTP server", style = MaterialTheme.typography.titleLarge)
        Text(
            if (running)
                "Running: ftp://${FtpServerManager.localIp()}:${FtpServerManager.PORT}\n" +
                    "User: user   Password: $password"
            else "Stopped. Your phone and computer must be on the same Wi-Fi."
        )
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            enabled = !running,
            singleLine = true
        )
        Button(onClick = {
            if (running) FtpService.stop(context) else FtpService.start(context, password)
            running = !running
        }) {
            Text(if (running) "Stop" else "Start")
        }
    }
}
