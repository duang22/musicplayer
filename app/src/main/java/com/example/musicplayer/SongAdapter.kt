package com.example.musicplayer



import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.musicplayer.data.SongEntity
import com.example.musicplayer.databinding.ItemSongBinding

class SongAdapter(
    private val onSongClick: (SongEntity) -> Unit
) : ListAdapter<SongEntity, SongAdapter.SongViewHolder>(SongDiff) {

    class SongViewHolder(val binding: ItemSongBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder {
        val binding = ItemSongBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return SongViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val song = getItem(position)
        holder.binding.textSongTitle.text = song.title
        holder.binding.textSongArtist.text = song.artist
        holder.itemView.setOnClickListener { onSongClick(song) }
    }

    private object SongDiff : DiffUtil.ItemCallback<SongEntity>() {
        override fun areItemsTheSame(oldItem: SongEntity, newItem: SongEntity): Boolean =
            oldItem.contentUri == newItem.contentUri

        override fun areContentsTheSame(oldItem: SongEntity, newItem: SongEntity): Boolean =
            oldItem == newItem
    }//比较新旧列表减少操作
}