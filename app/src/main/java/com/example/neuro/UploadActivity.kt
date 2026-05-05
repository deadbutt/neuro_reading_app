package com.example.neuro

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.neuro.api.RetrofitClient
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

    private lateinit var llSelectFile: LinearLayout
    private lateinit var llFileInfo: LinearLayout
    private lateinit var tvSelectHint: TextView
    private lateinit var tvFileName: TextView
    private lateinit var tvFileSize: TextView
    private lateinit var ivRemoveFile: ImageView
    private lateinit var etTitle: EditText
    private lateinit var etSummary: EditText
    private lateinit var etTags: EditText
    private lateinit var btnUpload: TextView
    private lateinit var flUploading: FrameLayout
    private lateinit var tvUploadProgress: TextView

    private var selectedFileUri: Uri? = null
    private var selectedFileName: String = ""
    private var selectedFileSize: Long = 0

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleSelectedFile(uri)
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

    private fun initViews() {
        llSelectFile = findViewById(R.id.ll_select_file)
        llFileInfo = findViewById(R.id.ll_file_info)
        tvSelectHint = findViewById(R.id.tv_select_hint)
        tvFileName = findViewById(R.id.tv_file_name)
        tvFileSize = findViewById(R.id.tv_file_size)
        ivRemoveFile = findViewById(R.id.iv_remove_file)
        etTitle = findViewById(R.id.et_title)
        etSummary = findViewById(R.id.et_summary)
        etTags = findViewById(R.id.et_tags)
        btnUpload = findViewById(R.id.btn_upload)
        flUploading = findViewById(R.id.fl_uploading)
        tvUploadProgress = findViewById(R.id.tv_upload_progress)
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.iv_back).setOnClickListener {
            finish()
        }

        llSelectFile.setOnClickListener {
            openFilePicker()
        }

        ivRemoveFile.setOnClickListener {
            clearSelectedFile()
        }

        btnUpload.setOnClickListener {
            uploadWork()
        }
    }

    private fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_VIEW -> {
                intent.data?.let { uri ->
                    handleSelectedFile(uri)
                }
            }
            Intent.ACTION_SEND -> {
                (intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM))?.let { uri ->
                    handleSelectedFile(uri)
                }
            }
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "text/plain",
                "application/epub+zip",
                "application/x-fictionbook",
                "application/x-mobipocket-ebook",
                "application/octet-stream"
            ))
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        filePickerLauncher.launch(intent)
    }

    private fun handleSelectedFile(uri: Uri) {
        selectedFileUri = uri

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            cursor.moveToFirst()
            selectedFileName = cursor.getString(nameIndex)
            selectedFileSize = cursor.getLong(sizeIndex)
        }

        tvFileName.text = selectedFileName
        tvFileSize.text = formatFileSize(selectedFileSize)
        llFileInfo.visibility = View.VISIBLE
        tvSelectHint.text = "重新选择文件"

        if (etTitle.text.isNullOrBlank()) {
            val titleFromFileName = selectedFileName.substringBeforeLast(".")
            etTitle.setText(titleFromFileName)
        }
    }

    private fun clearSelectedFile() {
        selectedFileUri = null
        selectedFileName = ""
        selectedFileSize = 0
        llFileInfo.visibility = View.GONE
        tvSelectHint.text = "点击选择文件"
    }

    private fun formatFileSize(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
        }
    }

    private fun uploadWork() {
        val uri = selectedFileUri
        if (uri == null) {
            Toast.makeText(this, "请先选择文件", Toast.LENGTH_SHORT).show()
            return
        }

        val title = etTitle.text.toString().trim()
        if (title.isBlank()) {
            Toast.makeText(this, "请输入作品标题", Toast.LENGTH_SHORT).show()
            return
        }

        val summary = etSummary.text.toString().trim()
        val tags = etTags.text.toString().trim()

        flUploading.visibility = View.VISIBLE
        tvUploadProgress.text = "准备上传..."

        lifecycleScope.launch {
            try {
                val file = copyUriToFile(uri)
                if (file == null) {
                    withContext(Dispatchers.Main) {
                        flUploading.visibility = View.GONE
                        Toast.makeText(this@UploadActivity, "无法读取文件", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                tvUploadProgress.text = "上传中..."

                val requestBody = file.asRequestBody("application/octet-stream".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", file.name, requestBody)

                val titleBody = title.toRequestBody("text/plain".toMediaTypeOrNull())
                val summaryBody = summary.toRequestBody("text/plain".toMediaTypeOrNull())
                val tagsBody = tags.toRequestBody("text/plain".toMediaTypeOrNull())

                val response = RetrofitClient.apiService.uploadWork(filePart, titleBody, summaryBody, tagsBody)

                withContext(Dispatchers.Main) {
                    flUploading.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(this@UploadActivity, "上传成功！", Toast.LENGTH_SHORT).show()
                        file.delete()
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        val msg = response.body()?.message ?: "上传失败"
                        Toast.makeText(this@UploadActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    flUploading.visibility = View.GONE
                    Toast.makeText(this@UploadActivity, "上传出错：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun copyUriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(cacheDir, selectedFileName)
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            null
        }
    }
}
