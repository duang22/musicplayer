package com.example.musicplayer.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recent_plays",
    indices = [Index(value = ["songUri"], unique = true)]
)
data class RecentPlayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val songUri: String
)