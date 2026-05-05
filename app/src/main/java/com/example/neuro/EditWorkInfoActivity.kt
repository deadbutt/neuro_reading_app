package com.example.neuro

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.neuro.api.RetrofitClient
import com.example.neuro.api.model.UpdateWorkRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

class EditWorkInfoActivity : AppCompatActivity() {

    private lateinit var etTitle: EditText
    private lateinit var etSummary: EditText
    private lateinit var etTags: EditText
    private lateinit var flCover: FrameLayout
    private lateinit var ivCover: ImageView
    private lateinit var llUploadHint: LinearLayout
    private lateinit var tvSave: TextView

    private var articleId: String? = null
    private var coverUrl: String? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadCover(uri)
            }
        }
    }

    companion object {
        private const val EXTRA_ARTICLE_ID = "article_id"

        fun start(context: Context, articleId: String) {
            context.startActivity(Intent(context, EditWorkInfoActivity::class.java).apply {
                putExtra(EXTRA_ARTICLE_ID, articleId)
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_work_info)

        articleId = intent.getStringExtra(EXTRA_ARTICLE_ID)

        initViews()
        setupClickListeners()
        loadWorkInfo()
    }

    private fun initViews() {
        etTitle = findViewById(R.id.et_title)
        etSummary = findViewById(R.id.et_summary)
        etTags = findViewById(R.id.et_tags)
        flCover = findViewById(R.id.fl_cover)
        ivCover = findViewById(R.id.iv_cover)
        llUploadHint = findViewById(R.id.ll_upload_hint)
        tvSave = findViewById(R.id.tv_save)
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }

        tvSave.setOnClickListener {
            saveWorkInfo()
        }

        flCover.setOnClickListener {
            pickImage()
        }
    }

    private fun loadWorkInfo() {
        if (articleId.isNullOrBlank()) return

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getWorkDetail(articleId!!)
                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = response.body()?.data ?: return@launch
                    etTitle.setText(data.title)
                    etSummary.setText(data.summary)
                    etTags.setText(data.tags.joinToString(", "))
                    coverUrl = data.cover
                    updateCoverDisplay()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditWorkInfoActivity, "加载失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateCoverDisplay() {
        if (!coverUrl.isNullOrBlank()) {
            llUploadHint.visibility = android.view.View.GONE
            ivCover.visibility = android.view.View.VISIBLE
            Glide.with(this)
                .load(coverUrl)
                .into(ivCover)
        } else {
            ivCover.visibility = android.view.View.GONE
            llUploadHint.visibility = android.view.View.VISIBLE
        }
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun uploadCover(uri: Uri) {
        lifecycleScope.launch {
            try {
                val file = getFileFromUri(uri)
                if (file == null) {
                    Toast.makeText(this@EditWorkInfoActivity, "无法读取文件", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val response = RetrofitClient.apiService.uploadCover(body)
                if (response.isSuccessful && response.body()?.code == 0) {
                    coverUrl = response.body()?.data?.url
                    updateCoverDisplay()
                    Toast.makeText(this@EditWorkInfoActivity, "封面上传成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@EditWorkInfoActivity, "上传失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@EditWorkInfoActivity, "上传出错: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(cacheDir, "cover_${System.currentTimeMillis()}.jpg")
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    private fun saveWorkInfo() {
        val title = etTitle.text.toString().trim()
        if (title.isBlank()) {
            Toast.makeText(this, "请输入作品标题", Toast.LENGTH_SHORT).show()
            return
        }

        if (articleId.isNullOrBlank()) {
            Toast.makeText(this, "作品ID无效", Toast.LENGTH_SHORT).show()
            return
        }

        val summary = etSummary.text.toString().trim().takeIf { it.isNotBlank() }
        val tagsText = etTags.text.toString().trim()
        val tags = if (tagsText.isNotBlank()) tagsText.split(",").map { it.trim() } else null

        tvSave.isEnabled = false

        lifecycleScope.launch {
            try {
                val request = UpdateWorkRequest(
                    title = title,
                    summary = summary,
                    tags = tags,
                    cover = coverUrl
                )

                val response = RetrofitClient.apiService.updateWork(articleId!!, request)

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(this@EditWorkInfoActivity, "保存成功", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        val msg = response.body()?.message ?: "保存失败"
                        Toast.makeText(this@EditWorkInfoActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                    tvSave.isEnabled = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@EditWorkInfoActivity, "保存出错：${e.message}", Toast.LENGTH_SHORT).show()
                    tvSave.isEnabled = true
                }
            }
        }
    }
}
