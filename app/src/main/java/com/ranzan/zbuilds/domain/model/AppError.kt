package com.ranzan.zbuilds.domain.model

/**
 * Errors raised by the app itself. They carry no text; the UI turns them into
 * localized messages, so no user-facing strings live outside string resources.
 */
sealed class AppError : Exception() {
    class InvalidSourceUrl : AppError()
    class NoReleases : AppError()
    class Http(val code: Int) : AppError()
    class TokenRejected(val code: Int) : AppError()
    class EmptyDownload : AppError()
    class SaveFailed : AppError()
}
