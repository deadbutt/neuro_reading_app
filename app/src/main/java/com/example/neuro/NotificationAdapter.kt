package com.example.neuro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.neuro.api.model.NotificationResponse
import com.example.neuro.util.UrlUtils

data class NotificationItem(
    val notificationId: String,
    val type: String,
    val title: String,
    val content: String,
    val relatedId: String,
    val fromUserName: String,
    val fromUserAvatar: String,
    val isRead: Boolean,
    val createTime: String
)

class NotificationAdapter(
    private var items: List<NotificationItem> = emptyList(),
    private val onItemClick: (NotificationItem) -> Unit = {}
) : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivAvatar: ImageView = view.findViewById(R.id.iv_notification_avatar)
        val tvTitle: TextView = view.findViewById(R.id.tv_notification_title)
        val tvContent: TextView = view.findViewById(R.id.tv_notification_content)
        val tvTime: TextView = view.findViewById(R.id.tv_notification_time)
        val vUnreadDot: View = view.findViewById(R.id.v_unread_dot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.fromUserName
        holder.tvContent.text = item.content
        holder.tvTime.text = item.createTime
        holder.vUnreadDot.visibility = if (item.isRead) View.GONE else View.VISIBLE

        if (item.fromUserAvatar.isNotEmpty()) {
            Glide.with(holder.itemView.context)
                .load(UrlUtils.normalize(item.fromUserAvatar))
                .placeholder(R.drawable.bg_profile_avatar)
                .circleCrop()
                .into(holder.ivAvatar)
        } else {
            holder.ivAvatar.setImageResource(R.drawable.bg_profile_avatar)
        }

        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<NotificationItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}

fun NotificationResponse.toNotificationItem(): NotificationItem {
    return NotificationItem(
        notificationId = this.notificationId,
        type = this.type,
        title = this.title,
        content = this.content,
        relatedId = this.relatedId,
        fromUserName = this.fromUserName,
        fromUserAvatar = this.fromUserAvatar,
        isRead = this.isRead,
        createTime = this.createTime
    )
}