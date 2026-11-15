package com.example.musicplayer.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val contentUri: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
    val seenInLatestScan: Boolean = true
)
