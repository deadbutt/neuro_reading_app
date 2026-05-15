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
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.bumptech.glide.Glide
import com.example.neuro.api.RetrofitClient
import com.example.neuro.api.model.UpdateProfileRequest
import com.example.neuro.databinding.FragmentProfileBinding
import com.example.neuro.util.UrlUtils
import com.example.neuro.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: UserViewModel by viewModels()

    companion object {
        private const val REQUEST_LOGIN = 1001
        private const val REQUEST_PICK_IMAGE = 1002
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupListeners()
        refreshUI()
        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.userProfile.collect { profile ->
                    profile?.let {
                        val normalizedAvatar = UrlUtils.normalize(it.avatar)
                        UserManager.updateProfile(requireContext(), it.nickname, normalizedAvatar)
                        binding.tvNickname.text = it.nickname
                        binding.tvShelfNum.text = it.bookshelfCount.toString()
                        binding.tvTimeNum.text = formatDuration(it.readDuration)
                        loadAvatar(normalizedAvatar)
                    }
                }
            }
        }
    }

    private fun setupListeners() {
        binding.btnLogin.setOnClickListener {
            LoginActivity.startForResult(this, REQUEST_LOGIN)
        }

        binding.ivProfileAvatar.setOnClickListener {
            if (UserManager.isLoggedIn(requireContext())) {
                showAvatarOptions()
            } else {
                LoginActivity.startForResult(this, REQUEST_LOGIN)
            }
        }

        binding.ivEditAvatar.setOnClickListener {
            showAvatarOptions()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmDialog()
        }

        binding.llMenuCreator.setOnClickListener {
            startActivity(Intent(requireContext(), CreateCenterActivity::class.java))
        }
        binding.llShelfCard.setOnClickListener {
            if (checkLogin()) {
                BookshelfActivity.start(requireContext())
            }
        }
        binding.llMenuHistory.setOnClickListener {
            if (checkLogin()) {
                ReadingHistoryActivity.start(requireContext())
            }
        }
        binding.llMenuCache.setOnClickListener {
            Toast.makeText(requireContext(), "离线缓存", Toast.LENGTH_SHORT).show()
        }
        binding.llMenuRewards.setOnClickListener {
            Toast.makeText(requireContext(), "福利中心", Toast.LENGTH_SHORT).show()
        }
        binding.llMenuHelp.setOnClickListener {
            Toast.makeText(requireContext(), "帮助反馈", Toast.LENGTH_SHORT).show()
        }
        binding.llMenuSettings.setOnClickListener {
            startActivity(Intent(requireContext(), ReaderSettingsActivity::class.java))
        }

        binding.tbNightMode.setOnCheckedChangeListener { _, isChecked ->
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

            binding.btnLogin.visibility = View.GONE
            binding.tvNickname.visibility = View.VISIBLE
            binding.tvNickname.text = nickname
            binding.tvLoginDesc.text = "欢迎来到Neuro阅读"
            binding.ivEditAvatar.visibility = View.VISIBLE
            binding.btnLogout.visibility = View.VISIBLE

            loadAvatar(avatar)
            viewModel.getUserProfile()
        } else {
            binding.btnLogin.visibility = View.VISIBLE
            binding.tvNickname.visibility = View.GONE
            binding.tvLoginDesc.text = getString(R.string.profile_login_desc)
            binding.ivEditAvatar.visibility = View.GONE
            binding.btnLogout.visibility = View.GONE

            binding.ivProfileAvatar.setImageResource(R.drawable.bg_profile_avatar)
            binding.tvShelfNum.text = "0"
            binding.tvTimeNum.text = "0"
        }
    }

    private fun loadAvatar(avatarUrl: String?) {
        if (!avatarUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(UrlUtils.normalize(avatarUrl))
                .placeholder(R.drawable.bg_profile_avatar)
                .error(R.drawable.bg_profile_avatar)
                .circleCrop()
                .into(binding.ivProfileAvatar)
        } else {
            binding.ivProfileAvatar.setImageResource(R.drawable.bg_profile_avatar)
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
        binding.ivProfileAvatar.setImageURI(imageUri)

        lifecycleScope.launch {
            try {
                val file = uriToFile(imageUri)
                if (file == null) {
                    Toast.makeText(requireContext(), "获取图片失败", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
                val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

                val response = RetrofitClient.apiService.uploadAvatar(body)
                if (response.isSuccessful && response.body()?.code == 0) {
                    response.body()?.data?.url?.let { avatarUrl ->
                        UserManager.updateProfile(requireContext(), avatar = avatarUrl)
                    }
                    Toast.makeText(requireContext(), "头像上传成功", Toast.LENGTH_SHORT).show()
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
