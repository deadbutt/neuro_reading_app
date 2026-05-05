package com.example.neuro

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.neuro.api.RetrofitClient
import com.example.neuro.api.model.ChapterInfo
import kotlinx.coroutines.launch

class WorkManageActivity : AppCompatActivity() {

    private lateinit var ivCover: ImageView
    private lateinit var tvCoverPlaceholder: TextView
    private lateinit var tvTitle: TextView
    private lateinit var tvStatus: TextView
    private lateinit var tvInfo: TextView
    private lateinit var tvSummary: TextView
    private lateinit var tvEditInfo: TextView
    private lateinit var tvAddChapter: TextView
    private lateinit var rvChapters: RecyclerView
    private lateinit var llEmptyChapters: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var ivMore: ImageView

    private var articleId: String? = null
    private var workTitle: String? = null
    private var cover: String? = null
    private var chapters = mutableListOf<ChapterInfo>()
    private lateinit var chapterAdapter: ChapterManageAdapter

    companion object {
        private const val EXTRA_ARTICLE_ID = "article_id"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_COVER = "cover"

        fun start(context: Context, articleId: String, title: String? = null, cover: String? = null) {
            context.startActivity(Intent(context, WorkManageActivity::class.java).apply {
                putExtra(EXTRA_ARTICLE_ID, articleId)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_COVER, cover)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_work_manage)

        articleId = intent.getStringExtra(EXTRA_ARTICLE_ID)
        workTitle = intent.getStringExtra(EXTRA_TITLE)
        cover = intent.getStringExtra(EXTRA_COVER)

        initViews()
        setupClickListeners()
        loadWorkDetail()
    }

    private fun initViews() {
        ivCover = findViewById(R.id.iv_cover)
        tvCoverPlaceholder = findViewById(R.id.tv_cover_placeholder)
        tvTitle = findViewById(R.id.tv_title)
        tvStatus = findViewById(R.id.tv_status)
        tvInfo = findViewById(R.id.tv_info)
        tvSummary = findViewById(R.id.tv_summary)
        tvEditInfo = findViewById(R.id.tv_edit_info)
        tvAddChapter = findViewById(R.id.tv_add_chapter)
        rvChapters = findViewById(R.id.rv_chapters)
        llEmptyChapters = findViewById(R.id.ll_empty_chapters)
        progressBar = findViewById(R.id.progress_bar)
        ivMore = findViewById(R.id.iv_more)

        chapterAdapter = ChapterManageAdapter(chapters,
            onEditClick = { chapter ->
                EditorActivity.start(this, articleId, workTitle, chapter.index)
            },
            onDeleteClick = { chapter ->
                showDeleteChapterDialog(chapter)
            }
        )
        rvChapters.layoutManager = LinearLayoutManager(this)
        rvChapters.adapter = chapterAdapter

        workTitle?.let { tvTitle.text = it }
        updateCoverDisplay()
    }

    private fun updateCoverDisplay() {
        if (!cover.isNullOrBlank()) {
            tvCoverPlaceholder.visibility = View.GONE
            ivCover.visibility = View.VISIBLE
            Glide.with(this)
                .load(cover)
                .into(ivCover)
        } else {
            ivCover.visibility = View.GONE
            tvCoverPlaceholder.visibility = View.VISIBLE
            val firstChar = workTitle?.firstOrNull()?.toString() ?: "?"
            tvCoverPlaceholder.text = firstChar
        }
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }

        ivMore.setOnClickListener {
            showMoreOptions()
        }

        tvEditInfo.setOnClickListener {
            EditWorkInfoActivity.start(this, articleId ?: return@setOnClickListener)
        }

        tvAddChapter.setOnClickListener {
            EditorActivity.start(this, articleId, workTitle)
        }
    }

