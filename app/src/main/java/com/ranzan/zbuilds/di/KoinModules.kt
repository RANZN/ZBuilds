package com.ranzan.zbuilds.di

import com.ranzan.zbuilds.data.local.AppDownloader
import com.ranzan.zbuilds.data.local.PackageInspectorImpl
import com.ranzan.zbuilds.data.local.TrackedAppsDataStore
import com.ranzan.zbuilds.data.remote.GitHubApiService
import com.ranzan.zbuilds.data.repository.AppRepositoryImpl
import com.ranzan.zbuilds.data.repository.DownloaderRepositoryImpl
import com.ranzan.zbuilds.data.repository.SuggestionRepositoryImpl
import com.ranzan.zbuilds.domain.repository.AppRepository
import com.ranzan.zbuilds.domain.repository.DownloaderRepository
import com.ranzan.zbuilds.domain.repository.PackageInspectorRepository
import com.ranzan.zbuilds.domain.repository.SuggestionRepository
import com.ranzan.zbuilds.domain.usecase.AddTrackedAppUseCase
import com.ranzan.zbuilds.domain.usecase.CheckAppUpdatesUseCase
import com.ranzan.zbuilds.domain.usecase.DeleteTrackedAppUseCase
import com.ranzan.zbuilds.domain.usecase.DownloadAppUseCase
import com.ranzan.zbuilds.domain.usecase.FetchSourceReleasesUseCase
import com.ranzan.zbuilds.domain.usecase.GetSourceSuggestionsUseCase
import com.ranzan.zbuilds.domain.usecase.GetTrackedAppsUseCase
import com.ranzan.zbuilds.ui.home.HomeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { GitHubApiService() }
    single { TrackedAppsDataStore(androidContext()) }
    single { PackageInspectorImpl(androidContext()) }
    single<PackageInspectorRepository> { get<PackageInspectorImpl>() }
    single { AppDownloader(androidContext(), get(), get()) }
}

val repositoryModule = module {
    single<AppRepository> { AppRepositoryImpl(get(), get(), get()) }
    single<DownloaderRepository> { DownloaderRepositoryImpl(get()) }
    single<SuggestionRepository> { SuggestionRepositoryImpl() }
}

val useCaseModule = module {
    factory { GetTrackedAppsUseCase(get()) }
    factory { AddTrackedAppUseCase(get()) }
    factory { DeleteTrackedAppUseCase(get()) }
    factory { FetchSourceReleasesUseCase(get()) }
    factory { CheckAppUpdatesUseCase(get()) }
    factory { DownloadAppUseCase(get()) }
    factory { GetSourceSuggestionsUseCase(get()) }
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
            packageInspector = get()
        )
    }
}

val appModules = listOf(
    appModule,
    repositoryModule,
    useCaseModule,
    viewModelModule
)
