package com.skyd.anivu

import android.content.Context
import android.net.Uri
import androidx.paging.PagingConfig
import androidx.paging.testing.asSnapshot
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.skyd.anivu.config.Const
import com.skyd.anivu.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.anivu.model.bean.article.ArticleBean
import com.skyd.anivu.model.bean.feed.FEED_VIEW_NAME
import com.skyd.anivu.model.bean.feed.FeedBean
import com.skyd.anivu.model.bean.group.GroupVo
import com.skyd.anivu.model.db.AppDatabase
import com.skyd.anivu.model.db.dao.ArticleDao
import com.skyd.anivu.model.db.dao.FeedDao
import com.skyd.anivu.model.db.dao.GroupDao
import com.skyd.anivu.model.db.dao.ReadHistoryDao
import com.skyd.anivu.model.repository.ReadRepository
import com.skyd.anivu.model.repository.RssHelper
import com.skyd.anivu.model.repository.SearchRepository
import com.skyd.anivu.model.repository.article.ArticleRepository
import com.skyd.anivu.model.repository.article.ArticleSort
import com.skyd.anivu.model.repository.feed.FeedRepository
import com.skyd.anivu.model.repository.feed.ReorderGroupRepository
import com.skyd.anivu.model.repository.feed.RequestHeadersRepository
import com.skyd.anivu.util.favicon.FaviconExtractor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import retrofit2.Retrofit
import java.io.IOException
import java.util.UUID


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(AndroidJUnit4::class)
class RssModule {
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
    private val pagingConfig = PagingConfig(pageSize = 20, enablePlaceholders = false)

    private lateinit var db: AppDatabase
    private lateinit var groupDao: GroupDao
    private lateinit var feedDao: FeedDao
    private lateinit var articleDao: ArticleDao
    private lateinit var readHistoryDao: ReadHistoryDao
    private var rssHelper: RssHelper = RssHelper(okHttpClient, faviconExtractor)
    private lateinit var reorderGroupRepository: ReorderGroupRepository
    private lateinit var feedRepository: FeedRepository
    private lateinit var articleRepository: ArticleRepository
    private lateinit var readRepository: ReadRepository
    private lateinit var searchRepository: SearchRepository
    private lateinit var requestHeadersRepository: RequestHeadersRepository


    /**
     * Pass
     * feedRepository.requestGroupAnyList
     */
    @Test
    fun test1() = runTest {
        feedRepository.requestGroupAnyList().first().also {
            assertTrue(it[0] is GroupVo.DefaultGroup)
        }
    }

    /**
     * Pass
     * feedRepository.setFeed
     * feedDao.getFeed
     */
    @Test
    fun test2() = runTest {
        feedRepository.setFeed(
            url = "https://blogs.nvidia.cn/feed/",
            groupId = null,
            nickname = "Nvidia",
        ).first()
        feedDao.getFeed("https://blogs.nvidia.cn/feed/").feed.apply {
            assertTrue(groupId == null && nickname == "Nvidia")
        }
    }

    /**
     * Pass
     * feedRepository.setFeed
     * articleRepository.requestArticleList
     * feedRepository.clearFeedArticles
     */
    @Test
    fun test3() = runTest {
        val url1 = "https://www.ximalaya.com/album/68370676.xml"
        val url2 = "https://blogs.nvidia.cn/feed/"
        feedRepository.setFeed(url = url1, groupId = null, nickname = null).first()
        feedRepository.setFeed(url = url2, groupId = null, nickname = null).first()
        articleRepository.requestArticleList(
            feedUrls = listOf(url1, url2),
            groupIds = emptyList(),
            articleIds = emptyList(),
        ).apply {
            assertTrue(asSnapshot().run {
                firstOrNull { it.feed.url == url1 } != null &&
                        firstOrNull { it.feed.url == url2 } != null
            })
        }
        feedRepository.clearFeedArticles(url = url1).first()
        articleRepository.requestArticleList(
            feedUrls = listOf(url1, url2),
            groupIds = emptyList(),
            articleIds = emptyList(),
        ).apply {
            assertTrue(asSnapshot().run {
                firstOrNull { it.feed.url == url1 } == null &&
                        firstOrNull { it.feed.url == url2 } != null
            })
        }
    }

