package com.example.musicplayer.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import java.io.IOException

class MediaStoreScanner(context: Context) {
    private val resolver = context.applicationContext.contentResolver
//获取全局
    fun scanSongs(): List<SongEntity> {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    //访问存储的 uri
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val songs = mutableListOf<SongEntity>()

        val cursor = resolver.query(
            collection, // 1. 查哪里
            projection,      // 2. 查哪些字段
            selection,       // 3. 按什么条件筛选
            null,    // 4. 筛选条件中的参数
            null        // 5. 结果怎么排序
        ) ?: throw IOException("MediaStore 查询未返回结果")
            //读取查询结果的工具
        cursor.use {
            val idColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durationColumn = it.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (it.moveToNext()) {
                val id = it.getLong(idColumn)
                val title = it.getString(titleColumn)
                val artist = it.getString(artistColumn)
                val uri = ContentUris.withAppendedId(collection, id)

                songs.add(
                    SongEntity(
                        contentUri = uri.toString(),
                        title = title?.takeIf { value -> value.isNotBlank() } ?: "未知标题",
                        artist = if (artist.isNullOrBlank() || artist == MediaStore.UNKNOWN_STRING) {
                            "未知艺术家"
                        } else {
                            artist
                        },
                        durationMs = it.getLong(durationColumn)
                    )
                )
            }
        }
        return songs
    }
}
