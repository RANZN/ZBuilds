package com.ranzan.zbuilds.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class AppFileType {
    APK,
    ZIP;

    val displayName: String
        get() = when (this) {
            APK -> "APK"
            ZIP -> "ZIP"
        }
}
