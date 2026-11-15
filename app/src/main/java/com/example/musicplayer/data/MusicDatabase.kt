package com.example.musicplayer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [SongEntity::class], version = 1, exportSchema = false)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao

    companion object {
        @Volatile
        private var instance: MusicDatabase? = null

        fun getInstance(context: Context): MusicDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    MusicDatabase::class.java,
                    "music.db"
                ).build().also { instance = it }
            }
    }
}