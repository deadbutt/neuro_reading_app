package com.example.neuro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.neuro.api.RetrofitClient
import com.example.neuro.api.model.ArticleChapterMeta
import kotlinx.coroutines.launch

class TocActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ARTICLE_ID = "article_id"
        private const val EXTRA_ARTICLE_TITLE = "article_title"

        fun start(context: Context, articleId: String, articleTitle: String) {
            context.startActivity(Intent(context, TocActivity::class.java).apply {
                putExtra(EXTRA_ARTICLE_ID, articleId)
                putExtra(EXTRA_ARTICLE_TITLE, articleTitle)
            })
        }
    }

    private lateinit var articleId: String
    private var articleTitle: String = ""
    private var isAscending = true

    private var chapters: List<ArticleChapterMeta> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_toc)

        articleId = intent.getStringExtra(EXTRA_ARTICLE_ID) ?: ""
        articleTitle = intent.getStringExtra(EXTRA_ARTICLE_TITLE) ?: ""

        if (articleId.isEmpty()) {
            Toast.makeText(this, R.string.error_invalid_article_id, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<TextView>(R.id.tv_toc_title)?.text =
            getString(R.string.toc_title_with_name, articleTitle)
        findViewById<View>(R.id.iv_toc_back).setOnClickListener { finish() }

        setupSortToggle()
        loadChapters()
    }

    private fun loadChapters() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getArticleDetail(articleId)
                if (response.isSuccessful && response.body()?.code == Constants.ApiCode.SUCCESS) {
                    val data = response.body()?.data
                    data?.let {
                        chapters = it.chapters
                        updateStats(it)
                        setupRecyclerView()
                    }
                } else {
                    Toast.makeText(this@TocActivity, R.string.error_load_chapters_failed, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TocActivity, R.string.error_network, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStats(article: com.example.neuro.api.model.ArticleMeta) {
        val statsText = if (chapters.isNotEmpty()) {
            val latestChapter = chapters.last()
            getString(R.string.toc_stats_format, chapters.size, latestChapter.title)
        } else {
            getString(R.string.toc_stats_empty)
        }
        findViewById<TextView>(R.id.tv_toc_stats)?.text = statsText
    }

    private fun setupSortToggle() {
        findViewById<View>(R.id.ll_toc_sort).setOnClickListener {
            isAscending = !isAscending
            updateSortUI()
            setupRecyclerView()
        }
    }

    private fun updateSortUI() {
        val tvLabel = findViewById<TextView>(R.id.tv_sort_label)
        val ivIcon = findViewById<ImageView>(R.id.iv_toc_sort_icon)
        if (isAscending) {
            tvLabel.text = getString(R.string.toc_sort_asc)
            ivIcon.setImageResource(R.drawable.ic_sort_asc)
        } else {
            tvLabel.text = getString(R.string.toc_sort_desc)
            ivIcon.setImageResource(R.drawable.ic_sort_desc)
        }
    }

    private fun setupRecyclerView() {
        val displayChapters = if (isAscending) chapters else chapters.reversed()
        val chapterItems = displayChapters.map { chapter ->
            ChapterItem(
                name = chapter.title
            )
        }
        val rv = findViewById<RecyclerView>(R.id.rv_toc)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = ChapterAdapter(chapterItems) { _, position ->
            val actualPosition = if (isAscending) position else chapters.size - 1 - position
            val chapter = chapters[actualPosition]
            ReaderActivity.start(this, articleId, chapter.index, articleTitle)
            finish()
        }
    }
}
