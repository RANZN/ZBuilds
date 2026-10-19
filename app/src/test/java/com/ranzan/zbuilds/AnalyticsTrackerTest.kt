package com.ranzan.zbuilds

import com.ranzan.zbuilds.domain.model.CrashReport
import com.ranzan.zbuilds.domain.repository.AnalyticsTracker
import com.ranzan.zbuilds.domain.usecase.ClearCrashReportsUseCase
import com.ranzan.zbuilds.domain.usecase.GetCrashReportsUseCase
import com.ranzan.zbuilds.domain.usecase.TrackCrashUseCase
import com.ranzan.zbuilds.domain.usecase.TrackEventUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsTrackerTest {

    private class FakeAnalyticsTracker : AnalyticsTracker {
        val events = mutableListOf<Pair<String, Map<String, String>>>()
        val breadcrumbs = mutableListOf<String>()
        val crashes = mutableListOf<CrashReport>()

        override fun trackEvent(name: String, parameters: Map<String, String>) {
            events.add(name to parameters)
        }

        override fun trackCrash(
            throwable: Throwable,
            isFatal: Boolean,
            metadata: Map<String, String>
        ) {
            crashes.add(
                CrashReport(
                    id = "test-id",
                    timestamp = System.currentTimeMillis(),
                    exceptionClass = throwable::class.java.name,
                    message = throwable.message,
                    stackTrace = throwable.stackTraceToString(),
                    threadName = Thread.currentThread().name,
                    deviceInfo = "Test Device",
                    appVersion = "1.0.0",
                    isFatal = isFatal,
                    breadcrumbs = breadcrumbs.toList(),
                    metadata = metadata
                )
            )
        }

        override fun logBreadcrumb(message: String) {
            breadcrumbs.add(message)
        }

        override fun getCrashReports(): Flow<List<CrashReport>> {
            return flowOf(crashes.toList())
        }

        override suspend fun clearCrashReports() {
            crashes.clear()
            breadcrumbs.clear()
        }
    }

    @Test
    fun `TrackEventUseCase records event name and parameters`() {
        val fake = FakeAnalyticsTracker()
        val useCase = TrackEventUseCase(fake)

        useCase("download_started", mapOf("app_id" to "123", "format" to "APK"))

        assertEquals(1, fake.events.size)
        assertEquals("download_started", fake.events[0].first)
        assertEquals("123", fake.events[0].second["app_id"])
        assertEquals("APK", fake.events[0].second["format"])
    }

    @Test
    fun `TrackCrashUseCase records exception details accurately`() {
        val fake = FakeAnalyticsTracker()
        val useCase = TrackCrashUseCase(fake)
        fake.logBreadcrumb("User opened sheet")

        val exception = IllegalArgumentException("Invalid release asset")
        useCase(exception, isFatal = true, mapOf("repo" to "RANZN/ZBuilds"))

        assertEquals(1, fake.crashes.size)
        val report = fake.crashes[0]
        assertEquals("java.lang.IllegalArgumentException", report.exceptionClass)
        assertEquals("Invalid release asset", report.message)
        assertTrue(report.isFatal)
        assertEquals("RANZN/ZBuilds", report.metadata["repo"])
        assertEquals(1, report.breadcrumbs.size)
        assertEquals("User opened sheet", report.breadcrumbs[0])
    }

    @Test
    fun `GetCrashReportsUseCase and ClearCrashReportsUseCase observe and clear logs`() = runTest {
        val fake = FakeAnalyticsTracker()
        val trackCrashUseCase = TrackCrashUseCase(fake)
        val getUseCase = GetCrashReportsUseCase(fake)
        val clearUseCase = ClearCrashReportsUseCase(fake)

        trackCrashUseCase(RuntimeException("Crash 1"))
        trackCrashUseCase(NullPointerException("Crash 2"))

        val initialReports = getUseCase().first()
        assertEquals(2, initialReports.size)

        clearUseCase()

        val clearedReports = getUseCase().first()
        assertEquals(0, clearedReports.size)
    }
}
