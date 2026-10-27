package com.ranzan.zbuilds.data.analytics

import android.content.Context
import com.ranzan.zbuilds.R
import com.ranzan.zbuilds.domain.model.CrashReport
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

/**
 * Thread-safe local storage data source for persisting, loading, and pruning crash diagnostic reports.
 */
class CrashStorageDataSource(
    private val context: Context
) {
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private val mutex = Mutex()
    private val unknownText by lazy { context.getString(R.string.common_unknown) }
    private val reportsDir by lazy {
        File(context.filesDir, "crash_reports").apply {
            if (!exists()) mkdirs()
        }
    }

    private val _crashReportsFlow = MutableStateFlow<List<CrashReport>>(emptyList())
    val crashReportsFlow: Flow<List<CrashReport>> = _crashReportsFlow.asStateFlow()

    init {
        scope.launch {
            loadReports()
        }
    }

    /**
     * Synchronously persists a crash report (crucial during uncaught exception handler execution
     * before the process may be killed by the OS).
     */
    fun saveReportSync(report: CrashReport) {
        try {
            val file = File(reportsDir, "crash_${report.timestamp}_${report.id}.json")
            val json = JSONObject().apply {
                put("id", report.id)
                put("timestamp", report.timestamp)
                put("exceptionClass", report.exceptionClass)
                put("message", report.message ?: "")
                put("stackTrace", report.stackTrace)
                put("threadName", report.threadName)
                put("deviceInfo", report.deviceInfo)
                put("appVersion", report.appVersion)
                put("isFatal", report.isFatal)
                put("breadcrumbs", JSONArray(report.breadcrumbs))

                val metaObj = JSONObject()
                report.metadata.forEach { (k, v) -> metaObj.put(k, v) }
                put("metadata", metaObj)
            }
            file.writeText(json.toString(2))
            pruneOldReports()
            _crashReportsFlow.value = readAllReportsFromDisk()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun clearAll() = mutex.withLock {
        try {
            reportsDir.listFiles()?.forEach { it.delete() }
            _crashReportsFlow.value = emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun loadReports() = mutex.withLock {
        _crashReportsFlow.value = readAllReportsFromDisk()
    }

    private fun readAllReportsFromDisk(): List<CrashReport> {
        return try {
            reportsDir.listFiles { file -> file.name.endsWith(".json") }
                ?.mapNotNull { file ->
                    parseReport(file)
                }
                ?.sortedByDescending { it.timestamp }
                ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun parseReport(file: File): CrashReport? {
        return try {
            val content = file.readText()
            val json = JSONObject(content)

            val breadcrumbs = mutableListOf<String>()
            val bcArray = json.optJSONArray("breadcrumbs")
            if (bcArray != null) {
                for (i in 0 until bcArray.length()) {
                    breadcrumbs.add(bcArray.getString(i))
                }
            }

            val metadata = mutableMapOf<String, String>()
            val metaObj = json.optJSONObject("metadata")
            if (metaObj != null) {
                val keys = metaObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    metadata[key] = metaObj.getString(key)
                }
            }

            CrashReport(
                id = json.getString("id"),
                timestamp = json.getLong("timestamp"),
                exceptionClass = json.getString("exceptionClass"),
                message = json.optString("message").ifEmpty { null },
                stackTrace = json.getString("stackTrace"),
                threadName = json.optString("threadName", unknownText),
                deviceInfo = json.optString("deviceInfo", unknownText),
                appVersion = json.optString("appVersion", unknownText),
                isFatal = json.optBoolean("isFatal", true),
                breadcrumbs = breadcrumbs,
                metadata = metadata
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun pruneOldReports(maxReports: Int = 25) {
        val files = reportsDir.listFiles { file -> file.name.endsWith(".json") }
            ?.sortedByDescending { it.lastModified() }
            ?: return

        if (files.size > maxReports) {
            files.drop(maxReports).forEach { it.delete() }
        }
    }
}
