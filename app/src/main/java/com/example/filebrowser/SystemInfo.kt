package com.example.filebrowser

import android.app.ActivityManager
import android.content.Context
import android.os.Environment
import android.os.StatFs

/** ব্যবহৃত ও মোট পরিমাণ */
data class Usage(val used: Long, val total: Long) {
    val fraction: Float get() = if (total > 0) used.toFloat() / total else 0f
    val free: Long get() = total - used
}

object SystemInfo {

    /** ইন্টারনাল/এক্সটার্নাল স্টোরেজের ব্যবহার (Android 7+ সমর্থিত) */
    @Suppress("DEPRECATION")
    fun storage(): Usage {
        val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
        val total = stat.totalBytes
        return Usage(used = total - stat.availableBytes, total = total)
    }

    /** RAM: ব্যবহৃত (used) ও অব্যবহৃত (available) */
    fun ram(context: Context): Usage {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        am.getMemoryInfo(info)
        return Usage(used = info.totalMem - info.availMem, total = info.totalMem)
    }
}
