package com.example.neuro

import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.neuro.api.RetrofitClient
import com.example.neuro.api.model.*
import kotlinx.coroutines.launch
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, LoginActivity::class.java))
        }

        fun startForResult(fragment: Fragment, requestCode: Int) {
            fragment.startActivityForResult(Intent(fragment.requireContext(), LoginActivity::class.java), requestCode)
        }
    }

    private var currentTab = 0
    private var countDownTimer: CountDownTimer? = null
    private var isCountingDown = false

    private lateinit var llLoginForm: LinearLayout
    private lateinit var llRegisterForm: LinearLayout
    private lateinit var tvTabLogin: TextView
    private lateinit var tvTabRegister: TextView
    private lateinit var vIndicatorLogin: View
    private lateinit var vIndicatorRegister: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        setupTabs()
        setupLoginForm()
        setupRegisterForm()
        setupCommonActions()
    }

    private fun initViews() {
        llLoginForm = findViewById(R.id.ll_login_form)
        llRegisterForm = findViewById(R.id.ll_register_form)
        tvTabLogin = findViewById(R.id.tv_tab_login)
        tvTabRegister = findViewById(R.id.tv_tab_register)
        vIndicatorLogin = findViewById(R.id.v_indicator_login)
        vIndicatorRegister = findViewById(R.id.v_indicator_register)

        findViewById<View>(R.id.iv_close).setOnClickListener { finish() }
    }

    private fun setupTabs() {
        tvTabLogin.setOnClickListener { switchTab(0) }
        tvTabRegister.setOnClickListener { switchTab(1) }
    }

    private fun switchTab(index: Int) {
        currentTab = index
        if (index == 0) {
            tvTabLogin.setTextColor(getColor(R.color.primary_red))
            tvTabLogin.setTypeface(null, Typeface.BOLD)
            vIndicatorLogin.visibility = View.VISIBLE

            tvTabRegister.setTextColor(getColor(R.color.tab_inactive))
            tvTabRegister.setTypeface(null, Typeface.NORMAL)
            vIndicatorRegister.visibility = View.INVISIBLE

            llLoginForm.visibility = View.VISIBLE
            llRegisterForm.visibility = View.GONE
        } else {
            tvTabRegister.setTextColor(getColor(R.color.primary_red))
            tvTabRegister.setTypeface(null, Typeface.BOLD)
            vIndicatorRegister.visibility = View.VISIBLE

            tvTabLogin.setTextColor(getColor(R.color.tab_inactive))
            tvTabLogin.setTypeface(null, Typeface.NORMAL)
            vIndicatorLogin.visibility = View.INVISIBLE

            llLoginForm.visibility = View.GONE
            llRegisterForm.visibility = View.VISIBLE
        }
    }

    private fun setupLoginForm() {
        val etAccount = findViewById<EditText>(R.id.et_login_account)
        val etPassword = findViewById<EditText>(R.id.et_login_password)
        val btnLogin = findViewById<TextView>(R.id.btn_login_submit)

        btnLogin.setOnClickListener {
            val account = etAccount.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (account.isEmpty() || !isValidAccount(account)) {
                Toast.makeText(this, getString(R.string.login_invalid_account), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "密码至少6位", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnLogin.isEnabled = false
            btnLogin.text = "登录中…"

            lifecycleScope.launch {
                try {
                    val request = LoginRequest(
                        account = account,
                        password = md5(password)
                    )
                    val response = RetrofitClient.apiService.login(request)

                    if (response.isSuccessful && response.body()?.code == 0) {
                        val data = response.body()?.data
                        if (data != null) {
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
                        } else {
                            btnLogin.isEnabled = true
                            btnLogin.text = getString(R.string.login_btn)
                            Toast.makeText(this@LoginActivity, "登录失败：数据为空", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        btnLogin.isEnabled = true
                        btnLogin.text = getString(R.string.login_btn)
                        val msg = response.body()?.message ?: "登录失败"
                        Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    btnLogin.isEnabled = true
                    btnLogin.text = getString(R.string.login_btn)
                    Toast.makeText(this@LoginActivity, "网络错误：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        findViewById<View>(R.id.tv_forgot_password).setOnClickListener {
            ForgotPasswordActivity.start(this)
        }
    }

    private fun setupRegisterForm() {
        val etAccount = findViewById<EditText>(R.id.et_register_account)
        val etNickname = findViewById<EditText>(R.id.et_register_nickname)
        val etCode = findViewById<EditText>(R.id.et_register_code)
        val etPassword = findViewById<EditText>(R.id.et_register_password)
        val etPasswordConfirm = findViewById<EditText>(R.id.et_register_password_confirm)
        val btnSendCode = findViewById<TextView>(R.id.btn_register_send_code)
        val btnRegister = findViewById<TextView>(R.id.btn_register_submit)

        btnSendCode.setOnClickListener {
            val account = etAccount.text.toString().trim()
            if (account.isEmpty() || !isValidAccount(account)) {
                Toast.makeText(this, getString(R.string.login_invalid_account), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 判断账号类型
            val type = if (Patterns.EMAIL_ADDRESS.matcher(account).matches()) "email" else "qq"

            btnSendCode.isEnabled = false

            lifecycleScope.launch {
                try {
                    val request = SendCodeRequest(account = account, type = type)
                    val response = RetrofitClient.apiService.sendVerificationCode(request)

                    if (response.isSuccessful && response.body()?.code == 0) {
                        startCountDown(btnSendCode)
                        Toast.makeText(this@LoginActivity, getString(R.string.login_code_sent), Toast.LENGTH_SHORT).show()
                    } else {
                        btnSendCode.isEnabled = true
                        val msg = response.body()?.message ?: "发送失败"
                        Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    btnSendCode.isEnabled = true
                    Toast.makeText(this@LoginActivity, "网络错误：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnRegister.setOnClickListener {
            val account = etAccount.text.toString().trim()
            val nickname = etNickname.text.toString().trim()
            val code = etCode.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val passwordConfirm = etPasswordConfirm.text.toString().trim()

            if (account.isEmpty() || !isValidAccount(account)) {
                Toast.makeText(this, getString(R.string.login_invalid_account), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (code.length != 6) {
                Toast.makeText(this, getString(R.string.login_invalid_code), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password.length < 6) {
                Toast.makeText(this, "密码至少6位", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != passwordConfirm) {
                Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnRegister.isEnabled = false
            btnRegister.text = "注册中…"

            lifecycleScope.launch {
                try {
                    val request = RegisterRequest(
                        account = account,
                        code = code,
                        password = md5(password),
                        confirmPassword = md5(passwordConfirm),
                        nickname = nickname.ifEmpty { null }
                    )
                    val response = RetrofitClient.apiService.register(request)

                    if (response.isSuccessful && response.body()?.code == 0) {
                        val data = response.body()?.data
                        if (data != null) {
                            UserManager.saveLoginInfo(
                                this@LoginActivity,
                                data.userId,
                                data.account,
                                data.nickname,
                                data.avatar,
                                data.token,
                                data.refreshToken
                            )
                            Toast.makeText(this@LoginActivity, "注册成功", Toast.LENGTH_SHORT).show()
                            setResult(RESULT_OK)
                            finish()
                        } else {
                            btnRegister.isEnabled = true
                            btnRegister.text = getString(R.string.login_tab_register)
                            Toast.makeText(this@LoginActivity, "注册失败：数据为空", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        btnRegister.isEnabled = true
                        btnRegister.text = getString(R.string.login_tab_register)
                        val msg = response.body()?.message ?: "注册失败"
                        Toast.makeText(this@LoginActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    btnRegister.isEnabled = true
                    btnRegister.text = getString(R.string.login_tab_register)
                    Toast.makeText(this@LoginActivity, "网络错误：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupCommonActions() {
        findViewById<View>(R.id.tv_user_agreement).setOnClickListener {
            Toast.makeText(this, "用户协议", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.tv_privacy_policy).setOnClickListener {
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