    /**
     * Pass
     * feedRepository.setFeed
     * feedDao.getFeed
     * feedRepository.createGroup
     * feedRepository.editFeedGroup
     * feedRepository.moveGroupFeedsTo
     * feedRepository.renameGroup
     * feedDao.getFeedsByGroupId
     * groupDao.getGroupById
     * feedRepository.removeFeed
     * feedDao.containsByUrl
     * feedRepository.deleteGroup
     */
    @Test
    fun test4() = runTest {
        val url1 = "https://www.ximalaya.com/album/68370676.xml"
        val url2 = "https://blogs.nvidia.cn/feed/"
        feedRepository.setFeed(url = url1, groupId = null, nickname = null).first()
        feedRepository.setFeed(url = url2, groupId = null, nickname = null).first()

        articleRepository.requestArticleList(
            feedUrls = listOf(url1, url2),
            groupIds = emptyList(),
            articleIds = emptyList(),
        ).apply {
            assertTrue(asSnapshot().run {
                firstOrNull { it.feed.url == url1 } != null &&
                        firstOrNull { it.feed.url == url2 } != null
            })
        }

        val groupId = UUID.randomUUID().toString()
        feedRepository.createGroup(
            GroupVo(groupId = groupId, name = "text", isExpanded = true)
        ).first()

        feedRepository.editFeedGroup(url1, groupId).first()
        assertTrue(feedDao.getFeed(url1).feed.groupId == groupId)

        feedRepository.moveGroupFeedsTo(GroupVo.DEFAULT_GROUP_ID, groupId).first()
        assertTrue(feedDao.getFeedsByGroupId(null).isEmpty())

        feedRepository.renameGroup(groupId, "text2").first()
        assertTrue(groupDao.getGroupById(groupId).name == "text2")

        feedRepository.removeFeed(url1).first()
        assertTrue(feedDao.containsByUrl(url1) == 0)

        feedRepository.deleteGroup(groupId).first()
        assertTrue(feedDao.containsByUrl(url2) == 0)
    }

    /**
     * Pass
     * feedRepository.createGroup
     * groupDao.getGroupById
     * feedRepository.changeGroupExpanded
     */
    @Test
    fun test5() = runTest {
        val groupId = UUID.randomUUID().toString()
        feedRepository.createGroup(
            GroupVo(groupId = groupId, name = "test5", isExpanded = true)
        ).first()
        assertTrue(groupDao.getGroupById(groupId).isExpanded)
        feedRepository.changeGroupExpanded(groupId, false).first()
        assertFalse(groupDao.getGroupById(groupId).isExpanded)
    }

    /**
     * Pass
     * feedRepository.createGroup
     * feedRepository.setFeed
     * articleRepository.requestArticleList
     * feedRepository.readAllInFeed
     * articleRepository.favoriteArticle
     * articleRepository.refreshArticleList
     * feedRepository.clearFeedArticles
     */
    @Test
    fun test6() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        val groupId = UUID.randomUUID().toString()
        feedRepository.createGroup(
            GroupVo(groupId = groupId, name = "test6", isExpanded = true)
        ).first()
        feedRepository.setFeed(url = url1, groupId = null, nickname = null).first()
        articleRepository.requestArticleList(
            feedUrls = listOf(url1),
            groupIds = emptyList(),
            articleIds = emptyList(),
        ).apply {
            assertTrue(asSnapshot().all { !it.articleWithEnclosure.article.isRead })
        }
        feedRepository.readAllInFeed(url1).first()
        articleRepository.requestArticleList(
            feedUrls = listOf(url1),
            groupIds = emptyList(),
            articleIds = emptyList(),
        ).apply {
            assertTrue(asSnapshot().all { it.articleWithEnclosure.article.isRead })
        }

