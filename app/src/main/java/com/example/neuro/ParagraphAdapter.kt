package com.example.neuro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

sealed class ReaderItem {
    abstract val chapterId: String

    data class ChapterHeader(
        override val chapterId: String,
        val title: String,
        val chapterNumber: Int
    ) : ReaderItem()

    data class Paragraph(
        override val chapterId: String,
        val text: String,
        val index: Int
    ) : ReaderItem()

    data class Loading(
        override val chapterId: String
    ) : ReaderItem()
}

class ParagraphAdapter(
    private var items: List<ReaderItem> = emptyList()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_PARAGRAPH = 1
        private const val TYPE_LOADING = 2
    }

    val currentItems: List<ReaderItem> get() = items

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_chapter_header_title)
        val tvNumber: TextView = view.findViewById(R.id.tv_chapter_header_number)
    }

    class ParagraphViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvText: TextView = view.findViewById(R.id.tv_paragraph_text)
    }

    class LoadingViewHolder(view: View) : RecyclerView.ViewHolder(view)

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ReaderItem.ChapterHeader -> TYPE_HEADER
            is ReaderItem.Paragraph -> TYPE_PARAGRAPH
            is ReaderItem.Loading -> TYPE_LOADING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_HEADER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_chapter_header, parent, false)
                HeaderViewHolder(view)
            }
            TYPE_LOADING -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_loading, parent, false)
                LoadingViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_paragraph, parent, false)
                ParagraphViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ReaderItem.ChapterHeader -> {
                val h = holder as HeaderViewHolder
                h.tvTitle.text = item.title
                h.tvNumber.text = "第${item.chapterNumber}章"
            }
            is ReaderItem.Paragraph -> {
                val h = holder as ParagraphViewHolder
                h.tvText.text = item.text
            }
            is ReaderItem.Loading -> {
                // 加载中，无需绑定
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<ReaderItem>) {
        items = newItems
        notifyDataSetChanged()
    }

    fun appendItems(newItems: List<ReaderItem>) {
        val startPos = items.size
        items = items + newItems
        notifyItemRangeInserted(startPos, newItems.size)
    }

    fun prependItems(newItems: List<ReaderItem>) {
        items = newItems + items
        notifyItemRangeInserted(0, newItems.size)
    }

    fun removeLoading() {
        val index = items.indexOfLast { it is ReaderItem.Loading }
        if (index >= 0) {
            items = items.toMutableList().apply { removeAt(index) }
            notifyItemRemoved(index)
        }
    }

    fun findChapterPosition(chapterId: String): Int {
        return items.indexOfFirst {
            it is ReaderItem.ChapterHeader && it.chapterId == chapterId
        }
    }
}
