package com.example.musicplayer.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY playlistId DESC")
    fun observePlaylists(): LiveData<List<PlaylistEntity>>

    @Insert
    suspend fun createPlaylist(playlist: PlaylistEntity): Long

    @Query("UPDATE playlists SET name = :name WHERE playlistId = :playlistId")
    suspend fun renamePlaylist(playlistId: Long, name: String): Int

    @Query("DELETE FROM playlists WHERE playlistId = :playlistId")
    suspend fun deletePlaylist(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongs(entries: List<PlaylistSongCrossRef>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songUri = :songUri")
    suspend fun removeSong(playlistId: Long, songUri: String): Int

    @Query("""
        SELECT songs.* FROM songs
        INNER JOIN playlist_songs AS relation ON songs.contentUri = relation.songUri
        WHERE relation.playlistId = :playlistId
        ORDER BY songs.title COLLATE NOCASE ASC
    """)
    fun observePlaylistSongs(playlistId: Long): LiveData<List<SongEntity>>
}