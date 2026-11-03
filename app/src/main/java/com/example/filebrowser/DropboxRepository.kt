package com.example.filebrowser

import android.content.Context
import com.dropbox.core.DbxRequestConfig
import com.dropbox.core.android.Auth
import com.dropbox.core.v2.DbxClientV2
import com.dropbox.core.v2.files.FolderMetadata
import com.dropbox.core.v2.files.Metadata
import com.dropbox.core.v2.files.WriteMode
import java.io.InputStream

class DropboxRepository(
    private val context: Context,
    private val appKey: String
) {
    private val prefs = context.getSharedPreferences("dropbox_prefs", Context.MODE_PRIVATE)
    private val requestConfig = DbxRequestConfig.newBuilder("FileManager-foss").build()

    val isLoggedIn: Boolean get() = prefs.getString(KEY_TOKEN, null) != null
    val hasAppKey: Boolean get() = appKey.isNotBlank()

    /** Dropbox লগইন পেজ খোলে */
    fun startLogin() {
        Auth.startOAuth2PKCE(context, appKey, requestConfig)
    }

    /** লগইন থেকে ফেরার পর (onResume-এ) টোকেন সংরক্ষণ করে */
    fun completeLoginIfPending(): Boolean {
        val token = Auth.getOAuth2Token() ?: return false
        prefs.edit().putString(KEY_TOKEN, token).apply()
        return true
    }

    fun logout() {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    fun client(): DbxClientV2? {
        val token = prefs.getString(KEY_TOKEN, null) ?: return null
        return DbxClientV2(requestConfig, token)
    }

    /** ফোল্ডারের সব আইটেম (ফোল্ডার আগে)। root হলে path = "" */
    fun list(client: DbxClientV2, path: String = ""): List<Metadata> {
        val first = client.files().listFolderBuilder(path).start()
        val all = first.entries.toMutableList()
        var cursor = first.cursor
        var hasMore = first.hasMore
        while (hasMore) {
            val next = client.files().listFolderContinue(cursor)
            all += next.entries
            cursor = next.cursor
            hasMore = next.hasMore
        }
        return all.sortedWith(
            compareBy<Metadata>({ it !is FolderMetadata }, { it.name.lowercase() })
        )
    }

    fun isFolder(m: Metadata) = m is FolderMetadata

    fun download(client: DbxClientV2, path: String): InputStream =
        client.files().download(path).inputStream

    fun upload(client: DbxClientV2, localStream: InputStream, remotePath: String) {
        localStream.use {
            client.files().uploadBuilder(remotePath)
                .withMode(WriteMode.ADD)
                .uploadAndFinish(it)
        }
    }

    fun createFolder(client: DbxClientV2, path: String) {
        client.files().createFolderV2(path)
    }

    /** Dropbox-এ মুছলে ট্র্যাশে যায়, স্থায়ী মোছে না */
    fun delete(client: DbxClientV2, path: String) {
        client.files().deleteV2(path)
    }

    companion object {
        private const val KEY_TOKEN = "access_token"
    }
}
