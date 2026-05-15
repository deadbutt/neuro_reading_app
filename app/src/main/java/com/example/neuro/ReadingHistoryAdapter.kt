package com.example.neuro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.neuro.api.model.ReadingHistoryResponse
import com.example.neuro.util.UrlUtils

class ReadingHistoryAdapter(
    private var list: MutableList<ReadingHistoryResponse>,
    private val onItemClick: (ReadingHistoryResponse) -> Unit,
    private val onDeleteClick: (ReadingHistoryResponse) -> Unit
) : RecyclerView.Adapter<ReadingHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivCover: ImageView = itemView.findViewById(R.id.iv_shelf_cover)
        val tvTitle: TextView = itemView.findViewById(R.id.tv_shelf_book_title)
        val tvAuthor: TextView = itemView.findViewById(R.id.tv_shelf_author)
        val tvChapter: TextView = itemView.findViewById(R.id.tv_shelf_tag)
        val progressBar: ProgressBar = itemView.findViewById(R.id.pb_shelf_progress)
        val tvProgress: TextView = itemView.findViewById(R.id.tv_shelf_progress_text)
        val tvLastReadTime: TextView = itemView.findViewById(R.id.tv_last_read_time)
        val llItem: LinearLayout = itemView.findViewById(R.id.ll_shelf_item)
        val ivDelete: ImageView = itemView.findViewById(R.id.iv_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reading_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.tvTitle.text = item.title
        holder.tvAuthor.text = item.author
        holder.tvChapter.text = item.chapterTitle
        holder.progressBar.progress = item.progress
        holder.tvProgress.text = "${item.progress}%"
        holder.tvLastReadTime.text = item.lastReadTime

        if (!item.cover.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(UrlUtils.normalize(item.cover))
                .placeholder(R.drawable.bg_shelf_cover_placeholder)
                .error(R.drawable.bg_shelf_cover_placeholder)
                .centerCrop()
                .into(holder.ivCover)
        } else {
            holder.ivCover.setImageResource(R.drawable.bg_shelf_cover_placeholder)
        }

        holder.llItem.setOnClickListener { onItemClick(item) }
        holder.ivDelete.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: MutableList<ReadingHistoryResponse>) {
        list = newList
        notifyDataSetChanged()
    }

    fun removeItem(historyId: String) {
        val index = list.indexOfFirst { it.historyId == historyId }
        if (index != -1) {
            list.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun clearAll() {
        val size = list.size
        list.clear()
        notifyItemRangeRemoved(0, size)
    }
}
