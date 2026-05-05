package com.example.neuro

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.neuro.api.RetrofitClient
import kotlinx.coroutines.launch

class SearchFragment : Fragment() {

    private val searchResults = mutableListOf<BookItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.tv_search_cancel).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        setupSearchInput(view)
        setupHistoryActions(view)
    }

    private fun setupSearchInput(view: View) {
        val etSearch = view.findViewById<EditText>(R.id.et_search)
        val ivClear = view.findViewById<View>(R.id.iv_search_clear)

        etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val hasText = !s.isNullOrEmpty()
                ivClear.visibility = if (hasText) View.VISIBLE else View.GONE

                if (hasText) {
                    performSearch(view, s.toString())
                } else {
                    showInitialState(view)
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        ivClear.setOnClickListener { etSearch.text.clear() }

        etSearch.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun performSearch(view: View, query: String) {
        saveSearchHistory(query)
        view.findViewById<View>(R.id.ll_search_history).visibility = View.GONE
        view.findViewById<View>(R.id.ll_search_hot).visibility = View.GONE
        view.findViewById<View>(R.id.ll_search_result).visibility = View.VISIBLE

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
                                desc = article.summary
                            )
                        })
                        showSearchResults(view)
                    }
                } else {
                    Toast.makeText(requireContext(), "搜索失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "网络错误", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSearchResults(view: View) {
        val resultRv = view.findViewById<RecyclerView>(R.id.rv_search_result)
        val emptyView = view.findViewById<View>(R.id.ll_search_empty)

        if (searchResults.isEmpty()) {
            emptyView.visibility = View.VISIBLE
            resultRv.visibility = View.GONE
        } else {
            emptyView.visibility = View.GONE
            resultRv.visibility = View.VISIBLE
            resultRv.layoutManager = LinearLayoutManager(requireContext())
            resultRv.adapter = BookAdapter(searchResults) { book ->
                if (book.bookId.isNotEmpty()) {
                    BookDetailActivity.start(requireContext(), book.bookId)
                }
            }
        }
    }

    private fun showInitialState(view: View) {
        view.findViewById<View>(R.id.ll_search_result).visibility = View.GONE
        view.findViewById<View>(R.id.ll_search_hot).visibility = View.VISIBLE
        view.findViewById<View>(R.id.ll_search_history).visibility = View.VISIBLE
    }

    private fun setupHistoryActions(view: View) {
        view.findViewById<View>(R.id.ll_clear_history).setOnClickListener {
            clearHistory(view)
        }

        loadHistoryTags(view)
    }

    private fun clearHistory(view: View) {
        val prefs = requireContext().getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
        view.findViewById<View>(R.id.ll_search_history).visibility = View.GONE
        Toast.makeText(requireContext(), R.string.msg_history_cleared, Toast.LENGTH_SHORT).show()
    }

    private fun loadHistoryTags(view: View) {
        val tagsContainer = view.findViewById<LinearLayout>(R.id.ll_history_tags)
        tagsContainer.removeAllViews()

        val historyTags = getSearchHistory()
        if (historyTags.isEmpty()) {
            view.findViewById<View>(R.id.ll_search_history).visibility = View.GONE
            return
        }

        historyTags.forEach { tag ->
            val tv = TextView(requireContext()).apply {
                text = tag
                setTextColor(requireContext().getColor(R.color.tab_inactive))
                textSize = 13f
                background = requireContext().getDrawable(R.drawable.bg_search_history_tag)
                setPadding(48, 12, 48, 12)
                setOnClickListener {
                    view.findViewById<EditText>(R.id.et_search).setText(tag)
                }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 12
                bottomMargin = 12
            }
            tagsContainer.addView(tv, params)
        }
    }

    private fun getSearchHistory(): List<String> {
        val prefs = requireContext().getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
        val historyString = prefs.getString(KEY_HISTORY, "") ?: ""
        return if (historyString.isEmpty()) emptyList() else historyString.split(",").filter { it.isNotEmpty() }
    }

    private fun saveSearchHistory(query: String) {
        if (query.isBlank()) return
        val prefs = requireContext().getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
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
    }
}
