package com.example.neuro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.neuro.util.UrlUtils

data class FollowingItem(
    val authorId: String,
    val authorName: String,
    val authorAvatar: String,
    val description: String = ""
)

class FollowingAdapter(
    private val items: MutableList<FollowingItem> = mutableListOf(),
    private val onItemClick: (FollowingItem) -> Unit
) : RecyclerView.Adapter<FollowingAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.iv_following_avatar)
        val tvName: TextView = view.findViewById(R.id.tv_following_name)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_following, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvName.text = item.authorName

        if (item.authorAvatar.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(UrlUtils.normalize(item.authorAvatar))
                .placeholder(R.drawable.bg_avatar_placeholder)
                .circleCrop()
                .into(holder.ivAvatar)
        } else {
            holder.ivAvatar.setImageResource(R.drawable.bg_avatar_placeholder)
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<FollowingItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
}
