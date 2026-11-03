package com.example.filebrowser

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.os.IBinder
import androidx.core.app.NotificationCompat

class FtpService : Service() {

    companion object {
        const val CHANNEL_ID = "ftp_server"
        const val NOTIF_ID = 1001
        const val EXTRA_PASSWORD = "password"

        fun start(context: Context, password: String) {
            val i = Intent(context, FtpService::class.java).putExtra(EXTRA_PASSWORD, password)
            // Android 7 (API 24-25)-এ startForegroundService নেই; ContextCompat সেটি সামলায়
            androidx.core.content.ContextCompat.startForegroundService(context, i)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, FtpService::class.java))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()
        startForeground(NOTIF_ID, buildNotification())

        val password = intent?.getStringExtra(EXTRA_PASSWORD) ?: "1234"
        @Suppress("DEPRECATION")
        FtpServerManager.start(this, Environment.getExternalStorageDirectory(), "user", password)
        return START_STICKY
    }

    override fun onDestroy() {
        FtpServerManager.stop()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "FTP সার্ভার", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("FTP সার্ভার চালু আছে")
            .setContentText("ftp://${FtpServerManager.localIp()}:${FtpServerManager.PORT}")
            .setSmallIcon(android.R.drawable.stat_sys_upload_done)
            .setOngoing(true)
            .build()
}
