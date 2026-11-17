package com.example.musicplayer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class,
        RecentPlayEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun recentPlayDao(): RecentPlayDao
    companion object {
        @Volatile
        private var instance: MusicDatabase? = null

        fun getInstance(context: Context): MusicDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "music.db"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build().also { instance = it }
            }
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `songs` ADD COLUMN `dateModifiedSeconds` INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `recent_plays` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `songUri` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_recent_plays_songUri`
                    ON `recent_plays` (`songUri`)
                """.trimIndent())
            }
        }
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `playlists` (
                        `playlistId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `playlist_songs` (
                        `playlistId` INTEGER NOT NULL,
                        `songUri` TEXT NOT NULL,
                        PRIMARY KEY(`playlistId`, `songUri`),
                        FOREIGN KEY(`playlistId`) REFERENCES `playlists`(`playlistId`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """.trimIndent())
                db.execSQL("""
                    CREATE INDEX IF NOT EXISTS `index_playlist_songs_playlistId`
                    ON `playlist_songs` (`playlistId`)
                """.trimIndent())
            }
        }
    }
}
