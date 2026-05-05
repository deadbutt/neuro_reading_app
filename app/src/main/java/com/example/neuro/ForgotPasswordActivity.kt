package com.example.neuro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Patterns
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.neuro.api.RetrofitClient
import com.example.neuro.api.model.ForgotPasswordRequest
import com.example.neuro.api.model.SendCodeRequest
import kotlinx.coroutines.launch
import java.security.MessageDigest

class ForgotPasswordActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ForgotPasswordActivity::class.java))
        }
    }

    private var countDownTimer: CountDownTimer? = null
    private var isCountingDown = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        val etAccount = findViewById<EditText>(R.id.et_account)
        val etCode = findViewById<EditText>(R.id.et_code)
        val etNewPassword = findViewById<EditText>(R.id.et_new_password)
        val etConfirmPassword = findViewById<EditText>(R.id.et_confirm_password)
        val btnSendCode = findViewById<TextView>(R.id.btn_send_code)
        val btnReset = findViewById<TextView>(R.id.btn_reset_password)

        findViewById<View>(R.id.iv_back).setOnClickListener { finish() }

        btnSendCode.setOnClickListener {
            val account = etAccount.text.toString().trim()
            if (account.isEmpty() || !isValidAccount(account)) {
                Toast.makeText(this, getString(R.string.login_invalid_account), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val type = if (Patterns.EMAIL_ADDRESS.matcher(account).matches()) "email" else "qq"

            btnSendCode.isEnabled = false

            lifecycleScope.launch {
                try {
                    val request = SendCodeRequest(account = account, type = type)
                    val response = RetrofitClient.apiService.sendVerificationCode(request)

                    if (response.isSuccessful && response.body()?.code == 0) {
                        startCountDown(btnSendCode)
                        Toast.makeText(this@ForgotPasswordActivity, getString(R.string.login_code_sent), Toast.LENGTH_SHORT).show()
                    } else {
                        btnSendCode.isEnabled = true
                        val msg = response.body()?.message ?: "发送失败"
                        Toast.makeText(this@ForgotPasswordActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    btnSendCode.isEnabled = true
                    Toast.makeText(this@ForgotPasswordActivity, "网络错误：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnReset.setOnClickListener {
            val account = etAccount.text.toString().trim()
            val code = etCode.text.toString().trim()
            val newPassword = etNewPassword.text.toString().trim()
            val confirmPassword = etConfirmPassword.text.toString().trim()

            if (account.isEmpty() || !isValidAccount(account)) {
                Toast.makeText(this, getString(R.string.login_invalid_account), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (code.length != 6) {
                Toast.makeText(this, getString(R.string.login_invalid_code), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword.length < 6) {
                Toast.makeText(this, "密码至少6位", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                Toast.makeText(this, "两次密码不一致", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            btnReset.isEnabled = false
            btnReset.text = "重置中…"

            lifecycleScope.launch {
                try {
                    val request = ForgotPasswordRequest(
                        account = account,
                        code = code,
                        newPassword = md5(newPassword)
                    )
                    val response = RetrofitClient.apiService.forgotPassword(request)

                    if (response.isSuccessful && response.body()?.code == 0) {
                        Toast.makeText(this@ForgotPasswordActivity, "密码重置成功，请重新登录", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        btnReset.isEnabled = true
                        btnReset.text = "重置密码"
                        val msg = response.body()?.message ?: "重置失败"
                        Toast.makeText(this@ForgotPasswordActivity, msg, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    btnReset.isEnabled = true
                    btnReset.text = "重置密码"
                    Toast.makeText(this@ForgotPasswordActivity, "网络错误：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
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
