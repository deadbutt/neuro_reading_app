package com.example.neuro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.text.style.RelativeSizeSpan
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.neuro.api.RetrofitClient
import com.example.neuro.api.model.CreateChapterRequest
import com.example.neuro.api.model.UpdateChapterRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class EditorActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_WORK_ID = "work_id"
        private const val EXTRA_WORK_TITLE = "work_title"
        private const val EXTRA_CHAPTER_ID = "chapter_id"

        fun start(context: Context, workId: String?, workTitle: String?, chapterId: String? = null) {
            context.startActivity(Intent(context, EditorActivity::class.java).apply {
                putExtra(EXTRA_WORK_ID, workId)
                putExtra(EXTRA_WORK_TITLE, workTitle)
                putExtra(EXTRA_CHAPTER_ID, chapterId)
            })
        }
    }

    private lateinit var tvWorkTitle: TextView
    private lateinit var etChapterTitle: EditText
    private lateinit var etContent: EditText
    private lateinit var tvWordCount: TextView
    private lateinit var tvSave: TextView
    private lateinit var tvPublish: TextView

    private var workId: String? = null
    private var chapterId: String? = null
    private var isNewChapter = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        workId = intent.getStringExtra(EXTRA_WORK_ID)
        chapterId = intent.getStringExtra(EXTRA_CHAPTER_ID)
        isNewChapter = chapterId == null

        initViews()
        setupClickListeners()
        setupTextWatcher()

        if (!workId.isNullOrBlank()) {
            loadChapter()
        }
    }

    private fun initViews() {
        tvWorkTitle = findViewById(R.id.tv_work_title)
        etChapterTitle = findViewById(R.id.et_chapter_title)
        etContent = findViewById(R.id.et_content)
        tvWordCount = findViewById(R.id.tv_word_count)
        tvSave = findViewById(R.id.tv_save)
        tvPublish = findViewById(R.id.tv_publish)

        val workTitle = intent.getStringExtra(EXTRA_WORK_TITLE)
        tvWorkTitle.text = workTitle ?: "新建作品"
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }

        tvSave.setOnClickListener {
            saveChapter("draft")
        }

        tvPublish.setOnClickListener {
            saveChapter("published")
        }

        findViewById<ImageView>(R.id.iv_bold).setOnClickListener {
            applyBold()
        }

        findViewById<ImageView>(R.id.iv_italic).setOnClickListener {
            applyItalic()
        }

        findViewById<ImageView>(R.id.iv_heading).setOnClickListener {
            applyHeading()
        }

        findViewById<ImageView>(R.id.iv_quote).setOnClickListener {
            insertQuote()
        }
    }

    private fun setupTextWatcher() {
        etContent.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val count = s?.length ?: 0
                tvWordCount.text = "${count}字"
            }
        })
    }

    private fun loadChapter() {
        if (chapterId.isNullOrBlank()) return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getChapterForEdit(workId!!, chapterId!!)
                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = response.body()?.data
                    etChapterTitle.setText(data?.title)
                    etContent.setText(data?.content)
                }
            } catch (e: Exception) {
                // 静默失败
            }
        }
    }

    private fun saveChapter(status: String) {
        val title = etChapterTitle.text.toString().trim()
        val content = etContent.text.toString().trim()

        if (title.isBlank()) {
            Toast.makeText(this, "请输入章节标题", Toast.LENGTH_SHORT).show()
            return
        }

        if (content.isBlank()) {
            Toast.makeText(this, "请输入章节内容", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val response = if (isNewChapter) {
                    val request = CreateChapterRequest(title, content, status)
                    RetrofitClient.apiService.createChapter(workId!!, request)
                } else {
                    val request = UpdateChapterRequest(title, content, status)
                    RetrofitClient.apiService.updateChapter(workId!!, chapterId!!, request)
                }

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        val msg = if (status == "published") "发布成功" else "保存成功"
                        Toast.makeText(this@EditorActivity, msg, Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@EditorActivity, "保存失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditorActivity, "网络错误", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun applyBold() {
        val start = etContent.selectionStart
        val end = etContent.selectionEnd
        if (start == end) return

        val span = StyleSpan(android.graphics.Typeface.BOLD)
        etContent.text?.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun applyItalic() {
        val start = etContent.selectionStart
        val end = etContent.selectionEnd
        if (start == end) return

        val span = StyleSpan(android.graphics.Typeface.ITALIC)
        etContent.text?.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun applyHeading() {
        val start = etContent.selectionStart
        val end = etContent.selectionEnd
        if (start == end) return

        val span = RelativeSizeSpan(1.5f)
        etContent.text?.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    private fun insertQuote() {
        val start = etContent.selectionStart
        val text = etContent.text
        text?.insert(start, "\n「」\n")
        etContent.setSelection(start + 2)
    }
}
