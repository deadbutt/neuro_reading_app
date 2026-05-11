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
import com.example.neuro.databinding.ActivityAuthorProfileBinding
import com.example.neuro.util.UrlUtils
import com.example.neuro.viewmodel.AuthorProfileUiState
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

        setupTabs()
        observeViewModel()
        viewModel.loadAuthorProfile(authorId)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is AuthorProfileUiState.Loading -> {}
                            is AuthorProfileUiState.Success -> {
                                viewModel.authorProfile.value?.let { author ->
                                    binding.tvAuthorName.text = author.name
                                    binding.tvAuthorDesc.text = author.description
                                    binding.tvTitle.text = "${author.name} 的主页"
                                    binding.tvFollowersCount.text = "${author.followersCount}"
                                    binding.tvWorksCount.text = "${author.worksCount}"
                                    binding.tvTotalWords.text = "${author.totalWords}"

                                    if (author.avatar.isNotEmpty()) {
                                        Glide.with(this@AuthorProfileActivity)
                                            .load(UrlUtils.normalize(author.avatar))
                                            .placeholder(R.drawable.bg_avatar_placeholder)
                                            .circleCrop()
                                            .into(binding.ivAuthorAvatar)
                                    }
                                }
                            }
                            is AuthorProfileUiState.Error -> {
                                Toast.makeText(this@AuthorProfileActivity, state.message, Toast.LENGTH_SHORT).show()
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
                        worksItems.clear()
                        worksItems.addAll(works.map { article ->
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

        val parent = selected.parent as? android.widget.FrameLayout
        val unselectedParent = unselected.parent as? android.widget.FrameLayout
        parent?.removeView(indicator)
        unselectedParent?.removeView(indicator)
        parent?.addView(indicator)
    }

    private fun showDynamicList() {
        binding.rvAuthorContent.layoutManager = LinearLayoutManager(this)
        binding.rvAuthorContent.adapter = FeedActivityAdapter(
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
        binding.rvAuthorContent.layoutManager = LinearLayoutManager(this)
        binding.rvAuthorContent.adapter = BookAdapter(worksItems) { book ->
            if (book.bookId.isNotEmpty()) {
                BookDetailActivity.start(this, book.bookId)
            }
        }
    }
}