        feedRepository.clearFeedArticles(url1).first()
        articleRepository.refreshArticleList(feedUrls = listOf(url1), full = false).first()
        val article = articleRepository.requestArticleList(
            feedUrls = listOf(url1),
            groupIds = emptyList(),
            articleIds = emptyList(),
        ).asSnapshot().first()
        val articleId = article.articleWithEnclosure.article.articleId
        articleRepository.favoriteArticle(
            articleId = articleId,
            favorite = true,
        ).first()
        assertTrue(
            articleDao.getArticleWithFeed(articleId)
                .first()?.articleWithEnclosure?.article?.isFavorite == true
        )
    }

    /**
     * Pass
     * feedRepository.setFeed
     * feedRepository.clearFeedArticles
     * feedDao.getFeedsByGroupId
     * articleRepository.refreshGroupArticles
     */
    @Test
    fun test7() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        feedRepository.setFeed(url = url1, groupId = null, nickname = null).first()
        feedRepository.clearFeedArticles(url1).first()
        assertTrue(feedDao.getFeedsByGroupId(null).first().articleCount == 0)

        articleRepository.refreshGroupArticles(null, false).first()
        assertTrue(feedDao.getFeedsByGroupId(null).first().articleCount > 0)
    }

    /**
     * Pass
     * feedRepository.setFeed
     * articleRepository.requestArticleList
     * articleRepository.readArticle
     * articleDao.getArticleWithFeed
     */
    @Test
    fun test8() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        feedRepository.setFeed(url = url1, groupId = null, nickname = null).first()
        val article = articleRepository.requestArticleList(
            feedUrls = listOf(url1),
            groupIds = emptyList(),
            articleIds = emptyList(),
        ).asSnapshot().first()
        val articleId = article.articleWithEnclosure.article.articleId
        assertFalse(article.articleWithEnclosure.article.isRead)
        articleRepository.readArticle(articleId, true).first()
        assertTrue(
            articleDao.getArticleWithFeed(articleId)
                .first()?.articleWithEnclosure?.article?.isRead == true
        )
    }

    /**
     * Pass
     * feedRepository.setFeed
     * searchRepository.updateQuery
     * searchRepository.listenSearchFeed
     * articleDao.innerUpdateArticle
     * searchRepository.listenSearchArticle
     */
    @Test
    fun test9() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        feedRepository.setFeed(url = url1, groupId = null, nickname = "Nvidia").first()
        searchRepository.updateQuery("Nvidia")
        assertTrue(searchRepository.listenSearchFeed().asSnapshot().first().feed.url == url1)
        val article = articleRepository.requestArticleList(
            feedUrls = listOf(url1),
            groupIds = emptyList(),
            articleIds = emptyList(),
        ).asSnapshot().first()
        val realArticle = article.articleWithEnclosure.article
        articleDao.innerUpsertArticle(realArticle.copy(title = "test9"))
        assertTrue(
            searchRepository.listenSearchArticle(listOf(url1), emptyList(), emptyList())
                .asSnapshot()
                .any { it.articleWithEnclosure.article.articleId == realArticle.articleId }
        )
    }

    /**
     * Pass
     * feedRepository.setFeed
     * articleRepository.requestArticleList
     * readRepository.requestArticleWithFeed
     */
    @Test
    fun test10() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        feedRepository.setFeed(url = url1, groupId = null, nickname = "Nvidia").first()
        val article = articleRepository.requestArticleList(
            feedUrls = listOf(url1),
            groupIds = emptyList(),
            articleIds = emptyList(),
        ).asSnapshot().first()
        assertEquals(
            readRepository.requestArticleWithFeed(article.articleWithEnclosure.article.articleId)
                .first(), article
        )
    }

    /**
     * Pass
     * feedRepository.createGroup
     * reorderGroupRepository.requestGroupList
     * reorderGroupRepository.reorderGroup
     */
    @Test
    fun test11() = runTest {
        val groupId1 = UUID.randomUUID().toString()
        val groupId2 = UUID.randomUUID().toString()
        val groupId3 = UUID.randomUUID().toString()
        feedRepository.createGroup(
            GroupVo(groupId = groupId1, name = "test11-1", isExpanded = true)
        ).first()
        feedRepository.createGroup(
            GroupVo(groupId = groupId2, name = "test11-2", isExpanded = true)
        ).first()
        feedRepository.createGroup(
            GroupVo(groupId = groupId3, name = "test11-3", isExpanded = true)
        ).first()
        var itemsSnapshot = reorderGroupRepository.requestGroupList().asSnapshot { scrollTo(3) }
        assertTrue(itemsSnapshot.run {
            get(0).groupId == groupId1 && get(1).groupId == groupId2 && get(2).groupId == groupId3
        })

        assertTrue(reorderGroupRepository.reorderGroup(fromIndex = 0, toIndex = -1).first() > 0)
        itemsSnapshot = reorderGroupRepository.requestGroupList().asSnapshot { scrollTo(3) }
        assertTrue(itemsSnapshot.run {
            get(0).groupId == groupId1 && get(1).groupId == groupId2 && get(2).groupId == groupId3
        })
    }

    /**
     * Pass
     * feedRepository.setFeed
     * requestHeadersRepository.getFeedHeaders
     * requestHeadersRepository.updateFeedHeaders
     */
    @Test
    fun test12() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        feedRepository.setFeed(url = url1, groupId = null, nickname = null).first()
        assertTrue(requestHeadersRepository.getFeedHeaders(url1).first()?.headers.isNullOrEmpty())

        requestHeadersRepository.updateFeedHeaders(
            url1, FeedBean.RequestHeaders(
                mapOf("Content-Type" to "text/xml; charset=utf-8")
            )
        ).first()
        assertTrue(
            requestHeadersRepository.getFeedHeaders(url1).first()!!.run {
                headers.size == 1 && headers["Content-Type"] == "text/xml; charset=utf-8"
            }
        )
    }

    /**
     * Pass
     * feedRepository.setFeed
     * articleRepository.requestArticleList
     * readRepository.requestArticleWithFeed
     */
    @Test
    fun test13() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        feedRepository.setFeed(url = url1, groupId = null, nickname = "Nvidia").first()
        val article = articleRepository.requestArticleList(
            feedUrls = listOf(url1),
            groupIds = emptyList(),
            articleIds = emptyList(),
        ).asSnapshot().first()
        assertEquals(
            readRepository.requestArticleWithFeed(article.articleWithEnclosure.article.articleId)
                .first(), article
        )
    }

    /**
     * Pass
     * feedRepository.setFeed
     * feedDao.getFeed
     * feedRepository.editFeedCustomDescription
     */
    @Test
    fun test14() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        feedRepository.setFeed(url = url1, groupId = null, nickname = null).first()
        feedRepository.editFeedCustomDescription(url1, "test13").first()
        assertEquals(feedDao.getFeed(url1).feed.customDescription, "test13")
    }

    /**
     * Pass
     * feedRepository.setFeed
     * feedDao.getFeed
     * feedRepository.editFeedNickname
     */
    @Test
    fun test15() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        feedRepository.setFeed(url = url1, groupId = null, nickname = null).first()
        feedRepository.editFeedNickname(url1, "nickname test13").first()
        assertEquals(feedDao.getFeed(url1).feed.nickname, "nickname test13")
    }

    /**
     * Pass
     * feedRepository.setFeed
     * feedDao.getFeed
     * feedRepository.editFeedCustomIcon
     */
    @Test
    fun test16() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        feedRepository.setFeed(url = url1, groupId = null, nickname = null).first()
        val icon =
            "https://www.gstatic.com/devrel-devsite/prod/v3239347c48d1e3c46204782fd038ba187a6753dfa7d7a0d08a574587ae2085f5/android/images/lockup.svg"
        feedRepository.editFeedCustomIcon(url1, Uri.parse(icon)).first()
        assertEquals(feedDao.getFeed(url1).feed.customIcon, icon)
    }

    /**
     * Pass
     * feedRepository.setFeed
     * feedDao.getFeed
     * feedRepository.editFeedSortXmlArticlesOnUpdate
     */
    @Test
    fun test17() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        feedRepository.setFeed(url = url1, groupId = null, nickname = null).first()
        feedRepository.editFeedSortXmlArticlesOnUpdate(url1, true).first()
        assertTrue(feedDao.getFeed(url1).feed.sortXmlArticlesOnUpdate)
        feedRepository.editFeedSortXmlArticlesOnUpdate(url1, false).first()
        assertFalse(feedDao.getFeed(url1).feed.sortXmlArticlesOnUpdate)
    }

    /**
     * Pass
     * feedRepository.setFeed
     * feedRepository.clearGroupArticles
     * feedDao.getFeedsByGroupId
     */
    @Test
    fun test18() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        feedRepository.setFeed(url = url1, groupId = null, nickname = null).first()
        feedRepository.clearGroupArticles(GroupVo.DEFAULT_GROUP_ID).first()
        assertTrue(feedDao.getFeedsByGroupId(null).all { it.articleCount == 0 })
    }

    /**
     * Pass
     * feedRepository.setFeed
     * articleRepository.requestArticleList
     * articleDao.queryArticleByGuid
     */
    @Test
    fun test19() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        feedRepository.setFeed(url = url1, groupId = null, nickname = "Nvidia").first()
        val article = articleRepository.requestArticleList(
            feedUrls = listOf(url1),
            groupIds = emptyList(),
            articleIds = emptyList(),
        ).asSnapshot().first { it.articleWithEnclosure.article.guid != null }
        assertEquals(
            article.articleWithEnclosure.article.articleId,
            articleDao.queryArticleByGuid(
                article.articleWithEnclosure.article.guid!!,
                url1
            )?.articleId
        )
    }

    /**
     * Pass
     * feedRepository.setFeed
     * articleRepository.requestArticleList
     * articleDao.queryArticleByLink
     */
    @Test
    fun test20() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        feedRepository.setFeed(url = url1, groupId = null, nickname = "Nvidia").first()
        val article = articleRepository.requestArticleList(
            feedUrls = listOf(url1),
            groupIds = emptyList(),
            articleIds = emptyList(),
        ).asSnapshot().first { it.articleWithEnclosure.article.link != null }
        assertEquals(
            article.articleWithEnclosure.article.link,
            articleDao.queryArticleByLink(
                article.articleWithEnclosure.article.link!!,
                url1
            )?.link
        )
    }

    /**
     * Pass
     * feedRepository.setFeed
     * articleRepository.requestArticleList
     * articleDao.queryLatestByFeedUrl
     */
    @Test
    fun test21() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        feedRepository.setFeed(url = url1, groupId = null, nickname = "Nvidia").first()
        val article = articleRepository.requestArticleList(
            feedUrls = listOf(url1),
            groupIds = emptyList(),
            articleIds = emptyList(),
        ).asSnapshot().maxBy { it.articleWithEnclosure.article.date ?: 0 }
        assertEquals(
            article.articleWithEnclosure.article.articleId,
            articleDao.queryLatestByFeedUrl(url1)?.articleId
        )
    }

    /**
     * Pass
     * feedRepository.setFeed
     * articleDao.readAllInGroup
     */
    @Test
    fun test22() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        feedRepository.setFeed(url = url1, groupId = null, nickname = "Nvidia").first()
        articleDao.readAllInGroup(null)
        assertTrue(
            articleRepository.requestArticleList(
                feedUrls = feedDao.getFeedsByGroupId(null).map { it.feed.url },
                groupIds = emptyList(),
                articleIds = emptyList(),
            ).asSnapshot().all { it.articleWithEnclosure.article.isRead })
    }

    /**
     * Pass
     * feedRepository.createGroup
     * reorderGroupRepository.requestGroupList
     * reorderGroupRepository.reorderGroup
     */
    @Test
    fun test23() = runTest {
        val groupId1 = UUID.randomUUID().toString()
        val groupId2 = UUID.randomUUID().toString()
        val groupId3 = UUID.randomUUID().toString()
        feedRepository.createGroup(
            GroupVo(groupId = groupId1, name = "test23-1", isExpanded = true)
        ).first()
        feedRepository.createGroup(
            GroupVo(groupId = groupId2, name = "test23-2", isExpanded = true)
        ).first()
        feedRepository.createGroup(
            GroupVo(groupId = groupId3, name = "test23-3", isExpanded = true)
        ).first()
        var itemsSnapshot = reorderGroupRepository.requestGroupList().asSnapshot { scrollTo(3) }
        assertTrue(itemsSnapshot.run {
            get(0).groupId == groupId1 && get(1).groupId == groupId2 && get(2).groupId == groupId3
        })
        assertTrue(reorderGroupRepository.reorderGroup(fromIndex = 2, toIndex = 1).first() > 0)
        itemsSnapshot = reorderGroupRepository.requestGroupList().asSnapshot { scrollTo(3) }
        assertTrue(itemsSnapshot.run {
            get(0).groupId == groupId1 && get(1).groupId == groupId3 && get(2).groupId == groupId2
        })
    }

    /**
     * Pass
     * feedRepository.createGroup
     * reorderGroupRepository.requestGroupList
     * reorderGroupRepository.reorderGroup
     */
    @Test
    fun test24() = runTest {
        val groupId1 = UUID.randomUUID().toString()
        val groupId2 = UUID.randomUUID().toString()
        val groupId3 = UUID.randomUUID().toString()
        feedRepository.createGroup(
            GroupVo(groupId = groupId1, name = "test24-1", isExpanded = true)
        ).first()
        feedRepository.createGroup(
            GroupVo(groupId = groupId2, name = "test24-2", isExpanded = true)
        ).first()
        feedRepository.createGroup(
            GroupVo(groupId = groupId3, name = "test24-3", isExpanded = true)
        ).first()
        var itemsSnapshot = reorderGroupRepository.requestGroupList().asSnapshot { scrollTo(3) }
        assertTrue(itemsSnapshot.run {
            get(0).groupId == groupId1 && get(1).groupId == groupId2 && get(2).groupId == groupId3
        })

        assertEquals(reorderGroupRepository.reorderGroup(fromIndex = 0, toIndex = 0).first(), 0)
    }

    /**
     * Pass
     * feedRepository.createGroup
     * reorderGroupRepository.requestGroupList
     * reorderGroupRepository.reorderGroup
     */
    @Test
    fun test25() = runTest {
        val groupId1 = UUID.randomUUID().toString()
        val groupId2 = UUID.randomUUID().toString()
        val groupId3 = UUID.randomUUID().toString()
        feedRepository.createGroup(
            GroupVo(groupId = groupId1, name = "test25-1", isExpanded = true)
        ).first()
        feedRepository.createGroup(
            GroupVo(groupId = groupId2, name = "test25-2", isExpanded = true)
        ).first()
        feedRepository.createGroup(
            GroupVo(groupId = groupId3, name = "test25-3", isExpanded = true)
        ).first()
        var itemsSnapshot = reorderGroupRepository.requestGroupList().asSnapshot { scrollTo(3) }
        assertTrue(itemsSnapshot.run {
            get(0).groupId == groupId1 && get(1).groupId == groupId2 && get(2).groupId == groupId3
        })

        assertTrue(reorderGroupRepository.reorderGroup(fromIndex = 0, toIndex = 1).first() > 0)
        itemsSnapshot = reorderGroupRepository.requestGroupList().asSnapshot { scrollTo(3) }
        assertTrue(itemsSnapshot.run {
            get(0).groupId == groupId2 && get(1).groupId == groupId1 && get(2).groupId == groupId3
        })
    }

    /**
     * Pass
     * feedRepository.setFeed
     * searchRepository.updateQuery
     * articleDao.getArticleList
     * searchRepository.listenSearchArticle
     */
    @Test
    fun test26() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        feedRepository.setFeed(url = url1, groupId = null, nickname = "Nvidia").first()
        searchRepository.updateQuery("")
        val excepted =
            articleDao.getArticleList(SimpleSQLiteQuery("SELECT * FROM $ARTICLE_TABLE_NAME"))
        val itemsSnapshot =
            searchRepository.listenSearchArticle(feedDao.getAllFeedUrl(), emptyList(), emptyList())
                .asSnapshot { scrollTo(excepted.size) }
        assertEquals(excepted, itemsSnapshot)
    }

    /**
     * Pass
     * feedRepository.setFeed
     * searchRepository.updateQuery
     * feedDao.getFeedList
     * searchRepository.listenSearchFeed
     */
    @Test
    fun test27() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        val url2 = "https://www.ximalaya.com/album/68370676.xml"
        feedRepository.setFeed(url = url1, groupId = null, nickname = "Nvidia").first()
        feedRepository.setFeed(url = url2, groupId = null, nickname = null).first()
        searchRepository.updateQuery("")
        val excepted = feedDao.getFeedList(SimpleSQLiteQuery("SELECT * FROM $FEED_VIEW_NAME"))
        val itemsSnapshot = searchRepository.listenSearchFeed().asSnapshot {
            scrollTo(excepted.size)
        }
        assertEquals(excepted, itemsSnapshot)
    }

    /**
     * Pass
     * feedRepository.setFeed
     * articleRepository.updateSort
     * articleDao.getArticleList
     * articleRepository.requestArticleList
     */
    @Test
    fun test28() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        feedRepository.setFeed(url = url1, groupId = null, nickname = "Nvidia").first()
        articleRepository.updateSort(ArticleSort.Date(asc = true))
        val excepted =
            articleDao.getArticleList(SimpleSQLiteQuery("SELECT * FROM $ARTICLE_TABLE_NAME"))
                .sortedBy { it.articleWithEnclosure.article.date }
        val itemsSnapshot =
            articleRepository.requestArticleList(listOf(url1), emptyList(), emptyList())
                .asSnapshot { scrollTo(excepted.size) }
        assertEquals(excepted, itemsSnapshot)
    }

    /**
     * Pass
     * feedRepository.setFeed
     * articleRepository.updateSort
     * articleDao.getArticleList
     * articleRepository.requestArticleList
     */
    @Test
    fun test29() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        feedRepository.setFeed(url = url1, groupId = null, nickname = "Nvidia").first()
        articleRepository.updateSort(ArticleSort.Title(asc = false))
        val excepted =
            articleDao.getArticleList(SimpleSQLiteQuery("SELECT * FROM $ARTICLE_TABLE_NAME"))
                .sortedByDescending { it.articleWithEnclosure.article.title }
        val itemsSnapshot =
            articleRepository.requestArticleList(listOf(url1), emptyList(), emptyList())
                .asSnapshot { scrollTo(excepted.size) }
        assertEquals(excepted, itemsSnapshot)
    }

    /**
     * Pass
     * feedRepository.setFeed
     * articleDao.readAllInFeed
     * articleRepository.filterRead
     * articleDao.getArticleList
     * articleRepository.requestArticleList
     */
    @Test
    fun test30() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        val url2 = "https://www.ximalaya.com/album/68370676.xml"
        feedRepository.setFeed(url = url1, groupId = null, nickname = "Nvidia").first()
        feedRepository.setFeed(url = url2, groupId = null, nickname = null).first()
        articleDao.readAllInFeed(url1)
        articleRepository.filterRead(true)
        val excepted =
            articleDao.getArticleList(SimpleSQLiteQuery("SELECT * FROM $ARTICLE_TABLE_NAME WHERE ${ArticleBean.FEED_URL_COLUMN} = \"$url1\""))
        val itemsSnapshot =
            articleRepository.requestArticleList(listOf(url1, url2), emptyList(), emptyList())
                .asSnapshot { scrollTo(excepted.size) }
        assertEquals(excepted, itemsSnapshot)
    }

    /**
     * Pass
     * feedRepository.setFeed
     * articleDao.favoriteArticle
     * articleDao.getArticleWithFeed
     * articleRepository.filterFavorite
     * articleRepository.updateSort
     * articleRepository.requestArticleList
     */
    @Test
    fun test31() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        feedRepository.setFeed(url = url1, groupId = null, nickname = "Nvidia").first()
        val (article1, article2) = articleRepository.requestArticleList(
            feedUrls = listOf(url1),
            groupIds = emptyList(),
            articleIds = emptyList(),
        ).asSnapshot { scrollTo(1) }
        articleDao.favoriteArticle(article1.articleWithEnclosure.article.articleId, true)
        articleDao.favoriteArticle(article2.articleWithEnclosure.article.articleId, true)
        val newArticle1 =
            articleDao.getArticleWithFeed(article1.articleWithEnclosure.article.articleId).first()!!
        val newArticle2 =
            articleDao.getArticleWithFeed(article2.articleWithEnclosure.article.articleId).first()!!
        articleRepository.filterFavorite(true)
        articleRepository.updateSort(ArticleSort.Date(asc = false))
        val excepted = listOf(newArticle1, newArticle2)
            .sortedByDescending { it.articleWithEnclosure.article.date }
        val itemsSnapshot =
            articleRepository.requestArticleList(listOf(url1), emptyList(), emptyList())
                .asSnapshot { scrollTo(excepted.size) }
        assertEquals(excepted, itemsSnapshot)
    }

    /**
     * Pass
     * feedRepository.setFeed
     * articleDao.readAllInFeed
     * articleRepository.updateSort
     * articleRepository.filterRead
     * articleRepository.requestArticleList
     */
    @Test
    fun test32() = runTest {
        val url1 = "https://blogs.nvidia.cn/feed/"
        feedRepository.setFeed(url = url1, groupId = null, nickname = "Nvidia").first()
        articleDao.readAllInFeed(url1)
        articleRepository.updateSort(ArticleSort.Title(asc = false))
        articleRepository.filterRead(false)
        assertTrue(
            articleRepository.requestArticleList(listOf(url1), emptyList(), emptyList())
                .asSnapshot().isEmpty()
        )
    }

    @Before
    fun init() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = AppDatabase.getInstance(context)
        db.clearAllTables()

        groupDao = db.groupDao()
        feedDao = db.feedDao()
        articleDao = db.articleDao()
        readHistoryDao = db.readHistoryDao()

        reorderGroupRepository = ReorderGroupRepository(groupDao, pagingConfig)
        feedRepository = FeedRepository(groupDao, feedDao, articleDao, rssHelper, pagingConfig)
        articleRepository = ArticleRepository(feedDao, articleDao, rssHelper, pagingConfig)
        searchRepository = SearchRepository(feedDao, articleDao, articleRepository, pagingConfig)
        readRepository = ReadRepository(articleDao, readHistoryDao)
        requestHeadersRepository = RequestHeadersRepository(feedDao)
    }

    @After
    @Throws(IOException::class)
    fun destroy() {
//        db.close()
    }
}