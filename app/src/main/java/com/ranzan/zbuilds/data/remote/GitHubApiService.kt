package com.ranzan.zbuilds.data.remote

import com.ranzan.zbuilds.core.utils.ArchitectureHelper
import com.ranzan.zbuilds.data.local.SettingsDataStore
import com.ranzan.zbuilds.data.remote.model.GitHubReleaseDto
import com.ranzan.zbuilds.domain.model.AppError
import com.ranzan.zbuilds.domain.model.GithubTokenStatus
import com.ranzan.zbuilds.domain.model.ReleaseAsset
import com.ranzan.zbuilds.domain.model.SourceRelease
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class GitHubApiService(
    private val settingsDataStore: SettingsDataStore? = null
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()
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

    /**
     * Builds a GET request for the GitHub API, adding the token when one is set.
     */
    private fun gitHubRequest(url: String, token: String?): Request {
        val builder = Request.Builder()
            .url(url)
            .header("Accept", "application/vnd.github.v3+json")
            .header("User-Agent", "ZBuilds-Android-App")
            .get()

        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }
        return builder.build()
    }

    suspend fun fetchReleases(sourceUrl: String): Result<SourceRelease> = withContext(Dispatchers.IO) {
        try {
            val repoPair = SourceUrlParser.parseGitHubRepo(sourceUrl)
                ?: return@withContext Result.failure(AppError.InvalidSourceUrl())

            val (owner, repo) = repoPair
            val apiUrl = SourceUrlParser.toReleasesApiUrl(owner, repo)
            val token = getAuthToken()

            val request = gitHubRequest(apiUrl, token)

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                // If /releases returned error or empty, try /releases/latest
                val latestUrl = SourceUrlParser.toLatestReleaseApiUrl(owner, repo)
                val fallbackRequest = gitHubRequest(latestUrl, token)

                val fallbackResponse = client.newCall(fallbackRequest).execute()
                if (!fallbackResponse.isSuccessful) {
                    return@withContext Result.failure(
                        AppError.Http(response.code)
                    )
                }

                val body = fallbackResponse.body?.string().orEmpty()
                val singleRelease = json.decodeFromString<GitHubReleaseDto>(body)
                return@withContext Result.success(mapToSourceRelease(singleRelease))
            }

            val responseBody = response.body?.string().orEmpty()
            val releases = json.decodeFromString<List<GitHubReleaseDto>>(responseBody)
            if (releases.isEmpty()) {
                return@withContext Result.failure(AppError.NoReleases())
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

    /**
     * Fetches one page of releases (newest first), keeping only APK/ZIP assets.
     * Used to load older releases on demand.
     */
    suspend fun fetchReleasePage(
        sourceUrl: String,
        page: Int,
        perPage: Int
    ): Result<List<SourceRelease>> = withContext(Dispatchers.IO) {
        try {
            val (owner, repo) = SourceUrlParser.parseGitHubRepo(sourceUrl)
                ?: return@withContext Result.failure(AppError.InvalidSourceUrl())

            val apiUrl = "${SourceUrlParser.toReleasesApiUrl(owner, repo)}?per_page=$perPage&page=$page"
            val token = getAuthToken()

            val request = gitHubRequest(apiUrl, token)

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(
                    AppError.Http(response.code)
                )
            }

            val body = response.body?.string().orEmpty()
            val releases = json.decodeFromString<List<GitHubReleaseDto>>(body)
            Result.success(releases.map { mapToSourceRelease(it) })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchLatestRelease(owner: String, repo: String): Result<SourceRelease> = withContext(Dispatchers.IO) {
        try {
            val apiUrl = SourceUrlParser.toReleasesApiUrl(owner, repo)
            val token = getAuthToken()

            val request = gitHubRequest(apiUrl, token)

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(AppError.Http(response.code))
            }

            val body = response.body?.string().orEmpty()
            val releases = json.decodeFromString<List<GitHubReleaseDto>>(body)
            if (releases.isEmpty()) {
                return@withContext Result.failure(AppError.NoReleases())
            }

            val latest = releases.firstOrNull { r ->
                r.assets.any { ArchitectureHelper.detectFileType(it.name) != null }
            } ?: releases.first()

            Result.success(mapToSourceRelease(latest))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validateToken(token: String): Result<GithubTokenStatus> = withContext(Dispatchers.IO) {
        try {
            val request = gitHubRequest("https://api.github.com/user", token.trim())

            val response = client.newCall(request).execute()
            val remaining = response.header("x-ratelimit-remaining")
            val limit = response.header("x-ratelimit-limit")

            if (response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                val jsonBody = JSONObject(body)
                Result.success(
                    GithubTokenStatus(
                        login = jsonBody.optString("login").ifBlank { null },
                        remainingRequests = remaining,
                        requestLimit = limit
                    )
                )
            } else if (response.code == 401 || response.code == 403) {
                Result.failure(AppError.TokenRejected(response.code))
            } else {
                Result.failure(AppError.Http(response.code))
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
