package com.example.neuro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.neuro.api.RetrofitClient
import kotlinx.coroutines.launch

class BookshelfActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, BookshelfActivity::class.java))
        }
    }

    private var isEditMode = false
    private lateinit var adapter: BookshelfAdapter
    private lateinit var tvEdit: TextView
    private lateinit var llEditBar: View
    private lateinit var llEmpty: View
    private lateinit var rv: RecyclerView

    private val books = mutableListOf<ShelfItem>()

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

        loadBookshelf()
    }

    override fun onResume() {
        super.onResume()
        loadBookshelf()
    }

    private fun loadBookshelf() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getBookshelf()
                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = response.body()?.data
                    data?.let { page ->
                        val list = page.list ?: emptyList()
                        val shelfItems = list.map { item ->
                            ShelfItem(
                                bookId = item.articleId,
                                title = item.title,
                                author = item.author,
                                tag = "书架",
                                progress = item.progress,
                                coverUrl = item.cover,
                                lastReadChapter = item.lastReadChapter
                            )
                        }
                        books.clear()
                        books.addAll(shelfItems)
                        adapter.updateData(books)
                        updateEmptyState()
                    }
                } else {
                    Toast.makeText(this@BookshelfActivity, "加载书架失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@BookshelfActivity, "网络错误", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateEmptyState() {
        if (books.isEmpty()) {
            llEmpty.visibility = View.VISIBLE
            rv.visibility = View.GONE
        } else {
            llEmpty.visibility = View.GONE
            rv.visibility = View.VISIBLE
        }
    }

    private fun setupRecyclerView() {
        rv.layoutManager = LinearLayoutManager(this)
        adapter = BookshelfAdapter(books, isEditMode = false) { book, _ ->
            if (isEditMode) return@BookshelfAdapter
            if (book.bookId.isNotEmpty()) {
                BookDetailActivity.start(this, book.bookId)
            }
        }
        rv.adapter = adapter
    }

    private fun toggleEditMode() {
        isEditMode = !isEditMode
        tvEdit.text = if (isEditMode) getString(R.string.shelf_done) else getString(R.string.shelf_edit)
        llEditBar.visibility = if (isEditMode) View.VISIBLE else View.GONE
        adapter = BookshelfAdapter(books, isEditMode = isEditMode) { book, _ ->
            if (isEditMode) return@BookshelfAdapter
            if (book.bookId.isNotEmpty()) {
                BookDetailActivity.start(this, book.bookId)
            }
        }
        rv.adapter = adapter
    }

    private fun setupEditBar() {
        findViewById<View>(R.id.ll_select_all).setOnClickListener {
            adapter.toggleSelectAll()
        }

        findViewById<View>(R.id.btn_shelf_delete).setOnClickListener {
            val selected = adapter.getSelectedBooks()
            if (selected.isEmpty()) {
                Toast.makeText(this, "请先选择要删除的书籍", Toast.LENGTH_SHORT).show()
            } else {
                removeSelectedFromBookshelf(selected)
            }
        }
    }

    private fun removeSelectedFromBookshelf(selected: List<ShelfItem>) {
        lifecycleScope.launch {
            try {
                var successCount = 0
                for (book in selected) {
                    val response = RetrofitClient.apiService.removeFromBookshelf(book.bookId)
                    if (response.isSuccessful && response.body()?.code == 0) {
                        successCount++
                    }
                }
                books.removeAll(selected)
                adapter.updateData(books)
                updateEmptyState()
                Toast.makeText(this@BookshelfActivity, "已删除 ${successCount} 本书", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(this@BookshelfActivity, "删除失败", Toast.LENGTH_SHORT).show()
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
