package com.example.filebrowser

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun SettingsScreen(settings: SettingsState) {
    val context = LocalContext.current
    var storage by remember { mutableStateOf(SystemInfo.storage()) }
    var ram by remember { mutableStateOf(SystemInfo.ram(context)) }

    // প্রতি ৩ সেকেন্ডে স্টোরেজ ও RAM-এর তথ্য নতুন করে পড়া হবে
    LaunchedEffect(Unit) {
        while (true) {
            storage = SystemInfo.storage()
            ram = SystemInfo.ram(context)
            delay(3000)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Browser settings", style = MaterialTheme.typography.titleLarge)

        SettingsSection("Theme") {
            ThemeMode.entries.forEach { mode ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = settings.theme == mode, onClick = { settings.updateTheme(mode) })
                    Text(mode.label)
                }
            }
        }

        SettingsSection("File browser") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Show hidden files")
                Spacer(Modifier.weight(1f))
                Switch(checked = settings.showHidden, onCheckedChange = { settings.updateShowHidden(it) })
            }
            Text("Default sort order")
            SortBy.entries.forEach { sort ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = settings.sort == sort, onClick = { settings.updateSort(sort) })
                    Text(sort.label)
                }
            }
        }

        SettingsSection("Storage") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DonutChart(
                    usedFraction = storage.fraction,
                    centerText = "${(storage.fraction * 100).toInt()}%"
                )
                Spacer(Modifier.size(16.dp))
                Column {
                    LegendRow(Color(0xFF3F51B5), "Used", formatSize(storage.used))
                    LegendRow(Color(0xFFB0BEC5), "Free", formatSize(storage.free))
                    Text("Total: ${formatSize(storage.total)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        SettingsSection("RAM") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                DonutChart(
                    usedFraction = ram.fraction,
                    centerText = "${(ram.fraction * 100).toInt()}%"
                )
                Spacer(Modifier.size(16.dp))
                Column {
                    LegendRow(Color(0xFFE53935), "Used", formatSize(ram.used))
                    LegendRow(Color(0xFF66BB6A), "Unused", formatSize(ram.free))
                    Text("Total: ${formatSize(ram.total)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = {
                Text(title, style = MaterialTheme.typography.titleMedium)
                content()
            }
        )
    }
}

/** ডোনাট চার্ট: ব্যবহৃত অংশ রঙিন, বাকিটা ধূসর */
@Composable
fun DonutChart(usedFraction: Float, centerText: String) {
    val usedColor = MaterialTheme.colorScheme.primary
    val freeColor = MaterialTheme.colorScheme.surfaceVariant
    Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val strokeWidth = 16.dp.toPx()
            val inset = strokeWidth / 2
            val arcSize = Size(this.size.width - strokeWidth, this.size.height - strokeWidth)
            drawArc(
                color = freeColor, startAngle = -90f, sweepAngle = 360f, useCenter = false,
                topLeft = Offset(inset, inset), size = arcSize, style = Stroke(strokeWidth)
            )
            drawArc(
                color = usedColor, startAngle = -90f, sweepAngle = 360f * usedFraction.coerceIn(0f, 1f),
                useCenter = false, topLeft = Offset(inset, inset), size = arcSize,
                style = Stroke(strokeWidth)
            )
        }
        Text(centerText, style = MaterialTheme.typography.titleSmall)
    }
}

@Composable
private fun LegendRow(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).padding(0.dp)) {
            Canvas(Modifier.fillMaxSize()) { drawRect(color) }
        }
        Spacer(Modifier.size(6.dp))
        Text("$label: $value", style = MaterialTheme.typography.bodySmall)
    }
}
