package com.example.musicplayer.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert

@Dao
interface SongDao {
    @Query("SELECT * FROM songs ORDER BY title COLLATE NOCASE ASC")
    fun observeSongs(): LiveData<List<SongEntity>>

    @Query("UPDATE songs SET seenInLatestScan = 0")
    suspend fun markAllUnseen()

    @Upsert
    suspend fun upsertSongs(songs: List<SongEntity>)
//更新 room
    @Query("DELETE FROM songs WHERE seenInLatestScan = 0")
    suspend fun deleteUnseen()
//- `markAllUnseen()`：先把表里原有记录标记为“本次尚未看到”。
//- `upsertSongs(songs)`：把刚扫到的歌曲插入或更新；新对象的 `seenInLatestScan` 默认为 `true`。
//- `deleteUnseen()`：删除这次没有查到的旧索引行。
    @Transaction
    suspend fun replaceIndexAfterSuccessfulScan(songs: List<SongEntity>) {
        markAllUnseen()
        upsertSongs(songs)
        deleteUnseen()
    }
}