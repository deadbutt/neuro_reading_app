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
import com.example.neuro.databinding.FragmentBookshelfBinding
import com.example.neuro.viewmodel.BookshelfUiState
import com.example.neuro.viewmodel.BookshelfViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BookshelfFragment : Fragment() {

    private var _binding: FragmentBookshelfBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BookshelfViewModel by viewModels()
    private var isEditMode = false
    private lateinit var adapter: BookshelfAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentBookshelfBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupEditBar()
        setupTabs()
        observeViewModel()

        binding.tvShelfEdit.setOnClickListener { toggleEditMode() }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadBookshelf()
    }

    private fun setupRecyclerView() {
        binding.rvShelf.layoutManager = LinearLayoutManager(requireContext())
        adapter = BookshelfAdapter(isEditMode = false) { book, _ ->
            if (isEditMode) return@BookshelfAdapter
            if (book.bookId.isNotEmpty()) {
                BookDetailActivity.start(requireContext(), book.bookId)
            }
        }
        binding.rvShelf.adapter = adapter
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is BookshelfUiState.Loading -> {}
                            is BookshelfUiState.Success -> {}
                            is BookshelfUiState.Error -> {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
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
                    viewModel.books.collect { items ->
                        adapter.updateData(items, isEditMode)
                        updateEmptyState(items.isEmpty())
                    }
                }
            }
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
        isEditMode = !isEditMode
        binding.tvShelfEdit.text = if (isEditMode) "完成" else "编辑"
        binding.llShelfEditBar.visibility = if (isEditMode) View.VISIBLE else View.GONE
        adapter.updateData(viewModel.books.value, isEditMode)
    }

    private fun setupEditBar() {
        binding.llSelectAll.setOnClickListener {
            val allSelected = viewModel.toggleSelectAll()
            adapter.updateData(viewModel.books.value, isEditMode)
        }

        binding.btnShelfDelete.setOnClickListener {
            val selected = viewModel.getSelectedBooks()
            if (selected.isEmpty()) {
                Toast.makeText(requireContext(), "请先选择要删除的书籍", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.removeBooks(selected)
            }
        }
    }

    private fun setupTabs() {
        binding.tvShelfTabRead.setOnClickListener {
            Toast.makeText(requireContext(), "读过", Toast.LENGTH_SHORT).show()
        }
        binding.tvShelfTabSubscribe.setOnClickListener {
            Toast.makeText(requireContext(), "订阅", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
