package com.example.filebrowser

import android.content.Context
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.drive.model.File as DriveFile
import java.io.InputStream

class DriveRepository(private val context: Context) {

    fun signInClient(): GoogleSignInClient =
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_READONLY))
                .build()
        )

    fun service(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential
            .usingOAuth2(context, listOf(DriveScopes.DRIVE_READONLY))
            .apply { selectedAccount = account.account }
        return Drive.Builder(NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName("FileManager-foss")
            .build()
    }

    /** ফোল্ডারের ভেতরের ফাইল ও ফোল্ডার */
    fun list(drive: Drive, folderId: String = "root"): List<DriveFile> =
        drive.files().list()
            .setQ("'$folderId' in parents and trashed = false")
            .setFields("files(id,name,mimeType,size)")
            .setPageSize(200)
            .execute()
            .files ?: emptyList()

    /** ডাউনলোডের জন্য স্ট্রিম */
    fun download(drive: Drive, fileId: String): InputStream =
        drive.files().get(fileId).executeMediaAsInputStream()

    fun isFolder(file: DriveFile) = file.mimeType == "application/vnd.google-apps.folder"
}
