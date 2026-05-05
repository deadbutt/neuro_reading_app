package com.example.neuro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

data class CommentItem(
    val name: String,
    val avatarUrl: String? = null,
    val time: String,
    val content: String,
    val likes: String,
    val isAuthor: Boolean = false
)

class CommentAdapter(
    private val comments: List<CommentItem>,
    private val onLikeClick: (CommentItem, Int) -> Unit = { _, _ -> },
    private val onReplyClick: (CommentItem) -> Unit = {}
) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.iv_comment_avatar)
        val tvName: TextView = view.findViewById(R.id.tv_comment_name)
        val tvAuthorBadge: TextView = view.findViewById(R.id.tv_author_badge)
        val tvTime: TextView = view.findViewById(R.id.tv_comment_time)
        val tvContent: TextView = view.findViewById(R.id.tv_comment_content)
        val ivLike: ImageView = view.findViewById(R.id.iv_comment_like)
        val tvLikes: TextView = view.findViewById(R.id.tv_comment_likes)
        val llLike: View = view.findViewById(R.id.ll_like)
        val llReply: View = view.findViewById(R.id.ll_reply)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = comments[position]
        holder.tvName.text = comment.name
        holder.tvAuthorBadge.visibility = if (comment.isAuthor) View.VISIBLE else View.GONE
        holder.tvTime.text = comment.time
        holder.tvContent.text = comment.content
        holder.tvLikes.text = comment.likes

        // 加载头像
        if (!comment.avatarUrl.isNullOrEmpty()) {
            Glide.with(holder.itemView.context)
                .load(comment.avatarUrl)
                .placeholder(R.drawable.bg_avatar_placeholder)
                .error(R.drawable.bg_avatar_placeholder)
                .circleCrop()
                .into(holder.ivAvatar)
        } else {
            holder.ivAvatar.setImageResource(R.drawable.bg_avatar_placeholder)
        }

        holder.llLike.setOnClickListener { onLikeClick(comment, position) }
        holder.llReply.setOnClickListener { onReplyClick(comment) }
    }

    override fun getItemCount(): Int = comments.size
}
