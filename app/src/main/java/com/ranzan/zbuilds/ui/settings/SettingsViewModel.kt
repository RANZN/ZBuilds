package com.ranzan.zbuilds.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ranzan.zbuilds.core.utils.FormatHelper
import com.ranzan.zbuilds.domain.usecase.ClearCrashReportsUseCase
import com.ranzan.zbuilds.domain.usecase.ClearDownloadCacheUseCase
import com.ranzan.zbuilds.domain.usecase.GetCrashReportsUseCase
import com.ranzan.zbuilds.domain.usecase.GetSettingsUseCase
import com.ranzan.zbuilds.domain.usecase.TrackCrashUseCase
import com.ranzan.zbuilds.domain.usecase.TrackEventUseCase
import com.ranzan.zbuilds.domain.usecase.UpdateSettingsUseCase
import com.ranzan.zbuilds.domain.usecase.ValidateGithubTokenUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val validateGithubTokenUseCase: ValidateGithubTokenUseCase,
    private val clearDownloadCacheUseCase: ClearDownloadCacheUseCase,
    private val getCrashReportsUseCase: GetCrashReportsUseCase,
    private val clearCrashReportsUseCase: ClearCrashReportsUseCase,
    private val trackCrashUseCase: TrackCrashUseCase,
    private val trackEventUseCase: TrackEventUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        observeSettings()
        observeCrashReports()
        trackEventUseCase("settings_screen_opened")
    }

    private fun observeSettings() {
        viewModelScope.launch {
            getSettingsUseCase().collectLatest { settings ->
                _uiState.update { current ->
                    current.copy(
                        settings = settings,
                        tokenInput = if (current.tokenInput.isEmpty()) settings.githubToken.orEmpty() else current.tokenInput
                    )
                }
            }
        }
    }

    private fun observeCrashReports() {
        viewModelScope.launch {
            getCrashReportsUseCase().collectLatest { reports ->
                _uiState.update { it.copy(crashReports = reports) }
            }
        }
    }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            is SettingsUiEvent.OnTokenInputChanged -> {
                _uiState.update { it.copy(tokenInput = event.token) }
            }

            is SettingsUiEvent.OnSaveToken -> {
                val token = _uiState.value.tokenInput.trim().ifBlank { null }
                viewModelScope.launch {
                    updateSettingsUseCase.updateGithubToken(token)
                    trackEventUseCase("token_saved", mapOf("has_token" to (token != null).toString()))
                    _uiState.update {
                        it.copy(snackbarMessage = if (token != null) "GitHub token saved" else "GitHub token cleared")
                    }
                }
            }

            is SettingsUiEvent.OnClearToken -> {
                viewModelScope.launch {
                    updateSettingsUseCase.updateGithubToken(null)
                    trackEventUseCase("token_cleared")
                    _uiState.update {
                        it.copy(
                            tokenInput = "",
                            tokenValidationMessage = null,
                            snackbarMessage = "GitHub token removed"
                        )
                    }
                }
            }

            is SettingsUiEvent.OnTestToken -> {
                val token = _uiState.value.tokenInput.trim()
                if (token.isBlank()) {
                    _uiState.update {
                        it.copy(
                            tokenValidationMessage = "Please enter a token first to test.",
                            isTokenValid = false
                        )
                    }
                    return
                }

                viewModelScope.launch {
                    _uiState.update { it.copy(isValidatingToken = true, tokenValidationMessage = null) }
                    trackEventUseCase("token_tested")
                    val result = validateGithubTokenUseCase(token)
                    result.fold(
                        onSuccess = { message ->
                            _uiState.update {
                                it.copy(
                                    isValidatingToken = false,
                                    tokenValidationMessage = message,
                                    isTokenValid = true
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isValidatingToken = false,
                                    tokenValidationMessage = error.message ?: "Validation failed",
                                    isTokenValid = false
                                )
                            }
                        }
                    )
                }
            }

            is SettingsUiEvent.OnDismissTokenFeedback -> {
                _uiState.update { it.copy(tokenValidationMessage = null) }
            }

            is SettingsUiEvent.OnToggleBuildFormats -> {
                if (!event.showApks && !event.showModules) {
                    _uiState.update {
                        it.copy(snackbarMessage = "At least one format (APKs or Modules) must remain selected.")
                    }
                    return
                }

                viewModelScope.launch {
                    updateSettingsUseCase.updateBuildFormats(event.showApks, event.showModules)
                    trackEventUseCase("build_formats_toggled", mapOf("apks" to event.showApks.toString(), "modules" to event.showModules.toString()))
                    _uiState.update { it.copy(snackbarMessage = "Build formats updated") }
                }
            }

            is SettingsUiEvent.OnFolderUpdated -> {
                viewModelScope.launch {
                    updateSettingsUseCase.updateStorageFolder(event.uri, event.path)
                    trackEventUseCase("storage_folder_changed", mapOf("custom" to (event.uri != null).toString()))
                    val msg = if (event.uri != null) "Download folder updated: ${event.path}" else "Reset to default app storage"
                    _uiState.update { it.copy(snackbarMessage = msg) }
                }
            }

            is SettingsUiEvent.OnToggleAutoClean -> {
                viewModelScope.launch {
                    updateSettingsUseCase.updateAutoClean(event.enabled)
                    trackEventUseCase("auto_clean_toggled", mapOf("enabled" to event.enabled.toString()))
                    val msg = if (event.enabled) "Auto-clean old versions enabled" else "Auto-clean disabled"
                    _uiState.update { it.copy(snackbarMessage = msg) }
                }
            }

            is SettingsUiEvent.OnSelectRetentionDays -> {
                viewModelScope.launch {
                    updateSettingsUseCase.updateCleanupRetentionDays(event.days)
                    trackEventUseCase("retention_days_changed", mapOf("days" to event.days.toString()))
                    _uiState.update {
                        it.copy(snackbarMessage = "Cleanup retention set to ${event.days} days")
                    }
                }
            }

            is SettingsUiEvent.OnClearCache -> {
                viewModelScope.launch {
                    _uiState.update { it.copy(isClearingCache = true) }
                    trackEventUseCase("storage_cache_cleared")
                    val result = clearDownloadCacheUseCase()
                    result.fold(
                        onSuccess = { freedBytes ->
                            val readable = FormatHelper.formatBytes(freedBytes)
                            _uiState.update {
                                it.copy(
                                    isClearingCache = false,
                                    snackbarMessage = "Cleaned storage: $readable freed"
                                )
                            }
                        },
                        onFailure = { error ->
                            _uiState.update {
                                it.copy(
                                    isClearingCache = false,
                                    snackbarMessage = "Failed to clean storage: ${error.message}"
                                )
                            }
                        }
                    )
                }
            }

            is SettingsUiEvent.OnClearCrashReports -> {
                viewModelScope.launch {
                    clearCrashReportsUseCase()
                    trackEventUseCase("crash_reports_cleared")
                    _uiState.update { it.copy(snackbarMessage = "Crash reports cleared") }
                }
            }

            is SettingsUiEvent.OnSimulateTestCrash -> {
                viewModelScope.launch {
                    trackCrashUseCase(
                        throwable = IllegalStateException("Test Diagnostic Crash generated from Settings"),
                        isFatal = false,
                        metadata = mapOf("source" to "settings_diagnostics_ui")
                    )
                    _uiState.update { it.copy(snackbarMessage = "Diagnostic test crash recorded successfully") }
                }
            }

            is SettingsUiEvent.OnClearSnackbar -> {
                _uiState.update { it.copy(snackbarMessage = null) }
            }
        }
    }
}
