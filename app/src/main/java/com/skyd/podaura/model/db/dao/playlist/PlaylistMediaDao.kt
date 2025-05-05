package com.skyd.podaura.model.db.dao.playlist

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.RoomRawQuery
import androidx.room.Transaction
import com.skyd.podaura.model.bean.article.ARTICLE_TABLE_NAME
import com.skyd.podaura.model.bean.article.ArticleBean
import com.skyd.podaura.model.bean.article.ArticleWithFeed
import com.skyd.podaura.model.bean.playlist.MediaUrlWithArticleIdBean
import com.skyd.podaura.model.bean.playlist.PLAYLIST_MEDIA_TABLE_NAME
import com.skyd.podaura.model.bean.playlist.PlaylistMediaBean
import com.skyd.podaura.model.bean.playlist.PlaylistMediaWithArticleBean
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistMediaDao {
    @Transaction
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistMedia(playlistMediaBean: PlaylistMediaBean)

    @Transaction
    @Query(
        "DELETE FROM $PLAYLIST_MEDIA_TABLE_NAME " +
                "WHERE ${PlaylistMediaBean.PLAYLIST_ID_COLUMN} = :playlistId AND " +
                "${PlaylistMediaBean.URL_COLUMN} = :url"
    )
    suspend fun deletePlaylistMedia(playlistId: String, url: String): Int

    @Transaction
    suspend fun deletePlaylistMedia(
        playlistId: String,
        mediaList: List<MediaUrlWithArticleIdBean>,
    ): Int {
        var count = 0
        mediaList.forEach { media ->
            if (playlistId.isEmpty()) return@forEach
            count += deletePlaylistMedia(playlistId = playlistId, url = media.url)
        }
        return count
    }

    @Transaction
    @Query(
        "UPDATE `$PLAYLIST_MEDIA_TABLE_NAME` " +
                "SET ${PlaylistMediaBean.ORDER_POSITION_COLUMN} = :orderPosition " +
                "WHERE ${PlaylistMediaBean.PLAYLIST_ID_COLUMN} = :playlistId AND " +
                "${PlaylistMediaBean.URL_COLUMN} = :url"
    )
    suspend fun reorderPlaylistMedia(playlistId: String, url: String, orderPosition: Double): Int

    @Transaction
    @Query(
        "UPDATE `$PLAYLIST_MEDIA_TABLE_NAME` " +
                "SET ${PlaylistMediaBean.ARTICLE_ID_COLUMN} = :articleId " +
                "WHERE ${PlaylistMediaBean.PLAYLIST_ID_COLUMN} = :playlistId AND " +
                "${PlaylistMediaBean.URL_COLUMN} = :url"
    )
    suspend fun updatePlaylistMediaArticleId(
        playlistId: String,
        url: String,
        articleId: String?,
    ): Int

    @Transaction
    @Query(
        "SELECT COALESCE(MAX(`${PlaylistMediaBean.ORDER_POSITION_COLUMN}`), 0) " +
                "FROM $PLAYLIST_MEDIA_TABLE_NAME " +
                "WHERE ${PlaylistMediaBean.PLAYLIST_ID_COLUMN} = :playlistId"
    )
    suspend fun getMaxOrder(playlistId: String): Double

    @Transaction
    @Query(
        "SELECT COALESCE(MIN(`${PlaylistMediaBean.ORDER_POSITION_COLUMN}`), 0) " +
                "FROM $PLAYLIST_MEDIA_TABLE_NAME " +
                "WHERE ${PlaylistMediaBean.PLAYLIST_ID_COLUMN} = :playlistId"
    )
    suspend fun getMinOrder(playlistId: String): Double

    @Transaction
    @Query(
        "SELECT `${PlaylistMediaBean.ORDER_POSITION_COLUMN}` FROM $PLAYLIST_MEDIA_TABLE_NAME " +
                "WHERE ${PlaylistMediaBean.PLAYLIST_ID_COLUMN} = :playlistId AND " +
                "${PlaylistMediaBean.URL_COLUMN} = :url"
    )
    suspend fun getOrderPosition(playlistId: String, url: String): Double?

    @Transaction
    @Query(
        "SELECT EXISTS (SELECT 1 FROM $PLAYLIST_MEDIA_TABLE_NAME WHERE " +
                "${PlaylistMediaBean.PLAYLIST_ID_COLUMN} = :playlistId AND " +
                "${PlaylistMediaBean.URL_COLUMN} = :url)"
    )
    suspend fun exists(playlistId: String, url: String): Int

    @Transaction
    @Query(
        "SELECT * FROM $PLAYLIST_MEDIA_TABLE_NAME " +
                "WHERE ${PlaylistMediaBean.PLAYLIST_ID_COLUMN} = :playlistId " +
                "ORDER BY ${PlaylistMediaBean.ORDER_POSITION_COLUMN} " +
                "LIMIT 1 OFFSET :index"
    )
    suspend fun getNth(playlistId: String, index: Int): PlaylistMediaWithArticleBean?

    @Query(
        "SELECT MIN(${PlaylistMediaBean.ORDER_POSITION_COLUMN}) FROM $PLAYLIST_MEDIA_TABLE_NAME " +
                "WHERE ${PlaylistMediaBean.ORDER_POSITION_COLUMN} > (" +
                "SELECT ${PlaylistMediaBean.ORDER_POSITION_COLUMN} " +
                "FROM $PLAYLIST_MEDIA_TABLE_NAME " +
                "WHERE ${PlaylistMediaBean.PLAYLIST_ID_COLUMN} = :playlistId AND " +
                "${PlaylistMediaBean.URL_COLUMN} = :url" +
                ")"
    )
    suspend fun getNextOrderPosition(playlistId: String, url: String): Double?

    @Transaction
    @RawQuery(observedEntities = [PlaylistMediaBean::class])
    fun getPlaylistMediaListPaging(sql: RoomRawQuery): PagingSource<Int, PlaylistMediaWithArticleBean>

    fun getPlaylistMediaListPaging(
        playlistId: String,
        orderByColumnName: String = PlaylistMediaBean.ORDER_POSITION_COLUMN,
        asc: Boolean = true,
    ): PagingSource<Int, PlaylistMediaWithArticleBean> = getPlaylistMediaListPaging(
        RoomRawQuery(
            sql = "SELECT * FROM $PLAYLIST_MEDIA_TABLE_NAME " +
                    "WHERE ${PlaylistMediaBean.PLAYLIST_ID_COLUMN} = ? " +
                    "ORDER BY $orderByColumnName ${if (asc) "ASC" else "DESC"}",
            onBindStatement = { it.bindText(1, playlistId) },
        )
    )

    @Transaction
    @Query(
        "SELECT * FROM $PLAYLIST_MEDIA_TABLE_NAME " +
                "WHERE ${PlaylistMediaBean.PLAYLIST_ID_COLUMN} = :playlistId " +
                "ORDER BY ${PlaylistMediaBean.ORDER_POSITION_COLUMN}"
    )
    suspend fun getPlaylistMediaList(playlistId: String): List<PlaylistMediaWithArticleBean>

    @Transaction
    @Query(
        "SELECT a.* FROM $ARTICLE_TABLE_NAME a " +
                "JOIN (\n" +
                "    SELECT ${PlaylistMediaBean.ARTICLE_ID_COLUMN}, ${PlaylistMediaBean.ORDER_POSITION_COLUMN} " +
                "    FROM $PLAYLIST_MEDIA_TABLE_NAME " +
                "    WHERE ${PlaylistMediaBean.PLAYLIST_ID_COLUMN} = :playlistId " +
                "    AND ${PlaylistMediaBean.ARTICLE_ID_COLUMN} IS NOT NULL " +
                ") p ON a.${ArticleBean.ARTICLE_ID_COLUMN} = p.${PlaylistMediaBean.ARTICLE_ID_COLUMN} " +
                "ORDER BY p.${PlaylistMediaBean.ORDER_POSITION_COLUMN} LIMIT :count"
    )
    suspend fun getPlaylistMediaArticles(playlistId: String, count: Int): List<ArticleWithFeed>

    @Transaction
    @Query(
        "SELECT ${PlaylistMediaBean.URL_COLUMN} " +
                "FROM $PLAYLIST_MEDIA_TABLE_NAME " +
                "WHERE ${PlaylistMediaBean.PLAYLIST_ID_COLUMN} = :playlistId " +
                "ORDER BY ${PlaylistMediaBean.ORDER_POSITION_COLUMN}"
    )
    suspend fun getPlaylistMediaIdList(playlistId: String): List<String>

    @Transaction
    @Query(
        "SELECT ${PlaylistMediaBean.PLAYLIST_ID_COLUMN} FROM $PLAYLIST_MEDIA_TABLE_NAME " +
                "WHERE ${PlaylistMediaBean.URL_COLUMN} IN (:urls) " +
                "GROUP BY ${PlaylistMediaBean.PLAYLIST_ID_COLUMN} " +
                "HAVING COUNT(DISTINCT ${PlaylistMediaBean.URL_COLUMN}) = :urlCount"
    )
    fun getCommonMediaPlaylistIdList(urls: List<String>, urlCount: Int): Flow<List<String>>

    @Transaction
    suspend fun reindexOrders(playlistId: String) {
        getPlaylistMediaIdList(playlistId).forEachIndexed { index, url ->
            reorderPlaylistMedia(
                playlistId = playlistId,
                url = url,
                orderPosition = (index * ORDER_DELTA) + ORDER_DELTA,
            )
        }
    }

    companion object {
        const val ORDER_DELTA = 10.0
        const val ORDER_MIN_DELTA = 0.05
    }
}