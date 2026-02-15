package com.skyd.podaura.model.repository.calendar

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.insertSeparators
import com.skyd.fundation.ext.hour
import com.skyd.fundation.ext.nextMidnight
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.repository.BaseRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class CalendarRepository(
    private val articleDao: ArticleDao,
    private val pagingConfig: PagingConfig,
) : BaseRepository() {
    fun requestArticlesInOneDay(day: Long): Flow<PagingData<Any>> = Pager(pagingConfig) {
        articleDao.getArticlesIn(
            startTimestamp = day,
            endTimestamp = day.nextMidnight(),
        )
    }.flow.map { pagingData ->
        pagingData.insertSeparators { before, after ->
            val beforeDate = before?.articleWithEnclosure?.article?.date
            val afterDate = after?.articleWithEnclosure?.article?.date
            if (afterDate == null) {
                null
            } else if (beforeDate == null || beforeDate.hour != afterDate.hour) {
                startOfTheHour(afterDate)
            } else {
                null
            }
        }
    }.flowOn(Dispatchers.IO)

    fun requestArticleHoursInOneDay(day: Long): Flow<List<Int>> = articleDao.getArticleHoursIn(
        startTimestamp = day,
        endTimestamp = day.nextMidnight(),
    ).flowOn(Dispatchers.IO)

    private fun startOfTheHour(timestamp: Long): Long {
        val oneHourMillis = 60 * 60 * 1000L
        return timestamp / oneHourMillis * oneHourMillis
    }
}