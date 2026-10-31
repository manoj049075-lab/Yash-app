package com.example.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.builder.ApkPackagingEngine
import com.example.data.BuildOutputFormat
import com.example.data.CachePolicyMode
import com.example.data.ExternalLinkRule
import com.example.data.ScreenOrientationMode
import com.example.data.SourceType
import com.example.data.WebAppProjectEntity
import com.example.ui.theme.ElectricCobalt
import com.example.ui.theme.EmeraldMint

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConfiguratorScreen(
    uiState: StudioUiState,
    onSelectStep: (Int) -> Unit,
    onUpdateDraft: ((WebAppProjectEntity) -> WebAppProjectEntity) -> Unit,
    onImportWebFiles: (List<Uri>) -> Unit,
    onImportIcon: (Uri?) -> Unit,
    onSaveDraft: () -> Unit,
    onBuild: (BuildOutputFormat) -> Unit,
    onLaunchSandbox: () -> Unit,
    onShowJsBridgeDialog: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val draft = uiState.draftProject

    val webFilesLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            onImportWebFiles(uris)
        }
    }

    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        onImportIcon(uri)
    }

    val stepTitles = listOf(
        "1. Source",
        "2. Icon & Splash",
        "3. WebView & UI",
        "4. Build APK"
    )

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = draft.appName.ifBlank { "New APK Project" },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${draft.packageName} • No Watermark",
                            style = MaterialTheme.typography.labelMedium,
                            color = EmeraldMint
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = onSaveDraft,
                            modifier = Modifier.testTag("save_draft_button"),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Save Project",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                        Button(
                            onClick = { onBuild(BuildOutputFormat.SIGNED_APK) },
                            modifier = Modifier.testTag("top_quick_build_apk_button"),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Android,
                                contentDescription = "Build APK",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Build APK")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    stepTitles.forEachIndexed { index, label ->
                        val selected = uiState.activeStep == index
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { onSelectStep(index) }
                                .testTag("step_tab_$index"),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                modifier = Modifier.padding(vertical = 9.dp, horizontal = 4.dp),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (uiState.activeStep) {
                0 -> {
                    item {
                        SourceStepSection(
                            draft = draft,
                            isUrlValid = uiState.isUrlValid,
                            onUpdateDraft = onUpdateDraft,
                            onPickFiles = {
                                webFilesLauncher.launch(
                                    arrayOf(
                                        "text/html",
                                        "text/css",
                                        "application/javascript",
                                        "text/javascript",
                                        "application/zip",
                                        "application/x-zip-compressed",
                                        "image/*",
                                        "*/*"
                                    )
                                )
                            },
                            onBuildNow = { onBuild(BuildOutputFormat.SIGNED_APK) }
                        )
                    }
                }
                1 -> {
                    item {
                        IdentityAndBrandingSection(
                            draft = draft,
                            isPackageValid = uiState.isPackageNameValid,
                            onUpdateDraft = onUpdateDraft,
                            onPickCustomIcon = {
                                iconPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                        )
                    }
                }
                2 -> {
                    item {
                        WebViewAndLayoutSection(
                            draft = draft,
                            onUpdateDraft = onUpdateDraft,
                            onShowJsBridgeInfo = { onShowJsBridgeDialog(true) }
                        )
                    }
                }
                3 -> {
                    item {
                        OfflinePermissionsAndBuildSection(
                            draft = draft,
                            onUpdateDraft = onUpdateDraft,
                            onBuild = onBuild,
                            onLaunchSandbox = onLaunchSandbox
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (uiState.activeStep > 0) {
                        OutlinedButton(
                            onClick = { onSelectStep(uiState.activeStep - 1) },
                            modifier = Modifier.testTag("prev_step_button")
                        ) {
                            Text("Previous Step")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (uiState.activeStep < 3) {
                        Button(
                            onClick = { onSelectStep(uiState.activeStep + 1) },
                            modifier = Modifier.testTag("next_step_button")
                        ) {
                            Text("Next Step")
                        }
                    } else {
                        Button(
                            onClick = { onBuild(BuildOutputFormat.SIGNED_APK) },
                            modifier = Modifier.testTag("footer_build_signed_apk_button")
                        ) {
                            Icon(Icons.Default.Android, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Build Signed APK Now")
                        }
                    }
                }
            }
        }
    }

    if (uiState.showJsBridgeDialog) {
        AlertDialog(
            onDismissRequest = { onShowJsBridgeDialog(false) },
            title = { Text("Native JavaScript Bridge (window.AndroidAPI)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Your HTML/JS files can call these native Android functions directly with zero watermarks:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = """
                                // Show native Android Toast
                                window.AndroidAPI.showToast("Hello from JS!");

                                // Trigger device vibration (ms)
                                window.AndroidAPI.vibrate(80);

                                // Copy text to system clipboard
                                window.AndroidAPI.copyToClipboard("Copied!");

                                // Read version & package ID
                                const ver = window.AndroidAPI.getAppVersion();
                                const pkg = window.AndroidAPI.getPackageName();
                            """.trimIndent(),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onShowJsBridgeDialog(false) }) {
                    Text("Got It")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SourceStepSection(
    draft: WebAppProjectEntity,
    isUrlValid: Boolean,
    onUpdateDraft: ((WebAppProjectEntity) -> WebAppProjectEntity) -> Unit,
    onPickFiles: () -> Unit,
    onBuildNow: () -> Unit
) {
    val isLocal = draft.sourceType == SourceType.LOCAL_FILES.name

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionCard(
            title = "App Name & Source Mode",
            subtitle = "Enter your App Name and choose whether to upload HTML/CSS/JS/ZIP files or convert a Website URL."
        ) {
            OutlinedTextField(
                value = draft.appName,
                onValueChange = { name ->
                    val monogram = name.split(" ")
                        .filter { it.isNotBlank() }
                        .joinToString("") { it.take(1).uppercase() }
                        .take(2)
                        .ifEmpty { "AP" }
                    onUpdateDraft { it.copy(appName = name, iconMonogram = monogram) }
                },
                label = { Text("App Name") },
                placeholder = { Text("Enter your app name") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("step1_app_name_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SourceType.entries.forEach { type ->
                    val selected = draft.sourceType == type.name
                    FilterChip(
                        selected = selected,
                        onClick = { onUpdateDraft { it.copy(sourceType = type.name) } },
                        label = { Text(type.label) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (type == SourceType.LOCAL_FILES) Icons.Default.Code else Icons.Default.Language,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("source_chip_${type.name}")
                    )
                }
            }
        }

        if (isLocal) {
            SectionCard(
                title = "Upload HTML, CSS, JavaScript or ZIP Files",
                subtitle = "Select your .html, .css, .js, media files, or a .zip project archive from your phone."
            ) {
                Button(
                    onClick = onPickFiles,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("upload_html_zip_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.UploadFile, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Upload HTML / CSS / JS / ZIP Files")
                }

                OutlinedTextField(
                    value = draft.entryHtmlFile,
                    onValueChange = { value -> onUpdateDraft { it.copy(entryHtmlFile = value) } },
                    label = { Text("Main Entry HTML File") },
                    placeholder = { Text("index.html") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("entry_html_input")
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = null,
                                tint = if (draft.assetFileCount > 0) EmeraldMint else ElectricCobalt,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (draft.assetFileCount > 0) {
                                    "Uploaded ${draft.assetFileCount} file(s) (${ApkPackagingEngine.formatBytes(draft.assetTotalBytes)})"
                                } else {
                                    "No files uploaded yet — tap the button above to select your files"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        if (draft.assetSummaryList.isNotBlank()) {
                            Text(
                                text = "Files: ${draft.assetSummaryList}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (draft.assetFileCount > 0) {
                    Button(
                        onClick = onBuildNow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("step1_direct_build_apk_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Android, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Build APK Now (Save / Send / Install)")
                    }
                }
            }
        } else {
            SectionCard(
                title = "Website URL to APK",
                subtitle = "Paste your website link below to convert it into a standalone, watermark-free Android APK."
            ) {
                OutlinedTextField(
                    value = draft.websiteUrl,
                    onValueChange = { url -> onUpdateDraft { it.copy(websiteUrl = url) } },
                    label = { Text("Website URL") },
                    placeholder = { Text("https://yourwebsite.com") },
                    singleLine = true,
                    isError = draft.websiteUrl.isNotBlank() && !isUrlValid,
                    supportingText = {
                        Text(
                            if (isUrlValid) "Valid Website URL ready for APK conversion"
                            else "Enter a URL starting with https:// or http://"
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("website_url_input")
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("https://", "http://").forEach { prefix ->
                        FilterChip(
                            selected = draft.websiteUrl.startsWith(prefix),
                            onClick = {
                                onUpdateDraft {
                                    val clean = it.websiteUrl.removePrefix("https://").removePrefix("http://")
                                    it.copy(websiteUrl = "$prefix$clean")
                                }
                            },
                            label = { Text("Insert $prefix") }
                        )
                    }
                }

                if (isUrlValid) {
                    Button(
                        onClick = onBuildNow,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("step1_url_direct_build_apk_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Android, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Build APK Now (Save / Send / Install)")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdentityAndBrandingSection(
    draft: WebAppProjectEntity,
    isPackageValid: Boolean,
    onUpdateDraft: ((WebAppProjectEntity) -> WebAppProjectEntity) -> Unit,
    onPickCustomIcon: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionCard(
            title = "Live App Icon & Splash Preview",
            subtitle = "Guaranteed 100% free of watermarks or forced branding."
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(parseComposeColor(draft.iconBgHex, ElectricCobalt))
                            .border(1.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = draft.iconMonogram.take(2).uppercase(),
                            style = MaterialTheme.typography.headlineMedium,
                            color = parseComposeColor(draft.iconTextHex, Color.White),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = draft.appName.ifBlank { "My App" }.take(12),
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(118.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(parseComposeColor(draft.splashBgHex, Color(0xFF0B0F17)))
                        .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(parseComposeColor(draft.iconBgHex, ElectricCobalt)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = draft.iconMonogram.take(2).uppercase(),
                                style = MaterialTheme.typography.titleSmall,
                                color = parseComposeColor(draft.iconTextHex, Color.White),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = draft.appName.ifBlank { "My App" },
                            style = MaterialTheme.typography.titleSmall,
                            color = Color.White
                        )
                        if (draft.splashTagline.isNotBlank()) {
                            Text(
                                text = draft.splashTagline,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        SectionCard(
            title = "App Name, Package ID & Versioning",
            subtitle = "Configure standard AndroidManifest identity."
        ) {
            OutlinedTextField(
                value = draft.appName,
                onValueChange = { name ->
                    val monogram = name.split(" ")
                        .filter { it.isNotBlank() }
                        .joinToString("") { it.take(1).uppercase() }
                        .take(2)
                        .ifEmpty { "AP" }
                    onUpdateDraft { it.copy(appName = name, iconMonogram = monogram) }
                },
                label = { Text("App Name (Launcher Label)") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("app_name_input")
            )

            OutlinedTextField(
                value = draft.packageName,
                onValueChange = { pkg -> onUpdateDraft { it.copy(packageName = pkg) } },
                label = { Text("Application Package Name") },
                placeholder = { Text("com.company.myapp") },
                singleLine = true,
                isError = !isPackageValid,
                supportingText = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isPackageValid) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = if (isPackageValid) EmeraldMint else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            if (isPackageValid) "Valid Android package identifier"
                            else "Use standard reverse-domain format (e.g. com.company.app)"
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("package_name_input")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = draft.versionName,
                    onValueChange = { v -> onUpdateDraft { it.copy(versionName = v) } },
                    label = { Text("Version Name") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("version_name_input")
                )

                OutlinedTextField(
                    value = draft.versionCode.toString(),
                    onValueChange = { codeStr ->
                        val code = codeStr.filter { it.isDigit() }.toIntOrNull() ?: 1
                        onUpdateDraft { it.copy(versionCode = code.coerceAtLeast(1)) }
                    },
                    label = { Text("Version Code") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("version_code_input")
                )
            }

            SettingToggleRow(
                title = "Auto-Increment Version Code on Every Build",
                subtitle = "Automatically bumps versionCode so updates install cleanly",
                checked = draft.autoIncrementVersion,
                onCheckedChange = { checked -> onUpdateDraft { it.copy(autoIncrementVersion = checked) } },
                testTag = "auto_increment_switch"
            )
        }

        SectionCard(
            title = "Icon & Watermark-Free Splash Screen",
            subtitle = "Pick a custom PNG/JPG icon from your device or style a monogram icon."
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = draft.iconMonogram,
                    onValueChange = { txt -> onUpdateDraft { it.copy(iconMonogram = txt.take(2).uppercase()) } },
                    label = { Text("Icon Monogram") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = draft.iconBgHex,
                    onValueChange = { hex -> onUpdateDraft { it.copy(iconBgHex = hex) } },
                    label = { Text("Icon Color Hex") },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("#2563EB", "#059669", "#7C3AED", "#DC2626", "#D97706", "#0891B2", "#0F172A").forEach { hex ->
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(parseComposeColor(hex, ElectricCobalt))
                            .border(
                                width = if (draft.iconBgHex.equals(hex, ignoreCase = true)) 2.5.dp else 1.dp,
                                color = if (draft.iconBgHex.equals(hex, ignoreCase = true)) Color.White else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { onUpdateDraft { it.copy(iconBgHex = hex) } }
                    )
                }
            }

            OutlinedButton(
                onClick = onPickCustomIcon,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pick_custom_icon_button")
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (draft.customIconPath.isNotBlank()) "Custom Image Icon Selected (Tap to Change)"
                    else "Upload Custom Image Icon from Gallery"
                )
            }

            SettingToggleRow(
                title = "Enable Startup Splash Screen",
                subtitle = "Shows your custom brand splash screen (0 watermarks)",
                checked = draft.splashEnabled,
                onCheckedChange = { en -> onUpdateDraft { it.copy(splashEnabled = en) } },
                testTag = "splash_enabled_switch"
            )

            if (draft.splashEnabled) {
                OutlinedTextField(
                    value = draft.splashTagline,
                    onValueChange = { line -> onUpdateDraft { it.copy(splashTagline = line) } },
                    label = { Text("Splash Screen Subtitle / Tagline (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Splash Duration: ${draft.splashDurationMs} ms",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = draft.splashDurationMs.toFloat(),
                    onValueChange = { v -> onUpdateDraft { it.copy(splashDurationMs = v.toInt()) } },
                    valueRange = 500f..4000f,
                    steps = 6
                )
            }
        }
    }
}

@Composable
private fun WebViewAndLayoutSection(
    draft: WebAppProjectEntity,
    onUpdateDraft: ((WebAppProjectEntity) -> WebAppProjectEntity) -> Unit,
    onShowJsBridgeInfo: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionCard(
            title = "Screen Orientation, Fullscreen & Toolbar",
            subtitle = "Control how your app window behaves on phones and tablets."
        ) {
            Text(
                text = "Screen Orientation:",
                style = MaterialTheme.typography.titleSmall
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ScreenOrientationMode.entries.forEach { mode ->
                    FilterChip(
                        selected = draft.orientationMode == mode.name,
                        onClick = { onUpdateDraft { it.copy(orientationMode = mode.name) } },
                        label = { Text(mode.label) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SettingToggleRow(
                title = "Fullscreen Immersive Mode",
                subtitle = "Hides Android status bar and navigation bar",
                checked = draft.fullscreenMode,
                onCheckedChange = { v -> onUpdateDraft { it.copy(fullscreenMode = v) } },
                testTag = "fullscreen_switch"
            )

            SettingToggleRow(
                title = "Show Top App Toolbar",
                subtitle = "Displays a native top bar with app title and refresh button",
                checked = draft.showTopToolbar,
                onCheckedChange = { v -> onUpdateDraft { it.copy(showTopToolbar = v) } },
                testTag = "toolbar_switch"
            )

            SettingToggleRow(
                title = "Swipe Pull-to-Refresh",
                subtitle = "Lets users pull down to reload the page",
                checked = draft.pullToRefresh,
                onCheckedChange = { v -> onUpdateDraft { it.copy(pullToRefresh = v) } },
                testTag = "pull_refresh_switch"
            )

            SettingToggleRow(
                title = "Back Button History Navigation",
                subtitle = "Navigates backward through visited pages before closing the app",
                checked = draft.backButtonHistory,
                onCheckedChange = { v -> onUpdateDraft { it.copy(backButtonHistory = v) } },
                testTag = "back_history_switch"
            )

            SettingToggleRow(
                title = "Keep Screen Awake",
                subtitle = "Prevents screen dimming while your app is open",
                checked = draft.keepScreenOn,
                onCheckedChange = { v -> onUpdateDraft { it.copy(keepScreenOn = v) } }
            )
        }

        SectionCard(
            title = "WebView Engine, Storage & Native Bridge",
            subtitle = "Enable HTML5 APIs, file uploads, zoom controls, and native Android hooks."
        ) {
            SettingToggleRow(
                title = "JavaScript Execution",
                subtitle = "Required for interactive web apps and scripts",
                checked = draft.enableJavaScript,
                onCheckedChange = { v -> onUpdateDraft { it.copy(enableJavaScript = v) } },
                testTag = "js_switch"
            )

            SettingToggleRow(
                title = "DOM Storage (localStorage & IndexedDB)",
                subtitle = "Persists client-side app state and offline data",
                checked = draft.enableDomStorage,
                onCheckedChange = { v -> onUpdateDraft { it.copy(enableDomStorage = v) } },
                testTag = "dom_storage_switch"
            )

            SettingToggleRow(
                title = "Pinch-to-Zoom Gesture Support",
                subtitle = "Allows multi-touch zooming on pages",
                checked = draft.enableZoom,
                onCheckedChange = { v -> onUpdateDraft { it.copy(enableZoom = v) } },
                testTag = "zoom_switch"
            )

            SettingToggleRow(
                title = "File Upload & Download Manager",
                subtitle = "Bridges HTML file inputs and downloads",
                checked = draft.enableFileUpload,
                onCheckedChange = { v -> onUpdateDraft { it.copy(enableFileUpload = v, enableDownloads = v) } },
                testTag = "file_upload_switch"
            )

            SettingToggleRow(
                title = "Desktop User-Agent Mode",
                subtitle = "Requests full desktop layout from websites",
                checked = draft.desktopUserAgent,
                onCheckedChange = { v -> onUpdateDraft { it.copy(desktopUserAgent = v) } }
            )

            SettingToggleRow(
                title = "Native JS Hardware Bridge (window.AndroidAPI)",
                subtitle = "Exposes native Toast, Vibration, Clipboard, and Version APIs to JavaScript",
                checked = draft.enableNativeJsBridge,
                onCheckedChange = { v -> onUpdateDraft { it.copy(enableNativeJsBridge = v) } },
                testTag = "js_bridge_switch"
            )

            OutlinedButton(
                onClick = onShowJsBridgeInfo,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("view_js_bridge_docs_button")
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("View window.AndroidAPI Code Examples")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OfflinePermissionsAndBuildSection(
    draft: WebAppProjectEntity,
    onUpdateDraft: ((WebAppProjectEntity) -> WebAppProjectEntity) -> Unit,
    onBuild: (BuildOutputFormat) -> Unit,
    onLaunchSandbox: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionCard(
            title = "Build & Install Output (Zero Watermarks)",
            subtitle = "Build your signed APK and immediately Save, Send, or Install it on your phone."
        ) {
            Button(
                onClick = { onBuild(BuildOutputFormat.SIGNED_APK) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("build_signed_apk_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Android, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Build Signed APK (Save / Send / Install)")
            }

            OutlinedButton(
                onClick = { onBuild(BuildOutputFormat.UNSIGNED_APK) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("build_unsigned_apk_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Verified, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Build Unsigned APK Package")
            }

            OutlinedButton(
                onClick = { onBuild(BuildOutputFormat.ANDROID_STUDIO_ZIP) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("build_studio_zip_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Archive, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Android Studio Project (.zip)")
            }

            OutlinedButton(
                onClick = onLaunchSandbox,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("launch_sandbox_step4_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Preview App Inside Studio")
            }
        }

        SectionCard(
            title = "Custom Offline Error Page & External Links",
            subtitle = "Replace Android's default WebView error screen with your own branded offline page."
        ) {
            OutlinedTextField(
                value = draft.customOfflineTitle,
                onValueChange = { t -> onUpdateDraft { it.copy(customOfflineTitle = t) } },
                label = { Text("Offline Screen Title") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("offline_title_input")
            )

            OutlinedTextField(
                value = draft.customOfflineMessage,
                onValueChange = { m -> onUpdateDraft { it.copy(customOfflineMessage = m) } },
                label = { Text("Offline Screen Message") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("offline_message_input")
            )

            Text(
                text = "External Link Handling Rule:",
                style = MaterialTheme.typography.titleSmall
            )
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ExternalLinkRule.entries.forEach { rule ->
                    FilterChip(
                        selected = draft.externalLinkRule == rule.name,
                        onClick = { onUpdateDraft { it.copy(externalLinkRule = rule.name) } },
                        label = { Text(rule.label) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Text(
                text = "Cache Mode:",
                style = MaterialTheme.typography.titleSmall
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CachePolicyMode.entries.forEach { mode ->
                    FilterChip(
                        selected = draft.cachePolicy == mode.name,
                        onClick = { onUpdateDraft { it.copy(cachePolicy = mode.name) } },
                        label = { Text(mode.label) }
                    )
                }
            }
        }

        SectionCard(
            title = "Android Manifest Hardware Permissions",
            subtitle = "Toggle only the permissions required by your web project."
        ) {
            SettingToggleRow(
                title = "Internet & Network State",
                subtitle = "android.permission.INTERNET",
                checked = draft.permInternet,
                onCheckedChange = { v -> onUpdateDraft { it.copy(permInternet = v) } }
            )
            SettingToggleRow(
                title = "Camera Access",
                subtitle = "android.permission.CAMERA",
                checked = draft.permCamera,
                onCheckedChange = { v -> onUpdateDraft { it.copy(permCamera = v) } }
            )
            SettingToggleRow(
                title = "Microphone Access",
                subtitle = "android.permission.RECORD_AUDIO",
                checked = draft.permMicrophone,
                onCheckedChange = { v -> onUpdateDraft { it.copy(permMicrophone = v) } }
            )
            SettingToggleRow(
                title = "GPS / Geolocation API",
                subtitle = "android.permission.ACCESS_FINE_LOCATION",
                checked = draft.permLocation,
                onCheckedChange = { v -> onUpdateDraft { it.copy(permLocation = v) } }
            )
            SettingToggleRow(
                title = "Haptic Vibration",
                subtitle = "android.permission.VIBRATE",
                checked = draft.permVibrate,
                onCheckedChange = { v -> onUpdateDraft { it.copy(permVibrate = v) } }
            )
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    testTag: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = if (testTag != null) Modifier.testTag(testTag) else Modifier
        )
    }
}
