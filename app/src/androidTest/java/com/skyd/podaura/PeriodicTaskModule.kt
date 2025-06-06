package com.skyd.podaura

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.paging.PagingConfig
import androidx.room.RoomRawQuery
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
import com.skyd.podaura.ext.getOrDefault
import com.skyd.podaura.ext.put
import com.skyd.podaura.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.db.AppDatabase
import com.skyd.podaura.model.db.builder
import com.skyd.podaura.model.db.dao.ArticleDao
import com.skyd.podaura.model.db.dao.FeedDao
import com.skyd.podaura.model.db.dao.GroupDao
import com.skyd.podaura.model.db.instance
import com.skyd.podaura.model.preference.createDataStore
import com.skyd.podaura.model.preference.data.delete.autodelete.AutoDeleteArticleBeforePreference
import com.skyd.podaura.model.preference.data.delete.autodelete.AutoDeleteArticleFrequencyPreference
import com.skyd.podaura.model.preference.data.delete.autodelete.AutoDeleteArticleKeepFavoritePreference
import com.skyd.podaura.model.preference.data.delete.autodelete.AutoDeleteArticleKeepUnreadPreference
import com.skyd.podaura.model.preference.data.delete.autodelete.UseAutoDeletePreference
import com.skyd.podaura.model.preference.rss.RssSyncFrequencyPreference
import com.skyd.podaura.model.repository.feed.FeedRepository
import com.skyd.podaura.model.worker.deletearticle.DeleteArticleWorker
import com.skyd.podaura.model.worker.deletearticle.listenDeleteArticleFrequency
import com.skyd.podaura.model.worker.rsssync.RssSyncWorker
import com.skyd.podaura.model.worker.rsssync.listenRssSyncConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
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

//    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
//        .addInterceptor(HttpLoggingInterceptor().apply {
//            level = HttpLoggingInterceptor.Level.BODY
//        })
//        .build()

//    private val retrofit = Retrofit
//        .Builder()
//        .baseUrl(Const.BASE_URL)
//        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
//        .client(okHttpClient)
//        .build()

    //    private val faviconExtractor = FaviconExtractor(retrofit)
    private val pagingConfig = PagingConfig(pageSize = 20, enablePlaceholders = false)

    private lateinit var context: Context
    private lateinit var dataStore: DataStore<Preferences>

    private lateinit var db: AppDatabase
    private lateinit var groupDao: GroupDao
    private lateinit var feedDao: FeedDao
    private lateinit var articleDao: ArticleDao

    //    private var rssHelper: RssHelper = RssHelper(okHttpClient, faviconExtractor)
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
            RoomRawQuery("SELECT * FROM $ARTICLE_TABLE_NAME WHERE ${ArticleBean.FEED_URL_COLUMN} = \"$url1\"")
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
            RoomRawQuery("SELECT * FROM $ARTICLE_TABLE_NAME WHERE ${ArticleBean.FEED_URL_COLUMN} = \"$url1\"")
        val size = articleDao.getArticleList(sql).size
        assertTrue(size > 0)
        dataStore.apply {
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
            RoomRawQuery("SELECT * FROM $ARTICLE_TABLE_NAME WHERE ${ArticleBean.FEED_URL_COLUMN} = \"$url1\"")
        assertTrue(articleDao.getArticleList(sql).isNotEmpty())
        dataStore.apply {
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
            RoomRawQuery("SELECT * FROM $ARTICLE_TABLE_NAME WHERE ${ArticleBean.FEED_URL_COLUMN} = \"$url1\"")
        val size = articleDao.getArticleList(sql).size
        assertTrue(size > 0)
        dataStore.apply {
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
            RoomRawQuery("SELECT * FROM $ARTICLE_TABLE_NAME WHERE ${ArticleBean.FEED_URL_COLUMN} = \"$url1\"")
        val articleList = articleDao.getArticleList(sql)
        val firstId = articleList.first().articleWithEnclosure.article.articleId
        val size = articleList.size
        assertTrue(size > 0)
        articleDao.favoriteArticle(firstId, true)
        dataStore.apply {
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
            dataStore.getOrDefault(RssSyncFrequencyPreference),
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
            dataStore.getOrDefault(AutoDeleteArticleFrequencyPreference),
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
            listenRssSyncConfig(context)
            dataStore.put(RssSyncFrequencyPreference.key, RssSyncFrequencyPreference.MANUAL)
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
            listenRssSyncConfig(context)
            dataStore.put(
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
            listenDeleteArticleFrequency(context)
            dataStore.put(UseAutoDeletePreference.key, true)
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
            listenDeleteArticleFrequency(context)
            dataStore.put(UseAutoDeletePreference.key, false)
            delay(2000)

            assertTrue(
                workManager.getWorkInfosForUniqueWorkFlow(DeleteArticleWorker.UNIQUE_WORK_NAME)
                    .first()
                    .run { firstOrNull() == null || first().state == WorkInfo.State.CANCELLED }
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

        dataStore = createDataStore(context)
        db = AppDatabase.instance(AppDatabase.builder())
        db.clearAllTables()

        groupDao = db.groupDao()
        feedDao = db.feedDao()
        articleDao = db.articleDao()
//        feedRepository =
//            FeedRepository(groupDao, feedDao, articleDao, rssHelper, pagingConfig)
    }

    @After
    @Throws(IOException::class)
    fun destroy() {
//        db.close()
    }
}