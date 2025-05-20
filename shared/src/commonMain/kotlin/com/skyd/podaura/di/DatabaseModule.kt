package com.skyd.podaura.di

import com.skyd.podaura.model.db.AppDatabase
import com.skyd.podaura.model.db.SearchDomainDatabase
import com.skyd.podaura.model.db.builder
import com.skyd.podaura.model.db.instance
import org.koin.dsl.module

val databaseModule = module {
    single { AppDatabase.instance(AppDatabase.builder()) }
    single { get<AppDatabase>().groupDao() }
    single { get<AppDatabase>().feedDao() }
    single { get<AppDatabase>().articleDao() }
    single { get<AppDatabase>().enclosureDao() }
    single { get<AppDatabase>().autoDownloadRuleDao() }
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

    single { SearchDomainDatabase.instance(SearchDomainDatabase.builder()) }
    single { get<SearchDomainDatabase>().searchDomainDao() }
}