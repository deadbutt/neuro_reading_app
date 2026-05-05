package com.example.neuro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.neuro.api.RetrofitClient
import kotlinx.coroutines.launch

class SearchActivity : AppCompatActivity() {

    private val searchResults = mutableListOf<BookItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        findViewById<View>(R.id.tv_search_cancel).setOnClickListener { finish() }

        setupSearchInput()
        setupHistoryActions()
    }

    private fun setupSearchInput() {
        val etSearch = findViewById<EditText>(R.id.et_search)
        val ivClear = findViewById<View>(R.id.iv_search_clear)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val hasText = !s.isNullOrEmpty()
                ivClear.visibility = if (hasText) View.VISIBLE else View.GONE

                if (hasText) {
                    performSearch(s.toString())
                } else {
                    showInitialState()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        ivClear.setOnClickListener { etSearch.text.clear() }
    }

    private fun performSearch(query: String) {
        saveSearchHistory(query)
        findViewById<View>(R.id.ll_search_history).visibility = View.GONE
        findViewById<View>(R.id.ll_search_hot).visibility = View.GONE
        findViewById<View>(R.id.ll_search_result).visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.searchArticles(keyword = query)
                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = response.body()?.data
                    data?.let { articles ->
                        searchResults.clear()
                        searchResults.addAll(articles.map { article ->
                            BookItem(
                                bookId = article.articleId,
                                title = article.title,
                                author = article.author,
                                desc = article.summary,
                                hotText = "${article.wordCount}字 · ${article.chapterCount}章"
                            )
                        })
                        showSearchResults()
                    }
                } else {
                    Toast.makeText(this@SearchActivity, "搜索失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SearchActivity, "网络错误", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSearchResults() {
        val resultRv = findViewById<RecyclerView>(R.id.rv_search_result)
        val emptyView = findViewById<View>(R.id.ll_search_empty)

        if (searchResults.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            resultRv.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            resultRv.visibility = View.VISIBLE
            resultRv.layoutManager = LinearLayoutManager(this)
            resultRv.adapter = BookAdapter(searchResults) { book ->
                if (book.bookId.isNotEmpty()) {
                    BookDetailActivity.start(this, book.bookId)
                }
            }
        }
    }

    private fun showInitialState() {
        findViewById<View>(R.id.ll_search_result).visibility = View.GONE
        findViewById<View>(R.id.ll_search_hot).visibility = View.VISIBLE
        findViewById<View>(R.id.ll_search_history).visibility = View.VISIBLE
    }

    private fun setupHistoryActions() {
        findViewById<View>(R.id.ll_clear_history).setOnClickListener {
            clearHistory()
        }

        loadHistoryTags()
    }

    private fun clearHistory() {
        findViewById<View>(R.id.ll_search_history).visibility = View.GONE
        Toast.makeText(this, R.string.msg_history_cleared, Toast.LENGTH_SHORT).show()
    }

    private fun loadHistoryTags() {
        val tagsContainer = findViewById<android.widget.LinearLayout>(R.id.ll_history_tags)
        tagsContainer.removeAllViews()

        val historyTags = getSearchHistory()
        if (historyTags.isEmpty()) {
            findViewById<View>(R.id.ll_search_history).visibility = View.GONE
            return
        }

        historyTags.forEach { tag ->
            val tv = android.widget.TextView(this).apply {
                text = tag
                setTextColor(getColor(R.color.tab_inactive))
                textSize = 13f
                background = getDrawable(R.drawable.bg_search_history_tag)
                setPadding(24, 12, 24, 12)
                setOnClickListener {
                    findViewById<EditText>(R.id.et_search).setText(tag)
                }
            }
            val params = android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 16
                bottomMargin = 12
            }
            tagsContainer.addView(tv, params)
        }
    }

    private fun getSearchHistory(): List<String> {
        val prefs = getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
        val historyString = prefs.getString(KEY_HISTORY, "") ?: ""
        return if (historyString.isEmpty()) emptyList() else historyString.split(",").filter { it.isNotEmpty() }
    }

    private fun saveSearchHistory(query: String) {
        if (query.isBlank()) return
        val prefs = getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
        val history = getSearchHistory().toMutableList()
        history.remove(query)
        history.add(0, query)
        if (history.size > MAX_HISTORY_SIZE) history.subList(MAX_HISTORY_SIZE, history.size).clear()
        prefs.edit().putString(KEY_HISTORY, history.joinToString(",")).apply()
    }

    companion object {
        private const val PREFS_SEARCH = "search_prefs"
        private const val KEY_HISTORY = "search_history"
        private const val MAX_HISTORY_SIZE = 10

        fun start(context: Context) {
            context.startActivity(Intent(context, SearchActivity::class.java))
        }
    }
}
