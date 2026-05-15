package com.example.neuro

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.neuro.databinding.FragmentBookshelfBinding
import com.example.neuro.viewmodel.BookshelfUiState
import com.example.neuro.viewmodel.BookshelfViewModel
import com.example.neuro.viewmodel.FeedViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BookshelfFragment : Fragment() {

    companion object {
        const val TAB_READING = 0
        const val TAB_FINISHED = 1
        const val TAB_SUBSCRIBE = 2
    }

    private var _binding: FragmentBookshelfBinding? = null
    private val binding get() = _binding!!

    private val bookshelfVM: BookshelfViewModel by viewModels()
    private val feedVM: FeedViewModel by viewModels()

    private var currentTab = TAB_READING
    private var isEditMode = false
    private lateinit var bookshelfAdapter: BookshelfAdapter
    private lateinit var feedAdapter: FeedActivityAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBookshelfBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupEditBar()
        setupTabs()
        observeBookshelf()
        observeFeed()

        binding.tvShelfEdit.setOnClickListener { toggleEditMode() }
    }

    override fun onResume() {
        super.onResume()
        if (currentTab == TAB_SUBSCRIBE) {
            feedVM.loadFeed(isRefresh = true)
        } else {
            bookshelfVM.loadBookshelf()
        }
    }

    private fun setupRecyclerView() {
        binding.rvShelf.layoutManager = LinearLayoutManager(requireContext())

        bookshelfAdapter = BookshelfAdapter(isEditMode = false) { book, _ ->
            if (isEditMode) return@BookshelfAdapter
            if (book.bookId.isNotEmpty()) {
                ReaderActivity.start(requireContext(), book.bookId, book.chapterIndex, book.title)
            }
        }

        feedAdapter = FeedActivityAdapter(emptyList(),
            onItemClick = { item ->
                item.bookId?.let { id ->
                    if (id.isNotEmpty()) {
                        BookDetailActivity.start(requireContext(), id)
                    }
                }
            },
            onLikeClick = { item, _ ->
                Toast.makeText(requireContext(), "点赞成功", Toast.LENGTH_SHORT).show()
            }
        )

        binding.rvShelf.adapter = bookshelfAdapter
    }

    private fun observeBookshelf() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    bookshelfVM.uiState.collect { state ->
                        when (state) {
                            is BookshelfUiState.Error -> {
                                if (currentTab != TAB_SUBSCRIBE) {
                                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                            is BookshelfUiState.RemoveResult -> {
                                Toast.makeText(requireContext(),
                                    "已删除 ${state.successCount} 本书", Toast.LENGTH_SHORT).show()
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    bookshelfVM.books.collect { items ->
                        if (currentTab != TAB_SUBSCRIBE) {
                            val filtered = if (currentTab == TAB_READING) {
                                items.filter { !it.isFinished || it.progress < 100 }
                            } else {
                                items.filter { it.isFinished && it.progress >= 100 }
                            }
                            bookshelfAdapter.updateData(filtered, isEditMode)
                            updateEmptyState(filtered.isEmpty())
                        }
                    }
                }
            }
        }
    }

    private fun observeFeed() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    feedVM.uiState.collect { state ->
                        if (currentTab != TAB_SUBSCRIBE) return@collect
                    }
                }

                launch {
                    feedVM.feedItems.collect { items ->
                        if (currentTab == TAB_SUBSCRIBE) {
                            val feedItems = items.map { r ->
                                FeedActivityItem(
                                    feedId = r.feedId,
                                    authorId = r.authorId,
                                    authorName = r.authorName,
                                    authorAvatar = r.authorAvatar,
                                    publishTime = r.publishTime,
                                    activityContent = r.activityContent,
                                    bookId = r.bookId,
                                    bookCover = r.bookCover ?: "",
                                    chapterPreview = r.chapterPreview ?: "",
                                    likeCount = formatCount(r.likeCount),
                                    commentCount = formatCount(r.commentCount),
                                    isLiked = r.isLiked
                                )
                            }
                            feedAdapter = FeedActivityAdapter(feedItems,
                                onItemClick = { item ->
                                    item.bookId?.let { id ->
                                        if (id.isNotEmpty()) {
                                            BookDetailActivity.start(requireContext(), id)
                                        }
                                    }
                                },
                                onLikeClick = { _, _ ->
                                    Toast.makeText(requireContext(), "点赞成功", Toast.LENGTH_SHORT).show()
                                }
                            )
                            binding.rvShelf.adapter = feedAdapter
                            updateEmptyState(items.isEmpty())
                        }
                    }
                }
            }
        }
    }

    private fun formatCount(count: Int): String {
        return when {
            count >= 10000 -> "${count / 10000}万"
            count >= 1000 -> "${count / 1000}k"
            else -> count.toString()
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            binding.llShelfEmpty.visibility = View.VISIBLE
            binding.rvShelf.visibility = View.GONE
        } else {
            binding.llShelfEmpty.visibility = View.GONE
            binding.rvShelf.visibility = View.VISIBLE
        }
    }

    private fun toggleEditMode() {
        if (currentTab == TAB_SUBSCRIBE) return
        isEditMode = !isEditMode
        binding.tvShelfEdit.text = if (isEditMode) "完成" else "编辑"
        binding.llShelfEditBar.visibility = if (isEditMode) View.VISIBLE else View.GONE
        bookshelfAdapter.updateData(bookshelfVM.books.value, isEditMode)
    }

    private fun setupEditBar() {
        binding.llSelectAll.setOnClickListener {
            val allSelected = bookshelfVM.toggleSelectAll()
            bookshelfAdapter.updateData(bookshelfVM.books.value, isEditMode)
        }

        binding.btnShelfDelete.setOnClickListener {
            val selected = bookshelfVM.getSelectedBooks()
            if (selected.isEmpty()) {
                Toast.makeText(requireContext(), "请先选择要删除的书籍", Toast.LENGTH_SHORT).show()
            } else {
                bookshelfVM.removeBooks(selected)
            }
        }
    }

    private fun setupTabs() {
        binding.tvShelfTabReading.setOnClickListener { switchTab(TAB_READING) }
        binding.tvShelfTabRead.setOnClickListener { switchTab(TAB_FINISHED) }
        binding.tvShelfTabSubscribe.setOnClickListener { switchTab(TAB_SUBSCRIBE) }
    }

    private fun switchTab(tab: Int) {
        if (tab == currentTab) return
        currentTab = tab

        isEditMode = false
        binding.tvShelfEdit.text = "编辑"
        binding.llShelfEditBar.visibility = View.GONE

        val ctx = requireContext()
        val tabs = listOf(
            binding.tvShelfTabReading to TAB_READING,
            binding.tvShelfTabRead to TAB_FINISHED,
            binding.tvShelfTabSubscribe to TAB_SUBSCRIBE
        )
        for ((tv, t) in tabs) {
            if (t == tab) {
                tv.setTextColor(ContextCompat.getColor(ctx, R.color.primary_red))
                tv.setTypeface(null, Typeface.BOLD)
            } else {
                tv.setTextColor(ContextCompat.getColor(ctx, R.color.tab_inactive))
                tv.setTypeface(null, Typeface.NORMAL)
            }
        }

        binding.tvShelfEdit.visibility = if (tab == TAB_SUBSCRIBE) View.GONE else View.VISIBLE

        when (tab) {
            TAB_READING -> {
                val items = bookshelfVM.books.value.filter { !it.isFinished || it.progress < 100 }
                binding.rvShelf.adapter = bookshelfAdapter
                bookshelfAdapter.updateData(items, false)
                updateEmptyState(items.isEmpty())
            }
            TAB_FINISHED -> {
                val items = bookshelfVM.books.value.filter { it.isFinished && it.progress >= 100 }
                binding.rvShelf.adapter = bookshelfAdapter
                bookshelfAdapter.updateData(items, false)
                updateEmptyState(items.isEmpty())
            }
            TAB_SUBSCRIBE -> {
                feedVM.loadFeed(isRefresh = true)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
