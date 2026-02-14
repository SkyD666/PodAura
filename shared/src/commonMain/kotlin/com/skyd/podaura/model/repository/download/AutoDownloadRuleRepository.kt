package com.skyd.podaura.model.repository.download

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.skyd.podaura.model.bean.download.autorule.AutoDownloadRuleBean
import com.skyd.podaura.model.db.dao.download.AutoDownloadRuleDao
import com.skyd.podaura.model.repository.BaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onEach

class AutoDownloadRuleRepository(
    private val autoDownloadRuleDao: AutoDownloadRuleDao,
    private val pagingConfig: PagingConfig,
) : BaseRepository() {
    fun requestAllRules(): Flow<PagingData<AutoDownloadRuleBean>> = Pager(pagingConfig) {
        autoDownloadRuleDao.getAllRules()
    }.flow.flowOn(Dispatchers.IO)

    fun getRule(feedUrl: String): Flow<AutoDownloadRuleBean> = flowOf(Unit).onEach {
        if (autoDownloadRuleDao.getRuleByFeedUrl(feedUrl).first() == null) {
            newRule(feedUrl = feedUrl)
        }
    }.flatMapLatest {
        autoDownloadRuleDao.getRuleByFeedUrl(feedUrl).filterNotNull()
    }.flowOn(Dispatchers.IO)

    private suspend fun newRule(feedUrl: String): AutoDownloadRuleBean {
        val rule = AutoDownloadRuleBean(
            feedUrl = feedUrl,
            requireWifi = true,
            requireBatteryNotLow = true,
            requireCharging = false,
            enabled = false,
            maxDownloadCount = 2,
            filterPattern = null,
        )
        autoDownloadRuleDao.upsert(rule)
        return rule
    }

    fun deleteRule(feedUrl: String): Flow<Int> = flow {
        emit(autoDownloadRuleDao.deleteByFeedUrl(feedUrl = feedUrl))
    }.flowOn(Dispatchers.IO)

    fun enableRule(feedUrl: String, enabled: Boolean): Flow<Int> = flow {
        emit(autoDownloadRuleDao.updateEnabled(feedUrl = feedUrl, enabled = enabled))
    }.flowOn(Dispatchers.IO)

    fun updateRequireWifi(feedUrl: String, requireWifi: Boolean): Flow<Int> = flow {
        emit(autoDownloadRuleDao.updateRequireWifi(feedUrl = feedUrl, requireWifi = requireWifi))
    }.flowOn(Dispatchers.IO)

    fun updateRequireBatteryNotLow(
        feedUrl: String,
        requireBatteryNotLow: Boolean,
    ): Flow<Int> = flow {
        emit(
            autoDownloadRuleDao.updateRequireBatteryNotLow(
                feedUrl = feedUrl,
                requireBatteryNotLow = requireBatteryNotLow,
            )
        )
    }.flowOn(Dispatchers.IO)

    fun updateRequireCharging(feedUrl: String, requireCharging: Boolean): Flow<Int> = flow {
        emit(
            autoDownloadRuleDao.updateRequireCharging(
                feedUrl = feedUrl,
                requireCharging = requireCharging,
            )
        )
    }.flowOn(Dispatchers.IO)

    fun updateRuleMaxDownloadCount(feedUrl: String, maxDownloadCount: Int): Flow<Int> = flow {
        emit(
            autoDownloadRuleDao.updateMaxDownloadCount(
                feedUrl = feedUrl,
                maxDownloadCount = maxDownloadCount.coerceIn(0, 5),
            )
        )
    }.flowOn(Dispatchers.IO)

    fun updateRuleFilterPattern(feedUrl: String, filterPattern: String?): Flow<Int> = flow {
        emit(
            autoDownloadRuleDao.updateFilterPattern(
                feedUrl = feedUrl,
                filterPattern = filterPattern,
            )
        )
    }.flowOn(Dispatchers.IO)
}
