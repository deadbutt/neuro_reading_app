package com.example.neuro

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.neuro.api.RetrofitClient
import kotlinx.coroutines.launch

class CreateCenterActivity : AppCompatActivity() {

    private lateinit var rvWorks: RecyclerView
    private lateinit var llEmpty: LinearLayout
    private lateinit var worksAdapter: WorksAdapter
    private var works = mutableListOf<WorkItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_center)

        initViews()
        setupClickListeners()
        loadMyWorks()
    }

    private fun initViews() {
        rvWorks = findViewById(R.id.rv_works)
        llEmpty = findViewById(R.id.ll_empty)

        worksAdapter = WorksAdapter(works, this) {
            updateEmptyState()
        }
        rvWorks.layoutManager = LinearLayoutManager(this)
        rvWorks.adapter = worksAdapter
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.iv_close).setOnClickListener {
            finish()
        }

        findViewById<LinearLayout>(R.id.ll_create_work).setOnClickListener {
            CreateWorkActivity.start(this)
        }

        findViewById<LinearLayout>(R.id.ll_upload_file).setOnClickListener {
            startActivity(Intent(this, UploadActivity::class.java))
        }
    }

    private fun loadMyWorks() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getMyWorks()
                if (response.isSuccessful && response.body()?.code == 0) {
                    val list = response.body()?.data?.list ?: emptyList()
                    works.clear()
                    works.addAll(list.map {
                        WorkItem(
                            articleId = it.articleId,
                            title = it.title,
                            summary = it.summary,
                            cover = it.cover,
                            chapterCount = it.chapterCount,
                            wordCount = it.wordCount,
                            status = it.status
                        )
                    })
                    worksAdapter.notifyDataSetChanged()
                    updateEmptyState()
                } else {
                    android.util.Log.e("CreateCenter", "加载失败: ${response.body()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("CreateCenter", "加载异常: ${e.message}")
            }
        }
    }

    private fun updateEmptyState() {
        llEmpty.visibility = if (works.isEmpty()) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        loadMyWorks()
    }
}
