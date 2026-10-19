package com.ranzan.zbuilds.data.local

import android.content.Context
import android.os.Environment
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.ranzan.zbuilds.domain.model.AppFileType
import com.ranzan.zbuilds.domain.model.DownloadProgress
import com.ranzan.zbuilds.domain.model.TrackedApp
import com.ranzan.zbuilds.domain.repository.PackageInspectorRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class AppDownloader(
    private val context: Context,
    private val trackedAppsDataStore: TrackedAppsDataStore,
    private val packageInspector: PackageInspectorRepository,
    private val settingsDataStore: SettingsDataStore? = null,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    private val progressFlows = ConcurrentHashMap<String, MutableStateFlow<DownloadProgress>>()
    private val activeJobs = ConcurrentHashMap<String, Job>()

    fun observeProgress(appId: String): Flow<DownloadProgress> {
        return progressFlows.getOrPut(appId) {
            MutableStateFlow(DownloadProgress.Idle(appId))
        }.asStateFlow()
    }

    suspend fun download(app: TrackedApp): Result<String> = withContext(Dispatchers.IO) {
        val flow = progressFlows.getOrPut(app.id) {
            MutableStateFlow(DownloadProgress.Idle(app.id))
        }

        try {
            val job = currentCoroutineContext()[Job]
            if (job != null) {
                activeJobs[app.id] = job
            }

            flow.value = DownloadProgress.Downloading(
                appId = app.id,
                progressPercent = 0.0f,
                downloadedBytes = 0,
                totalBytes = app.latestSizeBytes
            )

            val downloadDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                ?: context.filesDir
            if (!downloadDir.exists()) {
                downloadDir.mkdirs()
            }

            // Clean sanitized file name
            val fileName = app.assetName.ifBlank {
                val ext = if (app.fileType == AppFileType.APK) ".apk" else ".zip"
                "${app.name.replace(" ", "_")}_${app.latestVersion}$ext"
            }
            val destinationFile = File(downloadDir, fileName)

            val token = try { settingsDataStore?.getSettings()?.githubToken } catch (e: Exception) { null }
            val requestBuilder = Request.Builder()
                .url(app.latestDownloadUrl)
                .header("User-Agent", "ZBuilds-Android-App")
                .get()

            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                val errorMsg = "HTTP error ${response.code}: ${response.message}"
                flow.value = DownloadProgress.Failed(app.id, errorMsg)
                return@withContext Result.failure(IOException(errorMsg))
            }

            val body = response.body ?: throw IOException("Empty response body")
            val totalBytes = if (body.contentLength() > 0) body.contentLength() else app.latestSizeBytes

            body.byteStream().use { input ->
                FileOutputStream(destinationFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalDownloaded = 0L
                    var lastEmittedTime = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        if (!currentCoroutineContext().isActive) {
                            destinationFile.delete()
                            throw CancellationException("Download canceled")
                        }

                        output.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead

                        val now = System.currentTimeMillis()
                        if (now - lastEmittedTime > 200 || totalDownloaded == totalBytes) {
                            lastEmittedTime = now
                            val percent = if (totalBytes > 0) {
                                (totalDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                            } else {
                                0.5f
                            }
                            flow.value = DownloadProgress.Downloading(
                                appId = app.id,
                                progressPercent = percent,
                                downloadedBytes = totalDownloaded,
                                totalBytes = totalBytes
                            )
                        }
                    }
                    output.flush()
                }
            }

            // Post-download inspection
            var updatedPackageName = app.packageName
            var isInstalled = false
            var installedVersion: String? = null

            if (app.fileType == AppFileType.APK) {
                val detectedPkg = packageInspector.inspectApkPackageName(destinationFile.absolutePath)
                if (!detectedPkg.isNullOrBlank()) {
                    updatedPackageName = detectedPkg
                    isInstalled = packageInspector.isPackageInstalled(detectedPkg)
                    installedVersion = packageInspector.getInstalledVersionName(detectedPkg)
                }
            }

            val updatedApp = app.copy(
                downloadedVersion = app.latestVersion,
                localFilePath = destinationFile.absolutePath,
                packageName = updatedPackageName,
                isInstalled = isInstalled,
                installedVersion = installedVersion,
                hasUpdate = false,
                lastCheckedTimestamp = System.currentTimeMillis()
            )
            trackedAppsDataStore.updateTrackedApp(updatedApp)

            // Auto-clean older versions of this app if enabled
            val currentSettings = try { settingsDataStore?.getSettings() } catch (e: Exception) { null }
            if (currentSettings?.autoCleanOldVersions == true) {
                val prefix = app.name.replace(" ", "_")
                downloadDir.listFiles()?.forEach { file ->
                    if (file.isFile && file.name.startsWith(prefix) && file.name != fileName) {
                        file.delete()
                    }
                }
            }

            // Export to custom SAF folder if configured
            val customFolderUri = currentSettings?.downloadFolderUri
            if (!customFolderUri.isNullOrBlank()) {
                try {
                    val treeUri = Uri.parse(customFolderUri)
                    val pickedDir = DocumentFile.fromTreeUri(context, treeUri)
                    if (pickedDir != null && pickedDir.isDirectory) {
                        val mime = if (app.fileType == AppFileType.APK) "application/vnd.android.package-archive" else "application/zip"
                        val targetDoc = pickedDir.findFile(fileName) ?: pickedDir.createFile(mime, fileName)
                        if (targetDoc != null) {
                            context.contentResolver.openOutputStream(targetDoc.uri)?.use { out ->
                                destinationFile.inputStream().use { input ->
                                    input.copyTo(out)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            flow.value = DownloadProgress.Completed(
                appId = app.id,
                filePath = destinationFile.absolutePath,
                fileType = app.fileType
            )

            Result.success(destinationFile.absolutePath)
        } catch (e: CancellationException) {
            flow.value = DownloadProgress.Idle(app.id)
            Result.failure(e)
        } catch (e: Exception) {
            flow.value = DownloadProgress.Failed(app.id, e.message ?: "Download failed")
            Result.failure(e)
        } finally {
            activeJobs.remove(app.id)
        }
    }

    fun cancel(appId: String) {
        activeJobs[appId]?.cancel()
        activeJobs.remove(appId)
        progressFlows[appId]?.value = DownloadProgress.Idle(appId)
    }
}
