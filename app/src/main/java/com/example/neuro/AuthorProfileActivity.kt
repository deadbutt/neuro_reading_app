package com.example.neuro

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
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
import com.example.neuro.util.UrlUtils
import kotlinx.coroutines.launch

class AuthorProfileActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_AUTHOR_ID = "author_id"

        fun start(context: Context, authorId: String) {
            context.startActivity(Intent(context, AuthorProfileActivity::class.java).apply {
                putExtra(EXTRA_AUTHOR_ID, authorId)
            })
        }
    }

    private lateinit var authorId: String
    private var isFollowing = false
    private var currentTab = 0

    private val dynamicItems = mutableListOf<FeedActivityItem>()
    private val worksItems = mutableListOf<BookItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_author_profile)

        authorId = intent.getStringExtra(EXTRA_AUTHOR_ID) ?: ""
        if (authorId.isEmpty()) {
            Toast.makeText(this, "作者ID错误", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<View>(R.id.iv_back).setOnClickListener { finish() }

        loadAuthorProfile()
        setupFollowButton()
        setupTabs()
        showDynamicList()
    }

    private fun loadAuthorProfile() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getAuthorProfile(authorId)
                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = response.body()?.data
                    data?.let { author ->
                        findViewById<TextView>(R.id.tv_author_name).text = author.name
                        findViewById<TextView>(R.id.tv_author_desc).text = author.description
                        findViewById<TextView>(R.id.tv_title).text = "${author.name} 的主页"
                        findViewById<TextView>(R.id.tv_followers_count).text = "${author.followersCount}"
                        findViewById<TextView>(R.id.tv_works_count).text = "${author.worksCount}"
                        findViewById<TextView>(R.id.tv_total_words).text = "${author.totalWords}"

                        // 加载头像
                        if (author.avatar.isNotEmpty()) {
                            com.bumptech.glide.Glide.with(this@AuthorProfileActivity)
                                .load(UrlUtils.normalize(author.avatar))
                                .placeholder(R.drawable.bg_avatar_placeholder)
                                .circleCrop()
                                .into(findViewById(R.id.iv_author_avatar))
                        }

                        loadAuthorWorks()
                    }
                } else {
                    Toast.makeText(this@AuthorProfileActivity, "加载作者信息失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AuthorProfileActivity, "网络错误", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadAuthorWorks() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getAuthorWorks(authorId)
                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = response.body()?.data
                    data?.let { page ->
                        worksItems.clear()
                        worksItems.addAll(page.list.map { article ->
                            BookItem(
                                bookId = article.articleId,
                                title = article.title,
                                author = article.author,
                                desc = article.summary
                            )
                        })
                        if (currentTab == 1) {
                            showWorksList()
                        }
                    }
                }
            } catch (e: Exception) {
                // 静默失败
            }
        }
    }

    private fun setupFollowButton() {
        val followBtn = findViewById<TextView>(R.id.tv_follow_btn)
        followBtn.setOnClickListener {
            lifecycleScope.launch {
                try {
                    val response = if (isFollowing) {
                        RetrofitClient.apiService.unfollowAuthor(authorId)
                    } else {
                        RetrofitClient.apiService.followAuthor(authorId)
                    }
                    if (response.isSuccessful && response.body()?.code == 0) {
                        isFollowing = !isFollowing
                        updateFollowButton(followBtn)
                    } else {
                        Toast.makeText(this@AuthorProfileActivity, "操作失败", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@AuthorProfileActivity, "网络错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateFollowButton(followBtn: TextView) {
        if (isFollowing) {
            followBtn.text = getString(R.string.author_following)
            followBtn.setBackgroundResource(R.drawable.bg_btn_add_shelf)
            followBtn.setTextColor(getColor(R.color.primary_red))
            Toast.makeText(this, "已关注", Toast.LENGTH_SHORT).show()
        } else {
            followBtn.text = getString(R.string.author_follow)
            followBtn.setBackgroundResource(R.drawable.bg_send_btn)
            followBtn.setTextColor(getColor(R.color.white))
            Toast.makeText(this, "已取消关注", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTabs() {
        val tvDynamic = findViewById<TextView>(R.id.tv_tab_dynamic)
        val tvWorks = findViewById<TextView>(R.id.tv_tab_works)
        val vIndicatorDynamic = findViewById<View>(R.id.v_indicator_dynamic)

        tvDynamic.setOnClickListener {
            if (currentTab != 0) {
                currentTab = 0
                selectTab(tvDynamic, tvWorks, vIndicatorDynamic)
                showDynamicList()
            }
        }

        tvWorks.setOnClickListener {
            if (currentTab != 1) {
                currentTab = 1
                selectTab(tvWorks, tvDynamic, vIndicatorDynamic)
                showWorksList()
            }
        }
    }

    private fun selectTab(selected: TextView, unselected: TextView, indicator: View) {
        selected.setTextColor(getColor(R.color.primary_red))
        selected.setTypeface(null, Typeface.BOLD)
        unselected.setTextColor(getColor(R.color.tab_inactive))
        unselected.setTypeface(null, Typeface.NORMAL)

        val parent = selected.parent as? android.widget.FrameLayout
        val unselectedParent = unselected.parent as? android.widget.FrameLayout
        parent?.removeView(indicator)
        unselectedParent?.removeView(indicator)
        parent?.addView(indicator)
    }

    private fun showDynamicList() {
        val rv = findViewById<RecyclerView>(R.id.rv_author_content)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = FeedActivityAdapter(
            items = dynamicItems,
            onItemClick = { item ->
                Toast.makeText(this, "查看动态: ${item.activityContent}", Toast.LENGTH_SHORT).show()
            },
            onLikeClick = { _, _ ->
                Toast.makeText(this, "点赞成功", Toast.LENGTH_SHORT).show()
            },
            onCommentClick = { _ ->
                CommentsBottomSheet().show(supportFragmentManager, "comments")
            }
        )
    }

    private fun showWorksList() {
        val rv = findViewById<RecyclerView>(R.id.rv_author_content)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = BookAdapter(worksItems) { book ->
            if (book.bookId.isNotEmpty()) {
                BookDetailActivity.start(this, book.bookId)
            }
        }
    }
}
