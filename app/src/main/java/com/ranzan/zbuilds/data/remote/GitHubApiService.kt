package com.ranzan.zbuilds.data.remote

import com.ranzan.zbuilds.data.local.SettingsDataStore
import com.ranzan.zbuilds.data.remote.model.GitHubReleaseDto
import com.ranzan.zbuilds.data.remote.model.GitHubTokenCheckDto
import com.ranzan.zbuilds.domain.model.AppError
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

/**
 * GitHub REST API client. Returns network DTOs only; repositories convert them to domain models.
 */
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

    /**
     * Fetches the repository's releases (newest first). Falls back to the latest release
     * endpoint when the list endpoint fails.
     */
    suspend fun fetchReleases(sourceUrl: String): Result<List<GitHubReleaseDto>> = withContext(Dispatchers.IO) {
        try {
            val (owner, repo) = SourceUrlParser.parseGitHubRepo(sourceUrl)
                ?: return@withContext Result.failure(AppError.InvalidSourceUrl())
            val token = getAuthToken()

            val response = client.newCall(gitHubRequest(SourceUrlParser.toReleasesApiUrl(owner, repo), token)).execute()
            if (!response.isSuccessful) {
                val fallbackResponse = client.newCall(
                    gitHubRequest(SourceUrlParser.toLatestReleaseApiUrl(owner, repo), token)
                ).execute()
                if (!fallbackResponse.isSuccessful) {
                    return@withContext Result.failure(AppError.Http(response.code))
                }
                val body = fallbackResponse.body?.string().orEmpty()
                return@withContext Result.success(listOf(json.decodeFromString<GitHubReleaseDto>(body)))
            }

            val body = response.body?.string().orEmpty()
            Result.success(json.decodeFromString<List<GitHubReleaseDto>>(body))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches one page of releases (newest first). Used to load older releases on demand.
     */
    suspend fun fetchReleasePage(
        sourceUrl: String,
        page: Int,
        perPage: Int
    ): Result<List<GitHubReleaseDto>> = withContext(Dispatchers.IO) {
        try {
            val (owner, repo) = SourceUrlParser.parseGitHubRepo(sourceUrl)
                ?: return@withContext Result.failure(AppError.InvalidSourceUrl())

            val apiUrl = "${SourceUrlParser.toReleasesApiUrl(owner, repo)}?per_page=$perPage&page=$page"
            val response = client.newCall(gitHubRequest(apiUrl, getAuthToken())).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(AppError.Http(response.code))
            }

            val body = response.body?.string().orEmpty()
            Result.success(json.decodeFromString<List<GitHubReleaseDto>>(body))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun validateToken(token: String): Result<GitHubTokenCheckDto> = withContext(Dispatchers.IO) {
        try {
            val request = gitHubRequest("https://api.github.com/user", token.trim())

            val response = client.newCall(request).execute()
            val remaining = response.header("x-ratelimit-remaining")
            val limit = response.header("x-ratelimit-limit")

            if (response.isSuccessful) {
                val body = response.body?.string().orEmpty()
                Result.success(
                    GitHubTokenCheckDto(
                        login = JSONObject(body).optString("login").ifBlank { null },
                        rateLimitRemaining = remaining,
                        rateLimit = limit
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
}
