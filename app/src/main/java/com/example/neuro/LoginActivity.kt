package com.example.neuro

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.neuro.api.model.*
import com.example.neuro.databinding.ActivityLoginBinding
import com.example.neuro.viewmodel.LoginState
import com.example.neuro.viewmodel.UserViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.security.MessageDigest

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, LoginActivity::class.java))
        }

        fun startForResult(fragment: Fragment, requestCode: Int) {
            fragment.startActivityForResult(Intent(fragment.requireContext(), LoginActivity::class.java), requestCode)
        }
    }

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: UserViewModel by viewModels()

    private var currentTab = 0
    private var countDownTimer: CountDownTimer? = null
    private var isCountingDown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initViews()
        setupTabs()
        setupLoginForm()
        setupRegisterForm()
        setupCommonActions()
        observeViewModel()
    }

    private fun initViews() {
        binding.ivClose.setOnClickListener { finish() }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    when (state) {
                        is LoginState.Loading -> {
                            binding.btnLoginSubmit.isEnabled = false
                            binding.btnLoginSubmit.text = getString(R.string.login_loading)
                        }
                        is LoginState.Success -> {
                            val data = state.data
                            UserManager.saveLoginInfo(
                                this@LoginActivity,
                                data.userId,
                                data.account,
                                data.nickname,
                                data.avatar,
                                data.token,
                                data.refreshToken
                            )
                            Toast.makeText(this@LoginActivity, getString(R.string.login_success), Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        }
                        is LoginState.Error -> {
                            binding.btnLoginSubmit.isEnabled = true
                            binding.btnLoginSubmit.text = getString(R.string.login_btn)
                            binding.btnRegisterSubmit.isEnabled = true
                            binding.btnRegisterSubmit.text = getString(R.string.login_tab_register)
                            Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_SHORT).show()
                            viewModel.resetState()
                        }
                        else -> {}
                    }
                }
            }
        }
    }

    private fun setupTabs() {
        binding.tvTabLogin.setOnClickListener { switchTab(0) }
        binding.tvTabRegister.setOnClickListener { switchTab(1) }
    }

    private fun switchTab(index: Int) {
        currentTab = index
        if (index == 0) {
            binding.tvTabLogin.setTextColor(getColor(R.color.primary_red))
            binding.tvTabLogin.setTypeface(null, Typeface.BOLD)
            binding.vIndicatorLogin.visibility = View.VISIBLE

            binding.tvTabRegister.setTextColor(getColor(R.color.tab_inactive))
            binding.tvTabRegister.setTypeface(null, Typeface.NORMAL)
            binding.vIndicatorRegister.visibility = View.INVISIBLE

            binding.llLoginForm.visibility = View.VISIBLE
            binding.llRegisterForm.visibility = View.GONE
        } else {
            binding.tvTabRegister.setTextColor(getColor(R.color.primary_red))
            binding.tvTabRegister.setTypeface(null, Typeface.BOLD)
            binding.vIndicatorRegister.visibility = View.VISIBLE

            binding.tvTabLogin.setTextColor(getColor(R.color.tab_inactive))
            binding.tvTabLogin.setTypeface(null, Typeface.NORMAL)
            binding.vIndicatorLogin.visibility = View.INVISIBLE

            binding.llLoginForm.visibility = View.GONE
            binding.llRegisterForm.visibility = View.VISIBLE
        }
    }

    private fun setupLoginForm() {
        binding.btnLoginSubmit.setOnClickListener {
            val account = binding.etLoginAccount.text.toString().trim()
            val password = binding.etLoginPassword.text.toString().trim()

            if (account.isEmpty() || !isValidAccount(account)) {
                Toast.makeText(this, R.string.login_invalid_account, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < Constants.Validation.MIN_PASSWORD_LENGTH) {
                Toast.makeText(this, "密码至少${Constants.Validation.MIN_PASSWORD_LENGTH}位", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.login(account, md5(password))
        }

        binding.tvForgotPassword.setOnClickListener {
            ForgotPasswordActivity.start(this)
        }
    }

    private fun setupRegisterForm() {
        binding.btnRegisterSendCode.setOnClickListener {
            val account = binding.etRegisterAccount.text.toString().trim()
            if (account.isEmpty() || !isValidAccount(account)) {
                Toast.makeText(this, getString(R.string.login_invalid_account), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val type = if (Patterns.EMAIL_ADDRESS.matcher(account).matches()) "email" else "qq"

            binding.btnRegisterSendCode.isEnabled = false
            viewModel.sendVerificationCode(account, type) { success, message ->
                runOnUiThread {
                    if (success) {
                        startCountDown(binding.btnRegisterSendCode)
                    } else {
                        binding.btnRegisterSendCode.isEnabled = true
                    }
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnRegisterSubmit.setOnClickListener {
            val account = binding.etRegisterAccount.text.toString().trim()
            val nickname = binding.etRegisterNickname.text.toString().trim()
            val code = binding.etRegisterCode.text.toString().trim()
            val password = binding.etRegisterPassword.text.toString().trim()
            val passwordConfirm = binding.etRegisterPasswordConfirm.text.toString().trim()

            if (account.isEmpty() || !isValidAccount(account)) {
                Toast.makeText(this, getString(R.string.login_invalid_account), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (code.length != Constants.Validation.VERIFICATION_CODE_LENGTH) {
                Toast.makeText(this, getString(R.string.login_invalid_code), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < Constants.Validation.MIN_PASSWORD_LENGTH) {
                Toast.makeText(this, "密码至少${Constants.Validation.MIN_PASSWORD_LENGTH}位", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != passwordConfirm) {
                Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.btnRegisterSubmit.isEnabled = false
            binding.btnRegisterSubmit.text = "注册中…"
            viewModel.register(account, code, md5(password), md5(passwordConfirm), nickname.ifEmpty { null })
        }
    }

    private fun setupCommonActions() {
        binding.tvUserAgreement.setOnClickListener {
            Toast.makeText(this, "用户协议", Toast.LENGTH_SHORT).show()
        }

        binding.tvPrivacyPolicy.setOnClickListener {
            Toast.makeText(this, "隐私政策", Toast.LENGTH_SHORT).show()
        }
    }

    private fun isValidAccount(account: String): Boolean {
        val isQQ = account.matches(Regex("^\\d{5,11}$"))
        val isEmail = Patterns.EMAIL_ADDRESS.matcher(account).matches()
        return isQQ || isEmail
    }

    private fun startCountDown(btn: TextView) {
        isCountingDown = true
        btn.isEnabled = false

        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                btn.text = getString(R.string.login_send_code_retry, seconds)
            }

            override fun onFinish() {
                isCountingDown = false
                btn.isEnabled = true
                btn.text = getString(R.string.login_send_code)
            }
        }.start()
    }

    private fun md5(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer?.cancel()
    }
}
