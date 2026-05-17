package com.example.neuro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.neuro.api.model.ReadingHistoryResponse
import com.example.neuro.util.UrlUtils

class ReadingHistoryAdapter(
    private val items: MutableList<ReadingHistoryResponse> = mutableListOf(),
    private val onItemClick: (ReadingHistoryResponse) -> Unit = {},
    private val onDeleteClick: (ReadingHistoryResponse) -> Unit = {}
) : RecyclerView.Adapter<ReadingHistoryAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val llItem: LinearLayout = view.findViewById(R.id.ll_shelf_item)
        val ivCover: ImageView = view.findViewById(R.id.iv_shelf_cover)
        val tvTitle: TextView = view.findViewById(R.id.tv_shelf_book_title)
        val tvAuthor: TextView = view.findViewById(R.id.tv_shelf_author)
        val tvTag: TextView = view.findViewById(R.id.tv_shelf_tag)
        val tvLastReadTime: TextView = view.findViewById(R.id.tv_last_read_time)
        val ivDelete: ImageView = view.findViewById(R.id.iv_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_reading_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title
        holder.tvAuthor.text = item.author
        holder.tvTag.text = "· ${item.chapterTitle}"
        holder.tvLastReadTime.text = item.lastReadTime

        if (item.cover.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(UrlUtils.normalize(item.cover))
                .placeholder(R.drawable.bg_book_cover_placeholder)
                .into(holder.ivCover)
        } else {
            holder.ivCover.setImageResource(R.drawable.bg_book_cover_placeholder)
        }

        holder.llItem.setOnClickListener { onItemClick(item) }
        holder.ivDelete.setOnClickListener { onDeleteClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<ReadingHistoryResponse>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun removeItem(historyId: String) {
        val index = items.indexOfFirst { it.historyId == historyId }
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    fun clearAll() {
        val count = items.size
        items.clear()
        notifyItemRangeRemoved(0, count)
    }
}
