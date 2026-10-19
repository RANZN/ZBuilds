package com.ranzan.zbuilds.ui.settings

import com.ranzan.zbuilds.domain.model.AppSettings
import com.ranzan.zbuilds.domain.model.CrashReport

data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val tokenInput: String = "",
    val isValidatingToken: Boolean = false,
    val tokenValidationMessage: String? = null,
    val isTokenValid: Boolean = false,
    val isClearingCache: Boolean = false,
    val snackbarMessage: String? = null,
    val crashReports: List<CrashReport> = emptyList()
)

sealed interface SettingsUiEvent {
    data class OnTokenInputChanged(val token: String) : SettingsUiEvent
    data object OnSaveToken : SettingsUiEvent
    data object OnClearToken : SettingsUiEvent
    data object OnTestToken : SettingsUiEvent
    data object OnDismissTokenFeedback : SettingsUiEvent
    data class OnToggleBuildFormats(val showApks: Boolean, val showModules: Boolean) : SettingsUiEvent
    data class OnFolderUpdated(val uri: String?, val path: String?) : SettingsUiEvent
    data class OnToggleAutoClean(val enabled: Boolean) : SettingsUiEvent
    data class OnSelectRetentionDays(val days: Int) : SettingsUiEvent
    data object OnClearCache : SettingsUiEvent
    data object OnClearSnackbar : SettingsUiEvent
    data object OnClearCrashReports : SettingsUiEvent
    data object OnSimulateTestCrash : SettingsUiEvent
}
