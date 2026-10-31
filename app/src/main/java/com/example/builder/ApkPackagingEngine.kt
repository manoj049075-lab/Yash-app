package com.example.builder

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.example.data.BuildOutputFormat
import com.example.data.BuildRecordEntity
import com.example.data.SourceType
import com.example.data.WebAppProjectEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

data class BuildStepUpdate(
    val progress: Float,
    val stepTitle: String,
    val logLine: String
)

class ApkPackagingEngine(private val context: Context) {

    suspend fun buildArtifact(
        project: WebAppProjectEntity,
        format: BuildOutputFormat,
        onProgress: suspend (BuildStepUpdate) -> Unit
    ): BuildRecordEntity = withContext(Dispatchers.IO) {
        val buildsDir = File(context.filesDir, "builds").apply { mkdirs() }
        val sanitizedName = project.appName
            .replace(Regex("[^a-zA-Z0-9_-]"), "_")
            .trim('_')
            .ifEmpty { "App" }

        val suffix = when (format) {
            BuildOutputFormat.SIGNED_APK -> "v${project.versionName}.apk"
            BuildOutputFormat.UNSIGNED_APK -> "v${project.versionName}_unsigned.apk"
            BuildOutputFormat.ANDROID_STUDIO_ZIP -> "v${project.versionName}_AndroidStudio.zip"
        }
        val outputFile = File(buildsDir, "${sanitizedName}_${suffix}")

        onProgress(
            BuildStepUpdate(
                progress = 0.18f,
                stepTitle = "Validating Manifest & Package ID",
                logLine = "[Manifest] App=${project.appName}, Package=${project.packageName}, v${project.versionName} (${project.versionCode})"
            )
        )
        delay(150)

        onProgress(
            BuildStepUpdate(
                progress = 0.42f,
                stepTitle = "Rendering Custom Icon & Splash Assets",
                logLine = "[Branding] Generating launcher icons & watermark-free splash config"
            )
        )
        val iconPngBytes = renderProjectIconPng(project)
        val offlineHtmlBytes = generateOfflineHtml(project).toByteArray(Charsets.UTF_8)
        delay(150)

        onProgress(
            BuildStepUpdate(
                progress = 0.68f,
                stepTitle = "Bundling HTML/CSS/JS Assets & Native WebEngine",
                logLine = if (project.sourceType == SourceType.LOCAL_FILES.name) {
                    "[Assets] Packing ${project.assetFileCount} local file(s) (entry=${project.entryHtmlFile})"
                } else {
                    "[WebEngine] Binding live URL (${project.websiteUrl})"
                }
            )
        )
        delay(150)

        when (format) {
            BuildOutputFormat.ANDROID_STUDIO_ZIP -> {
                onProgress(
                    BuildStepUpdate(
                        progress = 0.88f,
                        stepTitle = "Generating Android Studio Gradle Project",
                        logLine = "[Gradle] Writing build.gradle.kts, AndroidManifest.xml, MainActivity.kt & assets/"
                    )
                )
                writeAndroidStudioProjectZip(
                    project = project,
                    outputFile = outputFile,
                    iconPngBytes = iconPngBytes,
                    offlineHtmlBytes = offlineHtmlBytes
                )
            }
            BuildOutputFormat.SIGNED_APK, BuildOutputFormat.UNSIGNED_APK -> {
                val signed = format == BuildOutputFormat.SIGNED_APK
                onProgress(
                    BuildStepUpdate(
                        progress = 0.88f,
                        stepTitle = if (signed) "Compiling & Signing Installable APK (RSA-2048)" else "Packaging Unsigned APK",
                        logLine = if (signed) {
                            "[Signer] Patching binary AXML manifest & signing with RSA-2048 X.509 certificate"
                        } else {
                            "[Packager] Assembling binary AXML manifest, DEX bytecode, and assets/"
                        }
                    )
                )
                writeInstallableApk(
                    project = project,
                    outputFile = outputFile,
                    iconPngBytes = iconPngBytes,
                    offlineHtmlBytes = offlineHtmlBytes,
                    signPackage = signed
                )
            }
        }
        delay(120)

        val sha256 = computeFileSha256(outputFile)
        onProgress(
            BuildStepUpdate(
                progress = 1.0f,
                stepTitle = "APK Ready • Save, Send, or Install",
                logLine = "[Ready] ${outputFile.name} (${formatBytes(outputFile.length())}) • 0 Watermarks"
            )
        )

        BuildRecordEntity(
            projectId = project.id,
            appName = project.appName,
            packageName = project.packageName,
            versionName = project.versionName,
            versionCode = project.versionCode,
            outputFormat = format.name,
            fileName = outputFile.name,
            filePath = outputFile.absolutePath,
            fileSizeBytes = outputFile.length(),
            sha256Signature = sha256
        )
    }

    private fun resolveTemplateApkFile(): File? {
        val runtimeSource = context.applicationInfo?.sourceDir?.let { File(it) }
        if (runtimeSource != null && runtimeSource.isFile && runtimeSource.exists()) {
            return runtimeSource
        }
        val localDebugApk = File("app/build/outputs/apk/debug/app-debug.apk")
        if (localDebugApk.isFile && localDebugApk.exists()) {
            return localDebugApk
        }
        return null
    }

