package com.example.neuro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class BookItem(
    val bookId: String = "",
    val title: String,
    val author: String,
    val desc: String,
    val coverResId: Int = 0,
    val coverUrl: String = ""
)

class BookAdapter(
    private val books: List<BookItem>,
    private val onItemClick: (BookItem) -> Unit
) : RecyclerView.Adapter<BookAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover: ImageView = view.findViewById(R.id.iv_book_cover)
        val tvTitle: TextView = view.findViewById(R.id.tv_book_title)
        val tvAuthor: TextView = view.findViewById(R.id.tv_book_author)
        val tvDesc: TextView = view.findViewById(R.id.tv_book_desc)
        val ivAddShelf: ImageView = view.findViewById(R.id.iv_add_shelf)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_book, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val book = books[position]
        holder.tvTitle.text = book.title
        holder.tvAuthor.text = book.author
        holder.tvDesc.text = book.desc
        if (book.coverResId != 0) {
            holder.ivCover.setImageResource(book.coverResId)
        } else if (book.coverUrl.isNotEmpty()) {
            com.bumptech.glide.Glide.with(holder.itemView.context)
                .load(book.coverUrl.replace("0.0.0.0", "47.118.22.220"))
                .placeholder(R.drawable.bg_book_cover_placeholder)
                .into(holder.ivCover)
        }
        holder.itemView.setOnClickListener { onItemClick(book) }
        holder.ivAddShelf.setOnClickListener { onItemClick(book) }
    }

    override fun getItemCount(): Int = books.size
}
