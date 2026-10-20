package com.ranzan.zbuilds.ui.settings

import androidx.lifecycle.viewModelScope
import com.ranzan.zbuilds.core.base.BaseViewModel
import com.ranzan.zbuilds.core.utils.CrashReportHelper
import com.ranzan.zbuilds.core.utils.FormatHelper
import com.ranzan.zbuilds.domain.model.AppSettings
import com.ranzan.zbuilds.domain.usecase.ClearCrashReportsUseCase
import com.ranzan.zbuilds.domain.usecase.ClearDownloadCacheUseCase
import com.ranzan.zbuilds.domain.usecase.GetCrashReportsUseCase
import com.ranzan.zbuilds.domain.usecase.GetSettingsUseCase
import com.ranzan.zbuilds.domain.usecase.TrackCrashUseCase
import com.ranzan.zbuilds.domain.usecase.UpdateSettingsUseCase
import com.ranzan.zbuilds.domain.usecase.ValidateGithubTokenUseCase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val validateGithubTokenUseCase: ValidateGithubTokenUseCase,
    private val clearDownloadCacheUseCase: ClearDownloadCacheUseCase,
    private val getCrashReportsUseCase: GetCrashReportsUseCase,
    private val clearCrashReportsUseCase: ClearCrashReportsUseCase,
    private val trackCrashUseCase: TrackCrashUseCase
) : BaseViewModel<SettingsUiState, SettingsUiAction, SettingsUiEffect>(SettingsUiState()) {

    companion object {
        const val GITHUB_GENERATE_TOKEN_URL = "https://github.com/settings/tokens/new?description=ZBuilds%20Android%20App&scopes=public_repo"
    }

    init {
        observeSettings()
        observeCrashReports()
    }

    private fun observeSettings() {
        viewModelScope.launch {
            getSettingsUseCase().collectLatest { settings ->
                setState {
                    copy(
                        settings = settings,
                        tokenInput = if (tokenInput.isEmpty()) settings.githubToken.orEmpty() else tokenInput
                    )
                }
            }
        }
    }

    private fun observeCrashReports() {
        viewModelScope.launch {
            getCrashReportsUseCase().collectLatest { reports ->
                setState { copy(crashReports = reports) }
            }
        }
    }

    override fun onAction(action: SettingsUiAction) {
        when (action) {
            is SettingsUiAction.OnTokenInputChanged -> {
                setState { copy(tokenInput = action.token) }
            }

            is SettingsUiAction.OnSaveToken -> {
                val token = currentState.tokenInput.trim().ifBlank { null }
                viewModelScope.launch {
                    updateSettingsUseCase.updateGithubToken(token)
                    val msg = if (token != null) "GitHub token saved" else "GitHub token cleared"
                    setState { copy(snackbarMessage = msg) }
                    sendEffect(SettingsUiEffect.ShowSnackbar(msg))
                }
            }

            is SettingsUiAction.OnClearToken -> {
                viewModelScope.launch {
                    updateSettingsUseCase.updateGithubToken(null)
                    val msg = "GitHub token removed"
                    setState {
                        copy(
                            tokenInput = "",
                            tokenValidationMessage = null,
                            snackbarMessage = msg
                        )
                    }
                    sendEffect(SettingsUiEffect.ShowSnackbar(msg))
                }
            }

            is SettingsUiAction.OnTestToken -> {
                val token = currentState.tokenInput.trim()
                if (token.isBlank()) {
                    setState {
                        copy(
                            tokenValidationMessage = "Please enter a token first to test.",
                            isTokenValid = false
                        )
                    }
                    return
                }

                viewModelScope.launch {
                    setState { copy(isValidatingToken = true, tokenValidationMessage = null) }
                    val result = validateGithubTokenUseCase(token)
                    result.fold(
                        onSuccess = { message ->
                            setState {
                                copy(
                                    isValidatingToken = false,
                                    tokenValidationMessage = message,
                                    isTokenValid = true
                                )
                            }
                        },
                        onFailure = { error ->
                            setState {
                                copy(
                                    isValidatingToken = false,
                                    tokenValidationMessage = error.message ?: "Validation failed",
                                    isTokenValid = false
                                )
                            }
                        }
                    )
                }
            }

            is SettingsUiAction.OnDismissTokenFeedback -> {
                setState { copy(tokenValidationMessage = null) }
            }

            is SettingsUiAction.OnToggleBuildFormats -> {
                if (!action.showApks && !action.showModules) {
                    val msg = "At least one format (APKs or Modules) must remain selected."
                    setState { copy(snackbarMessage = msg) }
                    sendEffect(SettingsUiEffect.ShowSnackbar(msg))
                    return
                }

                viewModelScope.launch {
                    updateSettingsUseCase.updateBuildFormats(action.showApks, action.showModules)
                    val msg = "Build formats updated"
                    setState { copy(snackbarMessage = msg) }
                    sendEffect(SettingsUiEffect.ShowSnackbar(msg))
                }
            }

            is SettingsUiAction.OnFolderUpdated -> {
                viewModelScope.launch {
                    updateSettingsUseCase.updateStorageFolder(action.uri, action.path)
                    val msg = if (action.uri != null) "Download folder updated: ${action.path}" else "Reset to default app storage"
                    setState { copy(snackbarMessage = msg) }
                    sendEffect(SettingsUiEffect.ShowSnackbar(msg))
                }
            }

            is SettingsUiAction.OnToggleAutoClean -> {
                viewModelScope.launch {
                    updateSettingsUseCase.updateAutoClean(action.enabled)
                    val msg = if (action.enabled) "Auto-clean old versions enabled" else "Auto-clean disabled"
                    setState { copy(snackbarMessage = msg) }
                    sendEffect(SettingsUiEffect.ShowSnackbar(msg))
                }
            }

            is SettingsUiAction.OnSelectRetentionDays -> {
                viewModelScope.launch {
                    updateSettingsUseCase.updateCleanupRetentionDays(action.days)
                    val msg = "Cleanup retention set to ${action.days} days"
                    setState { copy(snackbarMessage = msg) }
                    sendEffect(SettingsUiEffect.ShowSnackbar(msg))
                }
            }

            is SettingsUiAction.OnClearCache -> {
                viewModelScope.launch {
                    setState { copy(isClearingCache = true) }
                    val result = clearDownloadCacheUseCase()
                    result.fold(
                        onSuccess = { freedBytes ->
                            val readable = FormatHelper.formatBytes(freedBytes)
                            val msg = "Cleaned storage: $readable freed"
                            setState {
                                copy(
                                    isClearingCache = false,
                                    snackbarMessage = msg
                                )
                            }
                            sendEffect(SettingsUiEffect.ShowSnackbar(msg))
                        },
                        onFailure = { error ->
                            val msg = "Failed to clean storage: ${error.message}"
                            setState {
                                copy(
                                    isClearingCache = false,
                                    snackbarMessage = msg
                                )
                            }
                            sendEffect(SettingsUiEffect.ShowSnackbar(msg))
                        }
                    )
                }
            }

            is SettingsUiAction.OnClearCrashReports -> {
                viewModelScope.launch {
                    clearCrashReportsUseCase()
                    val msg = "Local crash reports cleared"
                    setState { copy(snackbarMessage = msg) }
                    sendEffect(SettingsUiEffect.ShowSnackbar(msg))
                }
            }

            is SettingsUiAction.OnSimulateTestCrash -> {
                viewModelScope.launch {
                    trackCrashUseCase(
                        throwable = IllegalStateException("Test Diagnostic Crash generated from Settings (FOSS Local Crash Reporting)"),
                        isFatal = false,
                        metadata = mapOf("source" to "settings_local_crash_ui")
                    )
                    val msg = "Diagnostic test crash recorded locally"
                    setState { copy(snackbarMessage = msg) }
                    sendEffect(SettingsUiEffect.ShowSnackbar(msg))
                }
            }

            is SettingsUiAction.OnReportCrashOnGithub -> {
                val url = CrashReportHelper.createGithubIssueUrl(action.report)
                sendEffect(SettingsUiEffect.OpenUrl(url))
            }

            is SettingsUiAction.OnCopyCrashReport -> {
                val markdown = CrashReportHelper.formatReportAsMarkdown(action.report)
                sendEffect(SettingsUiEffect.CopyToClipboard("Crash Report", markdown))
                sendEffect(SettingsUiEffect.ShowSnackbar("Crash report copied to clipboard"))
            }

            is SettingsUiAction.OnShareCrashReport -> {
                val markdown = CrashReportHelper.formatReportAsMarkdown(action.report)
                sendEffect(SettingsUiEffect.ShareText("ZBuilds Crash Report", markdown))
            }

            is SettingsUiAction.OnClearSnackbar -> {
                setState { copy(snackbarMessage = null) }
            }

            is SettingsUiAction.OnNavigateBackClick -> {
                sendEffect(SettingsUiEffect.NavigateBack)
            }

            is SettingsUiAction.OnGenerateTokenClick -> {
                sendEffect(SettingsUiEffect.OpenUrl(GITHUB_GENERATE_TOKEN_URL))
            }

            is SettingsUiAction.OnOpenProjectUrlClick -> {
                sendEffect(SettingsUiEffect.OpenUrl(AppSettings.DEFAULT_PROJECT_URL))
            }

            is SettingsUiAction.OnPickFolderClick -> {
                sendEffect(SettingsUiEffect.LaunchFolderPicker)
            }

            is SettingsUiAction.OnPasteTokenClick -> {
                sendEffect(SettingsUiEffect.RequestPasteToken)
            }
        }
    }

    fun onEvent(event: SettingsUiEvent) = onAction(event)
}
