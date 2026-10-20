package com.ranzan.zbuilds.di

import com.ranzan.zbuilds.data.analytics.AnalyticsTrackerImpl
import com.ranzan.zbuilds.data.analytics.CrashStorageDataSource
import com.ranzan.zbuilds.data.analytics.ZBuildsCrashHandler
import com.ranzan.zbuilds.data.local.AppDownloader
import com.ranzan.zbuilds.data.local.PackageInspectorImpl
import com.ranzan.zbuilds.data.local.SettingsDataStore
import com.ranzan.zbuilds.data.local.TrackedAppsDataStore
import com.ranzan.zbuilds.data.remote.GitHubApiService
import com.ranzan.zbuilds.data.repository.AppRepositoryImpl
import com.ranzan.zbuilds.data.repository.DownloaderRepositoryImpl
import com.ranzan.zbuilds.data.repository.SettingsRepositoryImpl
import com.ranzan.zbuilds.data.repository.SuggestionRepositoryImpl
import com.ranzan.zbuilds.domain.repository.AnalyticsTracker
import com.ranzan.zbuilds.domain.repository.AppRepository
import com.ranzan.zbuilds.domain.repository.DownloaderRepository
import com.ranzan.zbuilds.domain.repository.PackageInspectorRepository
import com.ranzan.zbuilds.domain.repository.SettingsRepository
import com.ranzan.zbuilds.domain.repository.SuggestionRepository
import com.ranzan.zbuilds.domain.usecase.AddTrackedAppUseCase
import com.ranzan.zbuilds.domain.usecase.CheckAppUpdatesUseCase
import com.ranzan.zbuilds.domain.usecase.ClearCrashReportsUseCase
import com.ranzan.zbuilds.domain.usecase.ClearDownloadCacheUseCase
import com.ranzan.zbuilds.domain.usecase.DeleteTrackedAppUseCase
import com.ranzan.zbuilds.domain.usecase.DownloadAppUseCase
import com.ranzan.zbuilds.domain.usecase.FetchSourceReleasesUseCase
import com.ranzan.zbuilds.domain.usecase.GetCrashReportsUseCase
import com.ranzan.zbuilds.domain.usecase.GetSettingsUseCase
import com.ranzan.zbuilds.domain.usecase.GetSourceSuggestionsUseCase
import com.ranzan.zbuilds.domain.usecase.GetTrackedAppsUseCase
import com.ranzan.zbuilds.domain.usecase.TrackCrashUseCase
import com.ranzan.zbuilds.domain.usecase.TrackEventUseCase
import com.ranzan.zbuilds.domain.usecase.UpdateSettingsUseCase
import com.ranzan.zbuilds.domain.usecase.ValidateGithubTokenUseCase
import com.ranzan.zbuilds.ui.home.HomeViewModel
import com.ranzan.zbuilds.ui.settings.SettingsViewModel
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    singleOf(::SettingsDataStore)
    singleOf(::GitHubApiService)
    singleOf(::TrackedAppsDataStore)
    singleOf(::PackageInspectorImpl) bind PackageInspectorRepository::class
    singleOf(::AppDownloader)
    singleOf(::CrashStorageDataSource)
    singleOf(::AnalyticsTrackerImpl) bind AnalyticsTracker::class
    singleOf(::ZBuildsCrashHandler)
}

val repositoryModule = module {
    singleOf(::AppRepositoryImpl) bind AppRepository::class
    singleOf(::DownloaderRepositoryImpl) bind DownloaderRepository::class
    singleOf(::SuggestionRepositoryImpl) bind SuggestionRepository::class
    singleOf(::SettingsRepositoryImpl) bind SettingsRepository::class
}

val useCaseModule = module {
    factoryOf(::GetTrackedAppsUseCase)
    factoryOf(::AddTrackedAppUseCase)
    factoryOf(::DeleteTrackedAppUseCase)
    factoryOf(::FetchSourceReleasesUseCase)
    factoryOf(::CheckAppUpdatesUseCase)
    factoryOf(::DownloadAppUseCase)
    factoryOf(::GetSourceSuggestionsUseCase)
    factoryOf(::GetSettingsUseCase)
    factoryOf(::UpdateSettingsUseCase)
    factoryOf(::ValidateGithubTokenUseCase)
    factoryOf(::ClearDownloadCacheUseCase)
    factoryOf(::TrackEventUseCase)
    factoryOf(::TrackCrashUseCase)
    factoryOf(::GetCrashReportsUseCase)
    factoryOf(::ClearCrashReportsUseCase)
}

val viewModelModule = module {
    viewModelOf(::HomeViewModel)
    viewModelOf(::SettingsViewModel)
}

val appModules = listOf(
    appModule,
    repositoryModule,
    useCaseModule,
    viewModelModule
)
