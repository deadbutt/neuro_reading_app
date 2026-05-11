package com.example.neuro

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuro.databinding.FragmentSearchBinding
import com.example.neuro.viewmodel.SearchUiState
import com.example.neuro.viewmodel.SearchViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private val searchResults = mutableListOf<BookItem>()
    private lateinit var adapter: BookAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSearchInput()
        setupHistoryActions()
        observeViewModel()

        binding.tvSearchCancel.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupRecyclerView() {
        binding.rvSearchResult.layoutManager = LinearLayoutManager(requireContext())
        adapter = BookAdapter(searchResults) { book ->
            if (book.bookId.isNotEmpty()) {
                BookDetailActivity.start(requireContext(), book.bookId)
            }
        }
        binding.rvSearchResult.adapter = adapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is SearchUiState.Loading -> {
                                binding.llSearchEmpty.visibility = View.GONE
                            }
                            is SearchUiState.Success -> {
                                // Handled by searchResults collection
                            }
                            is SearchUiState.Error -> {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    viewModel.searchResults.collect { articles ->
                        searchResults.clear()
                        searchResults.addAll(articles.map { article ->
                            BookItem(
                                bookId = article.articleId,
                                title = article.title,
                                author = article.author,
                                desc = article.summary
                            )
                        })
                        adapter.notifyDataSetChanged()
                        updateSearchResultVisibility()
                    }
                }
            }
        }
    }

    private fun updateSearchResultVisibility() {
        if (searchResults.isEmpty()) {
            binding.llSearchEmpty.visibility = View.VISIBLE
            binding.rvSearchResult.visibility = View.GONE
        } else {
            binding.llSearchEmpty.visibility = View.GONE
            binding.rvSearchResult.visibility = View.VISIBLE
        }
    }

    private fun setupSearchInput() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val hasText = !s.isNullOrEmpty()
                binding.ivSearchClear.visibility = if (hasText) View.VISIBLE else View.GONE

                if (hasText) {
                    performSearch(s.toString())
                } else {
                    showInitialState()
                }
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.ivSearchClear.setOnClickListener { binding.etSearch.text.clear() }

        binding.etSearch.requestFocus()
        val imm = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(binding.etSearch, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun performSearch(query: String) {
        saveSearchHistory(query)
        binding.llSearchHistory.visibility = View.GONE
        binding.llSearchHot.visibility = View.GONE
        binding.llSearchResult.visibility = View.VISIBLE
        viewModel.search(query)
    }

    private fun showInitialState() {
        binding.llSearchResult.visibility = View.GONE
        binding.llSearchHot.visibility = View.VISIBLE
        binding.llSearchHistory.visibility = View.VISIBLE
    }

    private fun setupHistoryActions() {
        binding.llClearHistory.setOnClickListener {
            clearHistory()
        }

        loadHistoryTags()
    }

    private fun clearHistory() {
        val prefs = requireContext().getSharedPreferences(PREFS_SEARCH, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
        binding.llSearchHistory.visibility = View.GONE
        Toast.makeText(requireContext(), R.string.msg_history_cleared, Toast.LENGTH_SHORT).show()
    }

    private fun loadHistoryTags() {
        binding.llHistoryTags.removeAllViews()

        val historyTags = getSearchHistory()
        if (historyTags.isEmpty()) {
            binding.llSearchHistory.visibility = View.GONE
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
                    binding.etSearch.setText(tag)
                }
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                marginEnd = 12
                bottomMargin = 12
            }
            binding.llHistoryTags.addView(tv, params)
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val PREFS_SEARCH = "search_prefs"
        private const val KEY_HISTORY = "search_history"
        private const val MAX_HISTORY_SIZE = 10
    }
}
