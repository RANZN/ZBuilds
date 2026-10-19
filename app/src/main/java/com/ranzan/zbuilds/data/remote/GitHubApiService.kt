package com.ranzan.zbuilds.data.remote

import com.ranzan.zbuilds.core.utils.ArchitectureHelper
import com.ranzan.zbuilds.data.remote.model.GitHubReleaseDto
import com.ranzan.zbuilds.domain.model.ReleaseAsset
import com.ranzan.zbuilds.domain.model.SourceRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class GitHubApiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    suspend fun fetchReleases(sourceUrl: String): Result<SourceRelease> = withContext(Dispatchers.IO) {
        try {
            val repoPair = SourceUrlParser.parseGitHubRepo(sourceUrl)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid GitHub repository URL: $sourceUrl"))

            val (owner, repo) = repoPair
            val apiUrl = SourceUrlParser.toReleasesApiUrl(owner, repo)

            val request = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "ZBuilds-Android-App")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                // If /releases returned error or empty, try /releases/latest
                val latestUrl = SourceUrlParser.toLatestReleaseApiUrl(owner, repo)
                val fallbackRequest = Request.Builder()
                    .url(latestUrl)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "ZBuilds-Android-App")
                    .get()
                    .build()

                val fallbackResponse = client.newCall(fallbackRequest).execute()
                if (!fallbackResponse.isSuccessful) {
                    return@withContext Result.failure(
                        IOException("GitHub API returned error code ${response.code}: ${response.message}")
                    )
                }

                val body = fallbackResponse.body?.string().orEmpty()
                val singleRelease = json.decodeFromString<GitHubReleaseDto>(body)
                return@withContext Result.success(mapToSourceRelease(singleRelease))
            }

            val responseBody = response.body?.string().orEmpty()
            val releases = json.decodeFromString<List<GitHubReleaseDto>>(responseBody)
            if (releases.isEmpty()) {
                return@withContext Result.failure(NoSuchElementException("No releases found in repository $owner/$repo"))
            }

            // Find the first release that has APK or ZIP assets
            val activeRelease = releases.firstOrNull { release ->
                release.assets.any { asset ->
                    val type = ArchitectureHelper.detectFileType(asset.name)
                    type != null
                }
            } ?: releases.first()

            Result.success(mapToSourceRelease(activeRelease))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchLatestRelease(owner: String, repo: String): Result<SourceRelease> = withContext(Dispatchers.IO) {
        try {
            val apiUrl = SourceUrlParser.toReleasesApiUrl(owner, repo)
            val request = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "ZBuilds-Android-App")
                .get()
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(IOException("Failed to fetch release: ${response.code}"))
            }

            val body = response.body?.string().orEmpty()
            val releases = json.decodeFromString<List<GitHubReleaseDto>>(body)
            if (releases.isEmpty()) {
                return@withContext Result.failure(NoSuchElementException("No releases found"))
            }

            val latest = releases.firstOrNull { r ->
                r.assets.any { ArchitectureHelper.detectFileType(it.name) != null }
            } ?: releases.first()

            Result.success(mapToSourceRelease(latest))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun mapToSourceRelease(dto: GitHubReleaseDto): SourceRelease {
        val assets = dto.assets.mapNotNull { asset ->
            val fileType = ArchitectureHelper.detectFileType(asset.name)
            if (fileType != null) {
                ReleaseAsset(
                    name = asset.name,
                    downloadUrl = asset.browserDownloadUrl,
                    sizeBytes = asset.size,
                    fileType = fileType,
                    arch = ArchitectureHelper.detectArchitecture(asset.name),
                    releaseTag = dto.tagName
                )
            } else {
                null
            }
        }

        return SourceRelease(
            tag = dto.tagName,
            title = dto.name ?: dto.tagName,
            publishedAt = dto.publishedAt.orEmpty(),
            body = dto.body.orEmpty(),
            assets = assets
        )
    }
}
