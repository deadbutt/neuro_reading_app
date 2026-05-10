package com.example.neuro

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.neuro.api.RetrofitClient
import com.example.neuro.api.model.CommentResponse
import com.example.neuro.api.model.PostCommentRequest
import com.example.neuro.util.UrlUtils
import kotlinx.coroutines.launch

class CommentsActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ARTICLE_ID = "article_id"

        fun start(context: Context, articleId: String) {
            context.startActivity(Intent(context, CommentsActivity::class.java).apply {
                putExtra(EXTRA_ARTICLE_ID, articleId)
            })
        }
    }

    private lateinit var articleId: String
    private lateinit var tvSortHot: TextView
    private lateinit var tvSortNew: TextView
    private lateinit var tvSortAuthor: TextView
    private lateinit var vIndicatorHot: View
    private lateinit var tvCommentCount: TextView
    private var currentSort: String = "hot"
    private var currentPage: Int = 1

    private val comments = mutableListOf<CommentItem>()
    private lateinit var adapter: CommentAdapter
    private lateinit var srl: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)

        articleId = intent.getStringExtra(EXTRA_ARTICLE_ID) ?: ""

        findViewById<View>(R.id.iv_back).setOnClickListener { finish() }

        tvSortHot = findViewById(R.id.tv_sort_hot)
        tvSortNew = findViewById(R.id.tv_sort_new)
        tvSortAuthor = findViewById(R.id.tv_sort_author)
        vIndicatorHot = findViewById(R.id.v_indicator_hot)
        tvCommentCount = findViewById(R.id.tv_comment_count)

        setupSortTabs()
        setupRecyclerView()
        setupSwipeRefresh()
        setupSendButton()
        loadComments()
    }

    private fun loadComments() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getArticleComments(
                    articleId = articleId,
                    page = currentPage,
                    sort = currentSort
                )
                
                android.util.Log.d("CommentsDebug", "HTTP isSuccessful=${response.isSuccessful}, code=${response.code()}")
                
                if (!response.isSuccessful) {
                    android.util.Log.d("CommentsDebug", "HTTP not successful")
                    tvCommentCount.text = "0条"
                    Toast.makeText(this@CommentsActivity, "暂无评论，快来抢沙发吧", Toast.LENGTH_SHORT).show()
                    srl.isRefreshing = false
                    return@launch
                }
                
                val body = response.body()
                android.util.Log.d("CommentsDebug", "body=$body")
                
                if (body == null) {
                    android.util.Log.d("CommentsDebug", "body is null!")
                    tvCommentCount.text = "0条"
                    Toast.makeText(this@CommentsActivity, "暂无评论，快来抢沙发吧", Toast.LENGTH_SHORT).show()
                    srl.isRefreshing = false
                    return@launch
                }
                
                android.util.Log.d("CommentsDebug", "body.code=${body.code}")
                
                if (body.code != 0) {
                    android.util.Log.d("CommentsDebug", "body.code != 0")
                    tvCommentCount.text = "0条"
                    Toast.makeText(this@CommentsActivity, "暂无评论，快来抢沙发吧", Toast.LENGTH_SHORT).show()
                    srl.isRefreshing = false
                    return@launch
                }
                
                val paginatedData = body.data
                val total = paginatedData?.total ?: 0
                val commentsList = paginatedData?.list ?: emptyList()
                
                android.util.Log.d("CommentsDebug", "total=$total, listSize=${commentsList.size}")
                
                val newComments = commentsList.map { it.toCommentItem() }
                
                tvCommentCount.text = "${total}条"
                
                if (currentPage == 1) {
                    comments.clear()
                }
                
                if (newComments.isEmpty() && currentPage == 1) {
                    Toast.makeText(this@CommentsActivity, "暂无评论，快来抢沙发吧", Toast.LENGTH_SHORT).show()
                } else {
                    comments.addAll(newComments)
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                tvCommentCount.text = "0条"
                Toast.makeText(this@CommentsActivity, "网络错误，请检查网络连接", Toast.LENGTH_SHORT).show()
            } finally {
                srl.isRefreshing = false
            }
        }
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

    private fun setupSortTabs() {
        val tabs = listOf(tvSortHot, tvSortNew, tvSortAuthor)
        tabs.forEachIndexed { index, tv ->
            tv.setOnClickListener { selectSort(index) }
        }
    }

    private fun selectSort(index: Int) {
        currentSort = when (index) {
            0 -> "hot"
            1 -> "new"
            2 -> "author"
            else -> "hot"
        }
        currentPage = 1
        val tabs = listOf(tvSortHot, tvSortNew, tvSortAuthor)
        for ((i, tv) in tabs.withIndex()) {
            if (i == index) {
                tv.setTextColor(getColor(R.color.primary_red))
                tv.setTypeface(null, Typeface.BOLD)
            } else {
                tv.setTextColor(getColor(R.color.tab_inactive))
                tv.setTypeface(null, Typeface.NORMAL)
            }
        }
        loadComments()
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rv_comments)
        rv.layoutManager = LinearLayoutManager(this)
        adapter = CommentAdapter(comments,
            onLikeClick = { _, _ ->
                Toast.makeText(this, R.string.msg_like_success, Toast.LENGTH_SHORT).show()
            },
            onReplyClick = {
                Toast.makeText(this, R.string.msg_reply_feature, Toast.LENGTH_SHORT).show()
            }
        )
        rv.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        srl = findViewById(R.id.srl_comments)
        srl.setOnRefreshListener {
            currentPage = 1
            loadComments()
        }
    }

    private fun setupSendButton() {
        findViewById<View>(R.id.btn_send).setOnClickListener {
            val input = findViewById<EditText>(R.id.et_comment_input)
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                postComment(text, input)
            }
        }
    }

    private fun postComment(content: String, input: EditText) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.postComment(
                    articleId = articleId,
                    request = PostCommentRequest(content = content)
                )
                if (response.isSuccessful && response.body()?.code == Constants.ApiCode.SUCCESS) {
                    Toast.makeText(this@CommentsActivity, R.string.msg_comment_sent, Toast.LENGTH_SHORT).show()
                    input.text.clear()
                    currentPage = 1
                    loadComments()
                } else {
                    Toast.makeText(this@CommentsActivity, R.string.error_send_failed, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@CommentsActivity, R.string.error_network, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
