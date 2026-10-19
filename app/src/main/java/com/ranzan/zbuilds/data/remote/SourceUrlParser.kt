package com.ranzan.zbuilds.data.remote

object SourceUrlParser {

    /**
     * Extracts (owner, repo) from GitHub URL or shorthand "owner/repo"
     */
    fun parseGitHubRepo(url: String): Pair<String, String>? {
        val trimmed = url.trim()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .removePrefix("github.com/")
            .trimStart('/')

        val segments = trimmed.split("/").filter { it.isNotBlank() }
        if (segments.size >= 2) {
            val owner = segments[0]
            val repo = segments[1]
                .removeSuffix(".git")
                .removeSuffix("/")
            return Pair(owner, repo)
        }
        return null
    }

    /**
     * Formats normalized GitHub repo releases API URL
     */
    fun toReleasesApiUrl(owner: String, repo: String): String {
        return "https://api.github.com/repos/$owner/$repo/releases"
    }

    /**
     * Formats normalized GitHub latest release API URL
     */
    fun toLatestReleaseApiUrl(owner: String, repo: String): String {
        return "https://api.github.com/repos/$owner/$repo/releases/latest"
    }
}
