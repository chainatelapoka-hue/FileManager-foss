package com.example.filebrowser

import android.os.Environment
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import java.io.File

data class QuickItem(val label: String, val dir: File, val icon: ImageVector)

object QuickAccess {
    @Suppress("DEPRECATION")
    val items: List<QuickItem> by lazy {
        listOf(
            QuickItem("Downloads", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), Icons.Default.Download),
            QuickItem("DCIM", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), Icons.Default.CameraAlt),
            QuickItem("Documents", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), Icons.Default.Description),
            QuickItem("Pictures", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), Icons.Default.Image),
            QuickItem("Movies", Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), Icons.Default.Movie)
        ).filter { it.dir.exists() }
    }
}

@Composable
fun QuickAccessRow(onOpen: (File) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(QuickAccess.items) { q ->
            AssistChip(
                onClick = { onOpen(q.dir) },
                label = { Text(q.label) },
                leadingIcon = { Icon(q.icon, contentDescription = null) }
            )
        }
    }
}
