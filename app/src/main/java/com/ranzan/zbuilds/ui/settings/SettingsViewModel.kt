package com.ranzan.zbuilds.ui.settings

import androidx.lifecycle.viewModelScope
import com.ranzan.zbuilds.R
import com.ranzan.zbuilds.core.base.BaseViewModel
import com.ranzan.zbuilds.core.base.UiText
import com.ranzan.zbuilds.core.utils.CrashReportHelper
import com.ranzan.zbuilds.core.utils.FormatHelper
import com.ranzan.zbuilds.domain.model.AppSettings
import com.ranzan.zbuilds.domain.usecase.ClearCrashReportsUseCase
import com.ranzan.zbuilds.domain.usecase.ClearDownloadCacheUseCase
import com.ranzan.zbuilds.domain.usecase.GetAppVersionUseCase
import com.ranzan.zbuilds.domain.usecase.GetCrashReportsUseCase
import com.ranzan.zbuilds.domain.usecase.GetSettingsUseCase
import com.ranzan.zbuilds.domain.usecase.UpdateSettingsUseCase
import com.ranzan.zbuilds.domain.usecase.ValidateGithubTokenUseCase
import com.ranzan.zbuilds.ui.util.toUiText
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
    private val validateGithubTokenUseCase: ValidateGithubTokenUseCase,
    private val clearDownloadCacheUseCase: ClearDownloadCacheUseCase,
    private val getCrashReportsUseCase: GetCrashReportsUseCase,
    private val clearCrashReportsUseCase: ClearCrashReportsUseCase,
    private val crashReportHelper: CrashReportHelper,
    private val getAppVersionUseCase: GetAppVersionUseCase
) : BaseViewModel<SettingsUiState, SettingsUiAction, SettingsUiEffect>(SettingsUiState()) {

    companion object {
        // Shown when GitHub does not report rate limits (the authenticated default)
        private const val DEFAULT_RATE_LIMIT = 5000

        const val GITHUB_GENERATE_TOKEN_URL = "https://github.com/settings/tokens/new?description=ZBuilds%20Android%20App&scopes=public_repo"
    }

    init {
        setState { copy(appVersion = getAppVersionUseCase()) }
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
                    val msg = UiText.res(if (token != null) R.string.settings_msg_token_saved else R.string.settings_msg_token_cleared)
                    sendEffect(SettingsUiEffect.ShowSnackbar(msg))
                }
            }

            is SettingsUiAction.OnClearToken -> {
                viewModelScope.launch {
                    updateSettingsUseCase.updateGithubToken(null)
                    val msg = UiText.res(R.string.settings_msg_token_removed)
                    setState {
                        copy(
                            tokenInput = "",
                            tokenValidationMessage = null
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
                            tokenValidationMessage = UiText.res(R.string.settings_msg_token_empty),
                            isTokenValid = false
                        )
                    }
                    return
                }

                viewModelScope.launch {
                    setState { copy(isValidatingToken = true, tokenValidationMessage = null) }
                    val result = validateGithubTokenUseCase(token)
                    result.fold(
                        onSuccess = { status ->
                            val message = UiText.res(
                                R.string.settings_msg_token_valid,
                                status.login ?: UiText.res(R.string.settings_token_unknown_user),
                                status.remainingRequests ?: DEFAULT_RATE_LIMIT,
                                status.requestLimit ?: DEFAULT_RATE_LIMIT
                            )
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
                                    tokenValidationMessage = error.toUiText(),
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
                    val msg = UiText.res(R.string.settings_msg_formats_required)
                    sendEffect(SettingsUiEffect.ShowSnackbar(msg))
                    return
                }

                viewModelScope.launch {
                    updateSettingsUseCase.updateBuildFormats(action.showApks, action.showModules)
                    val msg = UiText.res(R.string.settings_msg_formats_updated)
                    sendEffect(SettingsUiEffect.ShowSnackbar(msg))
                }
            }

            is SettingsUiAction.OnFolderUpdated -> {
                viewModelScope.launch {
                    updateSettingsUseCase.updateStorageFolder(action.uri, action.path)
                    val msg = if (action.uri != null) {
                        UiText.res(R.string.settings_msg_folder_updated, action.path.orEmpty())
                    } else {
                        UiText.res(R.string.settings_msg_folder_reset)
                    }
                    sendEffect(SettingsUiEffect.ShowSnackbar(msg))
                }
            }

            is SettingsUiAction.OnToggleAutoClean -> {
                viewModelScope.launch {
                    updateSettingsUseCase.updateAutoClean(action.enabled)
                    val msg = UiText.res(if (action.enabled) R.string.settings_msg_auto_clean_on else R.string.settings_msg_auto_clean_off)
                    sendEffect(SettingsUiEffect.ShowSnackbar(msg))
                }
            }

            is SettingsUiAction.OnSelectRetentionDays -> {
                viewModelScope.launch {
                    updateSettingsUseCase.updateCleanupRetentionDays(action.days)
                    val msg = UiText.plural(R.plurals.settings_msg_retention_set, action.days, action.days)
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
                            val msg = UiText.res(R.string.settings_msg_storage_cleaned, readable)
                            setState { copy(isClearingCache = false) }
                            sendEffect(SettingsUiEffect.ShowSnackbar(msg))
                        },
                        onFailure = { error ->
                            val msg = UiText.res(R.string.settings_msg_storage_clean_failed, error.toUiText())
                            setState { copy(isClearingCache = false) }
                            sendEffect(SettingsUiEffect.ShowSnackbar(msg))
                        }
                    )
                }
            }

            is SettingsUiAction.OnClearCrashReports -> {
                viewModelScope.launch {
                    clearCrashReportsUseCase()
                    val msg = UiText.res(R.string.settings_msg_crashes_cleared)
                    sendEffect(SettingsUiEffect.ShowSnackbar(msg))
                }
            }

            is SettingsUiAction.OnReportCrashOnGithub -> {
                val url = crashReportHelper.createGithubIssueUrl(action.report)
                sendEffect(SettingsUiEffect.OpenUrl(url))
            }

            is SettingsUiAction.OnCopyCrashReport -> {
                val markdown = crashReportHelper.formatReportAsMarkdown(action.report)
                sendEffect(SettingsUiEffect.CopyToClipboard(UiText.res(R.string.settings_crash_clipboard_label), markdown))
                sendEffect(SettingsUiEffect.ShowSnackbar(UiText.res(R.string.settings_msg_crash_copied)))
            }

            is SettingsUiAction.OnShareCrashReport -> {
                val markdown = crashReportHelper.formatReportAsMarkdown(action.report)
                sendEffect(SettingsUiEffect.ShareText(UiText.res(R.string.settings_crash_share_title), markdown))
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
