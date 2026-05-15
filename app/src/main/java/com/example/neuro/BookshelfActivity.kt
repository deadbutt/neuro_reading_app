package com.example.neuro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.neuro.viewmodel.BookshelfUiState
import com.example.neuro.viewmodel.BookshelfViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BookshelfActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, BookshelfActivity::class.java))
        }
    }

    private val viewModel: BookshelfViewModel by viewModels()

    private var isEditMode = false
    private lateinit var tvEdit: TextView
    private lateinit var llEditBar: View
    private lateinit var llEmpty: View
    private lateinit var rv: RecyclerView
    private lateinit var adapter: BookshelfAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bookshelf)

        findViewById<View>(R.id.iv_shelf_back).setOnClickListener { finish() }

        tvEdit = findViewById(R.id.tv_shelf_edit)
        llEditBar = findViewById(R.id.ll_shelf_edit_bar)
        llEmpty = findViewById(R.id.ll_shelf_empty)
        rv = findViewById(R.id.rv_shelf)

        tvEdit.setOnClickListener { toggleEditMode() }

        setupRecyclerView()
        setupEditBar()
        setupTabs()
        observeViewModel()

        viewModel.loadBookshelf()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadBookshelf()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is BookshelfUiState.Error -> {
                                Toast.makeText(this@BookshelfActivity, state.message, Toast.LENGTH_SHORT).show()
                            }
                            is BookshelfUiState.Empty -> {
                                updateEmptyState(true)
                            }
                            is BookshelfUiState.Success -> {
                                updateEmptyState(false)
                            }
                            is BookshelfUiState.RemoveResult -> {
                                Toast.makeText(this@BookshelfActivity,
                                    "已删除 ${state.successCount} 本书", Toast.LENGTH_SHORT).show()
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    viewModel.books.collect { books ->
                        adapter.updateData(books, isEditMode)
                        updateEmptyState(books.isEmpty())
                    }
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            llEmpty.visibility = View.VISIBLE
            rv.visibility = View.GONE
        } else {
            llEmpty.visibility = View.GONE
            rv.visibility = View.VISIBLE
        }
    }

    private fun setupRecyclerView() {
        rv.layoutManager = LinearLayoutManager(this)
        adapter = BookshelfAdapter(isEditMode = false) { book, _ ->
            if (isEditMode) return@BookshelfAdapter
            if (book.bookId.isNotEmpty()) {
                ReaderActivity.start(this, book.bookId, book.chapterIndex, book.title)
            }
        }
        rv.adapter = adapter
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        tvEdit.text = if (isEditMode) getString(R.string.shelf_done) else getString(R.string.shelf_edit)
        llEditBar.visibility = if (isEditMode) View.VISIBLE else View.GONE
        adapter.updateData(viewModel.books.value, isEditMode)
    }

    private fun setupEditBar() {
        findViewById<View>(R.id.ll_select_all).setOnClickListener {
            val allSelected = viewModel.toggleSelectAll()
            adapter.updateData(viewModel.books.value, isEditMode)
        }

        findViewById<View>(R.id.btn_shelf_delete).setOnClickListener {
            val selected = viewModel.getSelectedBooks()
            if (selected.isEmpty()) {
                Toast.makeText(this, "请先选择要删除的书籍", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.removeBooks(selected)
            }
        }
    }

    private fun setupTabs() {
        findViewById<View>(R.id.tv_shelf_tab_read).setOnClickListener {
            Toast.makeText(this, "读过", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.tv_shelf_tab_subscribe).setOnClickListener {
            Toast.makeText(this, "订阅", Toast.LENGTH_SHORT).show()
        }
    }
}
