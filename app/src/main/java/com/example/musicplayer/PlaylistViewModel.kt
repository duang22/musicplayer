package com.example.musicplayer

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.PlaylistRepository
import com.example.musicplayer.data.SongEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class PlaylistViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = PlaylistRepository(application)

    val playlists = repository.observePlaylists()

    private val _selectedPlaylistId = MutableLiveData<Long?>(null)
    val selectedPlaylistId: LiveData<Long?> = _selectedPlaylistId

    val playlistSongs: LiveData<List<SongEntity>> = _selectedPlaylistId.switchMap { id ->
        if (id == null) {
            MutableLiveData<List<SongEntity>>(emptyList())
        } else {
            repository.observeSongs(id)
        }
    }

    private val _message = MutableLiveData<String?>(null)
    val message: LiveData<String?> = _message

    fun selectPlaylist(id: Long?) {
        if (_selectedPlaylistId.value != id) _selectedPlaylistId.value = id
    }

    fun createPlaylist(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _message.value = "歌单名称不能为空"
            return
        }
        runWrite("已创建歌单") { repository.createPlaylist(trimmed) }
    }

    fun renamePlaylist(id: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            _message.value = "歌单名称不能为空"
            return
        }
        runWrite("已重命名歌单") { repository.renamePlaylist(id, trimmed) }
    }

    fun deletePlaylist(id: Long) {
        runWrite("已删除歌单") { repository.deletePlaylist(id) }
    }

    fun addSongs(id: Long, songs: List<SongEntity>) {
        if (songs.isEmpty()) return
        runWrite("添加完成，已有歌曲不会重复加入") { repository.addSongs(id, songs) }
    }

    fun removeSong(id: Long, song: SongEntity) {
        runWrite("已从歌单移除歌曲") { repository.removeSong(id, song) }
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun runWrite(successMessage: String, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                _message.value = successMessage
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("PlaylistViewModel", "歌单操作失败", e)
                _message.value = "操作失败，请稍后重试"
            }
        }
    }
}