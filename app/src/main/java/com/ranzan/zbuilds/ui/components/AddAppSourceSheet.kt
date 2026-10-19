package com.ranzan.zbuilds.ui.components

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import com.ranzan.zbuilds.core.utils.FormatHelper
import com.ranzan.zbuilds.domain.model.AppFileType
import com.ranzan.zbuilds.domain.model.ReleaseAsset
import com.ranzan.zbuilds.domain.model.SourceRelease
import com.ranzan.zbuilds.domain.model.SourceSuggestion
import com.ranzan.zbuilds.ui.theme.AccentBlue
import com.ranzan.zbuilds.ui.theme.AccentGreen
import com.ranzan.zbuilds.ui.theme.AccentOrange
import com.ranzan.zbuilds.ui.theme.DarkBackground
import com.ranzan.zbuilds.ui.theme.DarkCardBorder
import com.ranzan.zbuilds.ui.theme.DarkSurface
import com.ranzan.zbuilds.ui.theme.DarkSurfaceElevated
import com.ranzan.zbuilds.ui.theme.GreenPrimary
import com.ranzan.zbuilds.ui.theme.TextMuted
import com.ranzan.zbuilds.ui.theme.TextPrimary
import com.ranzan.zbuilds.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAppSourceSheet(
    isOpen: Boolean,
    sourceUrl: String,
    isFetching: Boolean,
    fetchedRelease: SourceRelease?,
    errorMessage: String?,
    sheetState: SheetState,
    suggestions: List<SourceSuggestion> = emptyList(),
    onUrlChange: (String) -> Unit,
    onFetchClick: () -> Unit,
    onSelectAsset: (ReleaseAsset, SourceRelease) -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .background(DarkCardBorder, RoundedCornerShape(2.dp))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = "Add App Source",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Enter any GitHub repo or release URL to discover available downloads",
                fontSize = 12.sp,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // URL input with Paste & Clear actions
            OutlinedTextField(
                value = sourceUrl,
                onValueChange = onUrlChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text("https://github.com/owner/repo/releases", color = TextMuted, fontSize = 13.sp)
                },
                leadingIcon = {
                    Icon(Icons.Default.Link, contentDescription = null, tint = GreenPrimary)
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (sourceUrl.isNotEmpty()) {
                            IconButton(onClick = { onUrlChange("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextMuted)
                            }
                        }
                        IconButton(onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            val clipData = clipboard?.primaryClip
                            if (clipData != null && clipData.itemCount > 0) {
                                val text = clipData.getItemAt(0).text?.toString() ?: ""
                                onUrlChange(text)
                            }
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Paste", tint = GreenPrimary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = GreenPrimary,
                    unfocusedBorderColor = DarkCardBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Quick suggestion chips from ViewModel / Remote Config
            if (suggestions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    suggestions.forEach { suggestion ->
                        AssistChip(
                            onClick = {
                                onUrlChange(suggestion.url)
                                onFetchClick()
                            },
                            label = { Text(suggestion.label, fontSize = 11.sp, color = TextPrimary) },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = DarkSurfaceElevated
                            ),
                            border = BorderStroke(1.dp, DarkCardBorder)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // Fetch Button
            Button(
                onClick = onFetchClick,
                enabled = sourceUrl.isNotBlank() && !isFetching,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = GreenPrimary,
                    contentColor = DarkBackground,
                    disabledContainerColor = DarkSurfaceElevated,
                    disabledContentColor = TextMuted
                )
            ) {
                if (isFetching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = DarkBackground
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fetching Available Downloads...", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Fetch Downloads", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Error display
            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1515)),
                    border = BorderStroke(1.dp, Color(0xFF552222))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFFF5252))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(errorMessage, color = Color(0xFFFF8A80), fontSize = 12.sp)
                    }
                }
            }

            // Fetched Release & Asset List
            if (fetchedRelease != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Release: ${fetchedRelease.tag} (${fetchedRelease.assets.size} downloads found)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGreen
                )
                Text(
                    text = "Select what you want to download and add to home screen:",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(fetchedRelease.assets, key = { it.name }) { asset ->
                        Card(
                            onClick = { onSelectAsset(asset, fetchedRelease) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceElevated),
                            border = BorderStroke(1.dp, DarkCardBorder)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (asset.fileType == AppFileType.APK) Icons.Default.Android else Icons.Default.FolderZip,
                                    contentDescription = null,
                                    tint = if (asset.fileType == AppFileType.APK) GreenPrimary else AccentOrange,
                                    modifier = Modifier.size(26.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = FormatHelper.deriveAppNameFromAsset(asset.name),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = TextPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 3.dp)
                                    ) {
                                        // Badge
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    if (asset.fileType == AppFileType.APK) AccentBlue.copy(alpha = 0.2f) else AccentOrange.copy(alpha = 0.2f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 5.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                asset.fileType.displayName,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (asset.fileType == AppFileType.APK) AccentBlue else AccentOrange
                                            )
                                        }

                                        Text(
                                            text = "${asset.arch} • ${FormatHelper.formatBytes(asset.sizeBytes)}",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(
                                            if (asset.fileType == AppFileType.APK) GreenPrimary.copy(alpha = 0.15f) else AccentOrange.copy(alpha = 0.15f),
                                            RoundedCornerShape(8.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Download,
                                        contentDescription = "Download",
                                        tint = if (asset.fileType == AppFileType.APK) GreenPrimary else AccentOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
