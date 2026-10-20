package com.ranzan.zbuilds

import com.ranzan.zbuilds.domain.model.CrashReport
import com.ranzan.zbuilds.domain.repository.CrashReporter
import com.ranzan.zbuilds.domain.usecase.ClearCrashReportsUseCase
import com.ranzan.zbuilds.domain.usecase.GetCrashReportsUseCase
import com.ranzan.zbuilds.domain.usecase.TrackCrashUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CrashReporterTest {

    private class FakeCrashReporter : CrashReporter {
        val crashes = mutableListOf<CrashReport>()

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
                    breadcrumbs = emptyList(),
                    metadata = metadata
                )
            )
        }

        override fun getCrashReports(): Flow<List<CrashReport>> {
            return flowOf(crashes.toList())
        }

        override suspend fun clearCrashReports() {
            crashes.clear()
        }
    }

    @Test
    fun `TrackCrashUseCase records exception details accurately`() {
        val fake = FakeCrashReporter()
        val useCase = TrackCrashUseCase(fake)

        val exception = IllegalArgumentException("Invalid release asset")
        useCase(exception, isFatal = true, mapOf("repo" to "RANZN/ZBuilds"))

        assertEquals(1, fake.crashes.size)
        val report = fake.crashes[0]
        assertEquals("java.lang.IllegalArgumentException", report.exceptionClass)
        assertEquals("Invalid release asset", report.message)
        assertTrue(report.isFatal)
        assertEquals("RANZN/ZBuilds", report.metadata["repo"])
    }

    @Test
    fun `GetCrashReportsUseCase and ClearCrashReportsUseCase observe and clear logs`() = runTest {
        val fake = FakeCrashReporter()
        val trackCrashUseCase = TrackCrashUseCase(fake)
        val getUseCase = GetCrashReportsUseCase(fake)
        val clearUseCase = ClearCrashReportsUseCase(fake)

        trackCrashUseCase(IllegalStateException("Simulated crash 1"))
        trackCrashUseCase(NullPointerException("Simulated crash 2"))

        val recorded = getUseCase().first()
        assertEquals(2, recorded.size)

        clearUseCase()

        val afterClear = getUseCase().first()
        assertEquals(0, afterClear.size)
    }
}
