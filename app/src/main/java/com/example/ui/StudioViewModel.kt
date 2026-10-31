package com.example.ui

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.builder.ApkPackagingEngine
import com.example.data.BuildOutputFormat
import com.example.data.BuildRecordEntity
import com.example.data.SourceType
import com.example.data.StudioRepository
import com.example.data.WebAppProjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

enum class StudioTab {
    DASHBOARD,
    CONFIGURATOR,
    BUILDS
}

data class BuildConsoleState(
    val isVisible: Boolean = false,
    val isRunning: Boolean = false,
    val progress: Float = 0f,
    val stepTitle: String = "",
    val logs: List<String> = emptyList(),
    val completedRecord: BuildRecordEntity? = null,
    val errorMessage: String? = null
)

data class StudioUiState(
    val activeTab: StudioTab = StudioTab.DASHBOARD,
    val activeStep: Int = 0,
    val draftProject: WebAppProjectEntity = WebAppProjectEntity(),
    val projects: List<WebAppProjectEntity> = emptyList(),
    val builds: List<BuildRecordEntity> = emptyList(),
    val buildConsole: BuildConsoleState = BuildConsoleState(),
    val sandboxProject: WebAppProjectEntity? = null,
    val showJsBridgeDialog: Boolean = false,
    val isDarkTheme: Boolean = true,
    val statusBanner: String? = null
) {
    val isPackageNameValid: Boolean
        get() = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
            .matches(draftProject.packageName.trim())

    val isUrlValid: Boolean
        get() = draftProject.websiteUrl.trim().startsWith("http://") ||
            draftProject.websiteUrl.trim().startsWith("https://")
}

