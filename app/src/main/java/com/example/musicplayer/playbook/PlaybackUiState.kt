package com.example.musicplayer.playback

data class PlaybackUiState(
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val title: String = "测试音频",
    val statusText: String = "尚未连接",
    val playButtonText: String = "播放",
    val errorMessage: String? = null,
    val hasMediaItems: Boolean = false,
    val playbackMode: PlaybackMode = PlaybackMode.ORDER
)
data class PlaybackProgress(
    val positionMs: Long = 0L,
    val durationMs: Long = 0L
)

