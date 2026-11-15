package com.example.musicplayer

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.musicplayer.data.AudioPermission
import com.example.musicplayer.data.PlaylistEntity
import com.example.musicplayer.data.SongEntity
import com.example.musicplayer.databinding.ActivityPlaylistBinding

class PlaylistActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPlaylistBinding

    private val playlistViewModel: PlaylistViewModel by lazy {
        ViewModelProvider(this)[PlaylistViewModel::class.java]
    }
    private val playerViewModel: MainViewModel by lazy {
        ViewModelProvider(this)[MainViewModel::class.java]
    }
    private var playlists: List<PlaylistEntity> = emptyList()
    private var availableSongs: List<SongEntity> = emptyList()
    private var currentSongs: List<SongEntity> = emptyList()
    private var shownPlaylistId: Long? = null

    private val playlistNameAdapter by lazy {
        ArrayAdapter<String>(this, android.R.layout.simple_list_item_single_choice, mutableListOf())
    }

    private val songAdapter = SongAdapter(
        onSongLongClick = { song -> confirmRemoveSong(song) }
    ) { song ->
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
        binding = ActivityPlaylistBinding.inflate(layoutInflater)
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

        binding.listPlaylists.choiceMode = ListView.CHOICE_MODE_SINGLE
        binding.listPlaylists.adapter = playlistNameAdapter
        binding.listPlaylists.setOnItemClickListener { _, _, position, _ ->
            playlists.getOrNull(position)?.let { playlistViewModel.selectPlaylist(it.playlistId) }
        }

        binding.recyclerPlaylistSongs.layoutManager = LinearLayoutManager(this)
        binding.recyclerPlaylistSongs.adapter = songAdapter

        binding.buttonCreatePlaylist.setOnClickListener {
            showNameDialog("新建歌单") { name -> playlistViewModel.createPlaylist(name) }
        }
        binding.buttonRenamePlaylist.setOnClickListener {
            val playlist = selectedPlaylist() ?: return@setOnClickListener
            showNameDialog("重命名歌单", playlist.name) { name ->
                playlistViewModel.renamePlaylist(playlist.playlistId, name)
            }
        }
        binding.buttonDeletePlaylist.setOnClickListener {
            val playlist = selectedPlaylist() ?: return@setOnClickListener
            AlertDialog.Builder(this)
                .setTitle("删除歌单")
                .setMessage("确定删除「${playlist.name}」吗？手机中的音频文件不会被删除。")
                .setPositiveButton("删除") { _, _ ->
                    playlistViewModel.deletePlaylist(playlist.playlistId)
                }
                .setNegativeButton("取消", null)
                .show()
        }
        binding.buttonAddSongs.setOnClickListener {
            val playlist = selectedPlaylist() ?: return@setOnClickListener
            showAddSongsDialog(playlist.playlistId)
        }
        playlistViewModel.playlists.observe(this) { items ->
            playlists = items
            playlistNameAdapter.clear()
            playlistNameAdapter.addAll(items.map { it.name })
            val selectedId = playlistViewModel.selectedPlaylistId.value
            if (selectedId != null && items.none { it.playlistId == selectedId }) {
                playlistViewModel.selectPlaylist(null)
            }
            renderSelection()
        }
        playlistViewModel.selectedPlaylistId.observe(this) { id ->
            if (shownPlaylistId != id) {
                shownPlaylistId = id
                currentSongs = emptyList()
                songAdapter.submitList(emptyList())
            }
            renderSelection()
        }
        playlistViewModel.playlistSongs.observe(this) { songs ->
            currentSongs = songs
            songAdapter.submitList(songs)
        }
        playlistViewModel.message.observe(this) { message ->
            if (message != null) {
                toast(message)
                playlistViewModel.clearMessage()
            }
        }
        playerViewModel.songs.observe(this) { songs -> availableSongs = songs }
    }

    override fun onStart() {
        super.onStart()
        playerViewModel.connect()
    }

    override fun onStop() {
        playerViewModel.disconnect()
        super.onStop()
    }

    private fun selectedPlaylist(): PlaylistEntity? {
        val id = playlistViewModel.selectedPlaylistId.value
        return playlists.firstOrNull { it.playlistId == id }
    }

    private fun renderSelection() {
        val selected = selectedPlaylist()
        binding.textSelectedPlaylist.text = if (selected == null) {
            getString(R.string.playlist_select_hint)
        } else {
            getString(R.string.playlist_selected, selected.name)
        }
        binding.buttonRenamePlaylist.isEnabled = selected != null
        binding.buttonDeletePlaylist.isEnabled = selected != null
        binding.buttonAddSongs.isEnabled = selected != null
        binding.listPlaylists.clearChoices()
        val index = playlists.indexOfFirst { it.playlistId == selected?.playlistId }
        if (index >= 0) binding.listPlaylists.setItemChecked(index, true)
    }

    private fun showNameDialog(title: String, initialName: String = "", onConfirm: (String) -> Unit) {
        val input = EditText(this)
        input.hint = "请输入歌单名称"
        input.setText(initialName)
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(input)
            .setPositiveButton("确定") { _, _ -> onConfirm(input.text.toString()) }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddSongsDialog(playlistId: Long) {
        val songs = availableSongs.toList()
        if (songs.isEmpty()) {
            toast("还没有歌曲，请先到主页扫描")
            return
        }
        val labels = songs.map { "${it.title} - ${it.artist}" }.toTypedArray()
        val checked = BooleanArray(songs.size)
        AlertDialog.Builder(this)
            .setTitle("选择歌曲")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton("添加") { _, _ ->
                val selectedSongs = songs.filterIndexed { index, _ -> checked[index] }
                playlistViewModel.addSongs(playlistId, selectedSongs)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun confirmRemoveSong(song: SongEntity) {
        val playlist = selectedPlaylist() ?: return
        if (currentSongs.none { it.contentUri == song.contentUri }) return
        AlertDialog.Builder(this)
            .setTitle("从歌单移除")
            .setMessage("确定从「${playlist.name}」移除「${song.title}」吗？音频文件不会被删除。")
            .setPositiveButton("移除") { _, _ ->
                playlistViewModel.removeSong(playlist.playlistId, song)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
