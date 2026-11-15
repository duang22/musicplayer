package com.example.musicplayer.data

import android.content.Context
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SongRepository(context: Context) {
    private val appContext = context.applicationContext
    private val dao = MusicDatabase.getInstance(appContext).songDao()
    private val scanner = MediaStoreScanner(appContext)

    fun observeSongs(): LiveData<List<SongEntity>> = dao.observeSongs()

    suspend fun scanAndSaveSongs(): Int = withContext(Dispatchers.IO) {
        checkAudioPermission()
        val songs = scanner.scanSongs()
        checkAudioPermission()
        dao.replaceIndexAfterSuccessfulScan(songs)
        songs.size
    }//扫描手机中的音乐，将扫描结果同步到本地数据库，最后返回找到的歌曲数量。

    private fun checkAudioPermission() {
        if (!AudioPermission.hasAccess(appContext)) {
            throw SecurityException("没有读取音频的权限")
        }
    }
}