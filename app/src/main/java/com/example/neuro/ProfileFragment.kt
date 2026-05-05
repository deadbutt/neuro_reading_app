package com.example.neuro

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.neuro.api.RetrofitClient
import com.example.neuro.api.model.UpdateProfileRequest
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

class ProfileFragment : Fragment() {

    private lateinit var ivAvatar: ImageView
    private lateinit var btnLogin: TextView
    private lateinit var tvNickname: TextView
    private lateinit var tvLoginDesc: TextView
    private lateinit var ivEditAvatar: ImageView
    private lateinit var btnLogout: TextView
    private lateinit var tvShelfNum: TextView
    private lateinit var tvTimeNum: TextView

    companion object {
        private const val REQUEST_LOGIN = 1001
        private const val REQUEST_PICK_IMAGE = 1002
        private const val IMAGE_SERVER_HOST = "47.118.22.220"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_profile, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners(view)
        refreshUI()
    }

    private fun initViews(view: View) {
        ivAvatar = view.findViewById(R.id.iv_profile_avatar)
        btnLogin = view.findViewById(R.id.btn_login)
        tvNickname = view.findViewById(R.id.tv_nickname)
        tvLoginDesc = view.findViewById(R.id.tv_login_desc)
        ivEditAvatar = view.findViewById(R.id.iv_edit_avatar)
        btnLogout = view.findViewById(R.id.btn_logout)
        tvShelfNum = view.findViewById(R.id.tv_shelf_num)
        tvTimeNum = view.findViewById(R.id.tv_time_num)
    }

    private fun setupListeners(view: View) {
        btnLogin.setOnClickListener {
            LoginActivity.startForResult(this, REQUEST_LOGIN)
        }

        ivAvatar.setOnClickListener {
            if (UserManager.isLoggedIn(requireContext())) {
                showAvatarOptions()
            } else {
                LoginActivity.startForResult(this, REQUEST_LOGIN)
            }
        }

        ivEditAvatar.setOnClickListener {
            showAvatarOptions()
        }

        btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }

