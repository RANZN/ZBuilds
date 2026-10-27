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
        const val DEFAULT_PROJECT_URL = "https://github.com/RANZN/ZBuilds"
    }
}
