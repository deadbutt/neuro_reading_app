package com.example.neuro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.neuro.api.model.CommentReplyResponse
import com.example.neuro.util.UrlUtils

data class CommentItem(
    val commentId: String = "",
    val name: String,
    val avatarUrl: String? = null,
    val time: String,
    val content: String,
    val likes: String,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val isAuthor: Boolean = false,
    val replyCount: Int = 0,
    val replies: List<CommentReplyResponse> = emptyList()
)

class CommentAdapter(
    private val comments: MutableList<CommentItem>,
    private val onLikeClick: (CommentItem, Int) -> Unit = { _, _ -> },
    private val onReplyClick: (CommentItem) -> Unit = {},
    private val onViewMoreReplies: (CommentItem, Int) -> Unit = { _, _ -> }
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
        val llReplies: LinearLayout = view.findViewById(R.id.ll_replies)
        val tvViewMoreReplies: TextView = view.findViewById(R.id.tv_view_more_replies)
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

        if (comment.isLiked) {
            holder.ivLike.setImageResource(R.drawable.ic_heart_filled)
            holder.tvLikes.setTextColor(holder.itemView.context.getColor(R.color.primary_red))
        } else {
            holder.ivLike.setImageResource(R.drawable.ic_heart_border)
            holder.tvLikes.setTextColor(holder.itemView.context.getColor(R.color.text_secondary))
        }

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

        // 显示回复列表
        holder.llReplies.removeAllViews()
        if (comment.replies.isNotEmpty()) {
            holder.llReplies.visibility = View.VISIBLE
            for (reply in comment.replies) {
                val replyView = LayoutInflater.from(holder.itemView.context)
                    .inflate(R.layout.item_comment_reply, holder.llReplies, false)

                val tvReplyName = replyView.findViewById<TextView>(R.id.tv_reply_name)
                val tvReplyContent = replyView.findViewById<TextView>(R.id.tv_reply_content)
                val ivReplyAvatar = replyView.findViewById<ImageView>(R.id.iv_reply_avatar)

                tvReplyName.text = reply.userName
                tvReplyContent.text = reply.content

                if (!reply.userAvatar.isNullOrEmpty()) {
                    Glide.with(holder.itemView.context)
                        .load(UrlUtils.normalize(reply.userAvatar))
                        .placeholder(R.drawable.bg_avatar_placeholder)
                        .circleCrop()
                        .into(ivReplyAvatar)
                } else {
                    ivReplyAvatar.setImageResource(R.drawable.bg_avatar_placeholder)
                }

                holder.llReplies.addView(replyView)
            }

            if (comment.replyCount > comment.replies.size) {
                holder.tvViewMoreReplies.visibility = View.VISIBLE
                holder.tvViewMoreReplies.text = "查看全部 ${comment.replyCount} 条回复"
                holder.tvViewMoreReplies.setOnClickListener { onViewMoreReplies(comment, position) }
            } else {
                holder.tvViewMoreReplies.visibility = View.GONE
            }
        } else {
            holder.llReplies.visibility = View.GONE
            holder.tvViewMoreReplies.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int = comments.size

    fun updateItem(position: Int, newItem: CommentItem) {
        if (position in 0 until comments.size) {
            comments[position] = newItem
            notifyItemChanged(position)
        }
    }
}
