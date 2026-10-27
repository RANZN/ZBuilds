package com.ranzan.zbuilds.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ranzan.zbuilds.R
import com.ranzan.zbuilds.core.utils.FormatHelper
import com.ranzan.zbuilds.domain.model.AppFileType
import com.ranzan.zbuilds.domain.model.DownloadProgress
import com.ranzan.zbuilds.domain.model.TrackedApp
import com.ranzan.zbuilds.domain.rules.downloadedBuildVersion
import com.ranzan.zbuilds.domain.rules.isLatestBuildInstalled
import com.ranzan.zbuilds.domain.rules.latestBuildVersion
import com.ranzan.zbuilds.ui.theme.AccentGreen
import com.ranzan.zbuilds.ui.theme.AccentOrange
import com.ranzan.zbuilds.ui.theme.AccentPurple
import com.ranzan.zbuilds.ui.theme.DarkBackground
import com.ranzan.zbuilds.ui.theme.DarkCardBorder
import com.ranzan.zbuilds.ui.theme.DarkSurface
import com.ranzan.zbuilds.ui.theme.DarkSurfaceElevated
import com.ranzan.zbuilds.ui.theme.GreenPrimary
import com.ranzan.zbuilds.ui.theme.TextMuted
import com.ranzan.zbuilds.ui.theme.TextPrimary
import com.ranzan.zbuilds.ui.theme.TextSecondary
import com.ranzan.zbuilds.ui.util.rememberInstalledAppIcon

@Composable
fun TrackedAppCard(
    app: TrackedApp,
    downloadProgress: DownloadProgress?,
    onDownloadClick: () -> Unit,
    onCheckUpdateClick: () -> Unit = {},
    onCancelDownloadClick: () -> Unit,
    onInstallClick: () -> Unit,
    onOpenClick: () -> Unit,
    onUninstallClick: (String) -> Unit = {},
    onShareZipClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }
    val isDownloading = downloadProgress is DownloadProgress.Downloading
    // The repository clears localFilePath when the downloaded file no longer exists
    val fileExists = app.localFilePath != null

    val installedAppBitmap = rememberInstalledAppIcon(
        packageName = app.packageName.takeIf { app.isInstalled && app.fileType == AppFileType.APK }
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = BorderStroke(1.dp, if (app.hasUpdate) AccentOrange.copy(alpha = 0.4f) else DarkCardBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Icon, Name, Badges, and Overflow menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // App Avatar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            color = if (installedAppBitmap != null) DarkSurfaceElevated else if (app.fileType == AppFileType.APK) DarkSurfaceElevated else AccentOrange.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (installedAppBitmap != null) {
                        Image(
                            bitmap = installedAppBitmap,
                            contentDescription = stringResource(R.string.card_icon_desc, app.name),
                            modifier = Modifier.size(34.dp),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Icon(
                            imageVector = if (app.fileType == AppFileType.APK) Icons.Default.Android else Icons.Default.FolderZip,
                            contentDescription = null,
                            tint = if (app.fileType == AppFileType.APK) GreenPrimary else AccentOrange,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = stringResource(R.string.card_version_label, app.latestBuildVersion, FormatHelper.formatBytes(app.latestSizeBytes)),
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = stringResource(R.string.card_options_desc),
                            tint = TextSecondary
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        val targetPkg = app.packageName

                        if (app.isInstalled && !targetPkg.isNullOrBlank()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.card_menu_open)) },
                                onClick = {
                                    showMenu = false
                                    onOpenClick()
                                },
                                leadingIcon = {
                                    Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = null)
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.card_menu_uninstall)) },
                                onClick = {
                                    showMenu = false
                                    onUninstallClick(targetPkg)
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = AccentOrange)
                                }
                            )
                        }

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.card_menu_check_updates)) },
                            onClick = {
                                showMenu = false
                                onCheckUpdateClick()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.SystemUpdate, contentDescription = null)
                            }
                        )

                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.card_menu_remove), color = Color(0xFFFF5252)) },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF5252))
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Download Progress Bar or Update Notice
            if (isDownloading) {
                val progress = downloadProgress as DownloadProgress.Downloading
                val percentInt = (progress.progressPercent * 100).toInt()
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.card_downloading),
                            fontSize = 12.sp,
                            color = GreenPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.card_progress_percent, percentInt),
                            fontSize = 12.sp,
                            color = GreenPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress.progressPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp),
                        color = GreenPrimary,
                        trackColor = DarkSurfaceElevated
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.card_progress_bytes, FormatHelper.formatBytes(progress.downloadedBytes), FormatHelper.formatBytes(progress.totalBytes)),
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            } else if (app.hasUpdate) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(AccentOrange.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(AccentOrange, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        val currentVer = app.installedVersion ?: app.downloadedBuildVersion
                        Text(
                            text = stringResource(R.string.card_update_available, currentVer, app.latestBuildVersion),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AccentOrange
                        )
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkSurfaceElevated, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = AccentGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.card_up_to_date, app.installedVersion ?: app.downloadedBuildVersion.ifBlank { app.latestBuildVersion }),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isDownloading) {
                    Button(
                        onClick = onCancelDownloadClick,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DarkSurfaceElevated,
                            contentColor = TextPrimary
                        )
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.card_btn_cancel_download), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (app.hasUpdate) {
                    Button(
                        onClick = onDownloadClick,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AccentOrange,
                            contentColor = DarkBackground
                        )
                    ) {
                        Icon(Icons.Default.SystemUpdate, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.card_btn_update_now), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (app.fileType == AppFileType.APK) {
                    if (fileExists) {
                        // Same version as the server is already installed, so installing again does nothing
                        val isLatestInstalled = app.isLatestBuildInstalled
                        Button(
                            onClick = onInstallClick,
                            enabled = !isLatestInstalled,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GreenPrimary,
                                contentColor = DarkBackground,
                                disabledContainerColor = DarkSurfaceElevated,
                                disabledContentColor = TextMuted
                            )
                        ) {
                            Icon(
                                imageVector = if (isLatestInstalled) Icons.Default.Check else Icons.Default.Android,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(if (isLatestInstalled) R.string.card_btn_installed else R.string.card_btn_install_apk),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (app.isInstalled) {
                            OutlinedIconButton(
                                onClick = onOpenClick,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, DarkCardBorder)
                            ) {
                                Icon(Icons.Default.OpenInNew, contentDescription = stringResource(R.string.card_btn_open_app), tint = TextPrimary)
                            }
                        }
                    } else {
                        Button(
                            onClick = onDownloadClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkSurfaceElevated,
                                contentColor = GreenPrimary
                            )
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.card_btn_download_apk), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // ZIP module file
                    if (fileExists) {
                        Button(
                            onClick = onShareZipClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentOrange,
                                contentColor = DarkBackground
                            )
                        ) {
                            Icon(Icons.Default.FolderZip, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.card_btn_flash_zip), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Button(
                            onClick = onDownloadClick,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = DarkSurfaceElevated,
                                contentColor = AccentOrange
                            )
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.card_btn_download_zip), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Quick Redownload icon button if file exists and not currently downloading
                if (!isDownloading && fileExists && !app.hasUpdate) {
                    OutlinedIconButton(
                        onClick = onDownloadClick,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, DarkCardBorder)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = stringResource(R.string.card_btn_redownload), tint = TextSecondary)
                    }
                }
            }
        }
    }
}
