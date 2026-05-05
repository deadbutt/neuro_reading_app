package com.example.neuro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class WorkItem(
    val articleId: String,
    val title: String,
    val summary: String,
    val cover: String,
    val chapterCount: Int,
    val wordCount: Int,
    val status: String
)

class WorksAdapter(
    private var works: List<WorkItem>,
    private val onItemClick: (WorkItem, Int) -> Unit
) : RecyclerView.Adapter<WorksAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_work_title)
        val tvInfo: TextView = view.findViewById(R.id.tv_work_info)
        val tvStatus: TextView = view.findViewById(R.id.tv_work_status)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_work, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val work = works[position]
        holder.tvTitle.text = work.title
        holder.tvInfo.text = "${work.chapterCount}章 · ${work.wordCount}字"
        holder.tvStatus.text = if (work.status == "draft") "草稿" else "已发布"
        holder.itemView.setOnClickListener { onItemClick(work, position) }
    }

    override fun getItemCount(): Int = works.size

    fun updateData(newWorks: List<WorkItem>) {
        works = newWorks
        notifyDataSetChanged()
    }
}
