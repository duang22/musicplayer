package com.example.musicplayer.playback

import android.content.ComponentName
import android.content.Context
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import androidx.media3.common.C
import android.os.Handler
import android.os.Looper
@OptIn(UnstableApi::class)
class PlaybackConnection(context: Context) {

    private val appContext = context.applicationContext
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null
    //控制器

    private val _state = MutableLiveData(PlaybackUiState())
    val state: LiveData<PlaybackUiState> = _state

//    private val playerListener = object : Player.Listener {
//        override fun onEvents(player: Player, events: Player.Events) {
//            publishState()
//            //有事件发生时更新状态
//        }
//    }
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                startProgressUpdates()
            } else {
                stopProgressUpdates()//停止更新
            }
        }

        override fun onEvents(player: Player, events: Player.Events) {
            publishState()
            publishProgress()
            //有事件发生时更新状态
        }
    }

    fun connect() {
        //链接控制器与
        if (controllerFuture != null) return

        _state.value = PlaybackUiState(
            isConnecting = true,
            statusText = "正在连接播放服务…"
        )

        val token = SessionToken(
            appContext,
            ComponentName(appContext, MusicPlaybackService::class.java)
        )//类似 intent 这里链接的 service

        val future = MediaController.Builder(appContext, token)
            .setListener(object : MediaController.Listener {
                override fun onDisconnected(controller: MediaController) {
                    if (this@PlaybackConnection.controller !== controller) return

                    releaseController()
                    _state.value = PlaybackUiState(
                        statusText = "播放服务连接已断开",
                        errorMessage = "请点击重新连接",

                    )
                }
            })
            .buildAsync()//异步代码，表示先发起链接，可以继续执行下面的

        controllerFuture = future
        //当前这一次连接任务，链接成功后传给controller

        future.addListener(
            //成功后执行
            {
                if (controllerFuture !== future) return@addListener

                try {
                    val connectedController = future.get()
                    controller = connectedController
                    if (connectedController.repeatMode == Player.REPEAT_MODE_OFF) {
                        connectedController.repeatMode = Player.REPEAT_MODE_ALL
                    }
                    connectedController.addListener(playerListener)
                    //注册监听器回调代码
                    publishState()
                    publishProgress() // 连接成功时先读取一次当前位置和总时长
                    if (connectedController.isPlaying) {
                        startProgressUpdates()
                    }
                } catch (e: CancellationException) {
                    releaseController()
                    _state.value = PlaybackUiState(statusText = "连接已取消")
                } catch (e: ExecutionException) {
                    releaseController()
                    _state.value = PlaybackUiState(
                        statusText = "连接失败",
                        errorMessage = e.cause?.message ?: "请检查服务声明后重新连接"
                    )
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    releaseController()
                    _state.value = PlaybackUiState(
                        statusText = "连接被中断",
                        errorMessage = "请点击重新连接"
                    )
                }
            },
            ContextCompat.getMainExecutor(appContext)
        )
    }

    fun togglePlayback(firstItem: MediaItem) {
        //处理“播放／暂停”按钮
        val current = controller ?: return

        if (current.mediaItemCount == 0) {
            current.setMediaItem(firstItem)
        }//若没有歌曲则放入音频

        val canPause = current.playWhenReady &&
                current.playbackState != Player.STATE_ENDED &&
                current.playerError == null
        //playWhenReady：已经请求播放，准备好就开始播。正在播放时也是 true

        if (canPause) {
            current.pause()
        } else {
            if (current.playbackState == Player.STATE_ENDED) {
                current.seekToDefaultPosition()
                //播完，跳到当前媒体的默认播放位置
            }
            if (current.playbackState == Player.STATE_IDLE) {
                current.prepare()
                //处于空闲状态，就准备音频
            }
            current.play()
            //继续播放
        }
    }

    fun disconnect() {
        releaseController()
        _state.value = PlaybackUiState(statusText = "页面控制已断开")
    }

    private fun releaseController() {
        progressHandler.removeCallbacks(progressRunnable)
        val oldFuture = controllerFuture
        controllerFuture = null
        //把变量清空
        _progress.value = PlaybackProgress()

        controller?.removeListener(playerListener)
        controller = null
        _progress.value = PlaybackProgress()
        if (oldFuture != null) {
            MediaController.releaseFuture(oldFuture)
            //释放链接任务
        }
    }

    private fun publishState() {
        //处理页面该显示什么
        val current = controller ?: return
        val error = current.playerError

        val status = when {
            error != null -> "播放失败"
            current.mediaItemCount == 0 -> "已连接，点击播放"
            current.playbackState == Player.STATE_ENDED -> "播放结束"
            current.playbackState == Player.STATE_BUFFERING -> {
                //是否处于缓冲
                if (current.playWhenReady) "正在缓冲…" else "已暂停"
            }
            current.isPlaying -> "正在播放"
            current.playbackState == Player.STATE_IDLE -> "等待播放"
            current.playWhenReady -> "等待恢复播放，例如等待音频焦点"
            else -> "已暂停"
        }

        val buttonText = when {
            error != null -> "重试播放"
            current.playbackState == Player.STATE_ENDED -> "重新播放"
            current.playWhenReady -> "暂停"
            else -> "播放"
        }

        _state.value = PlaybackUiState(
            isConnected = true,
            title = current.mediaMetadata.title?.toString() ?: "测试音频",
            statusText = status,
            playButtonText = buttonText,
            errorMessage = error?.let { "${it.errorCodeName}：${it.message.orEmpty()}" },
            hasMediaItems = current.mediaItemCount > 0 ,
            playbackMode = readPlaybackMode(current)
        )
    }
    fun playSong(item: MediaItem) {
        val current = controller ?: return // 获取控制器；没有就退出
        current.setMediaItem(item)        // 设置要播放的歌曲
        current.prepare()                 // 准备播放
        current.play()                    // 请求开始播放
    }
    fun playQueue(items: List<MediaItem>, startIndex: Int) {
        val current = controller ?: return
        if (startIndex !in items.indices) return

        current.setMediaItems(items, startIndex, C.TIME_UNSET)
        current.prepare()
        current.play()
    }
    fun skipToNext() {
        val current = controller ?: return
        if (current.mediaItemCount == 0) return

        if (current.hasNextMediaItem()) {
            current.seekToNextMediaItem()
        } else {
            current.seekTo(0, 0L)
        }
        current.play()
    }
    fun skipToPrevious() {
        val current = controller ?: return
        if (current.mediaItemCount == 0) return

        if (current.currentPosition > 3_000L) {
            current.seekTo(0L)
        } else if (current.hasPreviousMediaItem()) {
            current.seekToPreviousMediaItem()
        } else {
            current.seekTo(current.mediaItemCount - 1, 0L)
        }
        current.play()
    }
    // 切换到下一种播放模式，并更新页面状态
    fun togglePlaybackMode() {
        val current = controller ?: return
        val nextMode = readPlaybackMode(current).next()
        applyPlaybackMode(current, nextMode)
        publishState()
    }
    private fun readPlaybackMode(player: Player): PlaybackMode {
        return when {
            player.repeatMode == Player.REPEAT_MODE_ONE -> PlaybackMode.REPEAT_ONE
            player.shuffleModeEnabled -> PlaybackMode.SHUFFLE
            else -> PlaybackMode.ORDER
        }
    }
    private fun applyPlaybackMode(player: Player, mode: PlaybackMode) {
        player.shuffleModeEnabled = mode == PlaybackMode.SHUFFLE
        player.repeatMode = when (mode) {
            PlaybackMode.ORDER, PlaybackMode.SHUFFLE -> Player.REPEAT_MODE_ALL
            PlaybackMode.REPEAT_ONE -> Player.REPEAT_MODE_ONE
        }
    }
    private val _progress = MutableLiveData(PlaybackProgress())
    val progress: LiveData<PlaybackProgress> = _progress

    private val progressHandler = Handler(Looper.getMainLooper())
    private val progressRunnable = object : Runnable {
        override fun run() {
            val current = controller ?: return
            publishProgress()
            if (current.isPlaying) {
                progressHandler.postDelayed(this, 500L)
            }
        }//定时更新播放进度发到主线程消息队列
    }
    fun seekTo(positionMs: Long) {
        val current = controller ?: return
        if (current.mediaItemCount == 0) return

        val duration = current.duration
        if (duration <= 0L || duration == C.TIME_UNSET) return

        current.seekTo(positionMs.coerceIn(0L, duration))
        publishProgress()
    }//跳转到指定位置
    private fun startProgressUpdates() {
        progressHandler.removeCallbacks(progressRunnable)
        progressHandler.post(progressRunnable)
    }//启动进度更新
    private fun stopProgressUpdates() {
        progressHandler.removeCallbacks(progressRunnable)
        publishProgress()
    }
    private fun publishProgress() {
        val current = controller
        if (current == null) {
            _progress.value = PlaybackProgress()
            return
        }

        val duration = current.duration
            .takeIf { it > 0L && it != C.TIME_UNSET }
            ?: 0L
        val position = if (duration > 0L) {
            current.currentPosition.coerceIn(0L, duration)
        } else {
            0L
        }
        _progress.value = PlaybackProgress(
            positionMs = position,
            durationMs = duration
        )
    }//读取播放器当前位置和总时长
}
