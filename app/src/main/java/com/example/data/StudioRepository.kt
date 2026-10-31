package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

data class ImportedAssetsSummary(
    val workspaceDirPath: String,
    val entryHtmlFile: String,
    val fileCount: Int,
    val totalBytes: Long,
    val fileNames: List<String>
)

class StudioRepository(
    private val context: Context,
    private val dao: StudioDao
) {
    val allProjects: Flow<List<WebAppProjectEntity>> = dao.observeProjects()
    val allBuilds: Flow<List<BuildRecordEntity>> = dao.observeBuildRecords()

    suspend fun saveProject(project: WebAppProjectEntity): Long = withContext(Dispatchers.IO) {
        val updated = project.copy(updatedAt = System.currentTimeMillis())
        if (updated.id == 0L) {
            dao.insertProject(updated)
        } else {
            dao.updateProject(updated)
            updated.id
        }
    }

    suspend fun deleteProject(project: WebAppProjectEntity) = withContext(Dispatchers.IO) {
        if (project.workspaceDirPath.isNotBlank()) {
            File(project.workspaceDirPath).parentFile?.deleteRecursively()
        }
        dao.deleteProjectById(project.id)
    }

    suspend fun insertBuildRecord(record: BuildRecordEntity): Long = withContext(Dispatchers.IO) {
        dao.insertBuildRecord(record)
    }

    suspend fun deleteBuildRecord(record: BuildRecordEntity) = withContext(Dispatchers.IO) {
        File(record.filePath).delete()
        dao.deleteBuildRecordById(record.id)
    }

    suspend fun importWebAssets(
        uris: List<Uri>,
        existingWorkspacePath: String
    ): ImportedAssetsSummary = withContext(Dispatchers.IO) {
        val folderId = if (existingWorkspacePath.isNotBlank()) {
            File(existingWorkspacePath)
        } else {
            File(context.filesDir, "projects/proj_${System.currentTimeMillis()}/assets")
        }
        if (folderId.exists()) {
            folderId.deleteRecursively()
        }
        folderId.mkdirs()

        for (uri in uris) {
            val fileName = resolveDisplayName(uri) ?: "index.html"
            if (fileName.lowercase().endsWith(".zip")) {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    ZipInputStream(input).use { zis ->
                        var entry = zis.nextEntry
                        while (entry != null) {
                            val cleanName = entry.name.removePrefix("/").replace("..", "")
                            if (cleanName.isNotBlank() && !cleanName.startsWith("__MACOSX")) {
                                val outFile = File(folderId, cleanName)
                                if (entry.isDirectory) {
                                    outFile.mkdirs()
                                } else {
                                    outFile.parentFile?.mkdirs()
                                    FileOutputStream(outFile).use { fos ->
                                        zis.copyTo(fos)
                                    }
                                }
                            }
                            zis.closeEntry()
                            entry = zis.nextEntry
                        }
                    }
                }
            } else {
                val outFile = File(folderId, fileName)
                outFile.parentFile?.mkdirs()
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(outFile).use { fos ->
                        input.copyTo(fos)
                    }
                }
            }
        }

        // Flatten single nested root folder if ZIP extracted into a single sub-directory
        val topChildren = folderId.listFiles()?.toList().orEmpty()
        if (topChildren.size == 1 && topChildren.first().isDirectory) {
            val subDir = topChildren.first()
            subDir.listFiles()?.forEach { child ->
                child.renameTo(File(folderId, child.name))
            }
            subDir.delete()
        }

        val allFiles = folderId.walkTopDown().filter { it.isFile }.toList()
        val htmlFiles = allFiles.filter {
            it.extension.equals("html", ignoreCase = true) ||
                it.extension.equals("htm", ignoreCase = true)
        }

        val entryHtml = when {
            htmlFiles.any { it.name.equals("index.html", ignoreCase = true) } ->
                htmlFiles.first { it.name.equals("index.html", ignoreCase = true) }
                    .relativeTo(folderId).path
            htmlFiles.isNotEmpty() ->
                htmlFiles.first().relativeTo(folderId).path
            else -> {
                val cssLinks = allFiles.filter { it.extension.equals("css", ignoreCase = true) }
                    .joinToString("\n") { "<link rel=\"stylesheet\" href=\"${it.relativeTo(folderId).path}\" />" }
                val jsScripts = allFiles.filter { it.extension.equals("js", ignoreCase = true) }
                    .joinToString("\n") { "<script src=\"${it.relativeTo(folderId).path}\"></script>" }
                val autoIndex = File(folderId, "index.html")
                autoIndex.writeText(
                    """
                    <!DOCTYPE html>
                    <html>
                    <head>
                        <meta charset="UTF-8" />
                        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                        <title>App</title>
                        $cssLinks
                    </head>
                    <body>
                        <div id="app"></div>
                        $jsScripts
                    </body>
                    </html>
                    """.trimIndent()
                )
                "index.html"
            }
        }

        val finalFiles = folderId.walkTopDown().filter { it.isFile }.toList()
        val totalSize = finalFiles.sumOf { it.length() }
        val names = finalFiles.map { it.relativeTo(folderId).path }

        ImportedAssetsSummary(
            workspaceDirPath = folderId.absolutePath,
            entryHtmlFile = entryHtml,
            fileCount = finalFiles.size,
            totalBytes = totalSize,
            fileNames = names
        )
    }

    suspend fun importCustomIcon(uri: Uri): String = withContext(Dispatchers.IO) {
        val iconDir = File(context.filesDir, "projects/icons").apply { mkdirs() }
        val iconFile = File(iconDir, "icon_${System.currentTimeMillis()}.png")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(iconFile).use { fos ->
                input.copyTo(fos)
            }
        }
        iconFile.absolutePath
    }

    private fun resolveDisplayName(uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                name = cursor.getString(index)
            }
        }
        return name ?: uri.lastPathSegment?.substringAfterLast('/')
    }
}
