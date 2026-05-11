package com.example.neuro

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
import com.example.neuro.databinding.ActivityCreateCenterBinding
import com.example.neuro.viewmodel.CreateCenterUiState
import com.example.neuro.viewmodel.CreateCenterViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreateCenterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCreateCenterBinding
    private val viewModel: CreateCenterViewModel by viewModels()
    private lateinit var worksAdapter: WorksAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateCenterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        setupClickListeners()
        observeViewModel()
    }

    private fun initViews() {
        worksAdapter = WorksAdapter(mutableListOf(), this) {
            updateEmptyState()
        }
        binding.rvWorks.layoutManager = LinearLayoutManager(this)
        binding.rvWorks.adapter = worksAdapter
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is CreateCenterUiState.Loading -> {}
                            is CreateCenterUiState.Success -> {
                                updateEmptyState()
                            }
                            is CreateCenterUiState.Error -> {
                                Toast.makeText(this@CreateCenterActivity, state.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    viewModel.works.collect { works ->
                        worksAdapter.updateData(works.toMutableList())
                        updateEmptyState()
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        binding.ivClose.setOnClickListener {
            finish()
        }

        binding.llCreateWork.setOnClickListener {
            CreateWorkActivity.start(this)
        }

        binding.llUploadFile.setOnClickListener {
            startActivity(Intent(this, UploadActivity::class.java))
        }
    }

    private fun updateEmptyState() {
        binding.llEmpty.visibility = if (worksAdapter.itemCount == 0) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadMyWorks()
    }
}
