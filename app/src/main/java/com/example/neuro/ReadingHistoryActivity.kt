package com.example.neuro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuro.api.model.ReadingHistoryResponse
import com.example.neuro.databinding.ActivityReadingHistoryBinding
import com.example.neuro.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReadingHistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReadingHistoryBinding
    private val viewModel: UserViewModel by viewModels()
    private lateinit var adapter: ReadingHistoryAdapter

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ReadingHistoryActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReadingHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        setupListeners()
        observeViewModel()
        viewModel.getReadingHistory()
    }

    override fun onResume() {
        super.onResume()
        viewModel.getReadingHistory()
    }

    private fun initViews() {
        adapter = ReadingHistoryAdapter(
            mutableListOf(),
            onItemClick = { item ->
                ReaderActivity.start(this, item.articleId, item.chapterIndex, item.title)
            },
            onDeleteClick = { item ->
                showDeleteConfirm(item)
            }
        )
        binding.rvHistory.layoutManager = LinearLayoutManager(this)
        binding.rvHistory.adapter = adapter
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener {
            finish()
        }

        binding.tvClear.setOnClickListener {
            if (adapter.itemCount == 0) {
                Toast.makeText(this, "暂无记录", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showClearConfirm()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.readingHistory.collect { list ->
                        adapter.updateData(list.toMutableList())
                        binding.llEmpty.visibility = if (list.isEmpty()) {
                            android.view.View.VISIBLE
                        } else {
                            android.view.View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun showDeleteConfirm(item: ReadingHistoryResponse) {
        AlertDialog.Builder(this)
            .setTitle("删除记录")
            .setMessage("确定删除《${item.title}》的阅读记录吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteReadingHistory(item.historyId) { success, message ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    if (success) {
                        adapter.removeItem(item.historyId)
                        if (adapter.itemCount == 0) {
                            binding.llEmpty.visibility = android.view.View.VISIBLE
                        }
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showClearConfirm() {
        AlertDialog.Builder(this)
            .setTitle("清空历史")
            .setMessage("确定清空所有阅读历史吗？此操作不可恢复。")
            .setPositiveButton("清空") { _, _ ->
                viewModel.clearReadingHistory { success, message ->
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    if (success) {
                        adapter.clearAll()
                        binding.llEmpty.visibility = android.view.View.VISIBLE
                    }
                }
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
