package com.ranzan.zbuilds

import com.ranzan.zbuilds.core.utils.CrashReportHelper
import com.ranzan.zbuilds.domain.model.CrashReport
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReportHelperTest {

    @Test
    fun createGithubIssueUrl_generatesValidIssueUrlWithTitleAndBody() {
        val report = CrashReport(
            id = "test-123",
            timestamp = 1718000000000L,
            exceptionClass = "java.lang.NullPointerException",
            message = "Test NPE message",
            stackTrace = "java.lang.NullPointerException: Test NPE message\n\tat com.ranzan.zbuilds.Test.run(Test.kt:10)",
            threadName = "main",
            deviceInfo = "Google Pixel 8 (Android 14)",
            appVersion = "1.0.0",
            isFatal = true,
            breadcrumbs = listOf("User tapped button", "Network call initiated")
        )

        val url = CrashReportHelper.createGithubIssueUrl(report, repoUrl = "https://github.com/RANZN/ZBuilds")

        assertTrue(url.startsWith("https://github.com/RANZN/ZBuilds/issues/new?"))
        assertTrue(url.contains("title="))
        assertTrue(url.contains("body="))
        assertTrue(url.contains("NullPointerException"))
    }

    @Test
    fun formatReportAsMarkdown_containsStructuredSections() {
        val report = CrashReport(
            id = "test-456",
            timestamp = 1718000000000L,
            exceptionClass = "java.lang.IllegalStateException",
            message = "State invalid",
            stackTrace = "java.lang.IllegalStateException: State invalid",
            threadName = "DefaultDispatcher-worker-1",
            deviceInfo = "Samsung Galaxy S24 (Android 14)",
            appVersion = "1.0.0",
            isFatal = false,
            breadcrumbs = listOf("App started", "Downloaded release")
        )

        val markdown = CrashReportHelper.formatReportAsMarkdown(report)

        assertTrue(markdown.contains("# ZBuilds Crash Report"))
        assertTrue(markdown.contains("IllegalStateException"))
        assertTrue(markdown.contains("State invalid"))
        assertTrue(markdown.contains("Samsung Galaxy S24"))
        assertTrue(markdown.contains("Downloaded release"))
    }
}
