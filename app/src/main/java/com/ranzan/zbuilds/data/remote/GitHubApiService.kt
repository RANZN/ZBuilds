package com.ranzan.zbuilds.data.remote

import com.ranzan.zbuilds.core.utils.ArchitectureHelper
import com.ranzan.zbuilds.data.local.SettingsDataStore
import com.ranzan.zbuilds.data.remote.model.GitHubReleaseDto
import com.ranzan.zbuilds.domain.model.ReleaseAsset
import com.ranzan.zbuilds.domain.model.SourceRelease
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class GitHubApiService(
    private val settingsDataStore: SettingsDataStore? = null,
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

    private suspend fun getAuthToken(): String? {
        return try {
            settingsDataStore?.getSettings()?.githubToken?.takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun fetchReleases(sourceUrl: String): Result<SourceRelease> = withContext(Dispatchers.IO) {
        try {
            val repoPair = SourceUrlParser.parseGitHubRepo(sourceUrl)
                ?: return@withContext Result.failure(IllegalArgumentException("Invalid GitHub repository URL: $sourceUrl"))

            val (owner, repo) = repoPair
            val apiUrl = SourceUrlParser.toReleasesApiUrl(owner, repo)
            val token = getAuthToken()

            val requestBuilder = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "ZBuilds-Android-App")
                .get()

            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            val response = client.newCall(requestBuilder.build()).execute()
            if (!response.isSuccessful) {
                // If /releases returned error or empty, try /releases/latest
                val latestUrl = SourceUrlParser.toLatestReleaseApiUrl(owner, repo)
                val fallbackBuilder = Request.Builder()
                    .url(latestUrl)
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("User-Agent", "ZBuilds-Android-App")
                    .get()

                if (!token.isNullOrBlank()) {
                    fallbackBuilder.header("Authorization", "Bearer $token")
                }

                val fallbackResponse = client.newCall(fallbackBuilder.build()).execute()
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
                    ArchitectureHelper.detectFileType(asset.name) != null
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
            val token = getAuthToken()

            val requestBuilder = Request.Builder()
                .url(apiUrl)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "ZBuilds-Android-App")
                .get()

            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer $token")
            }

            val response = client.newCall(requestBuilder.build()).execute()
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

    suspend fun validateToken(token: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url("https://api.github.com/user")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "ZBuilds-Android-App")
                .header("Authorization", "Bearer ${token.trim()}")
                .get()
                .build()

            val response = client.newCall(request).execute()
            val remaining = response.header("x-ratelimit-remaining")
            val limit = response.header("x-ratelimit-limit")

            if (response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                val jsonBody = JSONObject(body)
                val login = jsonBody.optString("login", "User")
                Result.success("Token valid! Authenticated as @$login (${remaining ?: "5000"}/${limit ?: "5000"} req/hr remaining)")
            } else if (response.code == 401 || response.code == 403) {
                Result.failure(IOException("Authentication failed (${response.code}). Please verify your token."))
            } else {
                Result.failure(IOException("GitHub API responded with code ${response.code}: ${response.message}"))
            }
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
