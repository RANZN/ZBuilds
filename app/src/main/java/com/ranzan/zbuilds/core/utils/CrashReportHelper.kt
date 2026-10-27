package com.ranzan.zbuilds.core.utils

import com.ranzan.zbuilds.R
import com.ranzan.zbuilds.domain.model.AppSettings
import com.ranzan.zbuilds.domain.model.CrashReport
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Formats locally captured crash reports into markdown and builds pre-filled
 * GitHub issue URLs for FOSS community reporting. All text comes from string resources.
 */
class CrashReportHelper(private val strings: StringProvider) {

    /**
     * Builds a pre-filled GitHub new issue URL with structured crash diagnostics.
     */
    fun createGithubIssueUrl(
        report: CrashReport,
        repoUrl: String = AppSettings.DEFAULT_PROJECT_URL
    ): String {
        val cleanException = report.exceptionClass.substringAfterLast('.')
        val cleanMessage = report.message?.take(ISSUE_TITLE_MESSAGE_LENGTH)?.replace("\n", " ")
            ?: strings.getString(R.string.crash_uncaught_exception)
        val title = strings.getString(R.string.crash_issue_title, cleanException, cleanMessage)

        val body = buildString {
            appendHeading(3, R.string.crash_issue_heading)
            appendLine(strings.getString(R.string.crash_issue_intro))
            appendLine()
            appendHeading(4, R.string.crash_section_environment)
            appendEnvironment(report)
            appendLine()
            appendHeading(4, R.string.crash_section_exception)
            appendLine("`${report.exceptionClass}`: ${report.message ?: strings.getString(R.string.settings_crash_no_message)}")
            appendLine()
            appendHeading(4, R.string.crash_section_stack_trace)
            appendLine(CODE_FENCE)
            appendLine(report.stackTrace.take(ISSUE_STACK_TRACE_LENGTH))
            if (report.stackTrace.length > ISSUE_STACK_TRACE_LENGTH) {
                appendLine(strings.getString(R.string.crash_issue_truncated))
            }
            appendLine(CODE_FENCE)
            if (report.breadcrumbs.isNotEmpty()) {
                appendLine()
                appendHeading(4, R.string.crash_section_recent_breadcrumbs)
                appendLine(CODE_FENCE)
                report.breadcrumbs.takeLast(ISSUE_BREADCRUMB_COUNT).forEach { appendLine(it) }
                appendLine(CODE_FENCE)
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
            appendHeading(1, R.string.crash_report_title)
            appendEnvironment(report)
            appendLine()
            appendHeading(2, R.string.crash_section_exception)
            appendLine("${report.exceptionClass}: ${report.message.orEmpty()}")
            appendLine()
            appendHeading(2, R.string.crash_section_stack_trace)
            appendLine(CODE_FENCE)
            appendLine(report.stackTrace)
            appendLine(CODE_FENCE)
            if (report.breadcrumbs.isNotEmpty()) {
                appendLine()
                appendHeading(2, R.string.crash_section_breadcrumbs)
                appendLine(CODE_FENCE)
                report.breadcrumbs.forEach { appendLine(it) }
                appendLine(CODE_FENCE)
            }
        }
    }

    private fun StringBuilder.appendHeading(level: Int, titleRes: Int) {
        appendLine("${"#".repeat(level)} ${strings.getString(titleRes)}")
    }

    private fun StringBuilder.appendEnvironment(report: CrashReport) {
        appendField(R.string.crash_label_app_version, report.appVersion)
        appendField(R.string.crash_label_device, report.deviceInfo)
        appendField(R.string.crash_label_fatal, report.isFatal.toString())
        appendField(R.string.crash_label_thread, report.threadName)
        appendField(R.string.crash_label_date, FormatHelper.formatDate(report.timestamp).orEmpty())
    }

    private fun StringBuilder.appendField(labelRes: Int, value: String) {
        appendLine("- **${strings.getString(labelRes)}:** $value")
    }

    private companion object {
        const val CODE_FENCE = "```"
        const val ISSUE_TITLE_MESSAGE_LENGTH = 60
        const val ISSUE_STACK_TRACE_LENGTH = 2500
        const val ISSUE_BREADCRUMB_COUNT = 10
    }
}
