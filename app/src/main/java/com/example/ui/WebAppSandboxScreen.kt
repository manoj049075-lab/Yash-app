package com.example.ui

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.CachePolicyMode
import com.example.data.ExternalLinkRule
import com.example.data.SourceType
import com.example.data.WebAppProjectEntity
import com.example.ui.theme.ElectricCobalt
import com.example.ui.theme.EmeraldMint
import kotlinx.coroutines.delay
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebAppSandboxScreen(
    project: WebAppProjectEntity,
    isStandaloneApk: Boolean = false,
    onExitSandbox: () -> Unit
) {
    var showSplash by remember(project.id) { mutableStateOf(project.splashEnabled) }
    var simulateOffline by remember { mutableStateOf(false) }
    var hasNetworkError by remember { mutableStateOf(false) }
    var pageProgress by remember { mutableIntStateOf(0) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var fileUploadCallback by remember { mutableStateOf<ValueCallback<Array<Uri>>?>(null) }

    val fileChooserLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        val callback = fileUploadCallback
        if (uri != null) {
            callback?.onReceiveValue(arrayOf(uri))
        } else {
            callback?.onReceiveValue(null)
        }
        fileUploadCallback = null
    }

    LaunchedEffect(project.id, project.splashEnabled) {
        if (project.splashEnabled) {
            delay(project.splashDurationMs.toLong().coerceIn(400L, 4000L))
            showSplash = false
        }
    }

    BackHandler {
        val wv = webViewRef
        if (project.backButtonHistory && wv != null && wv.canGoBack()) {
            wv.goBack()
        } else {
            onExitSandbox()
        }
    }

    val startUrl = remember(project, isStandaloneApk) {
        if (project.sourceType == SourceType.LOCAL_FILES.name) {
            val entryName = project.entryHtmlFile.ifBlank { "index.html" }
            if (isStandaloneApk) {
                "file:///android_asset/webapp/$entryName"
            } else {
                val dir = File(project.workspaceDirPath)
                val entry = File(dir, entryName)
                if (entry.exists()) {
                    "file://${entry.absolutePath}"
                } else {
                    "file:///android_asset/webapp/$entryName"
                }
            }
        } else {
            project.websiteUrl
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(parseComposeColor(project.splashBgHex, Color(0xFF0B0F17)))
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag("sandbox_root")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (project.showTopToolbar) {
                Surface(
                    color = parseComposeColor(project.statusBarHex, Color(0xFF0B0F17)),
                    tonalElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = {
                                val wv = webViewRef
                                if (wv != null && wv.canGoBack()) wv.goBack() else onExitSandbox()
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                            Text(
                                text = project.appName,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                        if (project.pullToRefresh) {
                            IconButton(onClick = {
                                hasNetworkError = false
                                webViewRef?.reload()
                            }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh",
                                    tint = Color.White
                                )
                            }
                        }
                    }
                }
            }

            if (pageProgress in 1..99) {
                LinearProgressIndicator(
                    progress = { pageProgress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = ElectricCobalt
                )
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (simulateOffline || hasNetworkError) {
                    CustomOfflineErrorOverlay(
                        project = project,
                        onRetry = {
                            simulateOffline = false
                            hasNetworkError = false
                            webViewRef?.reload()
                        }
                    )
                } else {
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.javaScriptEnabled = project.enableJavaScript
                                settings.domStorageEnabled = project.enableDomStorage
                                settings.databaseEnabled = project.enableDomStorage
                                settings.setSupportZoom(project.enableZoom)
                                settings.builtInZoomControls = project.enableZoom
                                settings.displayZoomControls = false
                                settings.allowFileAccess = true
                                settings.allowContentAccess = true
                                if (project.desktopUserAgent) {
                                    settings.userAgentString =
                                        "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                                }
                                if (project.allowMixedContent) {
                                    settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                }
                                settings.cacheMode = when (project.cachePolicy) {
                                    CachePolicyMode.CACHE_FIRST_OFFLINE.name -> WebSettings.LOAD_CACHE_ELSE_NETWORK
                                    CachePolicyMode.NO_CACHE.name -> WebSettings.LOAD_NO_CACHE
                                    else -> WebSettings.LOAD_DEFAULT
                                }

                                if (project.enableNativeJsBridge) {
                                    addJavascriptInterface(
                                        SandboxAndroidBridge(ctx, project),
                                        "AndroidAPI"
                                    )
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val target = request?.url?.toString() ?: return false
                                        if (project.externalLinkRule == ExternalLinkRule.OPEN_IN_BROWSER.name &&
                                            target.startsWith("http") &&
                                            project.websiteUrl.isNotBlank()
                                        ) {
                                            val baseHost = Uri.parse(project.websiteUrl).host.orEmpty()
                                            val targetHost = request.url.host.orEmpty()
                                            if (baseHost.isNotBlank() && targetHost.isNotBlank() && !targetHost.endsWith(baseHost)) {
                                                ctx.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                                                return true
                                            }
                                        }
                                        return false
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        if (request?.isForMainFrame == true) {
                                            hasNetworkError = true
                                        }
                                    }
                                }

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        pageProgress = newProgress
                                    }

                                    override fun onShowFileChooser(
                                        webView: WebView?,
                                        filePathCallback: ValueCallback<Array<Uri>>?,
                                        fileChooserParams: FileChooserParams?
                                    ): Boolean {
                                        if (!project.enableFileUpload) return false
                                        fileUploadCallback = filePathCallback
                                        fileChooserLauncher.launch("*/*")
                                        return true
                                    }
                                }

                                loadUrl(startUrl)
                                webViewRef = this
                            }
                        }
                    )
                }
            }

            // Show Sandbox Inspector bar only when previewing inside Web2APK Studio (hidden in standalone installed APKs!)
            if (!isStandaloneApk) {
                Surface(
                    color = Color(0xFF0F172A).copy(alpha = 0.96f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.navigationBars)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    simulateOffline = false
                                    hasNetworkError = false
                                    webViewRef?.reload()
                                },
                                modifier = Modifier.testTag("sandbox_reload_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload App",
                                    tint = Color.White
                                )
                            }

                            IconButton(
                                onClick = { simulateOffline = !simulateOffline },
                                modifier = Modifier.testTag("sandbox_offline_toggle")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudOff,
                                    contentDescription = "Preview Custom Offline Page",
                                    tint = if (simulateOffline) EmeraldMint else Color(0xFF94A3B8)
                                )
                            }

                            if (project.enableNativeJsBridge) {
                                IconButton(
                                    onClick = {
                                        webViewRef?.evaluateJavascript(
                                            "if(window.AndroidAPI){window.AndroidAPI.showToast('AndroidAPI Bridge Active (' + window.AndroidAPI.getPackageName() + ')');}",
                                            null
                                        )
                                    },
                                    modifier = Modifier.testTag("sandbox_test_bridge_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Code,
                                        contentDescription = "Test JS Bridge",
                                        tint = ElectricCobalt
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = onExitSandbox,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("exit_sandbox_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Exit Preview")
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showSplash,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(parseComposeColor(project.splashBgHex, Color(0xFF0B0F17))),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(parseComposeColor(project.iconBgHex, ElectricCobalt)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = project.iconMonogram.take(2).uppercase(),
                            style = MaterialTheme.typography.displaySmall,
                            color = parseComposeColor(project.iconTextHex, Color.White),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = project.appName,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    if (project.splashTagline.isNotBlank()) {
                        Text(
                            text = project.splashTagline,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomOfflineErrorOverlay(
    project: WebAppProjectEntity,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(parseComposeColor(project.splashBgHex, Color(0xFF0B0F17)))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(22.dp))
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                tint = parseComposeColor(project.customOfflineRetryHex, ElectricCobalt),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = project.customOfflineTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = project.customOfflineMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(
                    containerColor = parseComposeColor(project.customOfflineRetryHex, ElectricCobalt)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("offline_retry_button")
            ) {
                Text("Retry Connection", color = Color.White)
            }
        }
    }
}

private class SandboxAndroidBridge(
    private val context: Context,
    private val project: WebAppProjectEntity
) {
    @JavascriptInterface
    fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    @JavascriptInterface
    fun vibrate(durationMs: Long) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(
                VibrationEffect.createOneShot(
                    durationMs.coerceIn(10L, 1500L),
                    VibrationEffect.DEFAULT_AMPLITUDE
                )
            )
        }
    }

    @JavascriptInterface
    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("WebApp", text))
    }

    @JavascriptInterface
    fun getAppVersion(): String = project.versionName

    @JavascriptInterface
    fun getPackageName(): String = project.packageName
}
