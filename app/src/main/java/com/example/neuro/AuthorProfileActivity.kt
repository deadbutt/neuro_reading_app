package com.example.neuro

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.neuro.base.UiState
import com.example.neuro.databinding.ActivityAuthorProfileBinding
import com.example.neuro.util.UrlUtils
import com.example.neuro.viewmodel.AuthorProfileViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AuthorProfileActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_AUTHOR_ID = "author_id"

        fun start(context: Context, authorId: String) {
            context.startActivity(Intent(context, AuthorProfileActivity::class.java).apply {
                putExtra(EXTRA_AUTHOR_ID, authorId)
            })
        }
    }

    private lateinit var binding: ActivityAuthorProfileBinding
    private val viewModel: AuthorProfileViewModel by viewModels()
    private lateinit var authorId: String
    private var currentTab = 0

    private val dynamicItems = mutableListOf<FeedActivityItem>()
    private val worksItems = mutableListOf<BookItem>()
    private var worksAdapter: BookAdapter? = null
    private var dynamicAdapter: FeedActivityAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAuthorProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        authorId = intent.getStringExtra(EXTRA_AUTHOR_ID) ?: ""
        if (authorId.isEmpty()) {
            Toast.makeText(this, "作者ID错误", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.ivBack.setOnClickListener { finish() }

        setupRecyclerView()
        setupTabs()
        observeViewModel()
        viewModel.loadAuthorProfile(authorId)
        viewModel.loadWorks(authorId)
        viewModel.loadFollowStatus(authorId)
    }

    private fun setupRecyclerView() {
        binding.rvAuthorContent.layoutManager = LinearLayoutManager(this)
        
        dynamicAdapter = FeedActivityAdapter(
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
        
        worksAdapter = BookAdapter(worksItems) { book ->
            if (book.bookId.isNotEmpty()) {
                BookDetailActivity.start(this, book.bookId)
            }
        }
        
        binding.rvAuthorContent.adapter = dynamicAdapter
        showDynamicList()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is UiState.Loading -> {}
                            is UiState.Success -> {
                                viewModel.authorProfile.value?.let { author ->
                                    binding.tvAuthorName.text = author.name
                                    binding.tvAuthorDesc.text = author.description
                                    binding.tvTitle.text = "${author.name} 的主页"
                                    binding.tvFollowersCount.text = "${author.followersCount}"
                                    binding.tvWorksCount.text = "${author.worksCount}"
                                    binding.tvTotalWords.text = "${author.totalWords}"

                                    val avatarUrl = author.avatar.takeIf { it.isNotEmpty() }
                                        ?: viewModel.works.value.firstOrNull()?.cover
                                    if (!avatarUrl.isNullOrEmpty()) {
                                        Glide.with(this@AuthorProfileActivity)
                                            .load(UrlUtils.normalize(avatarUrl))
                                            .placeholder(R.drawable.bg_avatar_placeholder)
                                            .circleCrop()
                                            .into(binding.ivAuthorAvatar)
                                    } else {
                                        binding.ivAuthorAvatar.setImageResource(R.drawable.bg_avatar_placeholder)
                                    }
                                }
                            }
                            is UiState.Error -> {
                                Toast.makeText(this@AuthorProfileActivity, state.message, Toast.LENGTH_SHORT).show()
                                if (state.message.contains("不存在") || state.message.contains("找不到")) {
                                    finish()
                                }
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    viewModel.isFollowing.collect { isFollowing ->
                        updateFollowButton(isFollowing)
                    }
                }

                launch {
                    viewModel.works.collect { works ->
                        val newItems = works.map { book ->
                            BookItem(
                                bookId = book.bookId,
                                title = book.title,
                                author = book.author.name,
                                desc = book.description,
                                coverUrl = book.cover ?: ""
                            )
                        }
                        worksAdapter?.updateData(newItems)
                        if (currentTab == 1) {
                            showWorksList()
                        }
                    }
                }
            }
        }
    }

    private fun updateFollowButton(isFollowing: Boolean) {
        if (isFollowing) {
            binding.tvFollowBtn.text = getString(R.string.author_following)
            binding.tvFollowBtn.setBackgroundResource(R.drawable.bg_btn_add_shelf)
            binding.tvFollowBtn.setTextColor(getColor(R.color.primary_red))
        } else {
            binding.tvFollowBtn.text = getString(R.string.author_follow)
            binding.tvFollowBtn.setBackgroundResource(R.drawable.bg_send_btn)
            binding.tvFollowBtn.setTextColor(getColor(R.color.white))
        }

        binding.tvFollowBtn.setOnClickListener {
            viewModel.toggleFollow(authorId)
        }
    }

    private fun setupTabs() {
        binding.tvTabDynamic.setOnClickListener {
            if (currentTab != 0) {
                currentTab = 0
                selectTab(binding.tvTabDynamic, binding.tvTabWorks, binding.vIndicatorDynamic)
                showDynamicList()
            }
        }

        binding.tvTabWorks.setOnClickListener {
            if (currentTab != 1) {
                currentTab = 1
                selectTab(binding.tvTabWorks, binding.tvTabDynamic, binding.vIndicatorDynamic)
                showWorksList()
            }
        }
    }

    private fun selectTab(selected: android.widget.TextView, unselected: android.widget.TextView, indicator: View) {
        selected.setTextColor(getColor(R.color.primary_red))
        selected.setTypeface(null, Typeface.BOLD)
        unselected.setTextColor(getColor(R.color.tab_inactive))
        unselected.setTypeface(null, Typeface.NORMAL)

        (indicator.parent as? android.view.ViewGroup)?.removeView(indicator)
        (selected.parent as? android.widget.FrameLayout)?.addView(indicator)
    }

    private fun showDynamicList() {
        dynamicAdapter?.notifyDataSetChanged()
    }

    private fun showWorksList() {
        if (binding.rvAuthorContent.adapter !== worksAdapter) {
            binding.rvAuthorContent.adapter = worksAdapter
        }
        worksAdapter?.notifyDataSetChanged()
    }
}
