package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class SourceType(val label: String) {
    LOCAL_FILES("HTML / CSS / JS / ZIP"),
    WEBSITE_URL("Website URL")
}

enum class ScreenOrientationMode(val label: String, val manifestValue: String) {
    PORTRAIT("Portrait", "portrait"),
    LANDSCAPE("Landscape", "landscape"),
    AUTO_SENSOR("Auto Rotate", "fullSensor")
}

enum class CachePolicyMode(val label: String) {
    DEFAULT("Smart Default Cache"),
    CACHE_FIRST_OFFLINE("Offline Cache-First"),
    NO_CACHE("Always Fresh (No Cache)")
}

enum class ExternalLinkRule(val label: String) {
    KEEP_IN_WEBVIEW("Open All Links Inside App"),
    OPEN_IN_BROWSER("Open External Domains in Browser")
}

enum class BuildOutputFormat(val label: String, val extension: String) {
    SIGNED_APK("Signed Installable APK", "apk"),
    UNSIGNED_APK("Unsigned APK Package", "apk"),
    ANDROID_STUDIO_ZIP("Android Studio Project (.zip)", "zip")
}

@Entity(tableName = "webapp_projects")
data class WebAppProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val appName: String = "",
    val packageName: String = "com.web2apk.app",
    val versionName: String = "1.0.0",
    val versionCode: Int = 1,
    val autoIncrementVersion: Boolean = true,
    val minSdk: Int = 24,
    val sourceType: String = SourceType.LOCAL_FILES.name,
    val websiteUrl: String = "",
    val workspaceDirPath: String = "",
    val entryHtmlFile: String = "index.html",
    val assetFileCount: Int = 0,
    val assetTotalBytes: Long = 0L,
    val assetSummaryList: String = "",

    // Branding & Splash Screen (100% Watermark-Free)
    val iconMonogram: String = "AP",
    val iconBgHex: String = "#1D4ED8",
    val iconTextHex: String = "#FFFFFF",
    val customIconPath: String = "",
    val splashEnabled: Boolean = true,
    val splashBgHex: String = "#0B0F17",
    val splashTagline: String = "",
    val splashDurationMs: Int = 1200,

    // Layout, Fullscreen, Toolbar & Orientation
    val orientationMode: String = ScreenOrientationMode.PORTRAIT.name,
    val fullscreenMode: Boolean = false,
    val keepScreenOn: Boolean = false,
    val showTopToolbar: Boolean = false,
    val statusBarHex: String = "#0B0F17",
    val navBarHex: String = "#0B0F17",
    val pullToRefresh: Boolean = true,
    val backButtonHistory: Boolean = true,

    // WebView Engine & Native Hardware Bridge
    val enableJavaScript: Boolean = true,
    val enableDomStorage: Boolean = true,
    val enableZoom: Boolean = false,
    val enableFileUpload: Boolean = true,
    val enableDownloads: Boolean = true,
    val desktopUserAgent: Boolean = false,
    val allowMixedContent: Boolean = true,
    val enableNativeJsBridge: Boolean = true,
    val cachePolicy: String = CachePolicyMode.DEFAULT.name,

    // Offline Error Page & External Link Handling
    val externalLinkRule: String = ExternalLinkRule.OPEN_IN_BROWSER.name,
    val deepLinkScheme: String = "",
    val customOfflineTitle: String = "Connection Unavailable",
    val customOfflineMessage: String = "Please check your internet connection and tap Retry.",
    val customOfflineRetryHex: String = "#3B82F6",

    // Hardware Permissions
    val permInternet: Boolean = true,
    val permCamera: Boolean = false,
    val permMicrophone: Boolean = false,
    val permLocation: Boolean = false,
    val permVibrate: Boolean = true,

    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val buildCount: Int = 0
)

@Entity(tableName = "build_records")
data class BuildRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val projectId: Long,
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val outputFormat: String,
    val fileName: String,
    val filePath: String,
    val fileSizeBytes: Long,
    val sha256Signature: String,
    val createdAt: Long = System.currentTimeMillis()
)
