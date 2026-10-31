package com.example.ui

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircleOutline
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.builder.ApkPackagingEngine
import com.example.data.BuildOutputFormat
import com.example.data.SourceType
import com.example.data.WebAppProjectEntity
import com.example.ui.theme.CyanGlow
import com.example.ui.theme.ElectricCobalt
import com.example.ui.theme.EmeraldMint

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    uiState: StudioUiState,
    onNewProject: (SourceType) -> Unit,
    onEditProject: (WebAppProjectEntity) -> Unit,
    onBuildProject: (WebAppProjectEntity, BuildOutputFormat) -> Unit,
    onTestProject: (WebAppProjectEntity) -> Unit,
    onDeleteProject: (WebAppProjectEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Studio Hero Banner
        item {
            StudioHeroBanner(
                projectCount = uiState.projects.size,
                buildCount = uiState.builds.size
            )
        }

        // 2. Quick Start Action Cards (HTML/CSS/JS/ZIP Upload vs. Website URL)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Start New APK Build",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickCreateCard(
                        title = "HTML / CSS / JS / ZIP",
                        subtitle = "Upload your .html, .css, .js, images or .zip archive to create an offline APK",
                        badgeText = "UPLOAD FILES",
                        icon = Icons.Default.FolderZip,
                        accentColor = ElectricCobalt,
                        testTag = "create_html_project_card",
                        onClick = { onNewProject(SourceType.LOCAL_FILES) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickCreateCard(
                        title = "Website URL to APK",
                        subtitle = "Enter any live Website URL to convert into an installable Android APK",
                        badgeText = "ENTER LINK",
                        icon = Icons.Default.Language,
                        accentColor = EmeraldMint,
                        testTag = "create_url_project_card",
                        onClick = { onNewProject(SourceType.WEBSITE_URL) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 3. Saved Projects Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Projects (${uiState.projects.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "No Watermark Verified",
                            tint = EmeraldMint,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "No Watermark • Save / Send / Install",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        // 4. Clean Empty State or Real User Projects
        if (uiState.projects.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                RoundedCornerShape(18.dp)
                            )
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircleOutline,
                            contentDescription = null,
                            tint = ElectricCobalt,
                            modifier = Modifier.size(42.dp)
                        )
                        Text(
                            text = "No Saved Projects Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap 'HTML / CSS / JS / ZIP' or 'Website URL to APK' above to upload your real files or website link and build your APK.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(uiState.projects, key = { it.id }) { project ->
                ProjectDashboardCard(
                    project = project,
                    onEdit = { onEditProject(project) },
                    onBuildSignedApk = { onBuildProject(project, BuildOutputFormat.SIGNED_APK) },
                    onBuildStudioZip = { onBuildProject(project, BuildOutputFormat.ANDROID_STUDIO_ZIP) },
                    onTestLive = { onTestProject(project) },
                    onDelete = { onDeleteProject(project) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StudioHeroBanner(
    projectCount: Int,
    buildCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF0F172A),
                            Color(0xFF1E3A8A),
                            Color(0xFF064E3B)
                        )
                    )
                )
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(22.dp)
                )
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(175.dp)
            ) {
                val w = size.width
                val h = size.height
                drawCircle(
                    color = CyanGlow.copy(alpha = 0.14f),
                    radius = h * 0.65f,
                    center = Offset(w * 0.88f, h * 0.25f)
                )
                val hexPath = Path().apply {
                    val cx = w * 0.85f
                    val cy = h * 0.48f
                    val r = 44.dp.toPx()
                    moveTo(cx, cy - r)
                    lineTo(cx + r * 0.86f, cy - r * 0.5f)
                    lineTo(cx + r * 0.86f, cy + r * 0.5f)
                    lineTo(cx, cy + r)
                    lineTo(cx - r * 0.86f, cy + r * 0.5f)
                    lineTo(cx - r * 0.86f, cy - r * 0.5f)
                    close()
                }
                drawPath(
                    path = hexPath,
                    color = CyanGlow.copy(alpha = 0.35f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = EmeraldMint.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(999.dp),
                    modifier = Modifier.border(
                        1.dp,
                        EmeraldMint.copy(alpha = 0.5f),
                        RoundedCornerShape(999.dp)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Verified,
                            contentDescription = "Watermark free",
                            tint = EmeraldMint,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "100% WATERMARK-FREE • 1. SAVE • 2. SEND • 3. INSTALL",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "HTML, CSS, JS & Website to APK Builder",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )

                Text(
                    text = "Upload your HTML/CSS/JS/ZIP files or paste any Website URL. Build a signed APK and immediately Save, Send, or Install it on your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFCBD5E1)
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatPill(label = "Projects", value = projectCount.toString())
                    StatPill(label = "Built APKs", value = buildCount.toString())
                    StatPill(label = "Watermarks", value = "0")
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = CyanGlow,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF94A3B8)
        )
    }
}

@Composable
private fun QuickCreateCard(
    title: String,
    subtitle: String,
    badgeText: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = accentColor.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(18.dp)
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(accentColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = accentColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelSmall,
                    color = accentColor
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                minLines = 3
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProjectDashboardCard(
    project: WebAppProjectEntity,
    onEdit: () -> Unit,
    onBuildSignedApk: () -> Unit,
    onBuildStudioZip: () -> Unit,
    onTestLive: () -> Unit,
    onDelete: () -> Unit
) {
    val isLocal = project.sourceType == SourceType.LOCAL_FILES.name
    val iconBg = parseComposeColor(project.iconBgHex, ElectricCobalt)
    val iconFg = parseComposeColor(project.iconTextHex, Color.White)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("project_card_${project.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = project.iconMonogram.take(2).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = iconFg,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = project.appName.ifBlank { "Untitled App" },
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Surface(
                            color = if (isLocal) ElectricCobalt.copy(alpha = 0.15f) else EmeraldMint.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = if (isLocal) "HTML/ZIP" else "URL",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isLocal) ElectricCobalt else EmeraldMint,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = "${project.packageName} • v${project.versionName} (${project.versionCode})",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = if (isLocal) {
                            "${project.assetFileCount} file(s) (${ApkPackagingEngine.formatBytes(project.assetTotalBytes)}) • Entry: ${project.entryHtmlFile}"
                        } else {
                            project.websiteUrl
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_project_${project.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Configure Project",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_project_${project.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete Project",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FeatureMicroTag(text = "No Watermark")
                FeatureMicroTag(text = project.orientationMode)
                if (project.fullscreenMode) FeatureMicroTag(text = "Fullscreen")
                if (project.pullToRefresh) FeatureMicroTag(text = "Pull-to-Refresh")
                if (project.enableNativeJsBridge) FeatureMicroTag(text = "window.AndroidAPI")
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onBuildSignedApk,
                    modifier = Modifier
                        .weight(1.15f)
                        .testTag("quick_build_apk_${project.id}"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Android,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Build APK",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                FilledTonalButton(
                    onClick = onBuildStudioZip,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("quick_build_zip_${project.id}"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Studio ZIP",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                OutlinedButton(
                    onClick = onTestLive,
                    modifier = Modifier
                        .weight(0.95f)
                        .testTag("quick_test_app_${project.id}"),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Preview",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureMicroTag(text: String) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

fun parseComposeColor(hex: String, fallback: Color): Color {
    return try {
        val clean = if (hex.trim().startsWith("#")) hex.trim() else "#${hex.trim()}"
        Color(android.graphics.Color.parseColor(clean))
    } catch (_: Exception) {
        fallback
    }
}
