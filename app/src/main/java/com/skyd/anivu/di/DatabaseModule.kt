package com.skyd.anivu.di

import com.skyd.anivu.model.db.AppDatabase
import com.skyd.anivu.model.db.SearchDomainDatabase
import org.koin.dsl.module

val databaseModule = module {
    single { AppDatabase.getInstance(get()) }
    single { get<AppDatabase>().groupDao() }
    single { get<AppDatabase>().feedDao() }
    single { get<AppDatabase>().articleDao() }
    single { get<AppDatabase>().enclosureDao() }
    single { get<AppDatabase>().articleCategoryDao() }
    single { get<AppDatabase>().downloadInfoDao() }
    single { get<AppDatabase>().torrentFileDao() }
    single { get<AppDatabase>().sessionParamsDao() }
    single { get<AppDatabase>().readHistoryDao() }
    single { get<AppDatabase>().mediaPlayHistoryDao() }
    single { get<AppDatabase>().rssModuleDao() }
    single { get<AppDatabase>().articleNotificationRuleDao() }
    single { get<AppDatabase>().playlistDao() }
    single { get<AppDatabase>().playlistItemDao() }
    single { get<AppDatabase>().playlistDao() }
    single { get<AppDatabase>().playlistDao() }

    single { SearchDomainDatabase.getInstance(get()) }
    single { get<SearchDomainDatabase>().searchDomainDao() }
}