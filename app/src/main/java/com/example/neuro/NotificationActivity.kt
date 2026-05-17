package com.example.neuro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuro.databinding.ActivityNotificationBinding
import com.example.neuro.viewmodel.NotificationViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class NotificationActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, NotificationActivity::class.java))
        }
    }

    private lateinit var binding: ActivityNotificationBinding
    private val viewModel: NotificationViewModel by viewModels()
    private lateinit var adapter: NotificationAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        viewModel.loadNotifications()
    }

    private fun setupRecyclerView() {
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        adapter = NotificationAdapter(
            onItemClick = { notification ->
                viewModel.markAsRead(notification.notificationId)
                when (notification.type) {
                    "comment_reply" -> {
                        if (notification.relatedId.isNotEmpty()) {
                            BookDetailActivity.start(this, notification.relatedId)
                        }
                    }
                    else -> {}
                }
            }
        )
        binding.rvNotifications.adapter = adapter
    }

    private fun setupListeners() {
        binding.ivBack.setOnClickListener { finish() }
        binding.tvMarkAllRead.setOnClickListener {
            viewModel.markAllAsRead()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.notifications.collect { items ->
                        adapter.updateData(items)
                        binding.llEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                        binding.rvNotifications.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is NotificationViewModel.NotificationUiState.Error -> {
                                Toast.makeText(this@NotificationActivity, state.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}