package com.example.neuro

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
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
import com.example.neuro.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BookshelfFragment : Fragment() {

    companion object {
        const val TAB_ALL = 0
        const val TAB_FAVORITE = 1
        const val TAB_FOLLOWING = 2
    }

    private var _binding: FragmentBookshelfBinding? = null
    private val binding get() = _binding!!

    private val bookshelfVM: BookshelfViewModel by viewModels()
    private val userVM: UserViewModel by viewModels()

    private var currentTab = TAB_ALL
    private var isEditMode = false
    private lateinit var bookshelfAdapter: BookshelfAdapter
    private lateinit var followingAdapter: FollowingAdapter

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
        observeFollowing()

        binding.tvShelfEdit.setOnClickListener { toggleEditMode() }
    }

    override fun onResume() {
        super.onResume()
        if (currentTab == TAB_FOLLOWING) {
            userVM.getFollowingList()
        } else {
            val category = if (currentTab == TAB_FAVORITE) "favorite" else "all"
            bookshelfVM.loadBookshelf(category)
        }
    }

    private fun setupRecyclerView() {
        binding.rvShelf.layoutManager = LinearLayoutManager(requireContext())

        bookshelfAdapter = BookshelfAdapter(isEditMode = false) { book, pos ->
            Log.d("BookshelfFragment", "=== onItemClick called ===")
            Log.d("BookshelfFragment", "bookId=${book.bookId}, title=${book.title}, pos=$pos")
            Log.d("BookshelfFragment", "isEditMode=$isEditMode")
            if (isEditMode) {
                Log.d("BookshelfFragment", "edit mode, skip")
                return@BookshelfAdapter
            }
            if (book.bookId.isEmpty()) {
                Log.d("BookshelfFragment", "bookId empty, skip")
                return@BookshelfAdapter
            }
            try {
                Log.d("BookshelfFragment", "starting BookDetailActivity...")
                BookDetailActivity.start(requireContext(), book.bookId)
                Log.d("BookshelfFragment", "BookDetailActivity.start called successfully")
            } catch (e: Exception) {
                Log.e("BookshelfFragment", "Failed to start BookDetailActivity", e)
                Toast.makeText(requireContext(), "跳转失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        followingAdapter = FollowingAdapter { item ->
            AuthorProfileActivity.start(requireContext(), item.authorId)
        }

        binding.rvShelf.adapter = bookshelfAdapter
    }

    private fun observeBookshelf() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    bookshelfVM.uiState.collect { state ->
                        when (state) {
                            is BookshelfUiState.Error -> {
                                if (currentTab != TAB_FOLLOWING) {
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
                        Log.d("BookshelfFragment", "books collected: size=${items.size}")
                        if (currentTab != TAB_FOLLOWING) {
                            bookshelfAdapter.updateData(items, isEditMode)
                            updateEmptyState(items.isEmpty())
                        }
                    }
                }
            }
        }
    }

    private fun observeFollowing() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    userVM.followingList.collect { authors ->
                        if (currentTab == TAB_FOLLOWING) {
                            val items = authors.map { a ->
                                FollowingItem(
                                    authorId = a.authorId,
                                    authorName = a.name,
                                    authorAvatar = a.avatar
                                )
                            }
                            followingAdapter.updateData(items)
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
        if (currentTab == TAB_FOLLOWING) return
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
        binding.tvShelfTabReading.setOnClickListener { switchTab(TAB_ALL) }
        binding.tvShelfTabRead.setOnClickListener { switchTab(TAB_FAVORITE) }
        binding.tvShelfTabSubscribe.setOnClickListener { switchTab(TAB_FOLLOWING) }
    }

    private fun switchTab(tab: Int) {
        if (tab == currentTab) return
        currentTab = tab

        isEditMode = false
        binding.tvShelfEdit.text = "编辑"
        binding.llShelfEditBar.visibility = View.GONE

        val ctx = requireContext()

        val tabs = listOf(
            Triple(binding.tvShelfTabReading, binding.vShelfIndicatorAll, TAB_ALL),
            Triple(binding.tvShelfTabRead, binding.vShelfIndicatorFavorite, TAB_FAVORITE),
            Triple(binding.tvShelfTabSubscribe, binding.vShelfIndicatorSubscribe, TAB_FOLLOWING)
        )
        for ((tv, indicator, t) in tabs) {
            if (t == tab) {
                tv.setTextColor(ContextCompat.getColor(ctx, R.color.primary_red))
                tv.setTypeface(null, Typeface.BOLD)
                indicator.visibility = View.VISIBLE
            } else {
                tv.setTextColor(ContextCompat.getColor(ctx, R.color.tab_inactive))
                tv.setTypeface(null, Typeface.NORMAL)
                indicator.visibility = View.INVISIBLE
            }
        }

        binding.tvShelfEdit.visibility = if (tab == TAB_FOLLOWING) View.GONE else View.VISIBLE

        when (tab) {
            TAB_ALL -> {
                binding.rvShelf.adapter = bookshelfAdapter
                bookshelfAdapter.updateData(bookshelfVM.books.value, isEditMode)
                updateEmptyState(bookshelfVM.books.value.isEmpty())
                bookshelfVM.loadBookshelf("all")
            }
            TAB_FAVORITE -> {
                binding.rvShelf.adapter = bookshelfAdapter
                bookshelfAdapter.updateData(bookshelfVM.books.value, isEditMode)
                updateEmptyState(bookshelfVM.books.value.isEmpty())
                bookshelfVM.loadBookshelf("favorite")
            }
            TAB_FOLLOWING -> {
                binding.rvShelf.adapter = followingAdapter
                userVM.getFollowingList()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
