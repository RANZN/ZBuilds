package com.ranzan.zbuilds.data.remote.mapper

import com.ranzan.zbuilds.data.remote.model.GitHubReleaseDto
import com.ranzan.zbuilds.data.remote.model.GitHubTokenCheckDto
import com.ranzan.zbuilds.domain.model.GithubTokenStatus
import com.ranzan.zbuilds.domain.model.ReleaseAsset
import com.ranzan.zbuilds.domain.model.SourceRelease
import com.ranzan.zbuilds.domain.rules.AssetNameRules
import com.ranzan.zbuilds.domain.rules.BuildVariantRules

/**
 * Converts a GitHub release into a domain release, keeping only APK/ZIP files.
 */
fun GitHubReleaseDto.toDomain(): SourceRelease {
    val assets = assets.mapNotNull { asset ->
        val fileType = AssetNameRules.detectFileType(asset.name) ?: return@mapNotNull null
        ReleaseAsset(
            name = asset.name,
            appName = AssetNameRules.deriveAppName(asset.name),
            downloadUrl = asset.browserDownloadUrl,
            sizeBytes = asset.size,
            fileType = fileType,
            arch = AssetNameRules.detectArchitecture(asset.name),
            variant = BuildVariantRules.resolveAssetVariant(asset.name, tagName),
            releaseTag = tagName
        )
    }

    return SourceRelease(
        tag = tagName,
        title = name ?: tagName,
        publishedAt = publishedAt.orEmpty(),
        body = body.orEmpty(),
        assets = assets
    )
}

fun GitHubTokenCheckDto.toDomain(): GithubTokenStatus = GithubTokenStatus(
    login = login,
    remainingRequests = rateLimitRemaining,
    requestLimit = rateLimit
)
