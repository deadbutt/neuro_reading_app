package com.example.neuro

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
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
    val chapterIndex: Int = 0,
    val isFinished: Boolean = false,
    val isFavorite: Boolean = false,
    val isSelected: Boolean = false
)

class BookshelfAdapter(
    private var isEditMode: Boolean = false,
    private val onCheckboxClick: ((ShelfItem, Int) -> Unit)? = null,
    private var items: List<ShelfItem> = emptyList(),
    private val onItemClick: (ShelfItem, Int) -> Unit
) : RecyclerView.Adapter<BookshelfAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover: ImageView = view.findViewById(R.id.iv_shelf_cover)
        val tvTitle: TextView = view.findViewById(R.id.tv_shelf_book_title)
        val tvAuthor: TextView = view.findViewById(R.id.tv_shelf_author)
        val tvTag: TextView = view.findViewById(R.id.tv_shelf_tag)
        val ivCheckbox: ImageView = view.findViewById(R.id.iv_shelf_checkbox)

        init {
            itemView.setOnClickListener {
                Log.d("BookshelfAdapter", "itemView clicked, isEditMode=$isEditMode")
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    val book = items[pos]
                    Log.d("BookshelfAdapter", "bookId=${book.bookId}, title=${book.title}")
                    if (!isEditMode && book.bookId.isNotEmpty()) {
                        Log.d("BookshelfAdapter", "calling onItemClick")
                        onItemClick(book, pos)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bookshelf, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val book = items[position]
        holder.tvTitle.text = book.title
        holder.tvAuthor.text = book.author

        if (book.lastReadChapter.isNotEmpty()) {
            holder.tvTag.text = "上次读到: ${book.lastReadChapter}"
        } else {
            holder.tvTag.text = "· ${book.tag}"
        }

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
                onCheckboxClick?.invoke(book, position)
            }
        } else {
            holder.ivCheckbox.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<ShelfItem>, editMode: Boolean) {
        items = newItems
        isEditMode = editMode
        notifyDataSetChanged()
    }
}
