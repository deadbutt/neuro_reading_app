package com.example.neuro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.neuro.api.RetrofitClient
import com.example.neuro.api.model.CreateWorkRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreateWorkActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etSummary: EditText
    private lateinit var etTags: EditText
    private lateinit var tvCreate: TextView

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, CreateWorkActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_work)

        initViews()
        setupClickListeners()
    }

    private fun initViews() {
        etTitle = findViewById(R.id.et_title)
        etSummary = findViewById(R.id.et_summary)
        etTags = findViewById(R.id.et_tags)
        tvCreate = findViewById(R.id.tv_create)
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }

        tvCreate.setOnClickListener {
            createWork()
        }
    }

    private fun createWork() {
        val title = etTitle.text.toString().trim()
        if (title.isBlank()) {
            Toast.makeText(this, "请输入作品标题", Toast.LENGTH_SHORT).show()
            return
        }

        val summary = etSummary.text.toString().trim().takeIf { it.isNotBlank() }
        val tagsText = etTags.text.toString().trim()
        val tags = if (tagsText.isNotBlank()) tagsText.split(",").map { it.trim() } else null

        tvCreate.isEnabled = false
        tvCreate.text = "创建中..."

        lifecycleScope.launch {
            try {
                val request = CreateWorkRequest(
                    title = title,
                    summary = summary,
                    tags = tags
                )

                val response = RetrofitClient.apiService.createWork(request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        val articleId = response.body()?.data?.articleId
                        Toast.makeText(this@CreateWorkActivity, "作品创建成功", Toast.LENGTH_SHORT).show()
                        EditorActivity.start(this@CreateWorkActivity, articleId, title)
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        val msg = response.body()?.message ?: "创建失败"
                        Toast.makeText(this@CreateWorkActivity, msg, Toast.LENGTH_SHORT).show()
                        tvCreate.isEnabled = true
                        tvCreate.text = "创建"
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@CreateWorkActivity, "创建出错：${e.message}", Toast.LENGTH_SHORT).show()
                    tvCreate.isEnabled = true
                    tvCreate.text = "创建"
                }
            }
        }
    }
}
