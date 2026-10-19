package com.ranzan.zbuilds.ui.settings

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ranzan.zbuilds.domain.model.AppSettings
import com.ranzan.zbuilds.ui.theme.AccentGreen
import com.ranzan.zbuilds.ui.theme.AccentGreenContainer
import com.ranzan.zbuilds.ui.theme.AccentGreenText
import com.ranzan.zbuilds.ui.theme.AccentOrange
import com.ranzan.zbuilds.ui.theme.AccentRed
import com.ranzan.zbuilds.ui.theme.AccentRedContainer
import com.ranzan.zbuilds.ui.theme.DarkBorder
import com.ranzan.zbuilds.ui.theme.DarkSurface
import com.ranzan.zbuilds.ui.theme.DarkSurfaceElevated
import com.ranzan.zbuilds.ui.theme.PureBlack
import com.ranzan.zbuilds.ui.theme.TextMuted
import com.ranzan.zbuilds.ui.theme.TextPrimary
import com.ranzan.zbuilds.ui.theme.TextSecondary
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    var isTokenVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.snackbarMessage) {
        state.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onEvent(SettingsUiEvent.OnClearSnackbar)
        }
    }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(treeUri, flags)
                val doc = DocumentFile.fromTreeUri(context, treeUri)
                viewModel.onEvent(
                    SettingsUiEvent.OnFolderUpdated(
                        uri = treeUri.toString(),
                        path = doc?.name ?: treeUri.lastPathSegment ?: "Custom Folder"
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PureBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PureBlack,
                    titleContentColor = TextPrimary
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: GitHub Token
            SettingsSectionCard(title = "GitHub Token") {
                Text(
                    text = "Optional Personal Access Token to bypass GitHub's 60 req/hr rate limits when checking releases and fetching assets:",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Action Buttons: Generate & Paste
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val url = "https://github.com/settings/tokens/new?description=ZBuilds%20Android%20App&scopes=public_repo"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, DarkBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Generate",
                            modifier = Modifier.size(15.dp),
                            tint = AccentGreen
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Generate", fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                val clip = clipboard?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                                if (!clip.isNullOrBlank()) {
                                    viewModel.onEvent(SettingsUiEvent.OnTokenInputChanged(clip))
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, DarkBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = "Paste",
                            modifier = Modifier.size(15.dp),
                            tint = AccentGreen
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Paste", fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = state.tokenInput,
                    onValueChange = { viewModel.onEvent(SettingsUiEvent.OnTokenInputChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("ghp_xxxxxxxxxxxx", color = TextMuted) },
                    visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        if (state.tokenInput.isNotEmpty()) {
                            IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                                Icon(
                                    imageVector = if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (isTokenVisible) "Hide token" else "Show token",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = AccentGreen,
                        unfocusedBorderColor = DarkBorder,
                        focusedContainerColor = DarkSurfaceElevated,
                        unfocusedContainerColor = DarkSurfaceElevated
                    ),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )

                if (state.settings.githubToken != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "✓ Token Saved & Active",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentGreenText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = "••••••••••••${state.settings.githubToken?.takeLast(4)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.onEvent(SettingsUiEvent.OnSaveToken)
                            isTokenVisible = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGreen,
                            contentColor = PureBlack
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { viewModel.onEvent(SettingsUiEvent.OnTestToken) },
                        enabled = !state.isValidatingToken && state.tokenInput.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceElevated,
                            contentColor = TextPrimary
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        if (state.isValidatingToken) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = AccentGreen,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Test")
                        }
                    }

                    if (state.settings.githubToken != null || state.tokenInput.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                viewModel.onEvent(SettingsUiEvent.OnClearToken)
                                isTokenVisible = false
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = BorderStroke(1.dp, DarkBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Clear")
                        }
                    }
                }

                // Test result feedback box
                if (state.tokenValidationMessage != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (state.isTokenValid) AccentGreenContainer else AccentRedContainer)
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = state.tokenValidationMessage.orEmpty(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (state.isTokenValid) AccentGreenText else AccentRed,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.onEvent(SettingsUiEvent.OnDismissTokenFeedback) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = if (state.isTokenValid) AccentGreenText else AccentRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Build Formats
            SettingsSectionCard(title = "Build Formats") {
                Text(
                    text = "Select which build formats to display. If both are enabled, both APK and ZIP assets will be tracked:",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val showApks = state.settings.showApks
                    val showModules = state.settings.showModules

                    // APKs Toggle Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (showApks) AccentGreenContainer else DarkSurfaceElevated)
                            .border(
                                1.dp,
                                if (showApks) AccentGreen else DarkBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                viewModel.onEvent(
                                    SettingsUiEvent.OnToggleBuildFormats(
                                        showApks = !showApks,
                                        showModules = showModules
                                    )
                                )
                            }
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "APKs",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = if (showApks) AccentGreenText else TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                if (showApks) {
                                    Text("✓", color = AccentGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Standard Android apps",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (showApks) TextSecondary else TextMuted
                                )
                            )
                        }
                    }

                    // Modules Toggle Card
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (showModules) AccentGreenContainer else DarkSurfaceElevated)
                            .border(
                                1.dp,
                                if (showModules) AccentGreen else DarkBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                viewModel.onEvent(
                                    SettingsUiEvent.OnToggleBuildFormats(
                                        showApks = showApks,
                                        showModules = !showModules
                                    )
                                )
                            }
                            .padding(12.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Modules",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = if (showModules) AccentGreenText else TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                if (showModules) {
                                    Text("✓", color = AccentGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Root / Magisk zips",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (showModules) TextSecondary else TextMuted
                                )
                            )
                        }
                    }
                }
            }

            // Section 3: Storage & Cleanup
            SettingsSectionCard(title = "Storage & Cleanup") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Download Folder:",
                        style = MaterialTheme.typography.labelMedium.copy(color = TextMuted)
                    )
                    if (state.settings.downloadFolderUri != null) {
                        Text(
                            text = "✓ Permission Granted",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentGreenText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = state.settings.downloadFolderPath ?: "Default: App Storage (Downloads/ZBuilds)",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = if (state.settings.downloadFolderUri != null) AccentGreenText else TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { folderPickerLauncher.launch(null) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGreen,
                            contentColor = PureBlack
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreateNewFolder,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Change Folder", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.onEvent(
                                SettingsUiEvent.OnFolderUpdated(
                                    uri = null,
                                    path = "Default: App Storage (Downloads/ZBuilds)"
                                )
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = BorderStroke(1.dp, DarkBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Use Default")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Auto-clean switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceElevated)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Auto-Clean Old Versions",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Automatically delete previous APKs/modules when a new update finishes downloading.",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = state.settings.autoCleanOldVersions,
                        onCheckedChange = { viewModel.onEvent(SettingsUiEvent.OnToggleAutoClean(it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = PureBlack,
                            checkedTrackColor = AccentGreen,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkBorder
                        )
                    )
                }

                if (state.settings.autoCleanOldVersions) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Auto-Cleanup Retention Period:",
                        style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val intervals = listOf(1 to "1 Day", 7 to "7 Days", 30 to "30 Days")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        intervals.forEach { (days, label) ->
                            val isSelected = state.settings.cleanupRetentionDays == days
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) AccentGreenContainer else DarkSurfaceElevated)
                                    .border(
                                        1.dp,
                                        if (isSelected) AccentGreen else DarkBorder,
                                        RoundedCornerShape(8.dp)
                                    )
                                    .clickable {
                                        viewModel.onEvent(SettingsUiEvent.OnSelectRetentionDays(days))
                                    }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        color = if (isSelected) AccentGreen else TextPrimary,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Clean Cache / Storage Button
                Button(
                    onClick = { viewModel.onEvent(SettingsUiEvent.OnClearCache) },
                    enabled = !state.isClearingCache,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurfaceElevated,
                        contentColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    if (state.isClearingCache) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = AccentGreen,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CleaningServices,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = AccentGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clean Cache / Storage", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Section 4: Diagnostics & Crash Analytics
            SettingsSectionCard(title = "Diagnostics & Crash Analytics") {
                Text(
                    text = "Real-time crash tracking and exception diagnostics. Uncaught errors are captured with stack traces, breadcrumbs, and device telemetry to prevent regressions.",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Crash Monitoring",
                        style = MaterialTheme.typography.labelMedium.copy(color = TextMuted)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentGreenContainer)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentGreenText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Recorded Reports",
                        style = MaterialTheme.typography.labelMedium.copy(color = TextMuted)
                    )
                    Text(
                        text = "${state.crashReports.size} stored",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (state.crashReports.isNotEmpty()) AccentOrange else TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.onEvent(SettingsUiEvent.OnSimulateTestCrash) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, DarkBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = "Test Tracker",
                            tint = AccentOrange,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Test Tracker", fontSize = 13.sp)
                    }

                    if (state.crashReports.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { viewModel.onEvent(SettingsUiEvent.OnClearCrashReports) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                            border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Clear Reports",
                                tint = AccentRed,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Reports", fontSize = 13.sp)
                        }
                    }
                }

                if (state.crashReports.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceElevated)
                            .padding(10.dp)
                    ) {
                        val latest = state.crashReports.first()
                        Text(
                            text = "Latest: ${latest.exceptionClass.substringAfterLast('.')}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentRed,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = latest.message ?: "No error message",
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = latest.deviceInfo,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontSize = 10.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Section 5: About ZBuilds
            SettingsSectionCard(title = "About ZBuilds") {
                Text(
                    text = "${AppSettings.APP_DESCRIPTION}",
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Version",
                        style = MaterialTheme.typography.labelMedium.copy(color = TextMuted)
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentGreenContainer)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = AppSettings.APP_VERSION,
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentGreenText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = AppSettings.TECH_STACK,
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        lineHeight = 16.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Clickable Project URL Row
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppSettings.DEFAULT_PROJECT_URL))
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Project Link",
                            tint = AccentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Project Source & Updates",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    color = TextPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Text(
                                text = AppSettings.DEFAULT_PROJECT_URL,
                                style = MaterialTheme.typography.labelSmall.copy(color = TextMuted)
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Open in browser",
                            tint = TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurface)
            .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(10.dp))
        content()
    }
}
