package com.example.musicplayer.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecentPlayDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun recordPlay(record: RecentPlayEntity)

    @Query("""
        SELECT songs.* FROM songs
        INNER JOIN recent_plays ON songs.contentUri = recent_plays.songUri
        ORDER BY recent_plays.id DESC
    """)
    fun observeRecentSongs(): LiveData<List<SongEntity>>

    @Query("DELETE FROM recent_plays")
    fun clearHistory()
}
