package com.example.musicplayer.playback

enum class PlaybackMode {
    ORDER,
    SHUFFLE,
    REPEAT_ONE;

    fun next(): PlaybackMode = when (this) {
        ORDER -> SHUFFLE
        SHUFFLE -> REPEAT_ONE
        REPEAT_ONE -> ORDER
    }
}