class StudioViewModel(
    private val appContext: Context,
    private val repository: StudioRepository,
    private val packagingEngine: ApkPackagingEngine
) : ViewModel() {

    private val internalState = MutableStateFlow(StudioUiState())

    val uiState: StateFlow<StudioUiState> = combine(
        internalState,
        repository.allProjects,
        repository.allBuilds
    ) { state, projects, builds ->
        state.copy(
            projects = projects,
            builds = builds
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = StudioUiState()
    )

    fun selectTab(tab: StudioTab) {
        internalState.update { it.copy(activeTab = tab, statusBanner = null) }
    }

    fun selectStep(step: Int) {
        internalState.update { it.copy(activeStep = step.coerceIn(0, 3)) }
    }

    fun toggleTheme() {
        internalState.update { it.copy(isDarkTheme = !it.isDarkTheme) }
    }

    fun setShowJsBridgeDialog(show: Boolean) {
        internalState.update { it.copy(showJsBridgeDialog = show) }
    }

    fun clearStatusBanner() {
        internalState.update { it.copy(statusBanner = null) }
    }

    fun startNewProject(sourceType: SourceType) {
        val suffix = (100..999).random()
        val newDraft = WebAppProjectEntity(
            appName = "",
            packageName = "com.web2apk.app$suffix",
            sourceType = sourceType.name,
            websiteUrl = "",
            iconMonogram = "AP",
            iconBgHex = if (sourceType == SourceType.LOCAL_FILES) "#2563EB" else "#059669"
        )
        internalState.update {
            it.copy(
                draftProject = newDraft,
                activeTab = StudioTab.CONFIGURATOR,
                activeStep = 0,
                statusBanner = null
            )
        }
    }

    fun openProjectForEditing(project: WebAppProjectEntity) {
        internalState.update {
            it.copy(
                draftProject = project,
                activeTab = StudioTab.CONFIGURATOR,
                activeStep = 0,
                statusBanner = null
            )
        }
    }

    fun updateDraft(transform: (WebAppProjectEntity) -> WebAppProjectEntity) {
        internalState.update { state ->
            val updated = transform(state.draftProject)
            state.copy(draftProject = updated)
        }
    }

    fun saveCurrentDraft(showFeedback: Boolean = true) {
        viewModelScope.launch {
            val current = internalState.value.draftProject
            val resolvedName = current.appName.trim().ifEmpty { "My App" }
            val toSave = current.copy(appName = resolvedName)
            val savedId = repository.saveProject(toSave)
            internalState.update {
                it.copy(
                    draftProject = toSave.copy(id = savedId),
                    statusBanner = if (showFeedback) "Saved project '$resolvedName'." else it.statusBanner
                )
            }
        }
    }

    fun deleteProject(project: WebAppProjectEntity) {
        viewModelScope.launch {
            repository.deleteProject(project)
            internalState.update {
                it.copy(statusBanner = "Deleted project '${project.appName}'.")
            }
        }
    }

    fun importWebFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            try {
                val current = internalState.value.draftProject
                val summary = repository.importWebAssets(uris, current.workspaceDirPath)
                val autoName = current.appName.ifBlank {
                    summary.entryHtmlFile.substringBeforeLast('.').replaceFirstChar { it.uppercase() }
                }
                val updated = current.copy(
                    appName = autoName,
                    sourceType = SourceType.LOCAL_FILES.name,
                    workspaceDirPath = summary.workspaceDirPath,
                    entryHtmlFile = summary.entryHtmlFile,
                    assetFileCount = summary.fileCount,
                    assetTotalBytes = summary.totalBytes,
                    assetSummaryList = summary.fileNames.take(8).joinToString(", ")
                )
                val savedId = repository.saveProject(updated)
                internalState.update {
                    it.copy(
                        draftProject = updated.copy(id = savedId),
                        statusBanner = "Uploaded ${summary.fileCount} file(s) • Entry: ${summary.entryHtmlFile}"
                    )
                }
            } catch (e: Exception) {
                internalState.update {
                    it.copy(statusBanner = "Failed to import files: ${e.localizedMessage}")
                }
            }
        }
    }

    fun importCustomIcon(uri: Uri?) {
        if (uri == null) return
        viewModelScope.launch {
            try {
                val iconPath = repository.importCustomIcon(uri)
                internalState.update { state ->
                    state.copy(
                        draftProject = state.draftProject.copy(customIconPath = iconPath),
                        statusBanner = "Custom launcher icon selected."
                    )
                }
            } catch (e: Exception) {
                internalState.update {
                    it.copy(statusBanner = "Could not load icon: ${e.localizedMessage}")
                }
            }
        }
    }

    fun launchSandbox(project: WebAppProjectEntity = internalState.value.draftProject) {
        viewModelScope.launch {
            val resolved = project.copy(appName = project.appName.trim().ifEmpty { "My App" })
            val savedId = repository.saveProject(resolved)
            val savedProject = resolved.copy(id = savedId)
            internalState.update {
                it.copy(
                    draftProject = savedProject,
                    sandboxProject = savedProject
                )
            }
        }
    }

    fun closeSandbox() {
        internalState.update { it.copy(sandboxProject = null) }
    }

    fun buildProject(
        project: WebAppProjectEntity = internalState.value.draftProject,
        format: BuildOutputFormat
    ) {
        if (project.sourceType == SourceType.LOCAL_FILES.name && project.assetFileCount == 0) {
            internalState.update {
                it.copy(
                    activeTab = StudioTab.CONFIGURATOR,
                    activeStep = 0,
                    statusBanner = "Please upload your HTML, CSS, JS, or ZIP files first before building."
                )
            }
            return
        }
        if (project.sourceType == SourceType.WEBSITE_URL.name &&
            !project.websiteUrl.trim().startsWith("http://") &&
            !project.websiteUrl.trim().startsWith("https://")
        ) {
            internalState.update {
                it.copy(
                    activeTab = StudioTab.CONFIGURATOR,
                    activeStep = 0,
                    statusBanner = "Please enter a valid Website URL (https:// or http://) before building."
                )
            }
            return
        }

        viewModelScope.launch {
            val resolvedName = project.appName.trim().ifEmpty {
                if (project.sourceType == SourceType.WEBSITE_URL.name) {
                    Uri.parse(project.websiteUrl.trim()).host?.substringBefore('.')?.replaceFirstChar { it.uppercase() } ?: "WebApp"
                } else {
                    "HTMLApp"
                }
            }
            val nextVersionCode = if (project.autoIncrementVersion && project.buildCount > 0) {
                project.versionCode + 1
            } else {
                project.versionCode
            }
            val projectToBuild = project.copy(
                appName = resolvedName,
                versionCode = nextVersionCode,
                buildCount = project.buildCount + 1
            )
            val savedId = repository.saveProject(projectToBuild)
            val persistedProject = projectToBuild.copy(id = savedId)

            internalState.update {
                it.copy(
                    draftProject = persistedProject,
                    buildConsole = BuildConsoleState(
                        isVisible = true,
                        isRunning = true,
                        progress = 0.05f,
                        stepTitle = "Building ${persistedProject.appName}...",
                        logs = listOf(
                            "[Studio] Starting watermark-free build for '${persistedProject.appName}'",
                            "[Format] ${format.label}"
                        )
                    )
                )
            }

            try {
                val record = packagingEngine.buildArtifact(persistedProject, format) { stepUpdate ->
                    internalState.update { state ->
                        state.copy(
                            buildConsole = state.buildConsole.copy(
                                progress = stepUpdate.progress,
                                stepTitle = stepUpdate.stepTitle,
                                logs = state.buildConsole.logs + stepUpdate.logLine
                            )
                        )
                    }
                }
                val recordId = repository.insertBuildRecord(record)
                val finalRecord = record.copy(id = recordId)

                internalState.update { state ->
                    state.copy(
                        buildConsole = state.buildConsole.copy(
                            isRunning = false,
                            progress = 1f,
                            stepTitle = "APK Built Successfully!",
                            completedRecord = finalRecord
                        )
                    )
                }
            } catch (e: Exception) {
                internalState.update { state ->
                    state.copy(
                        buildConsole = state.buildConsole.copy(
                            isRunning = false,
                            errorMessage = e.localizedMessage ?: "Build failed"
                        )
                    )
                }
            }
        }
    }

    fun dismissBuildConsole() {
        internalState.update {
            it.copy(buildConsole = BuildConsoleState(isVisible = false))
        }
    }

    /**
     * Option 1: Save APK directly to the phone's Downloads folder (and/or user-chosen URI).
     */
    fun saveApkToDownloads(context: Context, record: BuildRecordEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val srcFile = File(record.filePath)
                if (!srcFile.exists()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "APK file not found; please rebuild.", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val mimeType = if (record.fileName.endsWith(".apk")) {
                    "application/vnd.android.package-archive"
                } else {
                    "application/zip"
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, record.fileName)
                        put(MediaStore.Downloads.MIME_TYPE, mimeType)
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                        put(MediaStore.Downloads.IS_PENDING, 1)
                    }
                    val resolver = context.contentResolver
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { out ->
                            srcFile.inputStream().use { input -> input.copyTo(out) }
                        }
                        values.clear()
                        values.put(MediaStore.Downloads.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    downloadsDir.mkdirs()
                    val destFile = File(downloadsDir, record.fileName)
                    srcFile.inputStream().use { input ->
                        FileOutputStream(destFile).use { out -> input.copyTo(out) }
                    }
                }

                val msg = "Saved to Downloads/${record.fileName}"
                internalState.update { it.copy(statusBanner = msg) }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                val err = "Could not save to Downloads: ${e.localizedMessage}"
                internalState.update { it.copy(statusBanner = err) }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun exportBuildToUri(record: BuildRecordEntity, destinationUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val srcFile = File(record.filePath)
                if (!srcFile.exists()) {
                    internalState.update { it.copy(statusBanner = "Build file not found on disk.") }
                    return@launch
                }
                appContext.contentResolver.openOutputStream(destinationUri)?.use { out ->
                    srcFile.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                internalState.update {
                    it.copy(statusBanner = "Saved '${record.fileName}' to selected location.")
                }
            } catch (e: Exception) {
                internalState.update {
                    it.copy(statusBanner = "Save failed: ${e.localizedMessage}")
                }
            }
        }
    }

    /**
     * Option 2: Send / Share APK via WhatsApp, Telegram, Nearby Share, Bluetooth, etc.
     */
    fun sendBuildArtifact(context: Context, record: BuildRecordEntity) {
        try {
            val file = File(record.filePath)
            if (!file.exists()) {
                Toast.makeText(context, "APK file missing; please rebuild.", Toast.LENGTH_SHORT).show()
                return
            }
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val mimeType = if (record.fileName.endsWith(".apk")) {
                "application/vnd.android.package-archive"
            } else {
                "application/zip"
            }
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, record.fileName)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "Send ${record.fileName}").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            internalState.update {
                it.copy(statusBanner = "Unable to send file: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Option 3: Install APK directly using Android's system Package Installer.
     */
    fun installApkArtifact(context: Context, record: BuildRecordEntity) {
        try {
            val file = File(record.filePath)
            if (!file.exists()) {
                Toast.makeText(context, "APK file missing; please rebuild.", Toast.LENGTH_SHORT).show()
                return
            }
            if (!record.fileName.endsWith(".apk", ignoreCase = true)) {
                Toast.makeText(
                    context,
                    "This is an Android Studio .zip archive. Build a Signed APK to install directly on your phone.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                !context.packageManager.canRequestPackageInstalls()
            ) {
                Toast.makeText(
                    context,
                    "Please allow 'Install unknown apps' for Web2APK Studio, then tap Install APK again.",
                    Toast.LENGTH_LONG
                ).show()
                val settingsIntent = Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(settingsIntent)
                return
            }

            val apkUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(installIntent)
        } catch (e: Exception) {
            val msg = "Installer error: ${e.localizedMessage}"
            internalState.update { it.copy(statusBanner = msg) }
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    fun deleteBuildRecord(record: BuildRecordEntity) {
        viewModelScope.launch {
            repository.deleteBuildRecord(record)
            internalState.update {
                it.copy(statusBanner = "Deleted build '${record.fileName}'.")
            }
        }
    }

    class Factory(
        private val appContext: Context,
        private val repository: StudioRepository,
        private val packagingEngine: ApkPackagingEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StudioViewModel(appContext, repository, packagingEngine) as T
        }
    }
}
