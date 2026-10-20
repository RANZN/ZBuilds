package com.ranzan.zbuilds.core.utils

import com.ranzan.zbuilds.domain.model.AppSettings
import com.ranzan.zbuilds.domain.model.CrashReport
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Utility helper to format locally captured crash reports into markdown
 * and generate GitHub issue pre-filled URLs for FOSS community reporting.
 */
object CrashReportHelper {

    /**
     * Builds a pre-filled GitHub new issue URL with structured crash diagnostics.
     */
    fun createGithubIssueUrl(
        report: CrashReport,
        repoUrl: String = AppSettings.DEFAULT_PROJECT_URL
    ): String {
        val cleanException = report.exceptionClass.substringAfterLast('.')
        val cleanMessage = report.message?.take(60)?.replace("\n", " ") ?: "Uncaught Exception"
        val title = "[Crash] $cleanException: $cleanMessage"

        val body = buildString {
            appendLine("### Crash Report (Auto-generated)")
            appendLine("A crash was captured locally in ZBuilds.")
            appendLine()
            appendLine("#### Environment")
            appendLine("- **App Version:** ${report.appVersion}")
            appendLine("- **Device:** ${report.deviceInfo}")
            appendLine("- **Fatal:** ${report.isFatal}")
            appendLine("- **Thread:** ${report.threadName}")
            appendLine("- **Date:** ${FormatHelper.formatDate(report.timestamp)}")
            appendLine()
            appendLine("#### Exception")
            appendLine("`${report.exceptionClass}`: ${report.message ?: "No error message"}")
            appendLine()
            appendLine("#### Stack Trace")
            appendLine("```")
            appendLine(report.stackTrace.take(2500))
            if (report.stackTrace.length > 2500) {
                appendLine("... [truncated for URL length]")
            }
            appendLine("```")
            if (report.breadcrumbs.isNotEmpty()) {
                appendLine()
                appendLine("#### Recent Breadcrumbs")
                appendLine("```")
                report.breadcrumbs.takeLast(10).forEach { appendLine(it) }
                appendLine("```")
            }
        }

        val encodedTitle = URLEncoder.encode(title, StandardCharsets.UTF_8.name())
        val encodedBody = URLEncoder.encode(body, StandardCharsets.UTF_8.name())
        return "$repoUrl/issues/new?title=$encodedTitle&body=$encodedBody"
    }

    /**
     * Formats the complete crash report as markdown for copying to clipboard or sharing.
     */
    fun formatReportAsMarkdown(report: CrashReport): String {
        return buildString {
            appendLine("# ZBuilds Crash Report")
            appendLine("- **App Version:** ${report.appVersion}")
            appendLine("- **Device:** ${report.deviceInfo}")
            appendLine("- **Fatal:** ${report.isFatal}")
            appendLine("- **Thread:** ${report.threadName}")
            appendLine("- **Date:** ${FormatHelper.formatDate(report.timestamp)}")
            appendLine()
            appendLine("## Exception")
            appendLine("${report.exceptionClass}: ${report.message.orEmpty()}")
            appendLine()
            appendLine("## Stack Trace")
            appendLine("```")
            appendLine(report.stackTrace)
            appendLine("```")
            if (report.breadcrumbs.isNotEmpty()) {
                appendLine()
                appendLine("## Breadcrumbs")
                appendLine("```")
                report.breadcrumbs.forEach { appendLine(it) }
                appendLine("```")
            }
        }
    }
}
