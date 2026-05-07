package com.example.neuro

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.neuro.api.RetrofitClient
import com.example.neuro.api.model.UploadDocxResponse
import com.example.neuro.api.model.UploadWorkResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream

class UploadActivity : AppCompatActivity() {

    private lateinit var flFile: FrameLayout
    private lateinit var llFileHint: LinearLayout
    private lateinit var llFileInfo: LinearLayout
    private lateinit var tvFileName: TextView
    private lateinit var tvFileSize: TextView
    private lateinit var tvChangeFile: TextView
    private lateinit var etTitle: EditText
    private lateinit var etSummary: EditText
    private lateinit var etTags: EditText
    private lateinit var flCover: FrameLayout
    private lateinit var ivCover: ImageView
    private lateinit var tvCoverPlaceholder: TextView
    private lateinit var tvUpload: TextView
    private lateinit var progressBar: ProgressBar

    private var selectedFileUri: Uri? = null
    private var coverUrl: String? = null

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedFile(uri)
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                uploadCover(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload)

        initViews()
        setupClickListeners()

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    handleSelectedFile(uri)
                }
            }
            Intent.ACTION_SEND -> {
                when {
                    intent.type?.startsWith("image/") == true -> {
                        intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uri ->
                            uploadCover(uri)
                        }
                    }
                    else -> {
                        intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uri ->
                            handleSelectedFile(uri)
                        }
                    }
                }
            }
        }
    }

    private fun initViews() {
        flFile = findViewById(R.id.fl_file)
        llFileHint = findViewById(R.id.ll_file_hint)
        llFileInfo = findViewById(R.id.ll_file_info)
        tvFileName = findViewById(R.id.tv_file_name)
        tvFileSize = findViewById(R.id.tv_file_size)
        tvChangeFile = findViewById(R.id.tv_change_file)
        etTitle = findViewById(R.id.et_title)
        etSummary = findViewById(R.id.et_summary)
        etTags = findViewById(R.id.et_tags)
        flCover = findViewById(R.id.fl_cover)
        ivCover = findViewById(R.id.iv_cover)
        tvCoverPlaceholder = findViewById(R.id.tv_cover_placeholder)
        tvUpload = findViewById(R.id.tv_upload)
        progressBar = findViewById(R.id.progress_bar)
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }

        flFile.setOnClickListener {
            pickFile()
        }

        tvChangeFile.setOnClickListener {
            pickFile()
        }

        flCover.setOnClickListener {
            pickImage()
        }

        tvUpload.setOnClickListener {
            uploadWork()
        }
    }

    private fun pickFile() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "*/*"
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        val mimeTypes = arrayOf("text/plain", "text/markdown", "application/octet-stream")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        pickFileLauncher.launch(intent)
    }

    private fun pickImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        pickImageLauncher.launch(intent)
    }

    private fun handleSelectedFile(uri: Uri) {
        selectedFileUri = uri
        val fileName = getFileName(uri)
        val fileSize = getFileSize(uri)

        tvFileName.text = fileName
        tvFileSize.text = formatFileSize(fileSize)

        llFileHint.visibility = View.GONE
        llFileInfo.visibility = View.VISIBLE

        if (etTitle.text.isNullOrBlank()) {
            val nameWithoutExt = fileName.substringBeforeLast(".")
            etTitle.setText(nameWithoutExt)
        }
    }

    private fun getFileName(uri: Uri): String {
        var name = "unknown"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    private fun getFileSize(uri: Uri): Long {
        var size = 0L
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
            if (cursor.moveToFirst() && sizeIndex >= 0) {
                size = cursor.getLong(sizeIndex)
            }
        }
        return size
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        }
    }

    private fun uploadCover(uri: Uri) {
        lifecycleScope.launch {
            try {
                val file = getFileFromUri(uri)
                if (file == null) {
                    Toast.makeText(this@UploadActivity, "无法读取文件", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val response = RetrofitClient.apiService.uploadCover(body)
                if (response.isSuccessful && response.body()?.code == 0) {
                    coverUrl = response.body()?.data?.url
                    updateCoverDisplay()
                    Toast.makeText(this@UploadActivity, "封面上传成功", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@UploadActivity, "上传失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@UploadActivity, "上传出错: ${e.message}", Toast.LENGTH_SHORT).show()
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

    private fun updateCoverDisplay() {
        if (!coverUrl.isNullOrBlank()) {
            tvCoverPlaceholder.visibility = View.GONE
            ivCover.visibility = View.VISIBLE
            Glide.with(this)
                .load(coverUrl)
                .into(ivCover)
        } else {
            ivCover.visibility = View.GONE
            tvCoverPlaceholder.visibility = View.VISIBLE
        }
    }

    private fun uploadWork() {
        val uri = selectedFileUri
        if (uri == null) {
            Toast.makeText(this, "请先选择文件", Toast.LENGTH_SHORT).show()
            return
        }

        val title = etTitle.text.toString().trim()
        val summary = etSummary.text.toString().trim()
        val tags = etTags.text.toString().trim()
        val fileName = getFileName(uri)
        val isDocx = fileName.endsWith(".docx", ignoreCase = true)

        tvUpload.isEnabled = false
        tvUpload.text = "上传中..."
        progressBar.visibility = View.VISIBLE

        lifecycleScope.launch {
            try {
                val file = getFileFromUri(uri, "work_file")
                if (file == null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@UploadActivity, "无法读取文件", Toast.LENGTH_SHORT).show()
                        resetUploadButton()
                    }
                    return@launch
                }

                val mediaType = if (isDocx) {
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document".toMediaTypeOrNull()
                } else {
                    "text/plain".toMediaTypeOrNull()
                }
                val requestFile = file.asRequestBody(mediaType)
                val filePart = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val titlePart = title.ifBlank { null }?.toRequestBody("text/plain".toMediaTypeOrNull())
                val coverPart = coverUrl?.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = if (isDocx) {
                    RetrofitClient.apiService.uploadDocx(
                        file = filePart,
                        title = titlePart,
                        cover = coverPart
                    )
                } else {
                    val summaryPart = summary.ifBlank { null }?.toRequestBody("text/plain".toMediaTypeOrNull())
                    val tagsPart = tags.ifBlank { null }?.toRequestBody("text/plain".toMediaTypeOrNull())
                    RetrofitClient.apiService.uploadTxtWork(
                        file = filePart,
                        title = titlePart,
                        summary = summaryPart,
                        tags = tagsPart,
                        cover = coverPart
                    )
                }

                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.code == 0) {
                        val chapterCount = if (isDocx) {
                            (response.body()?.data as? UploadDocxResponse)?.chapterCount ?: 0
                        } else {
                            (response.body()?.data as? UploadWorkResponse)?.chapterCount ?: 0
                        }
                        val wordCount = if (isDocx) {
                            (response.body()?.data as? UploadDocxResponse)?.wordCount ?: 0
                        } else {
                            (response.body()?.data as? UploadWorkResponse)?.wordCount ?: 0
                        }
                        Toast.makeText(this@UploadActivity, "上传成功！共 $chapterCount 章，$wordCount 字", Toast.LENGTH_LONG).show()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        val msg = response.body()?.message ?: "上传失败"
                        Toast.makeText(this@UploadActivity, msg, Toast.LENGTH_SHORT).show()
                        resetUploadButton()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@UploadActivity, "上传出错：${e.message}", Toast.LENGTH_SHORT).show()
                    resetUploadButton()
                }
            }
        }
    }

    private fun getFileFromUri(uri: Uri, prefix: String): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val fileName = getFileName(uri)
            val tempFile = File(cacheDir, "${prefix}_${System.currentTimeMillis()}_$fileName")
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    private fun resetUploadButton() {
        tvUpload.isEnabled = true
        tvUpload.text = "上传作品"
        progressBar.visibility = View.GONE
    }
}
