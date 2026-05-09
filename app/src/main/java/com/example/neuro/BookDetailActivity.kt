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
import com.bumptech.glide.Glide
import com.example.neuro.api.model.ArticleMeta
import com.example.neuro.repository.ArticleRepository
import com.example.neuro.util.UrlUtils
import com.example.neuro.util.showToast
import kotlinx.coroutines.launch

class BookDetailActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ARTICLE_ID = "article_id"

        fun start(context: Context, articleId: String) {
            context.startActivity(Intent(context, BookDetailActivity::class.java).apply {
                putExtra(EXTRA_ARTICLE_ID, articleId)
            })
        }
    }

    private lateinit var articleId: String
    private var articleMeta: ArticleMeta? = null
    private val repository = ArticleRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_detail)

        articleId = intent.getStringExtra(EXTRA_ARTICLE_ID) ?: ""
        if (articleId.isEmpty()) {
            showToast("文章ID错误")
            finish()
            return
        }

        findViewById<View>(R.id.iv_back).setOnClickListener { finish() }
        findViewById<View>(R.id.tv_synopsis_expand).setOnClickListener { toggleSynopsis() }
        findViewById<View>(R.id.btn_start_read).setOnClickListener { startReading() }
        findViewById<View>(R.id.btn_add_shelf).setOnClickListener { toggleBookshelf() }
        findViewById<View>(R.id.tv_toc_view_all).setOnClickListener { viewAllChapters() }
        findViewById<View>(R.id.ll_comment_input).setOnClickListener { showComments() }
        findViewById<View>(R.id.tv_reviews_view_all).setOnClickListener { showComments() }
        findViewById<View>(R.id.iv_share).setOnClickListener { shareArticle() }
        findViewById<View>(R.id.ll_author_info).setOnClickListener { goToAuthorProfile() }

        loadArticleDetail()
    }

    private fun loadArticleDetail() {
        lifecycleScope.launch {
            when (val result = repository.getArticleDetail(articleId)) {
                is com.example.neuro.util.ApiResult.Success -> {
                    articleMeta = result.data
                    displayArticleDetail(result.data)
                }
                is com.example.neuro.util.ApiResult.Error -> {
                    showToast(result.message)
                }
                com.example.neuro.util.ApiResult.Loading -> {}
            }
        }
    }

    private fun displayArticleDetail(article: ArticleMeta) {
        findViewById<TextView>(R.id.tv_detail_book_title).text = article.title
        val authorText = if (article.author.isNotBlank()) article.author else "未知作者"
        findViewById<TextView>(R.id.tv_detail_author).text = getString(R.string.detail_author_format, authorText)
        findViewById<TextView>(R.id.tv_detail_synopsis).text = article.summary
        findViewById<TextView>(R.id.tv_detail_word_count).text = getString(R.string.detail_word_count_format, article.wordCount)
        android.util.Log.d("BookDetail", "article: id=${article.articleId}, title=${article.title}, author=${article.author}")

        // 加载封面
        val ivCover = findViewById<ImageView>(R.id.iv_detail_cover)
        if (!article.cover.isNullOrBlank()) {
            Glide.with(this)
                .load(UrlUtils.normalize(article.cover))
                .placeholder(R.drawable.bg_book_cover_placeholder)
                .into(ivCover)
        }

        // 动态渲染 tags
        setupTags(article.tags)

        // 显示章节列表
        setupChapterList(article)

        // 加载评论预览
        loadCommentsPreview()
    }

    private fun loadCommentsPreview() {
        lifecycleScope.launch {
            when (val result = repository.getArticleComments(articleId, page = 1, pageSize = 2)) {
                is com.example.neuro.util.ApiResult.Success -> {
                    val data = result.data
                    val comments = data.list ?: emptyList()
                    displayCommentsPreview(comments, data.total)
                }
                is com.example.neuro.util.ApiResult.Error -> {
                    displayCommentsPreview(emptyList(), 0)
                }
                com.example.neuro.util.ApiResult.Loading -> {}
            }
        }
    }

    private fun displayCommentsPreview(comments: List<com.example.neuro.api.model.CommentResponse>, total: Int) {
        val rvReviews = findViewById<RecyclerView>(R.id.rv_reviews_preview)
        val tvEmpty = findViewById<TextView>(R.id.tv_reviews_empty)
        val tvCount = findViewById<TextView>(R.id.tv_reviews_count)

        tvCount.text = getString(R.string.detail_reviews_count_format, total)

        if (comments.isEmpty()) {
            rvReviews.visibility = View.GONE
            tvEmpty.visibility = View.VISIBLE
        } else {
            rvReviews.visibility = View.VISIBLE
            tvEmpty.visibility = View.GONE

            val commentItems = comments.map { comment ->
                CommentItem(
                    name = comment.userName,
                    avatarUrl = UrlUtils.normalize(comment.userAvatar),
                    time = comment.createTime,
                    content = comment.content,
                    likes = formatCount(comment.likeCount),
                    isAuthor = false
                )
            }

            rvReviews.layoutManager = LinearLayoutManager(this)
            rvReviews.adapter = CommentAdapter(commentItems,
                onLikeClick = { _, _ ->
                    showToast(R.string.msg_like_success)
                },
                onReplyClick = {
                    showComments()
                }
            )
        }
    }

    private fun formatCount(count: Int): String {
        return when {
            count >= Constants.CountFormat.TEN_THOUSAND ->
                getString(R.string.format_ten_thousand, count / Constants.CountFormat.TEN_THOUSAND)
            count >= Constants.CountFormat.THOUSAND ->
                getString(R.string.format_thousand, count / Constants.CountFormat.THOUSAND)
            else -> count.toString()
        }
    }

    private fun setupTags(tags: List<String>?) {
        val tagsContainer = findViewById<android.widget.LinearLayout>(R.id.ll_tags)
        tagsContainer.removeAllViews()

        if (tags.isNullOrEmpty()) {
            findViewById<View>(R.id.ll_tags_container).visibility = View.GONE
            return
        }

        findViewById<View>(R.id.ll_tags_container).visibility = View.VISIBLE
        for (tag in tags) {
            val tv = TextView(this).apply {
                text = tag
                setTextColor(getColor(R.color.chip_text))
                textSize = 12f
                background = getDrawable(R.drawable.bg_chip_tag)
                gravity = android.view.Gravity.CENTER
                setPadding(
                    resources.getDimensionPixelSize(R.dimen.detail_chip_padding),
                    0,
                    resources.getDimensionPixelSize(R.dimen.detail_chip_padding),
                    0
                )
                layoutParams = android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                    resources.getDimensionPixelSize(R.dimen.detail_chip_height)
                ).apply {
                    marginEnd = 8
                }
            }
            tagsContainer.addView(tv)
        }
    }

    private fun setupChapterList(article: ArticleMeta) {
        val rvChapters = findViewById<RecyclerView>(R.id.rv_detail_chapters)
        rvChapters?.let {
            val chapterItems = article.chapters.map { chapter ->
                ChapterItem(
                    name = chapter.title
                )
            }
            it.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
            it.adapter = ChapterAdapter(chapterItems) { _, position ->
                val chapter = article.chapters[position]
                ReaderActivity.start(this, article.articleId, chapter.index, article.title)
            }
        }
    }

    private fun startReading() {
        val article = articleMeta ?: return
        val firstChapter = article.chapters.firstOrNull()
        if (firstChapter != null) {
            ReaderActivity.start(this, article.articleId, firstChapter.index, article.title)
        } else {
            showToast("暂无章节")
        }
    }

    private fun toggleBookshelf() {
        val article = articleMeta ?: return
        lifecycleScope.launch {
            when (val result = repository.addToBookshelf(articleId)) {
                is com.example.neuro.util.ApiResult.Success -> {
                    showToast("已加入书架")
                }
                is com.example.neuro.util.ApiResult.Error -> {
                    showToast(result.message)
                }
                com.example.neuro.util.ApiResult.Loading -> {}
            }
        }
    }

    private fun viewAllChapters() {
        val article = articleMeta ?: return
        TocActivity.start(this, articleId, article.title)
    }

    private fun showComments() {
        CommentsBottomSheet.newInstance(articleId).show(supportFragmentManager, "comments")
    }

    private fun shareArticle() {
        val article = articleMeta ?: return
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "推荐一篇文章：《${article.title}》—— ${article.author}\n${article.summary}")
        }
        startActivity(Intent.createChooser(shareIntent, "分享到"))
    }

    private fun goToAuthorProfile() {
        // 作者 ID 需要从后端获取，目前 API 中 author 是字符串
        // 简化处理：暂不跳转
        showToast("作者主页")
    }

    private fun toggleSynopsis() {
        val synopsisText = findViewById<TextView>(R.id.tv_detail_synopsis)
        val expandBtn = findViewById<TextView>(R.id.tv_synopsis_expand)
        if (synopsisText.maxLines == 4) {
            synopsisText.maxLines = Int.MAX_VALUE
            expandBtn.text = "收起"
        } else {
            synopsisText.maxLines = 4
            expandBtn.text = "展开"
        }
    }
}
