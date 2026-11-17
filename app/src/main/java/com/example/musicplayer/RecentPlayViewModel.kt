package com.example.musicplayer

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.musicplayer.data.RecentPlayRepository

class RecentPlayViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    val songs = RecentPlayRepository.observeRecentSongs(appContext)

    private val _isClearing = MutableLiveData(false)
    val isClearing: LiveData<Boolean> = _isClearing

    private val _message = MutableLiveData<String?>(null)
    val message: LiveData<String?> = _message

    fun clearHistory() {
        if (_isClearing.value == true) return
        _isClearing.value = true
        RecentPlayRepository.clearHistory(appContext) { success ->
            _isClearing.value = false
            _message.value = if (success) "已清空之前的播放记录" else "清空失败，请重试"
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
