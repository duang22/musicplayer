package com.example.musicplayer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.example.musicplayer.databinding.ActivityMainBinding
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.data.AudioPermission
import com.example.musicplayer.playback.PlaybackMode
import android.widget.SeekBar
import java.util.Locale
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isUserSeeking = false
    private val viewModel: MainViewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }
    private val audioPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            updateAudioPermissionUi()
            if (granted && AudioPermission.hasAccess(this)) {
                viewModel.scanSongs()
            } else {
                Toast.makeText(this, "未获得音频权限，暂时无法扫描手机歌曲", Toast.LENGTH_SHORT).show()
            }
        }

    private val songAdapter = SongAdapter { song ->
        when {
            !AudioPermission.hasAccess(this) ->
                Toast.makeText(this, "请先授予音频权限", Toast.LENGTH_SHORT).show()
            viewModel.playbackState.value?.isConnected != true ->
                Toast.makeText(this, "播放服务尚未连接，请稍后再试", Toast.LENGTH_SHORT).show()
            else -> viewModel.playSong(song)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or
                        WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }//优化内容显示

        binding.buttonPlayPause.setOnClickListener {
            viewModel.togglePlayback()
        }
        binding.buttonPlayMode.setOnClickListener {
            viewModel.togglePlaybackMode()
        }
        binding.buttonPrevious.setOnClickListener {
            viewModel.skipToPrevious()
        }
        binding.buttonNext.setOnClickListener {
            viewModel.skipToNext()
        }
        binding.buttonReconnect.setOnClickListener {
            viewModel.connect()
        }
        binding.recyclerSongs.layoutManager = LinearLayoutManager(this)
        binding.recyclerSongs.adapter = songAdapter

        binding.buttonScan.setOnClickListener { requestAudioAndScan() }
        binding.buttonPlaylists.setOnClickListener {
            startActivity(Intent(this, PlaylistActivity::class.java))
        }
        binding.buttonAudioSettings.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", packageName, null)
            )//打开设置
            startActivity(intent)
        }

        viewModel.songs.observe(this) { songs ->
            songAdapter.submitList(songs)
            binding.textSongCount.text = getString(R.string.song_count, songs.size)
        }
        viewModel.scanStatus.observe(this) { status ->
            binding.textScanStatus.text = status
        }
        viewModel.isScanning.observe(this) { scanning ->
            binding.buttonScan.isEnabled = !scanning
        }
        viewModel.playbackProgress.observe(this) { progress ->
            val duration = progress.durationMs
            val position = progress.positionMs
            binding.textDuration.text = formatTime(duration)

            if (!isUserSeeking) {
                binding.textCurrentPosition.text = formatTime(position)
                binding.seekBarProgress.progress = if (duration > 0L) {
                    (position * 1000L / duration).toInt().coerceIn(0, 1000)
                } else {
                    0
                }
            }
            binding.seekBarProgress.isEnabled =
                duration > 0L && viewModel.playbackState.value?.isConnected == true
        }//观察播放器的进度数据，并用它更新页面上的时间和进度条。
        viewModel.playbackState.observe(this) { state ->
            binding.textTitle.text = state.title
            binding.textStatus.text = state.statusText
            binding.buttonPrevious.isEnabled = state.isConnected && state.hasMediaItems
            binding.buttonNext.isEnabled = state.isConnected && state.hasMediaItems
            binding.buttonPlayPause.isEnabled = state.isConnected
            binding.buttonPlayPause.text = state.playButtonText

            binding.textError.text = state.errorMessage.orEmpty()
            binding.textError.isVisible = state.errorMessage != null

            binding.buttonReconnect.isVisible =
                !state.isConnecting && !state.isConnected
            binding.buttonPlayMode.isEnabled = state.isConnected
            binding.buttonPlayMode.text = when (state.playbackMode) {
                PlaybackMode.ORDER -> getString(R.string.play_mode_order)
                PlaybackMode.SHUFFLE -> getString(R.string.play_mode_shuffle)
                PlaybackMode.REPEAT_ONE -> getString(R.string.play_mode_repeat_one)
            }
            binding.seekBarProgress.isEnabled =
                state.isConnected && (viewModel.playbackProgress.value?.durationMs ?: 0L) > 0L
        }
        binding.seekBarProgress.setOnSeekBarChangeListener(
            //进度条注册监听器
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (!fromUser) return//由系统引起
                    val duration = viewModel.playbackProgress.value?.durationMs ?: 0L
                    if (duration <= 0L) return
                    val targetPosition = duration * progress / 1000L
                    binding.textCurrentPosition.text = formatTime(targetPosition)
                }//进度条变化

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isUserSeeking = true
                }//开始拖动

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    isUserSeeking = false
                    val bar = seekBar ?: return
                    val duration = viewModel.playbackProgress.value?.durationMs ?: 0L
                    if (duration > 0L) {
                        val targetPosition = duration * bar.progress / 1000L
                        viewModel.seekTo(targetPosition)
                        binding.textCurrentPosition.text = formatTime(targetPosition)
                    }//松手
                }
            }
        )
    }

    override fun onStart() {
        super.onStart()
        viewModel.connect()
        updateAudioPermissionUi()
    }

    override fun onStop() {
        viewModel.disconnect()
        super.onStop()
    }

    private fun requestAudioAndScan() {
        if (AudioPermission.hasAccess(this)) {
            viewModel.scanSongs()
            return
        }

        val permission = AudioPermission.requiredPermission()
        if (shouldShowRequestPermissionRationale(permission)) {
            AlertDialog.Builder(this)
                .setMessage("需要音频权限才能扫描和播放手机中的歌曲。")
                .setPositiveButton("继续") { _, _ -> audioPermissionLauncher.launch(permission) }
                .setNegativeButton("取消", null)
                .show()
            //弹窗
        } else {
            audioPermissionLauncher.launch(permission)//申请权限
        }
    }

    private fun updateAudioPermissionUi() {
        val granted = AudioPermission.hasAccess(this)
        binding.textAudioPermission.text = if (granted) {
            "已获得音频权限，可扫描和播放手机歌曲"
        } else {
            "未获得音频权限；可查看已保存的索引，选择手机歌曲前请先授权"
        }
        binding.buttonAudioSettings.isVisible = !granted
    }
    private fun formatTime(positionMs: Long): String {
        val totalSeconds = positionMs.coerceAtLeast(0L) / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
        }
    }

}
