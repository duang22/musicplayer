package com.example.musicplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.example.musicplayer.playback.PlaybackConnection
import com.example.musicplayer.playback.PlaybackUiState
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.musicplayer.data.SongEntity
import com.example.musicplayer.data.SongRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val connection = PlaybackConnection(application)
    private val repository = SongRepository(application)
    val songs: LiveData<List<SongEntity>> = repository.observeSongs()

    private val _isScanning = MutableLiveData(false)
    val isScanning: LiveData<Boolean> = _isScanning

    private val _scanStatus = MutableLiveData("点击扫描手机歌曲")
    val scanStatus: LiveData<String> = _scanStatus
    val playbackState: LiveData<PlaybackUiState> = connection.state

    private val sampleItem = MediaItem.Builder()
        .setMediaId("sample_audio")
        .setUri("android.resource://${application.packageName}/${R.raw.sample_audio}")
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle("测试音频")
                .setArtist("musicplayer")
                .build()
        )
        .build()

    fun connect() {
        connection.connect()
    }

    fun disconnect() {
        connection.disconnect()
    }

    fun togglePlayback() {
        connection.togglePlayback(sampleItem)
    }
    fun scanSongs() {
        if (_isScanning.value == true) return

        viewModelScope.launch {
            _isScanning.value = true
            _scanStatus.value = "正在扫描手机歌曲…"
            try {
                val count = repository.scanAndSaveSongs()
                _scanStatus.value = "扫描完成，共找到 $count 首歌曲"
            } catch (e: CancellationException) {
                throw e
            } catch (e: SecurityException) {
                _scanStatus.value = "没有读取音频的权限，请授权后重试"
            } catch (e: Exception) {
                Log.e("MainViewModel", "扫描歌曲失败", e)
                _scanStatus.value = "扫描失败，已保留上次的歌曲索引，请稍后重试"
            } finally {
                _isScanning.value = false
            }
        }
    }


    fun playSong(song: SongEntity) {
        val currentSongs = songs.value.orEmpty()
        val startIndex = currentSongs.indexOfFirst { it.contentUri == song.contentUri }
        if (startIndex == -1) return

        val items = currentSongs.map { songItem ->
            MediaItem.Builder()
                .setMediaId(songItem.contentUri)
                .setUri(songItem.contentUri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(songItem.title)
                        .setArtist(songItem.artist)
                        .build()
                )
                .build()
        }
        connection.playQueue(items, startIndex)
    }
    fun skipToPrevious() {
        connection.skipToPrevious()
    }
    fun skipToNext() {
        connection.skipToNext()
    }
    fun togglePlaybackMode() {
        connection.togglePlaybackMode()
    }

    override fun onCleared() {
        connection.disconnect()
        super.onCleared()
    }
}