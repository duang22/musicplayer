package com.example.musicplayer.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "songUri"],
    foreignKeys = [ForeignKey(
        entity = PlaylistEntity::class,
        parentColumns = ["playlistId"],
        childColumns = ["playlistId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["playlistId"])]
)
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songUri: String
)