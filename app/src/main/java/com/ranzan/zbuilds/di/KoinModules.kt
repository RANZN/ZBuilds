package com.ranzan.zbuilds.di

import com.ranzan.zbuilds.data.local.AppDownloader
import com.ranzan.zbuilds.data.local.PackageInspectorImpl
import com.ranzan.zbuilds.data.local.SettingsDataStore
import com.ranzan.zbuilds.data.local.TrackedAppsDataStore
import com.ranzan.zbuilds.data.remote.GitHubApiService
import com.ranzan.zbuilds.data.repository.AppRepositoryImpl
import com.ranzan.zbuilds.data.repository.DownloaderRepositoryImpl
import com.ranzan.zbuilds.data.repository.SettingsRepositoryImpl
import com.ranzan.zbuilds.data.repository.SuggestionRepositoryImpl
import com.ranzan.zbuilds.domain.repository.AppRepository
import com.ranzan.zbuilds.domain.repository.DownloaderRepository
import com.ranzan.zbuilds.domain.repository.PackageInspectorRepository
import com.ranzan.zbuilds.domain.repository.SettingsRepository
import com.ranzan.zbuilds.domain.repository.SuggestionRepository
import com.ranzan.zbuilds.domain.usecase.AddTrackedAppUseCase
import com.ranzan.zbuilds.domain.usecase.CheckAppUpdatesUseCase
import com.ranzan.zbuilds.domain.usecase.ClearDownloadCacheUseCase
import com.ranzan.zbuilds.domain.usecase.DeleteTrackedAppUseCase
import com.ranzan.zbuilds.domain.usecase.DownloadAppUseCase
import com.ranzan.zbuilds.domain.usecase.FetchSourceReleasesUseCase
import com.ranzan.zbuilds.domain.usecase.GetSettingsUseCase
import com.ranzan.zbuilds.domain.usecase.GetSourceSuggestionsUseCase
import com.ranzan.zbuilds.domain.usecase.GetTrackedAppsUseCase
import com.ranzan.zbuilds.domain.usecase.UpdateSettingsUseCase
import com.ranzan.zbuilds.domain.usecase.ValidateGithubTokenUseCase
import com.ranzan.zbuilds.ui.home.HomeViewModel
import com.ranzan.zbuilds.ui.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { SettingsDataStore(androidContext()) }
    single { GitHubApiService(get()) }
    single { TrackedAppsDataStore(androidContext()) }
    single { PackageInspectorImpl(androidContext()) }
    single<PackageInspectorRepository> { get<PackageInspectorImpl>() }
    single { AppDownloader(androidContext(), get(), get(), get()) }
}

val repositoryModule = module {
    single<AppRepository> { AppRepositoryImpl(get(), get(), get()) }
    single<DownloaderRepository> { DownloaderRepositoryImpl(get()) }
    single<SuggestionRepository> { SuggestionRepositoryImpl() }
    single<SettingsRepository> { SettingsRepositoryImpl(androidContext(), get(), get()) }
}

val useCaseModule = module {
    factory { GetTrackedAppsUseCase(get()) }
    factory { AddTrackedAppUseCase(get()) }
    factory { DeleteTrackedAppUseCase(get()) }
    factory { FetchSourceReleasesUseCase(get()) }
    factory { CheckAppUpdatesUseCase(get()) }
    factory { DownloadAppUseCase(get()) }
    factory { GetSourceSuggestionsUseCase(get()) }
    factory { GetSettingsUseCase(get()) }
    factory { UpdateSettingsUseCase(get()) }
    factory { ValidateGithubTokenUseCase(get()) }
    factory { ClearDownloadCacheUseCase(get()) }
}

val viewModelModule = module {
    viewModel {
        HomeViewModel(
            getTrackedAppsUseCase = get(),
            addTrackedAppUseCase = get(),
            deleteTrackedAppUseCase = get(),
            fetchSourceReleasesUseCase = get(),
            checkAppUpdatesUseCase = get(),
            downloadAppUseCase = get(),
            getSourceSuggestionsUseCase = get(),
            getSettingsUseCase = get()
        )
    }
    viewModel {
        SettingsViewModel(
            getSettingsUseCase = get(),
            updateSettingsUseCase = get(),
            validateGithubTokenUseCase = get(),
            clearDownloadCacheUseCase = get()
        )
    }
}

val appModules = listOf(
    appModule,
    repositoryModule,
    useCaseModule,
    viewModelModule
)
