package com.example.musicplayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val contentUri: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val seenInLatestScan: Boolean = true,
    @ColumnInfo(defaultValue = "0") val dateModifiedSeconds: Long = 0L
)
