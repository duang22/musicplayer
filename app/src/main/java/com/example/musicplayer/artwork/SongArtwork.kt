package com.example.musicplayer.artwork

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.Options
import com.bumptech.glide.load.data.DataFetcher
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.model.ModelLoader
import com.bumptech.glide.load.model.ModelLoaderFactory
import com.bumptech.glide.load.model.MultiModelLoaderFactory
import com.bumptech.glide.signature.ObjectKey
import com.example.musicplayer.R
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

// 修改时间变化时使用新的缓存键，避免重新扫描后继续显示旧封面。
data class SongArtwork(val audioUri: String, val dateModifiedSeconds: Long)

const val EXTRA_ARTWORK_MODIFIED = "com.example.musicplayer.ARTWORK_MODIFIED"

object ArtworkLoader {
    private var registered = false

    @Synchronized
    private fun register(context: Context) {
        if (registered) return
        val appContext = context.applicationContext
        Glide.get(appContext).registry.prepend(
            SongArtwork::class.java,
            InputStream::class.java,
            ArtworkModelLoader.Factory(appContext)
        )
        registered = true
    }

    fun load(view: ImageView, artwork: SongArtwork?) {
        register(view.context)
        val requests = Glide.with(view)
        if (artwork == null) {
            requests.clear(view)
            view.setImageResource(R.drawable.ic_song_placeholder)
            return
        }
        requests.load(artwork)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .placeholder(R.drawable.ic_song_placeholder)
            .error(R.drawable.ic_song_placeholder)
            .centerCrop()
            .into(view)
    }

    fun clear(view: ImageView) {
        Glide.with(view).clear(view)
    }

    // 在后台获取适当尺寸的压缩封面，供系统媒体控件使用。
    fun loadForMediaSession(context: Context, artwork: SongArtwork): ByteArray? {
        val appContext = context.applicationContext
        register(appContext)
        val requests = Glide.with(appContext)
        val target = requests.asBitmap()
            .load(artwork)
            .diskCacheStrategy(DiskCacheStrategy.DATA)
            .submit(512, 512)
        return try {
            val output = ByteArrayOutputStream()
            if (target.get().compress(Bitmap.CompressFormat.JPEG, 85, output)) {
                output.toByteArray()
            } else {
                null
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            null
        } catch (_: Exception) {
            null
        } finally {
            requests.clear(target)
        }
    }
}

private class ArtworkModelLoader(private val context: Context) : ModelLoader<SongArtwork, InputStream> {
    override fun handles(model: SongArtwork): Boolean = true

    override fun buildLoadData(
        model: SongArtwork,
        width: Int,
        height: Int,
        options: Options
    ): ModelLoader.LoadData<InputStream> =
        ModelLoader.LoadData(ObjectKey(model), ArtworkFetcher(context, model.audioUri))

    class Factory(private val context: Context) : ModelLoaderFactory<SongArtwork, InputStream> {
        override fun build(multiFactory: MultiModelLoaderFactory): ModelLoader<SongArtwork, InputStream> =
            ArtworkModelLoader(context)

        override fun teardown() = Unit
    }
}

private class ArtworkFetcher(context: Context, audioUri: String) : DataFetcher<InputStream> {
    private val appContext = context.applicationContext
    private val uri = Uri.parse(audioUri)
    @Volatile private var cancelled = false
    private var stream: InputStream? = null

    // Glide 在后台线程调用此方法；提取结果会以歌曲 Uri 和修改时间为键缓存。
    override fun loadData(priority: Priority, callback: DataFetcher.DataCallback<in InputStream>) {
        try {
            val bytes = if (cancelled) null else embeddedArtwork() ?: systemThumbnail()
            if (bytes == null || cancelled) {
                callback.onDataReady(null)
            } else {
                val result = ByteArrayInputStream(bytes)
                stream = result
                callback.onDataReady(result)
            }
        } catch (e: Exception) {
            callback.onLoadFailed(e)
        }
    }

    private fun embeddedArtwork(): ByteArray? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(appContext, uri)
            retriever.embeddedPicture
        } catch (_: Exception) {
            null
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
                // 封面不可用时仍显示默认图片。
            }
        }
    }

    private fun systemThumbnail(): ByteArray? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || cancelled) return null
        return try {
            val bitmap = appContext.contentResolver.loadThumbnail(uri, Size(512, 512), null)
            try {
                val output = ByteArrayOutputStream()
                if (bitmap.compress(Bitmap.CompressFormat.JPEG, 85, output)) output.toByteArray() else null
            } finally {
                bitmap.recycle()
            }
        } catch (_: Exception) {
            null
        }
    }

    override fun cleanup() {
        stream?.close()
        stream = null
    }

    override fun cancel() {
        cancelled = true
    }

    override fun getDataClass(): Class<InputStream> = InputStream::class.java
    override fun getDataSource(): DataSource = DataSource.LOCAL
}
