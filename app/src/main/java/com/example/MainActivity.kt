package com.example

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Android
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.builder.ApkPackagingEngine
import com.example.data.StudioDatabase
import com.example.data.StudioRepository
import com.example.data.WebAppProjectEntity
import com.example.ui.BuildConsoleDialog
import com.example.ui.BuildsScreen
import com.example.ui.ConfiguratorScreen
import com.example.ui.DashboardScreen
import com.example.ui.StudioTab
import com.example.ui.StudioUiState
import com.example.ui.StudioViewModel
import com.example.ui.WebAppSandboxScreen
import com.example.ui.theme.ElectricCobalt
import com.example.ui.theme.EmeraldMint
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check if this APK is a built standalone WebApp package containing assets/embedded_webapp_config.json
        val embeddedProject = readEmbeddedWebAppProject()
        if (embeddedProject != null) {
            applyStandaloneWindowConfig(embeddedProject)
            setContent {
                MyApplicationTheme(darkTheme = true) {
                    WebAppSandboxScreen(
                        project = embeddedProject,
                        isStandaloneApk = true,
                        onExitSandbox = { finish() }
                    )
                }
            }
            return
        }

        val appContext = applicationContext
        val database = StudioDatabase.getInstance(appContext)
        val repository = StudioRepository(appContext, database.studioDao())
        val packagingEngine = ApkPackagingEngine(appContext)

        setContent {
            val viewModel: StudioViewModel = viewModel(
                factory = StudioViewModel.Factory(appContext, repository, packagingEngine)
            )
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = uiState.isDarkTheme) {
                Web2ApkStudioApp(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }

    private fun readEmbeddedWebAppProject(): WebAppProjectEntity? {
        return try {
            assets.open("embedded_webapp_config.json").use { input ->
                val jsonText = input.bufferedReader().readText()
                ApkPackagingEngine.parseEmbeddedConfig(jsonText)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun applyStandaloneWindowConfig(project: WebAppProjectEntity) {
        if (project.keepScreenOn) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        requestedOrientation = when (project.orientationMode) {
            "LANDSCAPE" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            "AUTO_SENSOR" -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        if (project.fullscreenMode) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Web2ApkStudioApp(
    uiState: StudioUiState,
    viewModel: StudioViewModel
) {
    val context = LocalContext.current

    val activeSandbox = uiState.sandboxProject
    if (activeSandbox != null) {
        WebAppSandboxScreen(
            project = activeSandbox,
            isStandaloneApk = false,
            onExitSandbox = { viewModel.closeSandbox() }
        )
        return
    }

    if (uiState.activeTab != StudioTab.DASHBOARD) {
        BackHandler {
            viewModel.selectTab(StudioTab.DASHBOARD)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 640.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElectricCobalt),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Code,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Web2APK Studio",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "Zero-Watermark HTML/ZIP & URL Builder",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = EmeraldMint
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { viewModel.toggleTheme() },
                            modifier = Modifier.testTag("theme_toggle_button")
                        ) {
                            Icon(
                                imageVector = if (uiState.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle Dark/Light Theme"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            },
            bottomBar = {
                if (!isWideScreen) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        NavigationBarItem(
                            selected = uiState.activeTab == StudioTab.DASHBOARD,
                            onClick = { viewModel.selectTab(StudioTab.DASHBOARD) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.activeTab == StudioTab.DASHBOARD) Icons.Filled.Dashboard else Icons.Outlined.Dashboard,
                                    contentDescription = "Studio"
                                )
                            },
                            label = { Text("Studio") },
                            modifier = Modifier.testTag("nav_dashboard")
                        )
                        NavigationBarItem(
                            selected = uiState.activeTab == StudioTab.CONFIGURATOR,
                            onClick = { viewModel.selectTab(StudioTab.CONFIGURATOR) },
                            icon = {
                                Icon(
                                    imageVector = if (uiState.activeTab == StudioTab.CONFIGURATOR) Icons.Filled.Tune else Icons.Outlined.Tune,
                                    contentDescription = "Configurator"
                                )
                            },
                            label = { Text("Configurator") },
                            modifier = Modifier.testTag("nav_configurator")
                        )
                        NavigationBarItem(
                            selected = uiState.activeTab == StudioTab.BUILDS,
                            onClick = { viewModel.selectTab(StudioTab.BUILDS) },
                            icon = {
                                BadgedBox(
                                    badge = {
                                        if (uiState.builds.isNotEmpty()) {
                                            Badge(containerColor = EmeraldMint) {
                                                Text(
                                                    text = uiState.builds.size.toString(),
                                                    color = Color(0xFF042F2E),
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (uiState.activeTab == StudioTab.BUILDS) Icons.Filled.Android else Icons.Outlined.Android,
                                        contentDescription = "Builds"
                                    )
                                }
                            },
                            label = { Text("APK & ZIPs") },
                            modifier = Modifier.testTag("nav_builds")
                        )
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isWideScreen) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        NavigationRailItem(
                            selected = uiState.activeTab == StudioTab.DASHBOARD,
                            onClick = { viewModel.selectTab(StudioTab.DASHBOARD) },
                            icon = { Icon(Icons.Filled.Dashboard, contentDescription = "Studio") },
                            label = { Text("Studio") }
                        )
                        NavigationRailItem(
                            selected = uiState.activeTab == StudioTab.CONFIGURATOR,
                            onClick = { viewModel.selectTab(StudioTab.CONFIGURATOR) },
                            icon = { Icon(Icons.Filled.Tune, contentDescription = "Configurator") },
                            label = { Text("Configurator") }
                        )
                        NavigationRailItem(
                            selected = uiState.activeTab == StudioTab.BUILDS,
                            onClick = { viewModel.selectTab(StudioTab.BUILDS) },
                            icon = { Icon(Icons.Filled.Android, contentDescription = "APK & ZIPs") },
                            label = { Text("Builds (${uiState.builds.size})") }
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    AnimatedVisibility(visible = uiState.statusBanner != null) {
                        uiState.statusBanner?.let { message ->
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { viewModel.clearStatusBanner() }) {
                                        Text("OK")
                                    }
                                }
                            }
                        }
                    }

                    when (uiState.activeTab) {
                        StudioTab.DASHBOARD -> {
                            DashboardScreen(
                                uiState = uiState,
                                onNewProject = { sourceType -> viewModel.startNewProject(sourceType) },
                                onEditProject = { project -> viewModel.openProjectForEditing(project) },
                                onBuildProject = { project, format -> viewModel.buildProject(project, format) },
                                onTestProject = { project -> viewModel.launchSandbox(project) },
                                onDeleteProject = { project -> viewModel.deleteProject(project) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        StudioTab.CONFIGURATOR -> {
                            ConfiguratorScreen(
                                uiState = uiState,
                                onSelectStep = { step -> viewModel.selectStep(step) },
                                onUpdateDraft = { transform -> viewModel.updateDraft(transform) },
                                onImportWebFiles = { uris -> viewModel.importWebFiles(uris) },
                                onImportIcon = { uri -> viewModel.importCustomIcon(uri) },
                                onSaveDraft = { viewModel.saveCurrentDraft(true) },
                                onBuild = { format -> viewModel.buildProject(format = format) },
                                onLaunchSandbox = { viewModel.launchSandbox() },
                                onShowJsBridgeDialog = { show -> viewModel.setShowJsBridgeDialog(show) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        StudioTab.BUILDS -> {
                            BuildsScreen(
                                uiState = uiState,
                                onSaveApkToDownloads = { record -> viewModel.saveApkToDownloads(context, record) },
                                onSaveBuildToUri = { record, uri -> viewModel.exportBuildToUri(record, uri) },
                                onSendBuild = { record -> viewModel.sendBuildArtifact(context, record) },
                                onInstallBuild = { record -> viewModel.installApkArtifact(context, record) },
                                onDeleteBuild = { record -> viewModel.deleteBuildRecord(record) },
                                onGoToConfigurator = { viewModel.selectTab(StudioTab.CONFIGURATOR) },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }

    BuildConsoleDialog(
        consoleState = uiState.buildConsole,
        onDismiss = { viewModel.dismissBuildConsole() },
        onSaveCompletedToDownloads = { record -> viewModel.saveApkToDownloads(context, record) },
        onSendCompleted = { record -> viewModel.sendBuildArtifact(context, record) },
        onInstallCompleted = { record -> viewModel.installApkArtifact(context, record) },
        onViewAllBuilds = { viewModel.selectTab(StudioTab.BUILDS) }
    )
}
