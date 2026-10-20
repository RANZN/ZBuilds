package com.ranzan.zbuilds.ui.settings

import com.ranzan.zbuilds.core.base.UiAction
import com.ranzan.zbuilds.core.base.UiEffect
import com.ranzan.zbuilds.core.base.UiState
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
) : UiState

sealed interface SettingsUiAction : UiAction {
    data class OnTokenInputChanged(val token: String) : SettingsUiAction
    data object OnSaveToken : SettingsUiAction
    data object OnClearToken : SettingsUiAction
    data object OnTestToken : SettingsUiAction
    data object OnDismissTokenFeedback : SettingsUiAction
    data class OnToggleBuildFormats(val showApks: Boolean, val showModules: Boolean) : SettingsUiAction
    data class OnFolderUpdated(val uri: String?, val path: String?) : SettingsUiAction
    data class OnToggleAutoClean(val enabled: Boolean) : SettingsUiAction
    data class OnSelectRetentionDays(val days: Int) : SettingsUiAction
    data object OnClearCache : SettingsUiAction
    data object OnClearSnackbar : SettingsUiAction

    // Local Crash Reporting actions (FOSS)
    data object OnClearCrashReports : SettingsUiAction
    data object OnSimulateTestCrash : SettingsUiAction
    data class OnReportCrashOnGithub(val report: CrashReport) : SettingsUiAction
    data class OnCopyCrashReport(val report: CrashReport) : SettingsUiAction
    data class OnShareCrashReport(val report: CrashReport) : SettingsUiAction

    // Screen interactions delegated through MVI
    data object OnNavigateBackClick : SettingsUiAction
    data object OnGenerateTokenClick : SettingsUiAction
    data object OnOpenProjectUrlClick : SettingsUiAction
    data object OnPickFolderClick : SettingsUiAction
    data object OnPasteTokenClick : SettingsUiAction
}

typealias SettingsUiEvent = SettingsUiAction

sealed interface SettingsUiEffect : UiEffect {
    data object NavigateBack : SettingsUiEffect
    data class OpenUrl(val url: String) : SettingsUiEffect
    data object LaunchFolderPicker : SettingsUiEffect
    data object RequestPasteToken : SettingsUiEffect
    data class CopyToClipboard(val label: String, val text: String) : SettingsUiEffect
    data class ShareText(val title: String, val text: String) : SettingsUiEffect
    data class ShowSnackbar(val message: String) : SettingsUiEffect
}
