package com.example.neuro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.neuro.base.UiState
import com.example.neuro.databinding.FragmentBookListBinding
import com.example.neuro.viewmodel.BookListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BookListFragment : Fragment() {

    companion object {
        private const val ARG_LIST_TYPE = "list_type"

        const val TYPE_RECOMMEND = 0
        const val TYPE_HOT = 1
        const val TYPE_LATEST = 2

        fun newInstance(type: Int): BookListFragment {
            return BookListFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_LIST_TYPE, type)
                }
            }
        }
    }

    private var _binding: FragmentBookListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookListViewModel by viewModels()
    private var listType: Int = TYPE_RECOMMEND

    private lateinit var adapter: BookAdapter
    private val allBooks = mutableListOf<BookItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listType = arguments?.getInt(ARG_LIST_TYPE, TYPE_RECOMMEND) ?: TYPE_RECOMMEND
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBookListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupRefresh()
        setupLoadMore()
        observeViewModel()

        if (savedInstanceState == null) {
            viewModel.loadBooks(listType, isRefresh = true)
        }
    }

    private fun setupRecyclerView() {
        binding.rvBookList.apply {
            layoutManager = LinearLayoutManager(requireContext())
            setHasFixedSize(true)
            setItemViewCacheSize(20)
            recycledViewPool.setMaxRecycledViews(0, 20)
        }

        adapter = BookAdapter(allBooks) { book ->
            if (book.bookId.isNotEmpty()) {
                BookDetailActivity.start(requireContext(), book.bookId)
            }
        }
        binding.rvBookList.adapter = adapter
    }

    private fun setupRefresh() {
        binding.srlBookList.setColorSchemeResources(R.color.primary_red)
        binding.srlBookList.setOnRefreshListener {
            viewModel.loadBooks(listType, isRefresh = true)
        }
    }

    private fun setupLoadMore() {
        binding.rvBookList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val totalCount = layoutManager.itemCount

                if (lastVisible >= totalCount - 3) {
                    viewModel.loadBooks(listType, isRefresh = false)
                }
            }
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is UiState.Loading -> {
                                if (!binding.srlBookList.isRefreshing) {
                                    binding.pbLoadMore.visibility = View.VISIBLE
                                }
                            }
                            is UiState.Success -> {
                                binding.srlBookList.isRefreshing = false
                                binding.pbLoadMore.visibility = View.GONE
                            }
                            is UiState.Error -> {
                                binding.srlBookList.isRefreshing = false
                                binding.pbLoadMore.visibility = View.GONE
                                if (isAdded) {
                                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                                }
                            }
                            else -> {
                                binding.pbLoadMore.visibility = View.GONE
                            }
                        }
                    }
                }

                launch {
                    viewModel.books.collect { articles ->
                        val newItems = articles.map { article ->
                            BookItem(
                                bookId = article.articleId,
                                title = article.title,
                                author = article.author,
                                desc = article.summary,
                                coverUrl = article.cover?.replace(Constants.Network.PLACEHOLDER_IP, Constants.Network.REAL_IP) ?: ""
                            )
                        }

                        val isRefresh = allBooks.isEmpty() || newItems.size < allBooks.size
                        if (isRefresh) {
                            allBooks.clear()
                            allBooks.addAll(newItems)
                            adapter.notifyDataSetChanged()
                        } else {
                            val startPosition = allBooks.size
                            val itemsToAdd = newItems.drop(startPosition)
                            if (itemsToAdd.isNotEmpty()) {
                                allBooks.addAll(itemsToAdd)
                                adapter.notifyItemRangeInserted(startPosition, itemsToAdd.size)
                            }
                        }
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
