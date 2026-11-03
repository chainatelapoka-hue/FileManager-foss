package com.example.filebrowser

import android.os.Environment
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

enum class SortBy(val label: String) {
    NAME("Name"), SIZE("Size"), DATE("Date"), TYPE("Type")
}

class FileItem(val file: File) {
    val name: String = file.name
    val isDirectory: Boolean = file.isDirectory
    val size: Long = if (isDirectory) 0L else file.length()
    val lastModified: Long = file.lastModified()
}

@Suppress("DEPRECATION")
object FileOps {

    val root: File = Environment.getExternalStorageDirectory()

    fun list(dir: File, showHidden: Boolean, sort: SortBy): List<FileItem> {
        val bySort: Comparator<FileItem> = when (sort) {
            SortBy.NAME -> compareBy { it.name.lowercase() }
            SortBy.SIZE -> compareBy { it.size }
            SortBy.DATE -> compareByDescending { it.lastModified }
            SortBy.TYPE -> compareBy { it.file.extension.lowercase() }
        }
        val folderFirst = compareBy<FileItem> { !it.isDirectory }
        return (dir.listFiles()?.toList() ?: emptyList())
            .filter { showHidden || !it.name.startsWith(".") }
            .map { FileItem(it) }
            .sortedWith(folderFirst.then(bySort))
    }

    /** সাব-ফোল্ডারসহ নাম দিয়ে খোঁজা */
    fun search(dir: File, query: String, limit: Int = 300): List<FileItem> {
        val out = mutableListOf<FileItem>()
        dir.walkTopDown()
            .onEnter { !it.name.startsWith(".") }
            .onFail { _, _ -> }
            .filter { it != dir && it.name.contains(query, ignoreCase = true) }
            .take(limit)
            .forEach { out += FileItem(it) }
        return out
    }

    /** একই নাম থাকলে "name (1).ext" বানায় */
    fun uniqueTarget(dir: File, name: String): File {
        var candidate = File(dir, name)
        if (!candidate.exists()) return candidate
        val dot = name.lastIndexOf('.').takeIf { it > 0 } ?: name.length
        val base = name.substring(0, dot)
        val ext = name.substring(dot)
        var i = 1
        while (candidate.exists()) {
            candidate = File(dir, "$base (${i++})$ext")
        }
        return candidate
    }

    fun copy(src: File, dstDir: File) {
        val target = uniqueTarget(dstDir, src.name)
        if (src.isDirectory) {
            require(!dstDir.canonicalPath.startsWith(src.canonicalPath + File.separator) &&
                    dstDir.canonicalPath != src.canonicalPath) {
                "A folder cannot be copied into itself"
            }
            target.mkdirs()
            src.listFiles()?.forEach { copy(it, target) }
        } else {
            src.copyTo(target)
        }
    }

    fun move(src: File, dstDir: File) {
        val target = uniqueTarget(dstDir, src.name)
        if (!src.renameTo(target)) {   // আলাদা স্টোরেজে হলে renameTo ব্যর্থ হয়
            copy(src, dstDir)
            delete(src)
        }
    }

    fun delete(file: File) {
        if (!file.deleteRecursively()) error("Could not delete: ${file.name}")
    }

    fun rename(file: File, newName: String) {
        require(newName.isNotBlank()) { "Name cannot be empty" }
        val to = File(file.parentFile, newName)
        require(!to.exists()) { "A file with this name already exists" }
        if (!file.renameTo(to)) error("Rename failed")
    }

    fun createFolder(parent: File, name: String) {
        require(name.isNotBlank()) { "Name cannot be empty" }
        val dir = File(parent, name)
        require(!dir.exists()) { "A folder with this name already exists" }
        if (!dir.mkdirs()) error("Could not create folder")
    }

    fun unzip(zip: File, destDir: File) {
        val destPath = destDir.canonicalPath + File.separator
        ZipInputStream(zip.inputStream().buffered()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val out = File(destDir, entry.name)
                // Zip Slip আক্রমণ ঠেকানো
                require(out.canonicalPath.startsWith(destPath)) { "Unsafe zip entry" }
                if (entry.isDirectory) {
                    out.mkdirs()
                } else {
                    out.parentFile?.mkdirs()
                    FileOutputStream(out).use { zis.copyTo(it) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }

    /** নির্বাচিত ফাইল/ফোল্ডার একটি ZIP-এ */
    fun zip(sources: List<File>, out: File) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(out))).use { zos ->
            fun add(f: File, entryName: String) {
                if (f == out) return   // নিজের আউটপুটকে নিজের ভেতরে ঢোকাবে না
                if (f.isDirectory) {
                    zos.putNextEntry(ZipEntry("$entryName/"))
                    zos.closeEntry()
                    f.listFiles()?.forEach { add(it, "$entryName/${it.name}") }
                } else {
                    zos.putNextEntry(ZipEntry(entryName))
                    f.inputStream().use { it.copyTo(zos) }
                    zos.closeEntry()
                }
            }
            sources.forEach { add(it, it.name) }
        }
    }
}
