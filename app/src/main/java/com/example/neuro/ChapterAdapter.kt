package com.example.neuro

import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

data class ChapterItem(
    val name: String,
    val isCurrent: Boolean = false,
    val isVip: Boolean = false,
    val isTrial: Boolean = false
)

class ChapterAdapter(
    private var chapters: List<ChapterItem>,
    private val onChapterClick: (ChapterItem, Int) -> Unit = { _, _ -> }
) : RecyclerView.Adapter<ChapterAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCurrentDot: ImageView = view.findViewById(R.id.iv_toc_current_dot)
        val tvName: TextView = view.findViewById(R.id.tv_toc_chapter_name)
        val tvTagReading: TextView = view.findViewById(R.id.tv_toc_tag_reading)
        val tvTagTrial: TextView = view.findViewById(R.id.tv_toc_tag_trial)
        val tvTagVip: TextView = view.findViewById(R.id.tv_toc_tag_vip)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_toc, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chapter = chapters[position]
        holder.tvName.text = chapter.name

        if (chapter.isCurrent) {
            holder.tvName.setTypeface(null, Typeface.BOLD)
            holder.tvName.setTextColor(holder.tvName.resources.getColor(R.color.primary_red, null))
            holder.ivCurrentDot.visibility = View.VISIBLE
            holder.tvTagReading.visibility = View.VISIBLE
        } else {
            holder.tvName.setTypeface(null, Typeface.NORMAL)
            holder.tvName.setTextColor(holder.tvName.resources.getColor(R.color.text_primary, null))
            holder.ivCurrentDot.visibility = View.GONE
            holder.tvTagReading.visibility = View.GONE
        }

        holder.tvTagTrial.visibility = if (chapter.isTrial) View.VISIBLE else View.GONE
        holder.tvTagVip.visibility = if (chapter.isVip) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onChapterClick(chapter, position) }
    }

    override fun getItemCount(): Int = chapters.size

    fun updateData(newChapters: List<ChapterItem>) {
        chapters = newChapters
        notifyDataSetChanged()
    }
}
