package com.example.neuro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.neuro.api.model.ArticleMeta
import com.example.neuro.api.model.CommentResponse
import com.example.neuro.base.UiState
import com.example.neuro.databinding.ActivityBookDetailBinding
import com.example.neuro.util.UrlUtils
import com.example.neuro.util.showToast
import com.example.neuro.viewmodel.BookDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BookDetailActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ARTICLE_ID = "article_id"

        fun start(context: Context, articleId: String) {
            context.startActivity(Intent(context, BookDetailActivity::class.java).apply {
                putExtra(EXTRA_ARTICLE_ID, articleId)
            })
        }
    }

    private lateinit var binding: ActivityBookDetailBinding
    private val viewModel: BookDetailViewModel by viewModels()
    private lateinit var articleId: String
    private var articleMeta: ArticleMeta? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        articleId = intent.getStringExtra(EXTRA_ARTICLE_ID) ?: ""
        if (articleId.isEmpty()) {
            showToast("文章ID错误")
            finish()
            return
        }

        setupListeners()
        observeViewModel()
        viewModel.loadArticleDetail(articleId)
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener { finish() }
        binding.tvSynopsisExpand.setOnClickListener { toggleSynopsis() }
        binding.btnStartRead.setOnClickListener { startReading() }
        binding.btnAddShelf.setOnClickListener { toggleBookshelf() }
        binding.tvTocViewAll.setOnClickListener { viewAllChapters() }
        binding.llCommentInput.setOnClickListener { showComments() }
        binding.tvReviewsViewAll.setOnClickListener { showComments() }
        binding.ivShare.setOnClickListener { shareArticle() }
        binding.llAuthorInfo.setOnClickListener { goToAuthorProfile() }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is UiState.Loading -> {}
                            is UiState.Error -> {
                                showToast(state.message)
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    viewModel.article.collect { article ->
                        article?.let {
                            articleMeta = it
                            displayArticleDetail(it)
                        }
                    }
                }

                launch {
                    viewModel.previewComments.collect { comments ->
                        displayPreviewComments(comments)
                    }
                }
            }
        }
    }

    private fun displayArticleDetail(article: ArticleMeta) {
        binding.tvDetailBookTitle.text = article.title
        val authorText = if (article.author.isNotBlank()) article.author else "未知作者"
        binding.tvDetailAuthor.text = getString(R.string.detail_author_format, authorText)
        binding.tvDetailSynopsis.text = article.summary
        binding.tvDetailWordCount.text = getString(R.string.detail_word_count_format, article.wordCount)

        if (!article.cover.isNullOrBlank()) {
            Glide.with(this)
                .load(UrlUtils.normalize(article.cover))
                .placeholder(R.drawable.bg_book_cover_placeholder)
                .into(binding.ivDetailCover)
        }

        setupTags(article.tags)
        setupChapterList(article)
    }

    private fun displayPreviewComments(comments: List<CommentResponse>) {
        if (comments.isEmpty()) {
            binding.rvReviewsPreview.visibility = View.GONE
            binding.tvReviewsEmpty.visibility = View.VISIBLE
            binding.tvReviewsCount.text = "0"
            return
        }

        binding.rvReviewsPreview.visibility = View.VISIBLE
        binding.tvReviewsEmpty.visibility = View.GONE
        binding.tvReviewsCount.text = "${comments.size}"

        val commentItems = comments.map { it.toCommentItem() }
        binding.rvReviewsPreview.layoutManager = LinearLayoutManager(this)
        binding.rvReviewsPreview.adapter = CommentAdapter(commentItems,
            onLikeClick = { _, _ ->
                Toast.makeText(this, R.string.msg_like_success, Toast.LENGTH_SHORT).show()
            },
            onReplyClick = {
                showComments()
            }
        )
    }

    private fun CommentResponse.toCommentItem(): CommentItem {
        return CommentItem(
            name = this.userName,
            avatarUrl = UrlUtils.normalize(this.userAvatar),
            time = this.createTime,
            content = this.content,
            likes = formatCount(this.likeCount),
            isAuthor = false
        )
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
        binding.llTags.removeAllViews()

        if (tags.isNullOrEmpty()) {
            binding.llTagsContainer.visibility = View.GONE
            return
        }

        binding.llTagsContainer.visibility = View.VISIBLE
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
            binding.llTags.addView(tv)
        }
    }

    private fun setupChapterList(article: ArticleMeta) {
        val chapterItems = article.chapters.map { chapter ->
            ChapterItem(name = chapter.title)
        }
        binding.rvDetailChapters.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        binding.rvDetailChapters.adapter = ChapterAdapter(chapterItems) { _, position ->
            val chapter = article.chapters[position]
            ReaderActivity.start(this, article.articleId, chapter.index, article.title)
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
        viewModel.addToBookshelf(articleId,
            onSuccess = { showToast("已加入书架") },
            onError = { showToast(it) }
        )
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
        val article = articleMeta ?: return
        if (!article.creatorId.isNullOrBlank()) {
            AuthorProfileActivity.start(this, article.creatorId)
        } else {
            showToast("作者信息暂不可用")
        }
    }

    private fun toggleSynopsis() {
        if (binding.tvDetailSynopsis.maxLines == 4) {
            binding.tvDetailSynopsis.maxLines = Int.MAX_VALUE
            binding.tvSynopsisExpand.text = "收起"
        } else {
            binding.tvDetailSynopsis.maxLines = 4
            binding.tvSynopsisExpand.text = "展开"
        }
    }
}
