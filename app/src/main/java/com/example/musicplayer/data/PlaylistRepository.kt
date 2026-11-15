package com.example.musicplayer.data

import android.content.Context
import androidx.lifecycle.LiveData

class PlaylistRepository(context: Context) {
    private val dao = MusicDatabase.getInstance(context.applicationContext).playlistDao()

    fun observePlaylists(): LiveData<List<PlaylistEntity>> = dao.observePlaylists()

    fun observeSongs(playlistId: Long): LiveData<List<SongEntity>> =
        dao.observePlaylistSongs(playlistId)

    suspend fun createPlaylist(name: String) {
        dao.createPlaylist(PlaylistEntity(name = name))
    }

    suspend fun renamePlaylist(playlistId: Long, name: String) {
        check(dao.renamePlaylist(playlistId, name) > 0) { "歌单不存在" }
    }

    suspend fun deletePlaylist(playlistId: Long) {
        check(dao.deletePlaylist(playlistId) > 0) { "歌单不存在" }
    }

    suspend fun addSongs(playlistId: Long, songs: List<SongEntity>) {
        val entries = songs.map { song ->
            PlaylistSongCrossRef(playlistId = playlistId, songUri = song.contentUri)
        }
        dao.addSongs(entries)
    }

    suspend fun removeSong(playlistId: Long, song: SongEntity) {
        dao.removeSong(playlistId, song.contentUri)
    }
}