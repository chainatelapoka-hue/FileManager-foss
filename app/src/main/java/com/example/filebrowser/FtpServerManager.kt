package com.example.filebrowser

import android.content.Context
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.ftplet.Authority
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import java.io.File
import java.net.Inet4Address
import java.net.NetworkInterface

object FtpServerManager {

    const val PORT = 2221   // 1024-এর নিচের পোর্টে Android-এ অনুমতি লাগে, তাই 2221
    private var server: FtpServer? = null

    val isRunning: Boolean get() = server?.isStopped == false

    fun start(context: Context, rootDir: File, username: String, password: String) {
        if (isRunning) return

        val umFactory = PropertiesUserManagerFactory().apply {
            setFile(File(context.filesDir, "ftp-users.properties"))
            setPasswordEncryptor(ClearTextPasswordEncryptor())
        }
        val userManager = umFactory.createUserManager()

        val user = BaseUser().apply {
            name = username
            setPassword(password)
            homeDirectory = rootDir.absolutePath
            authorities = listOf<Authority>(WritePermission())
            maxIdleTime = 300
        }
        userManager.save(user)

        val listener = ListenerFactory().apply { port = PORT }.createListener()
        val factory = FtpServerFactory().apply {
            this.userManager = userManager
            addListener("default", listener)
        }
        server = factory.createServer().also { it.start() }
    }

    fun stop() {
        server?.stop()
        server = null
    }

    /** ফোনের Wi-Fi IP, যেমন 192.168.1.20 */
    fun localIp(): String? =
        NetworkInterface.getNetworkInterfaces()?.toList()
            ?.flatMap { it.inetAddresses.toList() }
            ?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
            ?.hostAddress
}
