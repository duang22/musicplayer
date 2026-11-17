package com.example.musicplayer

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.data.AudioPermission
import com.example.musicplayer.data.SongEntity
import com.example.musicplayer.databinding.ActivityRecentPlayBinding

class RecentPlayActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRecentPlayBinding

    private val recentViewModel: RecentPlayViewModel by lazy {
        ViewModelProvider(this)[RecentPlayViewModel::class.java]
    }
    private val playerViewModel: MainViewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }
    private var currentSongs: List<SongEntity> = emptyList()

    private val songAdapter = SongAdapter { song ->
        when {
            !AudioPermission.hasAccess(this) -> toast("请先在主页授予音频权限")
            playerViewModel.playbackState.value?.isConnected != true ->
                toast("播放服务尚未连接，请稍后再试")
            else -> playerViewModel.playSongsFromList(currentSongs, song)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        binding = ActivityRecentPlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val left = binding.root.paddingLeft
        val top = binding.root.paddingTop
        val right = binding.root.paddingRight
        val bottom = binding.root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom)
            insets
        }

        binding.recyclerRecentSongs.layoutManager = LinearLayoutManager(this)
        binding.recyclerRecentSongs.adapter = songAdapter
        binding.buttonClearRecent.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("清空最近播放")
                .setMessage("只清除播放记录，不会删除歌曲、音频文件或歌单。")
                .setPositiveButton("清空") { _, _ -> recentViewModel.clearHistory() }
                .setNegativeButton("取消", null)
                .show()
        }

        recentViewModel.songs.observe(this) { songs ->
            currentSongs = songs
            songAdapter.submitList(songs)
            binding.textRecentEmpty.isVisible = songs.isEmpty()
            updateClearButton()
        }
        recentViewModel.isClearing.observe(this) { updateClearButton() }
        recentViewModel.message.observe(this) { message ->
            if (message != null) {
                toast(message)
                recentViewModel.clearMessage()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        playerViewModel.connect()
    }

    override fun onStop() {
        playerViewModel.disconnect()
        super.onStop()
    }

    private fun updateClearButton() {
        binding.buttonClearRecent.isEnabled = recentViewModel.isClearing.value != true
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
