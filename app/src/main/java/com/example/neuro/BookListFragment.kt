package com.example.neuro

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.neuro.api.RetrofitClient
import kotlinx.coroutines.launch

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

    private var listType: Int = TYPE_RECOMMEND
    private var currentPage = 1
    private var isLoading = false
    private var hasMoreData = true

    private lateinit var rvBookList: RecyclerView
    private lateinit var srlBookList: SwipeRefreshLayout
    private lateinit var pbLoadMore: ProgressBar
    private lateinit var adapter: BookAdapter

    private val allBooks = mutableListOf<BookItem>()
    private var loadJob: kotlinx.coroutines.Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        listType = arguments?.getInt(ARG_LIST_TYPE, TYPE_RECOMMEND) ?: TYPE_RECOMMEND
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_book_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvBookList = view.findViewById(R.id.rv_book_list)
        srlBookList = view.findViewById(R.id.srl_book_list)
        pbLoadMore = view.findViewById(R.id.pb_load_more)

        setupRecyclerView()
        setupRefresh()
        setupLoadMore()
        loadData(true)
    }

    private fun setupRecyclerView() {
        rvBookList.layoutManager = LinearLayoutManager(requireContext())
        adapter = BookAdapter(allBooks) { book ->
            if (book.bookId.isNotEmpty()) {
                BookDetailActivity.start(requireContext(), book.bookId)
            }
        }
        rvBookList.adapter = adapter
    }

    private fun setupRefresh() {
        srlBookList.setColorSchemeResources(R.color.primary_red)
        srlBookList.setOnRefreshListener {
            currentPage = 1
            hasMoreData = true
            loadData(true)
        }
    }

    private fun setupLoadMore() {
        rvBookList.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy <= 0) return

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val totalCount = layoutManager.itemCount

                if (!isLoading && hasMoreData && lastVisible >= totalCount - 3) {
                    loadData(false)
                }
            }
        })
    }

    private fun loadData(isRefresh: Boolean) {
        if (isLoading) return
        if (!isRefresh && !hasMoreData) return
        isLoading = true

        loadJob?.cancel()

        if (isRefresh) {
            currentPage = 1
            hasMoreData = true
            srlBookList.isRefreshing = true
        } else {
            pbLoadMore.visibility = View.VISIBLE
        }

        loadJob = lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getArticles(page = currentPage, pageSize = 10)
                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = response.body()?.data
                    data?.let { page ->
                        val newItems = page.list.map { article ->
                            BookItem(
                                bookId = article.articleId,
                                title = article.title,
                                author = article.author,
                                desc = article.summary
                            )
                        }

                        if (isRefresh) {
                            allBooks.clear()
                        }

                        if (newItems.isEmpty()) {
                            hasMoreData = false
                            if (!isRefresh && isAdded) {
                                Toast.makeText(requireContext(), R.string.msg_no_more_data, Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            allBooks.addAll(newItems)
                            currentPage++
                            hasMoreData = page.hasMore
                        }

                        adapter.notifyDataSetChanged()
                    }
                } else if (isAdded) {
                    Toast.makeText(requireContext(), R.string.error_load_failed, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (isAdded) {
                    Toast.makeText(requireContext(), R.string.error_network, Toast.LENGTH_SHORT).show()
                }
            } finally {
                if (isAdded) {
                    srlBookList.isRefreshing = false
                    pbLoadMore.visibility = View.GONE
                }
                isLoading = false
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        loadJob?.cancel()
    }
}
