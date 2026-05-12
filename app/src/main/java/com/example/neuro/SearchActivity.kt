package com.example.neuro

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.neuro.base.UiState
import com.example.neuro.viewmodel.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchActivity : AppCompatActivity() {

    private val viewModel: SearchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        findViewById<View>(R.id.tv_search_cancel).setOnClickListener { finish() }

        viewModel.setHistory(loadHistory())
        observeViewModel()
        setupSearchInput()
        setupHistoryActions()
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is UiState.Error -> {
                                Toast.makeText(this@SearchActivity, state.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    viewModel.searchResults.collect { results ->
                        showSearchResults(results)
                    }
                }

                launch {
                    viewModel.history.collect { history ->
                        saveHistoryToPrefs(history)
                        renderHistoryTags(history)
                    }
                }
            }
        }
    }

    private fun setupSearchInput() {
        val etSearch = findViewById<EditText>(R.id.et_search)
        val ivClear = findViewById<View>(R.id.iv_search_clear)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val hasText = !s.isNullOrEmpty()
                ivClear.visibility = if (hasText) View.VISIBLE else View.GONE

                if (hasText) {
                    val query = s.toString()
                    findViewById<View>(R.id.ll_search_history).visibility = View.GONE
                    findViewById<View>(R.id.ll_search_hot).visibility = View.GONE
                    findViewById<View>(R.id.ll_search_result).visibility = View.VISIBLE
                    viewModel.updateHistory(query)
                    viewModel.search(query)
                } else {
                    showInitialState()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        ivClear.setOnClickListener { etSearch.text.clear() }
    }

    private fun showSearchResults(results: List<BookItem>) {
        val resultRv = findViewById<RecyclerView>(R.id.rv_search_result)
        val emptyView = findViewById<View>(R.id.ll_search_empty)

        if (results.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            resultRv.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            resultRv.visibility = View.VISIBLE
            resultRv.layoutManager = LinearLayoutManager(this)
            resultRv.adapter = BookAdapter(results) { book ->
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
            viewModel.clearHistory()
        }
    }

    private fun renderHistoryTags(history: List<String>) {
        val tagsContainer = findViewById<android.widget.LinearLayout>(R.id.ll_history_tags)
        val historyContainer = findViewById<View>(R.id.ll_search_history)

        if (history.isEmpty()) {
            historyContainer.visibility = View.GONE
            return
        }
        historyContainer.visibility = View.VISIBLE

        tagsContainer.removeAllViews()
        history.forEach { tag ->
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

    private fun loadHistory(): List<String> {
        val historyString = getPrefs().getString(KEY_HISTORY, "") ?: ""
        return if (historyString.isEmpty()) emptyList()
        else historyString.split(",").filter { it.isNotEmpty() }
    }

    private fun saveHistoryToPrefs(history: List<String>) {
        getPrefs().edit().putString(KEY_HISTORY, history.joinToString(",")).apply()
    }

    private fun getPrefs(): SharedPreferences {
        return getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
    }

    companion object {
        private const val PREFS_SEARCH = "search_prefs"
        private const val KEY_HISTORY = "search_history"

        fun start(context: Context) {
            context.startActivity(Intent(context, SearchActivity::class.java))
        }
    }
}
