package com.example.neuro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class FeedActivityItem(
    val authorName: String,
    val authorAvatarResId: Int = 0,
    val publishTime: String,
    val activityContent: String,
    val bookCoverResId: Int = 0,
    val chapterPreview: String,
    val readHeat: String,
    val likeCount: String,
    val commentCount: String,
    var isLiked: Boolean = false
)

class FeedActivityAdapter(
    private val items: List<FeedActivityItem>,
    private val onItemClick: (FeedActivityItem) -> Unit = {},
    private val onLikeClick: (FeedActivityItem, Int) -> Unit = { _, _ -> },
    private val onCommentClick: (FeedActivityItem) -> Unit = {}
) : RecyclerView.Adapter<FeedActivityAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAuthorAvatar: ImageView = view.findViewById(R.id.iv_author_avatar)
        val tvAuthorName: TextView = view.findViewById(R.id.tv_author_name)
        val tvPublishTime: TextView = view.findViewById(R.id.tv_publish_time)
        val ivBookIcon: ImageView = view.findViewById(R.id.iv_book_icon)
        val tvActivityContent: TextView = view.findViewById(R.id.tv_activity_content)
        val ivBookCover: ImageView = view.findViewById(R.id.iv_book_cover)
        val tvChapterPreview: TextView = view.findViewById(R.id.tv_chapter_preview)
        val tvReadHeat: TextView = view.findViewById(R.id.tv_read_heat)
        val ivLike: ImageView = view.findViewById(R.id.iv_like)
        val tvLikeCount: TextView = view.findViewById(R.id.tv_like_count)
        val ivComment: ImageView = view.findViewById(R.id.iv_comment)
        val tvCommentCount: TextView = view.findViewById(R.id.tv_comment_count)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_feed_activity, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvAuthorName.text = item.authorName
        holder.tvPublishTime.text = item.publishTime
        holder.tvActivityContent.text = item.activityContent
        holder.tvChapterPreview.text = item.chapterPreview
        holder.tvReadHeat.text = item.readHeat
        holder.tvLikeCount.text = item.likeCount
        holder.tvCommentCount.text = item.commentCount

        if (item.authorAvatarResId != 0) {
            holder.ivAuthorAvatar.setImageResource(item.authorAvatarResId)
        }
        if (item.bookCoverResId != 0) {
            holder.ivBookCover.setImageResource(item.bookCoverResId)
        }

        updateLikeIcon(holder, item.isLiked)

        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.ivLike.setOnClickListener {
            item.isLiked = !item.isLiked
            updateLikeIcon(holder, item.isLiked)
            onLikeClick(item, position)
        }
        holder.ivComment.setOnClickListener { onCommentClick(item) }
    }

    private fun updateLikeIcon(holder: ViewHolder, isLiked: Boolean) {
        if (isLiked) {
            holder.ivLike.setImageResource(R.drawable.ic_heart_filled)
        } else {
            holder.ivLike.setImageResource(R.drawable.ic_heart_border)
        }
    }

    override fun getItemCount(): Int = items.size
}
