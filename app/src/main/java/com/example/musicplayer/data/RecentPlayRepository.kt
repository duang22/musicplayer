package com.example.musicplayer.data

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.LiveData
import java.util.concurrent.Executors

object RecentPlayRepository {
    private val databaseExecutor = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())

    fun observeRecentSongs(context: Context): LiveData<List<SongEntity>> =
        MusicDatabase.getInstance(context.applicationContext).recentPlayDao().observeRecentSongs()

    fun recordPlay(context: Context, songUri: String) {
        val appContext = context.applicationContext
        databaseExecutor.execute {
            try {
                MusicDatabase.getInstance(appContext).recentPlayDao()
                    .recordPlay(RecentPlayEntity(songUri = songUri))
            } catch (e: Exception) {
                Log.e("RecentPlayRepository", "保存最近播放失败", e)
            }
        }
    }

    fun clearHistory(context: Context, onComplete: (Boolean) -> Unit) {
        val appContext = context.applicationContext
        databaseExecutor.execute {
            val success = try {
                MusicDatabase.getInstance(appContext).recentPlayDao().clearHistory()
                true
            } catch (e: Exception) {
                Log.e("RecentPlayRepository", "清空最近播放失败", e)
                false
            }
            mainHandler.post { onComplete(success) }
        }
    }
}