package com.skyd.anivu.model.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.skyd.anivu.model.bean.ArticleNotificationRuleBean
import com.skyd.anivu.model.bean.article.ArticleBean
import com.skyd.anivu.model.bean.article.ArticleCategoryBean
import com.skyd.anivu.model.bean.article.EnclosureBean
import com.skyd.anivu.model.bean.article.RssMediaBean
import com.skyd.anivu.model.bean.download.bt.BtDownloadInfoBean
import com.skyd.anivu.model.bean.download.bt.DownloadLinkUuidMapBean
import com.skyd.anivu.model.bean.download.bt.SessionParamsBean
import com.skyd.anivu.model.bean.download.bt.TorrentFileBean
import com.skyd.anivu.model.bean.feed.FeedBean
import com.skyd.anivu.model.bean.feed.FeedViewBean
import com.skyd.anivu.model.bean.group.GroupBean
import com.skyd.anivu.model.bean.history.MediaPlayHistoryBean
import com.skyd.anivu.model.bean.history.ReadHistoryBean
import com.skyd.anivu.model.bean.playlist.PlaylistBean
import com.skyd.anivu.model.bean.playlist.PlaylistMediaBean
import com.skyd.anivu.model.bean.playlist.PlaylistViewBean
import com.skyd.anivu.model.db.converter.RequestHeadersConverter
import com.skyd.anivu.model.db.dao.ArticleCategoryDao
import com.skyd.anivu.model.db.dao.ArticleDao
import com.skyd.anivu.model.db.dao.ArticleNotificationRuleDao
import com.skyd.anivu.model.db.dao.DownloadInfoDao
import com.skyd.anivu.model.db.dao.EnclosureDao
import com.skyd.anivu.model.db.dao.FeedDao
import com.skyd.anivu.model.db.dao.GroupDao
import com.skyd.anivu.model.db.dao.MediaPlayHistoryDao
import com.skyd.anivu.model.db.dao.ReadHistoryDao
import com.skyd.anivu.model.db.dao.RssModuleDao
import com.skyd.anivu.model.db.dao.SessionParamsDao
import com.skyd.anivu.model.db.dao.TorrentFileDao
import com.skyd.anivu.model.db.dao.playlist.PlaylistDao
import com.skyd.anivu.model.db.dao.playlist.PlaylistMediaDao
import com.skyd.anivu.model.db.migration.Migration10To11
import com.skyd.anivu.model.db.migration.Migration11To12
import com.skyd.anivu.model.db.migration.Migration12To13
import com.skyd.anivu.model.db.migration.Migration13To14
import com.skyd.anivu.model.db.migration.Migration14To15
import com.skyd.anivu.model.db.migration.Migration15To16
import com.skyd.anivu.model.db.migration.Migration16To17
import com.skyd.anivu.model.db.migration.Migration17To18
import com.skyd.anivu.model.db.migration.Migration18To19
import com.skyd.anivu.model.db.migration.Migration19To20
import com.skyd.anivu.model.db.migration.Migration1To2
import com.skyd.anivu.model.db.migration.Migration20To21
import com.skyd.anivu.model.db.migration.Migration21To22
import com.skyd.anivu.model.db.migration.Migration22To23
import com.skyd.anivu.model.db.migration.Migration2To3
import com.skyd.anivu.model.db.migration.Migration3To4
import com.skyd.anivu.model.db.migration.Migration4To5
import com.skyd.anivu.model.db.migration.Migration5To6
import com.skyd.anivu.model.db.migration.Migration6To7
import com.skyd.anivu.model.db.migration.Migration7To8
import com.skyd.anivu.model.db.migration.Migration8To9
import com.skyd.anivu.model.db.migration.Migration9To10

const val APP_DATA_BASE_FILE_NAME = "app.db"

@Database(
    entities = [
        FeedBean::class,
        ArticleBean::class,
        EnclosureBean::class,
        ArticleCategoryBean::class,
        BtDownloadInfoBean::class,
        DownloadLinkUuidMapBean::class,
        SessionParamsBean::class,
        TorrentFileBean::class,
        GroupBean::class,
        ReadHistoryBean::class,
        MediaPlayHistoryBean::class,
        ArticleNotificationRuleBean::class,
        RssMediaBean::class,
        PlaylistBean::class,
        PlaylistMediaBean::class,
    ],
    views = [FeedViewBean::class, PlaylistViewBean::class],
    version = 23,
)
@TypeConverters(
    value = [RequestHeadersConverter::class]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun groupDao(): GroupDao
    abstract fun feedDao(): FeedDao
    abstract fun articleDao(): ArticleDao
    abstract fun enclosureDao(): EnclosureDao
    abstract fun articleCategoryDao(): ArticleCategoryDao
    abstract fun downloadInfoDao(): DownloadInfoDao
    abstract fun torrentFileDao(): TorrentFileDao
    abstract fun sessionParamsDao(): SessionParamsDao
    abstract fun readHistoryDao(): ReadHistoryDao
    abstract fun mediaPlayHistoryDao(): MediaPlayHistoryDao
    abstract fun rssModuleDao(): RssModuleDao
    abstract fun articleNotificationRuleDao(): ArticleNotificationRuleDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistItemDao(): PlaylistMediaDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        private val migrations = arrayOf(
            Migration1To2(), Migration2To3(), Migration3To4(), Migration4To5(),
            Migration5To6(), Migration6To7(), Migration7To8(), Migration8To9(),
            Migration9To10(), Migration10To11(), Migration11To12(), Migration12To13(),
            Migration13To14(), Migration14To15(), Migration15To16(), Migration16To17(),
            Migration17To18(), Migration18To19(), Migration19To20(), Migration20To21(),
            Migration21To22(), Migration22To23(),
        )

        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    APP_DATA_BASE_FILE_NAME
                )
                    .addMigrations(*migrations)
                    .build()
                    .apply { instance = this }
            }
        }
    }
}
