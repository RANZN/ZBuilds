package com.ranzan.zbuilds.ui.settings

import android.content.ClipData
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
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ranzan.zbuilds.R
import com.ranzan.zbuilds.core.base.asString
import com.ranzan.zbuilds.core.utils.FormatHelper
import com.ranzan.zbuilds.domain.model.AppSettings
import com.ranzan.zbuilds.domain.model.CrashReport
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
import com.ranzan.zbuilds.ui.theme.ZBuildsTheme
import com.ranzan.zbuilds.ui.util.ClipboardHelper
import org.koin.androidx.compose.koinViewModel

/**
 * Host composable for SettingsScreen managing ViewModel injection,
 * collecting state, activity result launchers (folder picker), and one-shot effects.
 */
@Composable
fun SettingsScreenHost(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val customFolderLabel = stringResource(R.string.settings_folder_custom)

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { treeUri: Uri? ->
        if (treeUri != null) {
            try {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(treeUri, flags)
                val doc = DocumentFile.fromTreeUri(context, treeUri)
                viewModel.onAction(
                    SettingsUiAction.OnFolderUpdated(
                        uri = treeUri.toString(),
                        path = doc?.name ?: treeUri.lastPathSegment ?: customFolderLabel
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffect.collect { effect ->
            when (effect) {
                is SettingsUiEffect.NavigateBack -> onNavigateBack()
                is SettingsUiEffect.OpenUrl -> {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, effect.url.toUri())
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                is SettingsUiEffect.LaunchFolderPicker -> {
                    folderPickerLauncher.launch(null)
                }
                is SettingsUiEffect.RequestPasteToken -> ClipboardHelper.readText(context)?.let {
                    viewModel.onAction(SettingsUiAction.OnTokenInputChanged(it))
                }
                is SettingsUiEffect.CopyToClipboard -> {
                    try {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText(effect.label.asString(context), effect.text))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                is SettingsUiEffect.ShareText -> {
                    try {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_SUBJECT, effect.title.asString(context))
                            putExtra(Intent.EXTRA_TEXT, effect.text)
                        }
                        context.startActivity(Intent.createChooser(intent, effect.title.asString(context)))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                is SettingsUiEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(effect.message.asString(context))
                }
            }
        }
    }

    SettingsScreen(
        state = state,
        snackbarHostState = snackbarHostState,
        onAction = viewModel::onAction,
        modifier = modifier
    )
}

/**
 * Pure UI Composable for Settings Screen.
 * Contains zero hardcoded external URLs, zero Intent calls, and zero Android context dependencies
 * so that Compose Previews function reliably.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
    onAction: (SettingsUiAction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var isTokenVisible by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = PureBlack,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_title),
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { onAction(SettingsUiAction.OnNavigateBackClick) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_desc),
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
            SettingsSectionCard(title = stringResource(R.string.settings_section_github_token)) {
                Text(
                    text = stringResource(R.string.settings_github_desc),
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Action Buttons: Generate & Paste
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onAction(SettingsUiAction.OnGenerateTokenClick) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, DarkBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = stringResource(R.string.settings_btn_generate),
                            modifier = Modifier.size(15.dp),
                            tint = AccentGreen
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_btn_generate), fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { onAction(SettingsUiAction.OnPasteTokenClick) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = BorderStroke(1.dp, DarkBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = stringResource(R.string.settings_btn_paste),
                            modifier = Modifier.size(15.dp),
                            tint = AccentGreen
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.settings_btn_paste), fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = state.tokenInput,
                    onValueChange = { onAction(SettingsUiAction.OnTokenInputChanged(it)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.settings_token_placeholder), color = TextMuted) },
                    visualTransformation = if (isTokenVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        if (state.tokenInput.isNotEmpty()) {
                            IconButton(onClick = { isTokenVisible = !isTokenVisible }) {
                                Icon(
                                    imageVector = if (isTokenVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = stringResource(if (isTokenVisible) R.string.settings_token_hide else R.string.settings_token_show),
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
                            text = stringResource(R.string.settings_token_active),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentGreenText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Text(
                            text = stringResource(R.string.settings_token_masked, state.settings.githubToken.takeLast(4)),
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
                            onAction(SettingsUiAction.OnSaveToken)
                            isTokenVisible = false
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentGreen,
                            contentColor = PureBlack
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.settings_btn_save), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onAction(SettingsUiAction.OnTestToken) },
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
                            Text(stringResource(R.string.settings_btn_test))
                        }
                    }

                    if (state.settings.githubToken != null || state.tokenInput.isNotEmpty()) {
                        OutlinedButton(
                            onClick = {
                                onAction(SettingsUiAction.OnClearToken)
                                isTokenVisible = false
                            },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = BorderStroke(1.dp, DarkBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(stringResource(R.string.settings_btn_clear))
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
                                text = state.tokenValidationMessage?.asString().orEmpty(),
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = if (state.isTokenValid) AccentGreenText else AccentRed,
                                    fontWeight = FontWeight.Medium
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onAction(SettingsUiAction.OnDismissTokenFeedback) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = stringResource(R.string.settings_btn_dismiss),
                                    tint = if (state.isTokenValid) AccentGreenText else AccentRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Build Formats
            SettingsSectionCard(title = stringResource(R.string.settings_section_formats)) {
                Text(
                    text = stringResource(R.string.settings_formats_desc),
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
                                onAction(
                                    SettingsUiAction.OnToggleBuildFormats(
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
                                    text = stringResource(R.string.settings_format_apks),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = if (showApks) AccentGreenText else TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                if (showApks) {
                                    Text(stringResource(R.string.settings_check_mark), color = AccentGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.settings_format_apks_desc),
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
                                onAction(
                                    SettingsUiAction.OnToggleBuildFormats(
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
                                    text = stringResource(R.string.settings_format_modules),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        color = if (showModules) AccentGreenText else TextPrimary,
                                        fontWeight = FontWeight.Bold
                                    )
                                )
                                if (showModules) {
                                    Text(stringResource(R.string.settings_check_mark), color = AccentGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.settings_format_modules_desc),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    color = if (showModules) TextSecondary else TextMuted
                                )
                            )
                        }
                    }
                }
            }

            // Section 3: Storage & Cleanup
            SettingsSectionCard(title = stringResource(R.string.settings_section_storage)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_folder_label),
                        style = MaterialTheme.typography.labelMedium.copy(color = TextMuted)
                    )
                    if (state.settings.downloadFolderUri != null) {
                        Text(
                            text = stringResource(R.string.settings_folder_permission_granted),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentGreenText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = state.settings.downloadFolderPath ?: stringResource(R.string.settings_folder_default),
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
                        onClick = { onAction(SettingsUiAction.OnPickFolderClick) },
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
                        Text(stringResource(R.string.settings_btn_change_folder), fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            onAction(
                                SettingsUiAction.OnFolderUpdated(
                                    uri = null,
                                    // No saved path means the default folder, whose label comes from strings.xml
                                    path = null
                                )
                            )
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                        border = BorderStroke(1.dp, DarkBorder),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.settings_btn_use_default))
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
                            text = stringResource(R.string.settings_auto_clean_title),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.settings_auto_clean_desc),
                            style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Switch(
                        checked = state.settings.autoCleanOldVersions,
                        onCheckedChange = { onAction(SettingsUiAction.OnToggleAutoClean(it)) },
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
                        text = stringResource(R.string.settings_retention_period_title),
                        style = MaterialTheme.typography.labelMedium.copy(color = TextSecondary)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val intervals = listOf(
                        1 to stringResource(R.string.settings_retention_1_day),
                        7 to stringResource(R.string.settings_retention_7_days),
                        30 to stringResource(R.string.settings_retention_30_days)
                    )
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
                                        onAction(SettingsUiAction.OnSelectRetentionDays(days))
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
                    onClick = { onAction(SettingsUiAction.OnClearCache) },
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
                        Text(stringResource(R.string.settings_btn_clean_cache), fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // Section 4: Diagnostics & Crash Reports (FOSS Local)
            SettingsSectionCard(title = stringResource(R.string.settings_section_diagnostics)) {
                Text(
                    text = stringResource(R.string.settings_diagnostics_desc),
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_crash_monitoring_label),
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
                            text = stringResource(R.string.settings_crash_monitoring_status),
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
                        text = stringResource(R.string.settings_crash_recorded_reports),
                        style = MaterialTheme.typography.labelMedium.copy(color = TextMuted)
                    )
                    Text(
                        text = stringResource(R.string.settings_crash_reports_stored, state.crashReports.size),
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = if (state.crashReports.isNotEmpty()) AccentOrange else TextSecondary,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (state.crashReports.isNotEmpty()) {
                    val latest = state.crashReports.first()

                    // Latest crash preview card
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceElevated)
                            .padding(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_crash_latest_title, latest.exceptionClass.substringAfterLast('.')),
                            style = MaterialTheme.typography.labelMedium.copy(
                                color = AccentRed,
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = latest.message ?: stringResource(R.string.settings_crash_no_message),
                            style = MaterialTheme.typography.bodySmall.copy(color = TextPrimary),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(
                                R.string.settings_crash_meta,
                                latest.deviceInfo,
                                FormatHelper.formatDate(latest.timestamp) ?: stringResource(R.string.common_unknown)
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = TextMuted,
                                fontSize = 10.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action buttons: Report on GitHub (Primary) + Copy + Share + Clear
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onAction(SettingsUiAction.OnReportCrashOnGithub(latest)) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentGreen,
                                contentColor = PureBlack
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BugReport,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.settings_btn_report_github), fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { onAction(SettingsUiAction.OnCopyCrashReport(latest)) },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = BorderStroke(1.dp, DarkBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = AccentGreen
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.settings_btn_copy_log), fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { onAction(SettingsUiAction.OnShareCrashReport(latest)) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                            border = BorderStroke(1.dp, DarkBorder),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.settings_btn_share_log), fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { onAction(SettingsUiAction.OnClearCrashReports) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                            border = BorderStroke(1.dp, AccentRed.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = AccentRed
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.settings_btn_clear_crashes), fontSize = 12.sp)
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.settings_crash_no_reports),
                        style = MaterialTheme.typography.bodySmall.copy(color = TextMuted)
                    )
                }
            }

            // Section 5: About ZBuilds
            SettingsSectionCard(title = stringResource(R.string.settings_section_about)) {
                Text(
                    text = stringResource(R.string.settings_about_description),
                    style = MaterialTheme.typography.bodySmall.copy(color = TextSecondary)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.settings_version_label),
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
                            text = state.appVersion?.let { stringResource(R.string.settings_about_version, it) }
                                ?: stringResource(R.string.common_unknown),
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = AccentGreenText,
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = stringResource(R.string.settings_about_tech_stack),
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextMuted,
                        lineHeight = 16.sp
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Clickable Project URL Row (Screen delegates to onAction, no direct URI/Intent calls)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                        .clickable {
                            onAction(SettingsUiAction.OnOpenProjectUrlClick)
                        }
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = stringResource(R.string.settings_project_link_title),
                            tint = AccentGreen,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_project_link_title),
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
                            contentDescription = stringResource(R.string.settings_open_browser_desc),
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

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    ZBuildsTheme {
        SettingsScreen(
            state = SettingsUiState(
                settings = AppSettings(
                    githubToken = "ghp_mocktoken123456",
                    showApks = true,
                    showModules = true
                ),
                crashReports = listOf(
                    CrashReport(
                        id = "test-1",
                        timestamp = System.currentTimeMillis(),
                        exceptionClass = "java.lang.IllegalStateException",
                        message = "Sample crash message for preview",
                        stackTrace = "java.lang.IllegalStateException: Sample crash\n\tat com.ranzan.zbuilds...",
                        threadName = "main",
                        deviceInfo = "Google Pixel 8 (Android 14, API 34)",
                        appVersion = "1.0.0",
                        isFatal = false
                    )
                )
            )
        )
    }
}
