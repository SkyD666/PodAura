package com.skyd.podaura.model.db.migration

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

class Migration23To24 : Migration(23, 24) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_PlaylistMedia_articleId` ON `PlaylistMedia` (`articleId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_PlaylistMedia_orderPosition` ON `PlaylistMedia` (`orderPosition`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_MediaPlayHistory_articleId` ON `MediaPlayHistory` (`articleId`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_Group_orderPosition` ON `Group` (`orderPosition`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_Feed_mute` ON `Feed` (`mute`)")
        connection.execSQL("CREATE INDEX IF NOT EXISTS `index_Playlist_orderPosition` ON `Playlist` (`orderPosition`)")
    }
}