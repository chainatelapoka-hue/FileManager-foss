package com.example.filebrowser

import android.content.Context
import android.os.Environment
import java.io.File

object ApkBackup {
    /** শুধু এই অ্যাপের নিজের APK, Downloads ফোল্ডারে কপি হবে */
    @Suppress("DEPRECATION")
    fun backupOwnApk(context: Context): File {
        val src = File(context.packageManager.getApplicationInfo(context.packageName, 0).sourceDir)
        val dstDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val dst = File(dstDir, "FileManager-foss-backup.apk")
        src.copyTo(dst, overwrite = true)
        return dst
    }
}
