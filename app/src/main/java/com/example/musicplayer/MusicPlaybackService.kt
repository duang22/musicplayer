
package com.example.musicplayer.playback

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.example.musicplayer.MainActivity
import com.example.musicplayer.data.RecentPlayRepository
import com.example.musicplayer.artwork.ArtworkLoader
import com.example.musicplayer.artwork.EXTRA_ARTWORK_MODIFIED
import com.example.musicplayer.artwork.SongArtwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(UnstableApi::class)
class MusicPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private val artworkScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var artworkJob: Job? = null
    private val artworkListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            artworkJob?.cancel()
            val item = mediaItem ?: return
            val uri = Uri.parse(item.mediaId)
            if (uri.scheme != ContentResolver.SCHEME_CONTENT || uri.authority != MediaStore.AUTHORITY) return
            if (item.mediaMetadata.artworkData != null || item.mediaMetadata.artworkUri != null) return

            val player = mediaSession?.player ?: return
            val index = player.currentMediaItemIndex
            val modified = item.mediaMetadata.extras?.getLong(EXTRA_ARTWORK_MODIFIED, 0L) ?: 0L
            val artwork = SongArtwork(item.mediaId, modified)
            artworkJob = artworkScope.launch {
                val bytes = withContext(Dispatchers.IO) {
                    ArtworkLoader.loadForMediaSession(applicationContext, artwork)
                } ?: return@launch
                if (!isActive) return@launch
                val currentPlayer = mediaSession?.player ?: return@launch
                val currentItem = currentPlayer.currentMediaItem ?: return@launch
                if (currentPlayer.currentMediaItemIndex != index || currentItem.mediaId != item.mediaId ||
                    currentPlayer.mediaMetadata.artworkData != null ||
                    currentPlayer.mediaMetadata.artworkUri != null
                ) return@launch

                val metadata = currentItem.mediaMetadata.buildUpon()
                    .setArtworkData(bytes, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                    .build()
                currentPlayer.replaceMediaItem(index, currentItem.buildUpon().setMediaMetadata(metadata).build())
            }
        }
    }
    //播放器的控制入口
    private var pendingRecentRecord = false
    private val recentListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            if (events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION)) {
                pendingRecentRecord = true
            }
            if (!pendingRecentRecord || !player.isPlaying) return

            pendingRecentRecord = false
            val songUri = player.currentMediaItem?.mediaId ?: return
            val parsed = Uri.parse(songUri)
            if (parsed.scheme != ContentResolver.SCHEME_CONTENT ||
                parsed.authority != MediaStore.AUTHORITY
            ) return

            RecentPlayRepository.recordPlay(applicationContext, songUri)
        }
    }

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()
        //这段声音用来做什么

        val player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
        //负责把音频播放出来
        player.addListener(recentListener)
        player.addListener(artworkListener)

        val openPageIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }//设置 singletop 和cleartop
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openPageIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo
    ): MediaSession? {
        return mediaSession
    }



    override fun onDestroy() {
        artworkScope.cancel()
        mediaSession?.let { session ->
            session.player.removeListener(recentListener)
            session.player.removeListener(artworkListener)
            session.player.release()
            session.release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
