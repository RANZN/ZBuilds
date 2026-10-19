package com.ranzan.zbuilds.domain.model

data class AppSettings(
    val githubToken: String? = null,
    val showApks: Boolean = true,
    val showModules: Boolean = true,
    val downloadFolderUri: String? = null,
    val downloadFolderPath: String? = null,
    val autoCleanOldVersions: Boolean = true,
    val cleanupRetentionDays: Int = 7
) {
    companion object {
        const val DEFAULT_PROJECT_URL = "https://github.com/RANZN/Morphe-Builds"
        const val APP_VERSION = "v1.0.0"
        const val APP_DESCRIPTION = "Universal GitHub release tracker and updater for Android apps and root modules."
        const val TECH_STACK = "Jetpack Compose • Material 3 • Clean Architecture • Koin DI • Storage Access Framework"
    }
}
