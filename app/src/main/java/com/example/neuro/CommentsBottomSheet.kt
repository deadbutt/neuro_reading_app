package com.example.neuro

import android.content.Context
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.neuro.api.model.CommentResponse
import com.example.neuro.base.UiState
import com.example.neuro.databinding.FragmentCommentsBottomSheetBinding
import com.example.neuro.viewmodel.CommentsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CommentsBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_ARTICLE_ID = "article_id"

        const val SORT_INDEX_HOT = 0
        const val SORT_INDEX_NEW = 1

        fun newInstance(articleId: String): CommentsBottomSheet {
            return CommentsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_ARTICLE_ID, articleId)
                }
            }
        }
    }

    private var _binding: FragmentCommentsBottomSheetBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CommentsViewModel by viewModels()
    private lateinit var articleId: String

    private val comments = mutableListOf<CommentItem>()
    private lateinit var adapter: CommentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        articleId = arguments?.getString(ARG_ARTICLE_ID) ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCommentsBottomSheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSortTabs()
        setupRecyclerView()
        setupSwipeRefresh()
        setupSendButton()
        observeViewModel()

        viewModel.loadComments(articleId)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? com.google.android.material.bottomsheet.BottomSheetDialog
        dialog?.let {
            val bottomSheet = it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet)
                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                behavior.isDraggable = false
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is UiState.Loading -> {}
                            is UiState.Success -> {
                                binding.srlComments.isRefreshing = false
                            }
                            is UiState.Error -> {
                                binding.srlComments.isRefreshing = false
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    viewModel.comments.collect { newComments ->
                        comments.clear()
                        comments.addAll(newComments.map { it.toCommentItem() })
                        adapter.notifyDataSetChanged()

                        binding.tvCommentCount.text = "${newComments.size}条"
                        if (newComments.isEmpty()) {
                            binding.llEmptyState.visibility = View.VISIBLE
                        } else {
                            binding.llEmptyState.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun CommentResponse.toCommentItem(): CommentItem {
        return CommentItem(
            commentId = this.commentId,
            name = this.userName,
            avatarUrl = com.example.neuro.util.UrlUtils.normalize(this.userAvatar),
            time = this.createTime,
            content = this.content,
            likes = formatCount(this.likeCount),
            likeCount = this.likeCount,
            isLiked = this.isLiked,
            isAuthor = false,
            replyCount = this.replyCount,
            replies = this.replies ?: emptyList()
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
        val tabs = listOf(binding.tvSortHot, binding.tvSortNew)
        tabs.forEachIndexed { index, tv ->
            tv.setOnClickListener { selectSort(index) }
        }
    }

    private fun selectSort(index: Int) {
        val sort = when (index) {
            SORT_INDEX_HOT -> Constants.CommentSort.HOT
            SORT_INDEX_NEW -> Constants.CommentSort.NEW
            else -> Constants.CommentSort.HOT
        }

        val tabs = listOf(binding.tvSortHot, binding.tvSortNew)
        for ((i, tv) in tabs.withIndex()) {
            if (i == index) {
                tv.setTextColor(requireContext().getColor(R.color.primary_red))
                tv.setTypeface(null, Typeface.BOLD)
            } else {
                tv.setTextColor(requireContext().getColor(R.color.tab_inactive))
                tv.setTypeface(null, Typeface.NORMAL)
            }
        }
        viewModel.setSort(sort)
        viewModel.loadComments(articleId)
    }

    private fun setupRecyclerView() {
        binding.rvComments.layoutManager = LinearLayoutManager(requireContext())
        adapter = CommentAdapter(comments,
            onLikeClick = { comment, position ->
                likeComment(comment, position)
            },
            onReplyClick = { comment ->
                showReplyInput(comment)
            },
            onViewMoreReplies = { comment, position ->
                Toast.makeText(requireContext(), "查看更多回复功能开发中", Toast.LENGTH_SHORT).show()
            }
        )
        binding.rvComments.adapter = adapter
    }

    private fun likeComment(comment: CommentItem, position: Int) {
        val responseComment = viewModel.comments.value.getOrNull(position) ?: return
        viewModel.toggleLike(responseComment.commentId, responseComment.isLiked)
    }

    private fun showReplyInput(comment: CommentItem) {
        binding.etCommentInput.hint = "回复 ${comment.name}:"
        binding.etCommentInput.tag = comment.commentId
        binding.etCommentInput.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etCommentInput, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun setupSwipeRefresh() {
        binding.srlComments.setOnRefreshListener {
            viewModel.refreshComments(articleId)
        }
    }

    private fun setupSendButton() {
        binding.btnSend.setOnClickListener {
            val text = binding.etCommentInput.text.toString().trim()
            if (text.isNotEmpty()) {
                val parentId = binding.etCommentInput.tag as? String
                viewModel.postComment(articleId, text, parentId) { success, message ->
                    if (success) {
                        binding.etCommentInput.text.clear()
                        binding.etCommentInput.hint = getString(R.string.comments_hint)
                        binding.etCommentInput.tag = null
                        Toast.makeText(requireContext(), R.string.msg_comment_sent, Toast.LENGTH_SHORT).show()
                        // 刷新评论列表以显示新回复
                        viewModel.refreshComments(articleId)
                    } else {
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
