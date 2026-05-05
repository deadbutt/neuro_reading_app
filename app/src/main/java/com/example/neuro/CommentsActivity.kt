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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

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
    private var currentSort: Int = 0

    private val comments = mutableListOf<CommentItem>()
    private lateinit var adapter: CommentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_comments)

        articleId = intent.getStringExtra(EXTRA_ARTICLE_ID) ?: ""

        findViewById<View>(R.id.iv_back).setOnClickListener { finish() }

        tvSortHot = findViewById(R.id.tv_sort_hot)
        tvSortNew = findViewById(R.id.tv_sort_new)
        tvSortAuthor = findViewById(R.id.tv_sort_author)
        vIndicatorHot = findViewById(R.id.v_indicator_hot)

        setupSortTabs()
        setupRecyclerView()
        setupSwipeRefresh()
        setupSendButton()
    }

    private fun setupSortTabs() {
        val tabs = listOf(tvSortHot, tvSortNew, tvSortAuthor)
        tabs.forEachIndexed { index, tv ->
            tv.setOnClickListener { selectSort(index) }
        }
    }

    private fun selectSort(index: Int) {
        currentSort = index
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
        val srl = findViewById<SwipeRefreshLayout>(R.id.srl_comments)
        srl.setOnRefreshListener {
            srl.postDelayed({ srl.isRefreshing = false }, 1500L)
        }
    }

    private fun setupSendButton() {
        findViewById<View>(R.id.btn_send).setOnClickListener {
            val input = findViewById<EditText>(R.id.et_comment_input)
            val text = input.text.toString().trim()
            if (text.isNotEmpty()) {
                Toast.makeText(this, R.string.msg_comment_sent, Toast.LENGTH_SHORT).show()
                input.text.clear()
            }
        }
    }
}
