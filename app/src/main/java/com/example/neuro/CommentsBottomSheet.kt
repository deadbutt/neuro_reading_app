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
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.neuro.api.RetrofitClient
import com.example.neuro.api.model.CommentResponse
import com.example.neuro.api.model.PostCommentRequest
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class CommentsBottomSheet : BottomSheetDialogFragment() {

    companion object {
        private const val ARG_ARTICLE_ID = "article_id"

        const val SORT_INDEX_HOT = 0
        const val SORT_INDEX_NEW = 1
        const val SORT_INDEX_AUTHOR = 2

        fun newInstance(articleId: String): CommentsBottomSheet {
            return CommentsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString(ARG_ARTICLE_ID, articleId)
                }
            }
        }
    }

    private lateinit var articleId: String
    private lateinit var tvSortHot: TextView
    private lateinit var tvSortNew: TextView
    private lateinit var tvSortAuthor: TextView
    private lateinit var vIndicatorHot: View
    private var currentSort: String = Constants.CommentSort.HOT
    private var currentPage: Int = 1
    private val comments = mutableListOf<CommentItem>()
    private lateinit var adapter: CommentAdapter
    private lateinit var rv: RecyclerView
    private lateinit var srl: SwipeRefreshLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        articleId = arguments?.getString(ARG_ARTICLE_ID) ?: ""
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_comments_bottom_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tvSortHot = view.findViewById(R.id.tv_sort_hot)
        tvSortNew = view.findViewById(R.id.tv_sort_new)
        tvSortAuthor = view.findViewById(R.id.tv_sort_author)
        vIndicatorHot = view.findViewById(R.id.v_indicator_hot)

        setupSortTabs()
        setupRecyclerView(view)
        setupSwipeRefresh(view)
        setupSendButton(view)

        loadComments()
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? com.google.android.material.bottomsheet.BottomSheetDialog
        dialog?.let {
            val bottomSheet = it.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let { sheet ->
                val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(sheet)
                behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            }
        }
        view?.postDelayed({
            val etInput = view?.findViewById<EditText>(R.id.et_comment_input)
            etInput?.requestFocus()
            val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(etInput, InputMethodManager.SHOW_IMPLICIT)
        }, 300)
    }

    private fun loadComments() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getArticleComments(
                    articleId = articleId,
                    page = currentPage,
                    sort = currentSort
                )
                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = response.body()?.data
                    data?.let { page ->
                        val newComments = page.list.map { it.toCommentItem() }
                        if (currentPage == 1) {
                            comments.clear()
                        }
                        comments.addAll(newComments)
                        adapter.notifyDataSetChanged()
                    }
                } else {
                    Toast.makeText(requireContext(), R.string.error_load_comments_failed, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_SHORT).show()
            } finally {
                srl.isRefreshing = false
            }
        }
    }

    private fun CommentResponse.toCommentItem(): CommentItem {
        return CommentItem(
            name = this.userName,
            avatarUrl = this.userAvatar.replace(Constants.Network.PLACEHOLDER_IP, Constants.Network.REAL_IP),
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
            SORT_INDEX_HOT -> Constants.CommentSort.HOT
            SORT_INDEX_NEW -> Constants.CommentSort.NEW
            SORT_INDEX_AUTHOR -> Constants.CommentSort.AUTHOR
            else -> Constants.CommentSort.HOT
        }
        currentPage = 1
        val tabs = listOf(tvSortHot, tvSortNew, tvSortAuthor)
        for ((i, tv) in tabs.withIndex()) {
            if (i == index) {
                tv.setTextColor(requireContext().getColor(R.color.primary_red))
                tv.setTypeface(null, Typeface.BOLD)
            } else {
                tv.setTextColor(requireContext().getColor(R.color.tab_inactive))
                tv.setTypeface(null, Typeface.NORMAL)
            }
        }
        loadComments()
    }

    private fun setupRecyclerView(view: View) {
        rv = view.findViewById(R.id.rv_comments)
        rv.layoutManager = LinearLayoutManager(requireContext())
        adapter = CommentAdapter(comments,
            onLikeClick = { comment, position ->
                likeComment(comment, position)
            },
            onReplyClick = {
                Toast.makeText(requireContext(), R.string.msg_reply_feature, Toast.LENGTH_SHORT).show()
            }
        )
        rv.adapter = adapter
    }

    private fun likeComment(comment: CommentItem, position: Int) {
        Toast.makeText(requireContext(), R.string.msg_like_success, Toast.LENGTH_SHORT).show()
    }

    private fun setupSwipeRefresh(view: View) {
        srl = view.findViewById(R.id.srl_comments)
        srl.setOnRefreshListener {
            currentPage = 1
            loadComments()
        }
    }

    private fun setupSendButton(view: View) {
        view.findViewById<View>(R.id.btn_send).setOnClickListener {
            val input = view.findViewById<EditText>(R.id.et_comment_input)
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
                    Toast.makeText(requireContext(), R.string.msg_comment_sent, Toast.LENGTH_SHORT).show()
                    input.text.clear()
                    currentPage = 1
                    loadComments()
                } else {
                    Toast.makeText(requireContext(), R.string.error_send_failed, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
