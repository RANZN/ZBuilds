package com.ranzan.zbuilds.ui.util

import com.ranzan.zbuilds.R
import com.ranzan.zbuilds.core.base.UiText
import com.ranzan.zbuilds.domain.model.AppError

/**
 * Turns an error into text for the user. App errors use string resources;
 * other errors (e.g. network failures) show the system message.
 */
fun Throwable?.toUiText(): UiText = when (this) {
    is AppError.InvalidSourceUrl -> UiText.res(R.string.error_invalid_source_url)
    is AppError.NoReleases -> UiText.res(R.string.error_no_releases)
    is AppError.Http -> UiText.res(R.string.error_github_http, code)
    is AppError.TokenRejected -> UiText.res(R.string.error_token_rejected, code)
    is AppError.EmptyDownload -> UiText.res(R.string.error_download_empty)
    is AppError.SaveFailed -> UiText.res(R.string.error_download_save)
    else -> this?.message?.takeIf { it.isNotBlank() }?.let { UiText.Raw(it) } ?: UiText.res(R.string.error_unknown)
}