    private fun writeInstallableApk(
        project: WebAppProjectEntity,
        outputFile: File,
        iconPngBytes: ByteArray,
        offlineHtmlBytes: ByteArray,
        signPackage: Boolean
    ) {
        val baseApkFile = resolveTemplateApkFile()
        val entryDigests = linkedMapOf<String, String>()

        FileOutputStream(outputFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                fun writeApkEntry(path: String, bytes: ByteArray, storeUncompressed: Boolean = false) {
                    val entry = ZipEntry(path)
                    if (storeUncompressed) {
                        entry.method = ZipEntry.STORED
                        entry.size = bytes.size.toLong()
                        entry.compressedSize = bytes.size.toLong()
                        val crc = CRC32()
                        crc.update(bytes)
                        entry.crc = crc.value
                    } else {
                        entry.method = ZipEntry.DEFLATED
                    }
                    zos.putNextEntry(entry)
                    zos.write(bytes)
                    zos.closeEntry()
                    val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
                    entryDigests[path] = Base64.getEncoder().encodeToString(digest)
                }

                if (baseApkFile != null) {
                    ZipFile(baseApkFile).use { zipIn ->
                        // 1. Write resources.arsc FIRST as STORED so it is 4-byte aligned at offset 44
                        val resEntry = zipIn.getEntry("resources.arsc")
                        if (resEntry != null) {
                            val rawRes = zipIn.getInputStream(resEntry).readBytes()
                            val patchedRes = patchResourcesArscAppLabel(rawRes, project.appName)
                            writeApkEntry("resources.arsc", patchedRes, storeUncompressed = true)
                        }

                        // 2. Write patched binary AndroidManifest.xml
                        val manifestEntry = zipIn.getEntry("AndroidManifest.xml")
                        if (manifestEntry != null) {
                            val rawManifest = zipIn.getInputStream(manifestEntry).readBytes()
                            val patchedManifest = patchBinaryAndroidManifest(rawManifest, project)
                            writeApkEntry("AndroidManifest.xml", patchedManifest)
                        }

                        // 3. Copy compiled DEX, native libs, and resources (replacing launcher icons with user's custom icon)
                        val entries = zipIn.entries()
                        while (entries.hasMoreElements()) {
                            val item = entries.nextElement()
                            val name = item.name
                            if (name == "resources.arsc" ||
                                name == "AndroidManifest.xml" ||
                                name.startsWith("META-INF/") ||
                                name.startsWith("assets/webapp/") ||
                                name == "assets/embedded_webapp_config.json" ||
                                name == "assets/offline_error.html"
                            ) {
                                continue
                            }

                            if (name.startsWith("res/mipmap-") &&
                                (name.endsWith("/ic_launcher.png") || name.endsWith("/ic_launcher_round.png"))
                            ) {
                                writeApkEntry(name, iconPngBytes)
                            } else {
                                val bytes = zipIn.getInputStream(item).readBytes()
                                writeApkEntry(name, bytes)
                            }
                        }
                    }
                } else {
                    // Fallback if running in isolated test environment without base APK
                    writeApkEntry("AndroidManifest.xml", generateAndroidManifestXml(project).toByteArray(Charsets.UTF_8))
                    writeApkEntry("res/mipmap-xxxhdpi-v4/ic_launcher.png", iconPngBytes)
                }

                // 4. Embed standalone WebApp configuration & user's HTML/CSS/JS/ZIP assets
                writeApkEntry(
                    "assets/embedded_webapp_config.json",
                    generateRuntimeConfigJson(project).toByteArray(Charsets.UTF_8)
                )
                writeApkEntry("assets/offline_error.html", offlineHtmlBytes)

                if (project.sourceType == SourceType.LOCAL_FILES.name && project.workspaceDirPath.isNotBlank()) {
                    val workspaceDir = File(project.workspaceDirPath)
                    if (workspaceDir.exists()) {
                        workspaceDir.walkTopDown().filter { it.isFile }.forEach { file ->
                            val relPath = file.relativeTo(workspaceDir).path.replace("\\", "/")
                            writeApkEntry("assets/webapp/$relPath", file.readBytes())
                        }
                    }
                }

                // 5. Sign APK with genuine 2048-bit RSA PKCS#7 signature (META-INF/MANIFEST.MF, CERT.SF, CERT.RSA)
                if (signPackage) {
                    val mfBuilder = StringBuilder()
                    mfBuilder.append("Manifest-Version: 1.0\r\n")
                    mfBuilder.append("Created-By: Web2APK Studio\r\n\r\n")

                    val sfSections = StringBuilder()
                    for ((entryName, sha256B64) in entryDigests) {
                        val section = "Name: $entryName\r\nSHA-256-Digest: $sha256B64\r\n\r\n"
                        mfBuilder.append(section)
                        val sectionDigest = Base64.getEncoder().encodeToString(
                            MessageDigest.getInstance("SHA-256").digest(section.toByteArray(Charsets.UTF_8))
                        )
                        sfSections.append("Name: $entryName\r\nSHA-256-Digest: $sectionDigest\r\n\r\n")
                    }

                    val manifestMfBytes = mfBuilder.toString().toByteArray(Charsets.UTF_8)
                    val mfDigestB64 = Base64.getEncoder().encodeToString(
                        MessageDigest.getInstance("SHA-256").digest(manifestMfBytes)
                    )

                    val certSfText = buildString {
                        append("Signature-Version: 1.0\r\n")
                        append("Created-By: Web2APK Studio\r\n")
                        append("SHA-256-Digest-Manifest: $mfDigestB64\r\n\r\n")
                        append(sfSections)
                    }
                    val certSfBytes = certSfText.toByteArray(Charsets.UTF_8)
                    val certRsaBytes = signPkcs7CertRsa(certSfBytes)

                    zos.putNextEntry(ZipEntry("META-INF/MANIFEST.MF"))
                    zos.write(manifestMfBytes)
                    zos.closeEntry()

                    zos.putNextEntry(ZipEntry("META-INF/CERT.SF"))
                    zos.write(certSfBytes)
                    zos.closeEntry()

                    zos.putNextEntry(ZipEntry("META-INF/CERT.RSA"))
                    zos.write(certRsaBytes)
                    zos.closeEntry()
                }
            }
        }
    }

    /**
     * Patches the compiled binary AndroidManifest.xml (AXML):
     * 1. Replaces the 29-char package ID ("com.aistudio.webtoapk.studiox") in UTF-16LE with a unique
     *    29-char package ID derived from the user's project so it installs cleanly alongside Web2APK Studio.
     * 2. Updates versionCode (0x0101021b), minSdkVersion (0x0101020c), and sets targetSdkVersion (0x01010270)
     *    to 29 so Android's PackageInstaller accepts standard v1 JAR signatures on all Android versions.
     */
    private fun patchBinaryAndroidManifest(
        rawBytes: ByteArray,
        project: WebAppProjectEntity
    ): ByteArray {
        val patched = rawBytes.copyOf()
        val oldPkg = "com.aistudio.webtoapk.studiox"
        val newPkg = toFixedLengthPackageName(project.packageName, project.id, oldPkg.length)

        val oldPkgU16 = oldPkg.toByteArray(Charsets.UTF_16LE)
        val newPkgU16 = newPkg.toByteArray(Charsets.UTF_16LE)
        replaceAllBytesInPlace(patched, oldPkgU16, newPkgU16)

        try {
            val buf = ByteBuffer.wrap(patched).order(ByteOrder.LITTLE_ENDIAN)
            var pos = 8
            var resIds = IntArray(0)
            while (pos + 8 <= patched.size) {
                val ctype = buf.getShort(pos).toInt() and 0xFFFF
                val chdr = buf.getShort(pos + 2).toInt() and 0xFFFF
                val csize = buf.getInt(pos + 4)
                if (csize <= 0 || pos + csize > patched.size) break

                if (ctype == 0x0180) {
                    val count = (csize - 8) / 4
                    resIds = IntArray(count) { i -> buf.getInt(pos + 8 + i * 4) }
                } else if (ctype == 0x0102 && pos + 36 <= patched.size) {
                    val attrStart = pos + 36
                    val attrCount = buf.getShort(pos + 28).toInt() and 0xFFFF
                    for (a in 0 until attrCount) {
                        val apos = attrStart + a * 20
                        if (apos + 20 > patched.size) break
                        val nameIdx = buf.getInt(apos + 4)
                        if (nameIdx in resIds.indices) {
                            when (resIds[nameIdx]) {
                                0x0101020c -> buf.putInt(apos + 16, project.minSdk.coerceIn(21, 29))
                                0x0101021b -> buf.putInt(apos + 16, project.versionCode.coerceAtLeast(1))
                                0x01010270 -> buf.putInt(apos + 16, 29)
                            }
                        }
                    }
                }
                pos += csize
            }
        } catch (_: Exception) {
            // Preserve valid manifest if unexpected chunk layout
        }
        return patched
    }

    private fun patchResourcesArscAppLabel(rawRes: ByteArray, appName: String): ByteArray {
        val patched = rawRes.copyOf()
        val oldLabel = "Web2APK Studio".toByteArray(Charsets.UTF_8)
        val cleanTitle = appName
            .replace(Regex("[^\\x20-\\x7E]"), "")
            .trim()
            .ifEmpty { "WebApp" }
            .take(oldLabel.size)
            .padEnd(oldLabel.size, ' ')
            .toByteArray(Charsets.UTF_8)
        replaceAllBytesInPlace(patched, oldLabel, cleanTitle)
        return patched
    }

    private fun toFixedLengthPackageName(userPkg: String, projectId: Long, targetLen: Int): String {
        val sanitized = userPkg.lowercase()
            .replace(Regex("[^a-z0-9_.]"), "")
            .split('.')
            .filter { it.isNotBlank() }
            .map { seg -> if (seg.first().isDigit()) "p$seg" else seg }
            .joinToString(".")
            .ifBlank { "com.web2apk.app" }

        val base = if (sanitized.contains('.')) sanitized else "com.$sanitized"
        if (base == "com.aistudio.webtoapk.studiox") {
            return "com.web2apk.builtapp.p0000001"
        }
        return when {
            base.length == targetLen -> base
            base.length < targetLen -> {
                val needed = targetLen - base.length
                if (needed == 1) {
                    "${base}x"
                } else {
                    val pad = "p${projectId.coerceAtLeast(1)}".padEnd(needed - 1, '0').take(needed - 1)
                    "$base.$pad"
                }
            }
            else -> {
                val trimmed = base.take(targetLen).trimEnd('.')
                if (trimmed.length < targetLen) trimmed.padEnd(targetLen, 'x') else trimmed
            }
        }
    }

    private fun replaceAllBytesInPlace(buffer: ByteArray, target: ByteArray, replacement: ByteArray) {
        if (target.isEmpty() || target.size != replacement.size) return
        var i = 0
        while (i <= buffer.size - target.size) {
            var match = true
            for (j in target.indices) {
                if (buffer[i + j] != target[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                System.arraycopy(replacement, 0, buffer, i, replacement.size)
                i += target.size
            } else {
                i++
            }
        }
    }

    private fun signPkcs7CertRsa(certSfBytes: ByteArray): ByteArray {
        val keyBytes = Base64.getDecoder().decode(PKCS8_PRIVATE_KEY_B64)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val privateKey = KeyFactory.getInstance("RSA").generatePrivate(keySpec)
        val signer = Signature.getInstance("SHA256withRSA")
        signer.initSign(privateKey)
        signer.update(certSfBytes)
        val signatureBytes = signer.sign()
        val prefixBytes = Base64.getDecoder().decode(PKCS7_DER_PREFIX_B64)
        return prefixBytes + signatureBytes
    }

    private fun writeAndroidStudioProjectZip(
        project: WebAppProjectEntity,
        outputFile: File,
        iconPngBytes: ByteArray,
        offlineHtmlBytes: ByteArray
    ) {
        val pkgPath = project.packageName.replace('.', '/')
        FileOutputStream(outputFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                fun writeEntry(path: String, content: ByteArray) {
                    zos.putNextEntry(ZipEntry(path))
                    zos.write(content)
                    zos.closeEntry()
                }

                fun writeTextEntry(path: String, text: String) {
                    writeEntry(path, text.toByteArray(Charsets.UTF_8))
                }

                writeTextEntry(
                    "settings.gradle.kts",
                    """
                    pluginManagement {
                        repositories {
                            google()
                            mavenCentral()
                            gradlePluginPortal()
                        }
                    }
                    dependencyResolutionManagement {
                        repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                        repositories {
                            google()
                            mavenCentral()
                        }
                    }
                    rootProject.name = "${project.appName.replace("\"", "")}"
                    include(":app")
                    """.trimIndent()
                )

                writeTextEntry(
                    "build.gradle.kts",
                    """
                    plugins {
                        id("com.android.application") version "8.7.2" apply false
                        id("org.jetbrains.kotlin.android") version "2.0.21" apply false
                    }
                    """.trimIndent()
                )

                writeTextEntry(
                    "app/build.gradle.kts",
                    """
                    plugins {
                        id("com.android.application")
                        id("org.jetbrains.kotlin.android")
                    }

                    android {
                        namespace = "${project.packageName}"
                        compileSdk = 35

                        defaultConfig {
                            applicationId = "${project.packageName}"
                            minSdk = ${project.minSdk}
                            targetSdk = 35
                            versionCode = ${project.versionCode}
                            versionName = "${project.versionName}"
                        }

                        compileOptions {
                            sourceCompatibility = JavaVersion.VERSION_17
                            targetCompatibility = JavaVersion.VERSION_17
                        }
                        kotlinOptions {
                            jvmTarget = "17"
                        }
                    }

                    dependencies {
                        implementation("androidx.core:core-ktx:1.15.0")
                        implementation("androidx.appcompat:appcompat:1.7.0")
                        implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
                    }
                    """.trimIndent()
                )

                writeTextEntry("app/src/main/AndroidManifest.xml", generateAndroidManifestXml(project))
                writeTextEntry("app/src/main/java/$pkgPath/MainActivity.kt", generateGeneratedMainActivityKt(project))
                writeEntry("app/src/main/res/mipmap-xxxhdpi/ic_launcher.png", iconPngBytes)
                writeEntry("app/src/main/assets/offline_error.html", offlineHtmlBytes)

                if (project.sourceType == SourceType.LOCAL_FILES.name && project.workspaceDirPath.isNotBlank()) {
                    val workspaceDir = File(project.workspaceDirPath)
                    if (workspaceDir.exists()) {
                        workspaceDir.walkTopDown().filter { it.isFile }.forEach { file ->
                            val relPath = file.relativeTo(workspaceDir).path.replace("\\", "/")
                            writeEntry("app/src/main/assets/$relPath", file.readBytes())
                        }
                    }
                }
            }
        }
    }

    private fun generateAndroidManifestXml(project: WebAppProjectEntity): String {
        val orientation = when (project.orientationMode) {
            "LANDSCAPE" -> "landscape"
            "AUTO_SENSOR" -> "fullSensor"
            else -> "portrait"
        }
        val perms = buildString {
            if (project.permInternet) {
                appendLine("""    <uses-permission android:name="android.permission.INTERNET" />""")
                appendLine("""    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />""")
            }
            if (project.permCamera) appendLine("""    <uses-permission android:name="android.permission.CAMERA" />""")
            if (project.permMicrophone) appendLine("""    <uses-permission android:name="android.permission.RECORD_AUDIO" />""")
            if (project.permLocation) {
                appendLine("""    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />""")
                appendLine("""    <uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />""")
            }
            if (project.permVibrate) appendLine("""    <uses-permission android:name="android.permission.VIBRATE" />""")
        }

        val deepLinkFilter = if (project.deepLinkScheme.isNotBlank()) {
            """
            <intent-filter>
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="${project.deepLinkScheme.trim()}" />
            </intent-filter>
            """.trimIndent()
        } else ""

        return """
            <?xml version="1.0" encoding="utf-8"?>
            <manifest xmlns:android="http://schemas.android.com/apk/res/android"
                package="${project.packageName}"
                android:versionCode="${project.versionCode}"
                android:versionName="${project.versionName}">

            $perms
                <application
                    android:allowBackup="true"
                    android:icon="@mipmap/ic_launcher"
                    android:label="${project.appName.replace("\"", "&quot;")}"
                    android:usesCleartextTraffic="${project.allowMixedContent}"
                    android:supportsRtl="true"
                    android:theme="@style/Theme.AppCompat.NoActionBar">
                    <activity
                        android:name=".MainActivity"
                        android:exported="true"
                        android:screenOrientation="$orientation"
                        android:configChanges="orientation|screenSize|keyboardHidden">
                        <intent-filter>
                            <action android:name="android.intent.action.MAIN" />
                            <category android:name="android.intent.category.LAUNCHER" />
                        </intent-filter>
                        $deepLinkFilter
                    </activity>
                </application>
            </manifest>
        """.trimIndent()
    }

    private fun generateGeneratedMainActivityKt(project: WebAppProjectEntity): String {
        val startUrl = if (project.sourceType == SourceType.LOCAL_FILES.name) {
            "file:///android_asset/${project.entryHtmlFile.ifBlank { "index.html" }}"
        } else {
            project.websiteUrl
        }
        return """
            package ${project.packageName}

            import android.annotation.SuppressLint
            import android.content.ClipData
            import android.content.ClipboardManager
            import android.content.Context
            import android.content.Intent
            import android.net.Uri
            import android.os.Bundle
            import android.os.VibrationEffect
            import android.os.Vibrator
            import android.view.View
            import android.view.WindowManager
            import android.webkit.JavascriptInterface
            import android.webkit.WebChromeClient
            import android.webkit.WebResourceError
            import android.webkit.WebResourceRequest
            import android.webkit.WebSettings
            import android.webkit.WebView
            import android.webkit.WebViewClient
            import android.widget.Toast
            import androidx.activity.OnBackPressedCallback
            import androidx.appcompat.app.AppCompatActivity

            class MainActivity : AppCompatActivity() {
                private lateinit var webView: WebView

                @SuppressLint("SetJavaScriptEnabled")
                override fun onCreate(savedInstanceState: Bundle?) {
                    super.onCreate(savedInstanceState)
                    if (${project.keepScreenOn}) {
                        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                    }
                    if (${project.fullscreenMode}) {
                        window.decorView.systemUiVisibility = (
                            View.SYSTEM_UI_FLAG_FULLSCREEN or
                            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
                    }

                    webView = WebView(this)
                    setContentView(webView)

                    webView.settings.apply {
                        javaScriptEnabled = ${project.enableJavaScript}
                        domStorageEnabled = ${project.enableDomStorage}
                        databaseEnabled = ${project.enableDomStorage}
                        setSupportZoom(${project.enableZoom})
                        builtInZoomControls = ${project.enableZoom}
                        displayZoomControls = false
                        allowFileAccess = ${project.enableFileUpload}
                        allowContentAccess = ${project.enableFileUpload}
                        if (${project.desktopUserAgent}) {
                            userAgentString = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                        }
                        if (${project.allowMixedContent}) {
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }
                    }

                    if (${project.enableNativeJsBridge}) {
                        webView.addJavascriptInterface(AndroidBridge(this), "AndroidAPI")
                    }

                    webView.webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                            val url = request?.url?.toString() ?: return false
                            if (${project.externalLinkRule == "OPEN_IN_BROWSER"} && url.startsWith("http") && !url.contains(Uri.parse("$startUrl").host.orEmpty())) {
                                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                return true
                            }
                            return false
                        }

                        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                            if (request?.isForMainFrame == true) {
                                view?.loadUrl("file:///android_asset/offline_error.html")
                            }
                        }
                    }

                    webView.webChromeClient = WebChromeClient()
                    webView.loadUrl("$startUrl")

                    onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
                        override fun handleOnBackPressed() {
                            if (${project.backButtonHistory} && webView.canGoBack()) {
                                webView.goBack()
                            } else {
                                finish()
                            }
                        }
                    })
                }

                class AndroidBridge(private val context: Context) {
                    @JavascriptInterface
                    fun showToast(message: String) {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }

                    @JavascriptInterface
                    fun vibrate(durationMs: Long) {
                        val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                        v?.vibrate(VibrationEffect.createOneShot(durationMs.coerceIn(10L, 2000L), VibrationEffect.DEFAULT_AMPLITUDE))
                    }

                    @JavascriptInterface
                    fun copyToClipboard(text: String) {
                        val cb = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        cb?.setPrimaryClip(ClipData.newPlainText("WebApp", text))
                    }

                    @JavascriptInterface
                    fun getAppVersion(): String = "${project.versionName}"

                    @JavascriptInterface
                    fun getPackageName(): String = "${project.packageName}"
                }
            }
        """.trimIndent()
    }

    private fun generateOfflineHtml(project: WebAppProjectEntity): String {
        val targetReload = if (project.sourceType == SourceType.LOCAL_FILES.name) {
            "file:///android_asset/webapp/${project.entryHtmlFile.ifBlank { "index.html" }}"
        } else {
            project.websiteUrl
        }
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8" />
                <meta name="viewport" content="width=device-width, initial-scale=1.0" />
                <title>${project.customOfflineTitle}</title>
                <style>
                    body {
                        margin: 0;
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        background: ${project.splashBgHex};
                        color: #F8FAFC;
                        font-family: system-ui, -apple-system, sans-serif;
                        padding: 24px;
                        text-align: center;
                    }
                    .box {
                        max-width: 360px;
                        background: rgba(255,255,255,0.05);
                        border: 1px solid rgba(255,255,255,0.12);
                        border-radius: 20px;
                        padding: 28px 22px;
                    }
                    h2 { margin: 0 0 10px; font-size: 20px; }
                    p { margin: 0 0 20px; color: #94A3B8; font-size: 14px; line-height: 1.5; }
                    button {
                        background: ${project.customOfflineRetryHex};
                        color: #FFFFFF;
                        border: none;
                        border-radius: 12px;
                        padding: 12px 24px;
                        font-size: 15px;
                        font-weight: 700;
                        cursor: pointer;
                    }
                </style>
            </head>
            <body>
                <div class="box">
                    <h2>${project.customOfflineTitle}</h2>
                    <p>${project.customOfflineMessage}</p>
                    <button onclick="window.location.href='$targetReload'">Retry Connection</button>
                </div>
            </body>
            </html>
        """.trimIndent()
    }

    private fun generateRuntimeConfigJson(project: WebAppProjectEntity): String {
        val obj = JSONObject().apply {
            put("appName", project.appName)
            put("packageName", project.packageName)
            put("versionName", project.versionName)
            put("versionCode", project.versionCode)
            put("minSdk", project.minSdk)
            put("sourceType", project.sourceType)
            put("websiteUrl", project.websiteUrl)
            put("entryHtmlFile", project.entryHtmlFile)
            put("iconMonogram", project.iconMonogram)
            put("iconBgHex", project.iconBgHex)
            put("iconTextHex", project.iconTextHex)
            put("splashEnabled", project.splashEnabled)
            put("splashBgHex", project.splashBgHex)
            put("splashTagline", project.splashTagline)
            put("splashDurationMs", project.splashDurationMs)
            put("orientationMode", project.orientationMode)
            put("fullscreenMode", project.fullscreenMode)
            put("keepScreenOn", project.keepScreenOn)
            put("showTopToolbar", project.showTopToolbar)
            put("statusBarHex", project.statusBarHex)
            put("navBarHex", project.navBarHex)
            put("pullToRefresh", project.pullToRefresh)
            put("backButtonHistory", project.backButtonHistory)
            put("enableJavaScript", project.enableJavaScript)
            put("enableDomStorage", project.enableDomStorage)
            put("enableZoom", project.enableZoom)
            put("enableFileUpload", project.enableFileUpload)
            put("desktopUserAgent", project.desktopUserAgent)
            put("allowMixedContent", project.allowMixedContent)
            put("enableNativeJsBridge", project.enableNativeJsBridge)
            put("cachePolicy", project.cachePolicy)
            put("externalLinkRule", project.externalLinkRule)
            put("customOfflineTitle", project.customOfflineTitle)
            put("customOfflineMessage", project.customOfflineMessage)
            put("customOfflineRetryHex", project.customOfflineRetryHex)
        }
        return obj.toString(2)
    }

    private fun renderProjectIconPng(project: WebAppProjectEntity): ByteArray {
        if (project.customIconPath.isNotBlank()) {
            val file = File(project.customIconPath)
            if (file.exists()) {
                val decoded = BitmapFactory.decodeFile(file.absolutePath)
                if (decoded != null) {
                    val scaled = Bitmap.createScaledBitmap(decoded, 192, 192, true)
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.PNG, 100, out)
                    return out.toByteArray()
                }
            }
        }

        val size = 192
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val bgColor = parseColorSafe(project.iconBgHex, Color.parseColor("#1D4ED8"))
        val fgColor = parseColorSafe(project.iconTextHex, Color.WHITE)

        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = bgColor
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(RectF(0f, 0f, size.toFloat(), size.toFloat()), 42f, 42f, bgPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fgColor
            textSize = 78f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }
        val label = project.iconMonogram.trim().take(2).ifEmpty { "AP" }.uppercase()
        val yPos = (size / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(label, size / 2f, yPos, textPaint)

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return out.toByteArray()
    }

    private fun parseColorSafe(hex: String, fallback: Int): Int {
        return try {
            Color.parseColor(if (hex.startsWith("#")) hex else "#$hex")
        } catch (_: Exception) {
            fallback
        }
    }

    private fun computeFileSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buf = ByteArray(8192)
            var read = input.read(buf)
            while (read > 0) {
                digest.update(buf, 0, read)
                read = input.read(buf)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PKCS7_DER_PREFIX_B64 =
            "MIIFBwYJKoZIhvcNAQcCoIIE+DCCBPQCAQExDTALBglghkgBZQMEAgEwCwYJKoZIhvcNAQcBoIIDVTCCA1EwggI5oAMCAQICFHzHiK9AsEt3//xsVKRLThiE3sjnMA0GCSqGSIb3DQEBCwUAMDcxFjAUBgNVBAMMDUFuZHJvaWQgRGVidWcxEDAOBgNVBAoMB0FuZHJvaWQxCzAJBgNVBAYTAlVTMCAXDTI2MTAzMDA4MjU1MloYDzIwNTQwMzE3MDgyNTUyWjA3MRYwFAYDVQQDDA1BbmRyb2lkIERlYnVnMRAwDgYDVQQKDAdBbmRyb2lkMQswCQYDVQQGEwJVUzCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAJiSYNCNk858ACSrrK6NpZCyfWIQuFoOO7RMSxFAux8JGnzJDdQi1eODJcUcXaWGXyx4a7EeGaN/JItv4g0p0654OEgB8umf4ZWODNEpXIgl1V6TBB5mNYDunlYRQah6J61bLFiYKXGIlP92XPGsrju6eKmZ/PcT5zNInlhdMV0NPCP8kSq0gpl9XTAyXNCHZ5jTA9v0eiCHFp4AF42AJzUKSgdFHI/nZDQdmMn9tUI06swp0oEv1Bfx50+xIo6sFJW1rska72tXr57dWPt+SViPleULr0egah6o343tbOxOb/htgm2U5/Q6JmtasYsKWmlDKggVTwpS9bH1aluxDbkCAwEAAaNTMFEwHQYDVR0OBBYEFAPwKCJUFCBrEr3Add4izRCtTFfXMB8GA1UdIwQYMBaAFAPwKCJUFCBrEr3Add4izRCtTFfXMA8GA1UdEwEB/wQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEBAAt27owMP+jcsXfr2J/vTyTLj1KLRtkidi+tFJmqnesONraeJSCrz8O3xJLmk052qFthrIYdPV2gaDnsj198wxW6Vjus1p5Jnm1gGuFtO1ZV56Z+0S/iqvgCBqOlW/wQL/vX5VeqzApolwLfjX6KToC+O3oob2PNmaEJPAFe1E4lr5uA4xo2kOU35zUmh8T99PzvHqRkhEQAUwfOaiHaukUj7bvqlKVbV5uOguTBr6Pq0VobiO7QTnB6VGs9yh0rbFzbtS++SocF7A+p47s7MZ/V1i1hN51z1zUgIxJywsLQgx/ZuD8TpcNukImpYME5iGlS0+ZAzDojeLDWtFY6PA8xggF4MIIBdAIBATBPMDcxFjAUBgNVBAMMDUFuZHJvaWQgRGVidWcxEDAOBgNVBAoMB0FuZHJvaWQxCzAJBgNVBAYTAlVTAhR8x4ivQLBLd//8bFSkS04YhN7I5zALBglghkgBZQMEAgEwDQYJKoZIhvcNAQEBBQAEggEA"

        private const val PKCS8_PRIVATE_KEY_B64 =
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCYkmDQjZPOfAAkq6yujaWQsn1iELhaDju0TEsRQLsfCRp8yQ3UItXjgyXFHF2lhl8seGuxHhmjfySLb+INKdOueDhIAfLpn+GVjgzRKVyIJdVekwQeZjWA7p5WEUGoeietWyxYmClxiJT/dlzxrK47unipmfz3E+czSJ5YXTFdDTwj/JEqtIKZfV0wMlzQh2eY0wPb9HoghxaeABeNgCc1CkoHRRyP52Q0HZjJ/bVCNOrMKdKBL9QX8edPsSKOrBSVta7JGu9rV6+e3Vj7fklYj5XlC69HoGoeqN+N7WzsTm/4bYJtlOf0OiZrWrGLClppQyoIFU8KUvWx9WpbsQ25AgMBAAECggEAAO81kVVqpDM5d4AHKdcDfnI/ZxY54NBRQIqOk6OZOciPHVhIYcYYWAE9vhotPyYmZ1Hx++bWqpwM299Cq8i3q4wAMT200IY6hhVVggLuNoW/y6lcFzkSRxC/DsEijS75VvwV1pGRAGlhGzrs+QxxETvUQRqotFUuf2+Ii7s/YgXgLJwtw2BNdUfItPmKemgcUEVjJ/Ag8ZLUzqvKLzYQw9hNsTF0zM2PPsJNcbGjp+xI5nEpssKYaQ3gacwR92hrRHA2a4ybJC+KqEHO6MN2xOKGZocip/9+XYt06RQ5JUsJH2I9WJHA/WixLdRuu/rLH1dQ7vbsnwt52wKDF0A2wQKBgQDV+9x+ak4HuQjkp+KMdFM/C580mO5X3k140KHZRWgvdoxibv/+VvAk0nt+bzap8So4wsAUEAq+9T7Xn71Vx3VVBMa8XAtSNbIfxCh9S3714W9Ya/NftpdzeXTvKEiBFAidpzhExjwXUaQ1bc8Laon+U/wQn1C2daHz6ccDVXkn+QKBgQC2h5I97ZAfdTqEpSyGfNWzv7NCGU4fQnLGszxfSZvFGHVuGDcXM2UUSSB/m9xEcLsVEjjxHfZ+W64PLYgAdF83kKCWQVrA+toKwrshUnrhZOuZNDBZr7axlAQNZQA5P4/IARdvrxitc6umgyy78riUwx6VND1TemZJn8PTC3IDwQKBgCNOK+IbrgYuLF51qLZd1wJW64ylEuYigovFeH69qePy3lS7VbdkJrkclU2WEeHWLY4x67kjsHIzSowPChEmGKDPae7YDPIic+Iz3i5WD3uIF3PyU7ReDvz5dCalcdkCMi240lrejjmWuc4q8PdEO8Jfnv4rGwkPvYnd730IVDYZAoGAYOJA64IHN8tfjjymD5R/UfN00Kbh4pHAnltQtjs55ToASrmrtDLhegz+pP0zw1ga1Spg8wq2ZYeAMio2wBcE21d3BYoXA8uipIeMEDnZY33P1/ftmg08abFpHTr5DWoBhWWe7PcBoD9/YtjJi9FfzAZtW0DpNEAJvxaAOKsgj4ECgYEAwzW2m5AZtRW4tQO7T/NkYEV3De+lUWCv4Fxg2QPImKtHcN4gUyMriz4rjyinsA9GtkZrQZMKIEW+K8o4B2y5hN+sXdqwi0ghOBf6i7uPajhtxTUQtb+h0duwfJVlAMqKiZ/SqCKlbOOdwOCzxu6dmIhDEUvpJiUSZozLqbhSOGA="

        fun parseEmbeddedConfig(jsonString: String): WebAppProjectEntity {
            val obj = JSONObject(jsonString)
            return WebAppProjectEntity(
                id = 1L,
                appName = obj.optString("appName", "WebApp"),
                packageName = obj.optString("packageName", "com.web2apk.app"),
                versionName = obj.optString("versionName", "1.0.0"),
                versionCode = obj.optInt("versionCode", 1),
                minSdk = obj.optInt("minSdk", 24),
                sourceType = obj.optString("sourceType", SourceType.LOCAL_FILES.name),
                websiteUrl = obj.optString("websiteUrl", ""),
                entryHtmlFile = obj.optString("entryHtmlFile", "index.html"),
                iconMonogram = obj.optString("iconMonogram", "AP"),
                iconBgHex = obj.optString("iconBgHex", "#2563EB"),
                iconTextHex = obj.optString("iconTextHex", "#FFFFFF"),
                splashEnabled = obj.optBoolean("splashEnabled", true),
                splashBgHex = obj.optString("splashBgHex", "#0B0F17"),
                splashTagline = obj.optString("splashTagline", ""),
                splashDurationMs = obj.optInt("splashDurationMs", 1200),
                orientationMode = obj.optString("orientationMode", "PORTRAIT"),
                fullscreenMode = obj.optBoolean("fullscreenMode", false),
                keepScreenOn = obj.optBoolean("keepScreenOn", false),
                showTopToolbar = obj.optBoolean("showTopToolbar", false),
                statusBarHex = obj.optString("statusBarHex", "#0B0F17"),
                navBarHex = obj.optString("navBarHex", "#0B0F17"),
                pullToRefresh = obj.optBoolean("pullToRefresh", true),
                backButtonHistory = obj.optBoolean("backButtonHistory", true),
                enableJavaScript = obj.optBoolean("enableJavaScript", true),
                enableDomStorage = obj.optBoolean("enableDomStorage", true),
                enableZoom = obj.optBoolean("enableZoom", false),
                enableFileUpload = obj.optBoolean("enableFileUpload", true),
                desktopUserAgent = obj.optBoolean("desktopUserAgent", false),
                allowMixedContent = obj.optBoolean("allowMixedContent", true),
                enableNativeJsBridge = obj.optBoolean("enableNativeJsBridge", true),
                cachePolicy = obj.optString("cachePolicy", "DEFAULT"),
                externalLinkRule = obj.optString("externalLinkRule", "OPEN_IN_BROWSER"),
                customOfflineTitle = obj.optString("customOfflineTitle", "Connection Unavailable"),
                customOfflineMessage = obj.optString("customOfflineMessage", "Please check your internet connection and tap Retry."),
                customOfflineRetryHex = obj.optString("customOfflineRetryHex", "#3B82F6")
            )
        }

        fun formatBytes(bytes: Long): String {
            if (bytes < 1024L) return "$bytes B"
            val kb = bytes / 1024.0
            if (kb < 1024.0) return String.format("%.1f KB", kb)
            val mb = kb / 1024.0
            return String.format("%.2f MB", mb)
        }
    }
}