        view.findViewById<View>(R.id.ll_menu_creator).setOnClickListener {
            startActivity(Intent(requireContext(), CreateCenterActivity::class.java))
        }
        view.findViewById<View>(R.id.ll_menu_history).setOnClickListener {
            if (checkLogin()) Toast.makeText(requireContext(), "阅读历史", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.ll_menu_cache).setOnClickListener {
            Toast.makeText(requireContext(), "离线缓存", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.ll_menu_rewards).setOnClickListener {
            Toast.makeText(requireContext(), "福利中心", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.ll_menu_help).setOnClickListener {
            Toast.makeText(requireContext(), "帮助反馈", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<View>(R.id.ll_menu_settings).setOnClickListener {
            startActivity(Intent(requireContext(), ReaderSettingsActivity::class.java))
        }

        val tbNightMode = view.findViewById<CompoundButton>(R.id.tb_night_mode)
        tbNightMode.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
        }
    }

    private fun refreshUI() {
        val context = requireContext()
        val isLoggedIn = UserManager.isLoggedIn(context)

        if (isLoggedIn) {
            val nickname = UserManager.getNickname(context) ?: "用户"
            val avatar = UserManager.getAvatar(context)

            btnLogin.visibility = View.GONE
            tvNickname.visibility = View.VISIBLE
            tvNickname.text = nickname
            tvLoginDesc.text = "欢迎来到Neuro阅读"
            ivEditAvatar.visibility = View.VISIBLE
            btnLogout.visibility = View.VISIBLE

            loadAvatar(avatar)
            fetchUserProfile()
        } else {
            btnLogin.visibility = View.VISIBLE
            tvNickname.visibility = View.GONE
            tvLoginDesc.text = getString(R.string.profile_login_desc)
            ivEditAvatar.visibility = View.GONE
            btnLogout.visibility = View.GONE

            ivAvatar.setImageResource(R.drawable.bg_profile_avatar)
            tvShelfNum.text = "0"
            tvTimeNum.text = "0"
        }
    }

    private fun loadAvatar(avatarUrl: String?) {
        if (!avatarUrl.isNullOrEmpty()) {
            val normalizedUrl = avatarUrl.replace("0.0.0.0", IMAGE_SERVER_HOST)
            Glide.with(this)
                .load(normalizedUrl)
                .placeholder(R.drawable.bg_profile_avatar)
                .error(R.drawable.bg_profile_avatar)
                .circleCrop()
                .into(ivAvatar)
        } else {
            ivAvatar.setImageResource(R.drawable.bg_profile_avatar)
        }
    }

    private fun fetchUserProfile() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getUserProfile()
                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = response.body()?.data
                    data?.let {
                        val normalizedAvatar = it.avatar.replace("0.0.0.0", IMAGE_SERVER_HOST)
                        UserManager.updateProfile(requireContext(), it.nickname, normalizedAvatar)
                        tvNickname.text = it.nickname
                        tvShelfNum.text = it.bookshelfCount.toString()
                        tvTimeNum.text = formatDuration(it.readDuration)
                        loadAvatar(normalizedAvatar)
                    }
                }
            } catch (e: Exception) {
                // 静默失败，使用本地缓存数据
            }
        }
    }

    private fun formatDuration(minutes: Long): String {
        return when {
            minutes < 60 -> "${minutes}分钟"
            minutes < 1440 -> "${minutes / 60}小时"
            else -> "${minutes / 1440}天"
        }
    }

    private fun checkLogin(): Boolean {
        return if (UserManager.isLoggedIn(requireContext())) {
            true
        } else {
            LoginActivity.startForResult(this, REQUEST_LOGIN)
            false
        }
    }

    private fun showAvatarOptions() {
        val options = arrayOf("更换头像", "查看大图")
        AlertDialog.Builder(requireContext())
            .setItems(options) { _, which ->
                when (which) {
                    0 -> pickImageFromGallery()
                    1 -> {
                        val avatar = UserManager.getAvatar(requireContext())
                        if (!avatar.isNullOrEmpty()) {
                            // 可以跳转到图片查看页面
                            Toast.makeText(requireContext(), "查看大图功能开发中", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .show()
    }

    private fun pickImageFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, REQUEST_PICK_IMAGE)
    }

    private fun showLogoutConfirmDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("退出登录")
            .setMessage("确定要退出当前账号吗？")
            .setPositiveButton("确定") { _, _ -> performLogout() }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun performLogout() {
        UserManager.clearLoginInfo(requireContext())
        refreshUI()
        Toast.makeText(requireContext(), "已退出登录", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_LOGIN -> {
                if (resultCode == Activity.RESULT_OK) {
                    refreshUI()
                }
            }
            REQUEST_PICK_IMAGE -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    val imageUri = data.data
                    imageUri?.let { uploadAvatar(it) }
                }
            }
        }
    }

    private fun uploadAvatar(imageUri: Uri) {
        // 先显示本地图片
        ivAvatar.setImageURI(imageUri)

        lifecycleScope.launch {
            try {
                // 从Uri获取文件
                val file = uriToFile(imageUri)
                if (file == null) {
                    Toast.makeText(requireContext(), "获取图片失败", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                // 创建MultipartBody.Part
                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                // 上传头像
                val response = RetrofitClient.apiService.uploadAvatar(body)

                if (response.isSuccessful && response.body()?.code == 0) {
                    val avatarUrl = response.body()?.data?.url?.replace("0.0.0.0", IMAGE_SERVER_HOST)
                    if (!avatarUrl.isNullOrEmpty()) {
                        // 更新用户资料中的头像URL
                        val updateResponse = RetrofitClient.apiService.updateUserProfile(
                            UpdateProfileRequest(avatar = avatarUrl)
                        )
                        if (updateResponse.isSuccessful && updateResponse.body()?.code == 0) {
                            UserManager.updateProfile(requireContext(), avatar = avatarUrl)
                            loadAvatar(avatarUrl)
                            Toast.makeText(requireContext(), "头像更新成功", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(requireContext(), "头像更新失败", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val msg = response.body()?.message ?: "上传失败"
                    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "上传错误：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("avatar_", ".jpg", requireContext().cacheDir)
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override fun onResume() {
        super.onResume()
        refreshUI()
    }
}
