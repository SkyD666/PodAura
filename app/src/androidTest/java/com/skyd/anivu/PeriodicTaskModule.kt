package com.skyd.anivu

import android.content.Context
import android.util.Log
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.await
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.skyd.anivu.config.Const
import com.skyd.anivu.ext.dataStore
import com.skyd.anivu.ext.getOrDefault
import com.skyd.anivu.ext.put
import com.skyd.anivu.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.anivu.model.bean.article.ArticleBean
import com.skyd.anivu.model.db.AppDatabase
import com.skyd.anivu.model.db.dao.ArticleDao
import com.skyd.anivu.model.db.dao.FeedDao
import com.skyd.anivu.model.db.dao.GroupDao
import com.skyd.anivu.model.preference.data.delete.autodelete.AutoDeleteArticleBeforePreference
import com.skyd.anivu.model.preference.data.delete.autodelete.AutoDeleteArticleFrequencyPreference
import com.skyd.anivu.model.preference.data.delete.autodelete.AutoDeleteArticleKeepFavoritePreference
import com.skyd.anivu.model.preference.data.delete.autodelete.AutoDeleteArticleKeepUnreadPreference
import com.skyd.anivu.model.preference.data.delete.autodelete.UseAutoDeletePreference
import com.skyd.anivu.model.preference.rss.RssSyncFrequencyPreference
import com.skyd.anivu.model.repository.RssHelper
import com.skyd.anivu.model.repository.feed.FeedRepository
import com.skyd.anivu.model.repository.feed.ReorderGroupRepository
import com.skyd.anivu.model.worker.deletearticle.DeleteArticleWorker
import com.skyd.anivu.model.worker.deletearticle.listenerDeleteArticleFrequency
import com.skyd.anivu.model.worker.rsssync.RssSyncWorker
import com.skyd.anivu.model.worker.rsssync.listenerRssSyncConfig
import com.skyd.anivu.util.favicon.FaviconExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import retrofit2.Retrofit
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class PeriodicTaskModule {
    private lateinit var workManager: WorkManager

    private val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit = Retrofit
        .Builder()
        .baseUrl(Const.BASE_URL)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .client(okHttpClient)
        .build()

    private val faviconExtractor = FaviconExtractor(retrofit)


    private lateinit var context: Context

    private lateinit var db: AppDatabase
    private lateinit var groupDao: GroupDao
    private lateinit var feedDao: FeedDao
    private lateinit var articleDao: ArticleDao
    private var rssHelper: RssHelper = RssHelper(okHttpClient, faviconExtractor)
    private lateinit var reorderGroupRepository: ReorderGroupRepository
    private lateinit var feedRepository: FeedRepository

    /**
     * Pass
     * RssSyncWorker.doWork
     */
    @Test
    fun test1() = runTest {
        val url1 = "https://www.ximalaya.com/album/68370676.xml"
        feedRepository.setFeed(url = url1, groupId = null, nickname = null).first()
        feedRepository.clearFeedArticles(url1).first()
        val sql =
            SimpleSQLiteQuery("SELECT * FROM $ARTICLE_TABLE_NAME WHERE ${ArticleBean.FEED_URL_COLUMN} = \"$url1\"")
        assertTrue(articleDao.getArticleList(sql).isEmpty())

        val worker = TestListenableWorkerBuilder<RssSyncWorker>(context).build()
        runBlocking {
            worker.doWork()
            assertTrue(articleDao.getArticleList(sql).isNotEmpty())
        }
    }

    /**
     * Pass
     * DeleteArticleWorker.doWork
     */
    @Test
    fun test2() = runTest {
        db.clearAllTables()
        val url1 = "https://www.ximalaya.com/album/68370676.xml"
        feedRepository.setFeed(url = url1, groupId = null, nickname = null).first()
        val sql =
            SimpleSQLiteQuery("SELECT * FROM $ARTICLE_TABLE_NAME WHERE ${ArticleBean.FEED_URL_COLUMN} = \"$url1\"")
        val size = articleDao.getArticleList(sql).size
        assertTrue(size > 0)
        context.dataStore.apply {
            put(AutoDeleteArticleBeforePreference.key, 0)
            put(AutoDeleteArticleKeepUnreadPreference.key, true)
        }
        val worker = TestListenableWorkerBuilder<DeleteArticleWorker>(context).build()
        runBlocking {
            worker.doWork()
            assertTrue(articleDao.getArticleList(sql).size == size)
        }
    }

    /**
     * Pass
     * DeleteArticleWorker.doWork
     */
    @Test
    fun test3() = runTest {
        val url1 = "https://www.ximalaya.com/album/68370676.xml"
        feedRepository.setFeed(url = url1, groupId = null, nickname = null).first()
        val sql =
            SimpleSQLiteQuery("SELECT * FROM $ARTICLE_TABLE_NAME WHERE ${ArticleBean.FEED_URL_COLUMN} = \"$url1\"")
        assertTrue(articleDao.getArticleList(sql).isNotEmpty())
        context.dataStore.apply {
            put(AutoDeleteArticleBeforePreference.key, 0)
            put(AutoDeleteArticleKeepUnreadPreference.key, false)
        }
        val worker = TestListenableWorkerBuilder<DeleteArticleWorker>(context).build()
        runBlocking {
            worker.doWork()
            assertTrue(articleDao.getArticleList(sql).isEmpty())
        }
    }

    /**
     * Pass
     * DeleteArticleWorker.doWork
     */
    @Test
    fun test4() = runTest {
        val url1 = "https://www.ximalaya.com/album/68370676.xml"
        feedRepository.setFeed(url = url1, groupId = null, nickname = null).first()
        val sql =
            SimpleSQLiteQuery("SELECT * FROM $ARTICLE_TABLE_NAME WHERE ${ArticleBean.FEED_URL_COLUMN} = \"$url1\"")
        val size = articleDao.getArticleList(sql).size
        assertTrue(size > 0)
        context.dataStore.apply {
            put(AutoDeleteArticleBeforePreference.key, 1.days.inWholeMilliseconds)
        }
        val worker = TestListenableWorkerBuilder<DeleteArticleWorker>(context).build()
        runBlocking {
            worker.doWork()
            assertTrue(articleDao.getArticleList(sql).size == size)
        }
    }

    /**
     * Pass
     * DeleteArticleWorker.doWork
     */
    @Test
    fun test5() = runTest {
        val url1 = "https://www.ximalaya.com/album/68370676.xml"
        feedRepository.setFeed(url = url1, groupId = null, nickname = null).first()
        val sql =
            SimpleSQLiteQuery("SELECT * FROM $ARTICLE_TABLE_NAME WHERE ${ArticleBean.FEED_URL_COLUMN} = \"$url1\"")
        val articleList = articleDao.getArticleList(sql)
        val firstId = articleList.first().articleWithEnclosure.article.articleId
        val size = articleList.size
        assertTrue(size > 0)
        articleDao.favoriteArticle(firstId, true)
        context.dataStore.apply {
            put(AutoDeleteArticleBeforePreference.key, 0)
            put(AutoDeleteArticleKeepUnreadPreference.key, false)
            put(AutoDeleteArticleKeepFavoritePreference.key, true)
        }
        val worker = TestListenableWorkerBuilder<DeleteArticleWorker>(context).build()
        runBlocking {
            worker.doWork()
            val list = articleDao.getArticleList(sql)
            assertTrue(
                list.first().articleWithEnclosure.article.articleId == firstId && list.size == 1
            )
        }
    }

    /**
     * Pass
     * RssSyncWorker constraints
     */
    @Test
    fun test6() = runTest {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresCharging(false)
            .setRequiresBatteryNotLow(true)
            .build()

        val request = OneTimeWorkRequestBuilder<RssSyncWorker>()
            .setConstraints(constraints)
            .build()

        val testDriver = WorkManagerTestInitHelper.getTestDriver(context)
        workManager.enqueue(request).apply {
            result.get()
            testDriver!!.setAllConstraintsMet(request.id)
        }

        val workInfo = workManager.getWorkInfoById(request.id).get()
        assertTrue(workInfo!!.state == WorkInfo.State.SUCCEEDED || workInfo.state == WorkInfo.State.RUNNING)
    }

    /**
     * Pass
     * RssSyncWorker PeriodicWork
     */
    @Test
    fun test7() = runTest {
        val request = PeriodicWorkRequestBuilder<RssSyncWorker>(
            context.dataStore.getOrDefault(RssSyncFrequencyPreference),
            TimeUnit.MILLISECONDS
        ).build()

        val testDriver = WorkManagerTestInitHelper.getTestDriver(context)
        workManager.enqueue(request).apply {
            testDriver!!.setPeriodDelayMet(request.id)
            await()
        }

        val workInfo = workManager.getWorkInfoById(request.id).get()
        assertNotEquals(workInfo!!.state, WorkInfo.State.SUCCEEDED)
    }

    /**
     * Pass
     * DeleteArticleWorker PeriodicWork
     */
    @Test
    fun test8() = runTest {
        val request = PeriodicWorkRequestBuilder<DeleteArticleWorker>(
            context.dataStore.getOrDefault(AutoDeleteArticleFrequencyPreference),
            TimeUnit.MILLISECONDS
        ).build()

        val testDriver = WorkManagerTestInitHelper.getTestDriver(context)
        workManager.enqueue(request).apply {
            testDriver!!.setPeriodDelayMet(request.id)
            await()
        }

        val workInfo = workManager.getWorkInfoById(request.id).get()
        assertNotEquals(workInfo!!.state, WorkInfo.State.SUCCEEDED)
    }

    /**
     * Pass
     * listenerRssSyncConfig
     * RssSyncFrequencyPreference.MANUAL
     */
    @Test
    fun test9() = runTest {
        withContext(Dispatchers.Default) {
            listenerRssSyncConfig(context)
            context.dataStore.put(RssSyncFrequencyPreference.key, RssSyncFrequencyPreference.MANUAL)
            delay(2000)

            assertTrue(
                workManager.getWorkInfosForUniqueWorkFlow(RssSyncWorker.UNIQUE_WORK_NAME)
                    .first().run { isEmpty() || first().state == WorkInfo.State.CANCELLED }
            )
        }
    }

    /**
     * Pass
     * listenerRssSyncConfig
     * RssSyncFrequencyPreference.EVERY_15_MINUTE
     */
    @Test
    fun test10() = runTest {
        withContext(Dispatchers.Default) {
            listenerRssSyncConfig(context)
            context.dataStore.put(
                RssSyncFrequencyPreference.key,
                RssSyncFrequencyPreference.EVERY_15_MINUTE,
            )
            delay(2000)

            assertFalse(
                workManager.getWorkInfosForUniqueWorkFlow(RssSyncWorker.UNIQUE_WORK_NAME)
                    .first().first().state.isFinished
            )
        }
    }

    /**
     * Pass
     * listenerDeleteArticleFrequency
     * UseAutoDeletePreference
     */
    @Test
    fun test11() = runTest {
        withContext(Dispatchers.Default) {
            listenerDeleteArticleFrequency(context)
            context.dataStore.put(UseAutoDeletePreference.key, true)
            delay(2000)

            assertFalse(
                workManager.getWorkInfosForUniqueWorkFlow(DeleteArticleWorker.UNIQUE_WORK_NAME)
                    .first().first().state.isFinished
            )
        }
    }

    /**
     * Pass
     * listenerDeleteArticleFrequency
     * UseAutoDeletePreference
     */
    @Test
    fun test12() = runTest {
        withContext(Dispatchers.Default) {
            listenerDeleteArticleFrequency(context)
            context.dataStore.put(UseAutoDeletePreference.key, false)
            delay(2000)

            assertTrue(
                workManager.getWorkInfosForUniqueWorkFlow(DeleteArticleWorker.UNIQUE_WORK_NAME)
                    .first().run { firstOrNull() == null || first().state == WorkInfo.State.CANCELLED }
            )
        }
    }

    @Before
    fun init() {
        context = ApplicationProvider.getApplicationContext()
        val config = Configuration.Builder()
            .setMinimumLoggingLevel(Log.DEBUG)
            .setExecutor(SynchronousExecutor())
            .build()

        // Initialize WorkManager for instrumentation tests.
        WorkManagerTestInitHelper.initializeTestWorkManager(context, config)
        workManager = WorkManager.getInstance(context)

        db = AppDatabase.getInstance(context)
        db.clearAllTables()

        groupDao = db.groupDao()
        feedDao = db.feedDao()
        articleDao = db.articleDao()
        reorderGroupRepository = ReorderGroupRepository(groupDao)
        feedRepository =
            FeedRepository(groupDao, feedDao, articleDao, reorderGroupRepository, rssHelper)
    }

    @After
    @Throws(IOException::class)
    fun destroy() {
//        db.close()
    }
}