    private fun showMoreOptions() {
        val options = arrayOf("发布作品", "删除作品")
        AlertDialog.Builder(this)
            .setTitle("操作")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> publishWork()
                    1 -> showDeleteWorkDialog()
                }
            }
            .show()
    }

    private fun loadWorkDetail() {
        if (articleId.isNullOrBlank()) return

        progressBar.visibility = View.VISIBLE
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getWorkDetail(articleId!!)
                progressBar.visibility = View.GONE
                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = response.body()?.data ?: return@launch
                    workTitle = data.title
                    cover = data.cover

                    tvTitle.text = data.title
                    tvStatus.text = when (data.status) {
                        "draft" -> "草稿"
                        "published" -> "已发布"
                        else -> "未知"
                    }
                    tvInfo.text = "${data.chapters.size}章 · ${data.wordCount}字"
                    tvSummary.text = data.summary.ifBlank { "暂无简介" }

                    chapters.clear()
                    chapters.addAll(data.chapters)
                    chapterAdapter.notifyDataSetChanged()

                    llEmptyChapters.visibility = if (chapters.isEmpty()) View.VISIBLE else View.GONE
                    rvChapters.visibility = if (chapters.isEmpty()) View.GONE else View.VISIBLE

                    updateCoverDisplay()
                } else {
                    Toast.makeText(this@WorkManageActivity, "加载失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                progressBar.visibility = View.GONE
                Toast.makeText(this@WorkManageActivity, "网络错误: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun publishWork() {
        if (articleId.isNullOrBlank()) return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.publishWork(articleId!!)
                if (response.isSuccessful && response.body()?.code == 0) {
                    Toast.makeText(this@WorkManageActivity, "发布成功", Toast.LENGTH_SHORT).show()
                    loadWorkDetail()
                } else {
                    val msg = response.body()?.message ?: "发布失败"
                    Toast.makeText(this@WorkManageActivity, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@WorkManageActivity, "网络错误", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteWorkDialog() {
        AlertDialog.Builder(this)
            .setTitle("删除作品")
            .setMessage("确定要删除这部作品吗？此操作不可恢复。")
            .setPositiveButton("删除") { _, _ ->
                deleteWork()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteWork() {
        if (articleId.isNullOrBlank()) return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteWork(articleId!!)
                if (response.isSuccessful && response.body()?.code == 0) {
                    Toast.makeText(this@WorkManageActivity, "删除成功", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    val msg = response.body()?.message ?: "删除失败"
                    Toast.makeText(this@WorkManageActivity, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@WorkManageActivity, "网络错误", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteChapterDialog(chapter: ChapterInfo) {
        AlertDialog.Builder(this)
            .setTitle("删除章节")
            .setMessage("确定要删除「${chapter.title}」吗？")
            .setPositiveButton("删除") { _, _ ->
                deleteChapter(chapter.index)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteChapter(chapterIndex: Int) {
        if (articleId.isNullOrBlank()) return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.deleteChapter(articleId!!, chapterIndex)
                if (response.isSuccessful && response.body()?.code == 0) {
                    Toast.makeText(this@WorkManageActivity, "删除成功", Toast.LENGTH_SHORT).show()
                    loadWorkDetail()
                } else {
                    val msg = response.body()?.message ?: "删除失败"
                    Toast.makeText(this@WorkManageActivity, msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@WorkManageActivity, "网络错误", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadWorkDetail()
    }
}

class ChapterManageAdapter(
    private var chapters: List<ChapterInfo>,
    private val onEditClick: (ChapterInfo) -> Unit,
    private val onDeleteClick: (ChapterInfo) -> Unit
) : RecyclerView.Adapter<ChapterManageAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvIndex: TextView = view.findViewById(R.id.tv_index)
        val tvTitle: TextView = view.findViewById(R.id.tv_chapter_title)
        val tvInfo: TextView = view.findViewById(R.id.tv_chapter_info)
        val ivEdit: ImageView = view.findViewById(R.id.iv_edit)
        val ivDelete: ImageView = view.findViewById(R.id.iv_delete)
    }

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manage_chapter, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val chapter = chapters[position]
        holder.tvIndex.text = "${chapter.index + 1}"
        holder.tvTitle.text = chapter.title
        holder.tvInfo.text = "${chapter.wordCount}字"
        holder.ivEdit.setOnClickListener { onEditClick(chapter) }
        holder.ivDelete.setOnClickListener { onDeleteClick(chapter) }
    }

    override fun getItemCount(): Int = chapters.size
}
