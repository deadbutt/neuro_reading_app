package com.example.neuro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.neuro.util.UrlUtils

data class ShelfItem(
    val bookId: String = "",
    val title: String,
    val author: String,
    val tag: String,
    val progress: Int,
    val coverUrl: String = "",
    val lastReadChapter: String = "",
    var isSelected: Boolean = false
)

class BookshelfAdapter(
    private val books: MutableList<ShelfItem>,
    private val isEditMode: Boolean = false,
    private val onItemClick: (ShelfItem, Int) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<BookshelfAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover: ImageView = view.findViewById(R.id.iv_shelf_cover)
        val tvTitle: TextView = view.findViewById(R.id.tv_shelf_book_title)
        val tvAuthor: TextView = view.findViewById(R.id.tv_shelf_author)
        val tvTag: TextView = view.findViewById(R.id.tv_shelf_tag)
        val pbProgress: ProgressBar = view.findViewById(R.id.pb_shelf_progress)
        val tvProgress: TextView = view.findViewById(R.id.tv_shelf_progress_text)
        val ivCheckbox: ImageView = view.findViewById(R.id.iv_shelf_checkbox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bookshelf, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val book = books[position]
        holder.tvTitle.text = book.title
        holder.tvAuthor.text = book.author
        holder.tvTag.text = "· ${book.tag}"
        holder.pbProgress.progress = book.progress
        holder.tvProgress.text = "${book.progress}%"

        // 加载封面
        if (book.coverUrl.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(UrlUtils.normalize(book.coverUrl))
                .placeholder(R.drawable.bg_book_cover_placeholder)
                .into(holder.ivCover)
        } else {
            holder.ivCover.setImageResource(R.drawable.bg_book_cover_placeholder)
        }

        if (isEditMode) {
            holder.ivCheckbox.visibility = View.VISIBLE
            holder.ivCheckbox.setImageResource(
                if (book.isSelected) R.drawable.ic_shelf_checkbox_checked
                else R.drawable.ic_shelf_checkbox_unchecked
            )
            holder.ivCheckbox.setOnClickListener {
                book.isSelected = !book.isSelected
                holder.ivCheckbox.setImageResource(
                    if (book.isSelected) R.drawable.ic_shelf_checkbox_checked
                    else R.drawable.ic_shelf_checkbox_unchecked
                )
            }
        } else {
            holder.ivCheckbox.visibility = View.GONE
        }

        holder.itemView.setOnClickListener { onItemClick(book, position) }
    }

    override fun getItemCount(): Int = books.size

    fun getSelectedBooks(): List<ShelfItem> = books.filter { it.isSelected }

    fun toggleSelectAll(): Boolean {
        val allSelected = books.all { it.isSelected }
        books.forEach { it.isSelected = !allSelected }
        notifyDataSetChanged()
        return !allSelected
    }

    fun updateData(newBooks: List<ShelfItem>) {
        books.clear()
        books.addAll(newBooks)
        notifyDataSetChanged()
    }
}
