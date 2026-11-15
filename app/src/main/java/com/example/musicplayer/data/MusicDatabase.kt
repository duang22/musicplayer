package com.example.musicplayer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
@Database(
    entities = [SongEntity::class, PlaylistEntity::class, PlaylistSongCrossRef::class],
    version = 2,
    exportSchema = false
)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    companion object {
        @Volatile
        private var instance: MusicDatabase? = null

        fun getInstance(context: Context): MusicDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "music.db"
                ).addMigrations(MIGRATION_1_2)
                    .build().also { instance = it }
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
