package com.example.neuro

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.neuro.api.RetrofitClient
import kotlinx.coroutines.launch

data class WorkItem(
    val articleId: String,
    val title: String,
    val summary: String,
    val cover: String?,
    val chapterCount: Int,
    val wordCount: Int,
    val status: String?
)

class WorksAdapter(
    private var works: MutableList<WorkItem>,
    private val lifecycleOwner: LifecycleOwner,
    private val onWorkDeleted: () -> Unit = {}
) : RecyclerView.Adapter<WorksAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val flCover: FrameLayout = view.findViewById(R.id.fl_cover)
        val ivCover: ImageView = view.findViewById(R.id.iv_cover)
        val tvCoverPlaceholder: TextView = view.findViewById(R.id.tv_cover_placeholder)
        val tvTitle: TextView = view.findViewById(R.id.tv_work_title)
        val tvInfo: TextView = view.findViewById(R.id.tv_work_info)
        val tvStatus: TextView = view.findViewById(R.id.tv_work_status)
        val tvSummary: TextView = view.findViewById(R.id.tv_work_summary)
        val tvEdit: TextView = view.findViewById(R.id.tv_edit)
        val tvDelete: TextView = view.findViewById(R.id.tv_delete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_work, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val work = works[position]
        val context = holder.itemView.context

        holder.tvTitle.text = work.title
        holder.tvInfo.text = "${work.chapterCount}章 · ${work.wordCount}字"
        holder.tvStatus.text = when (work.status) {
            "draft" -> "草稿"
            "published" -> "已发布"
            else -> "未知"
        }
        holder.tvSummary.text = work.summary.ifBlank { "暂无简介" }

        if (!work.cover.isNullOrBlank()) {
            holder.tvCoverPlaceholder.visibility = View.GONE
            holder.ivCover.visibility = View.VISIBLE
            Glide.with(context)
                .load(work.cover)
                .into(holder.ivCover)
        } else {
            holder.ivCover.visibility = View.GONE
            holder.tvCoverPlaceholder.visibility = View.VISIBLE
            val firstChar = work.title.firstOrNull()?.toString() ?: "?"
            holder.tvCoverPlaceholder.text = firstChar
        }

        holder.itemView.setOnClickListener {
            WorkManageActivity.start(context, work.articleId, work.title, work.cover)
        }

        holder.tvEdit.setOnClickListener {
            WorkManageActivity.start(context, work.articleId, work.title, work.cover)
        }

        holder.tvDelete.setOnClickListener {
            showDeleteDialog(context, work, position)
        }
    }

    private fun showDeleteDialog(context: android.content.Context, work: WorkItem, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("删除作品")
            .setMessage("确定要删除「${work.title}」吗？此操作不可恢复。")
            .setPositiveButton("删除") { _, _ ->
                deleteWork(context, work.articleId, position)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteWork(context: android.content.Context, articleId: String, position: Int) {
        lifecycleOwner.lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteWork(articleId)
                if (response.isSuccessful && response.body()?.code == 0) {
                    works.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, works.size)
                    onWorkDeleted()
                    android.widget.Toast.makeText(context, "删除成功", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    val msg = response.body()?.message ?: "删除失败"
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                android.widget.Toast.makeText(context, "网络错误", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int = works.size

    fun updateData(newWorks: List<WorkItem>) {
        works.clear()
        works.addAll(newWorks)
        notifyDataSetChanged()
    }
}
