package com.example.neuro.base

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.neuro.util.showToast

abstract class BaseActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutId())
        initViews()
        initData()
        setupListeners()
    }
    
    protected abstract fun getLayoutId(): Int
    
    protected open fun initViews() {}
    
    protected open fun initData() {}
    
    protected open fun setupListeners() {}
    
    protected fun showToast(message: String) {
        showToast(message, Toast.LENGTH_SHORT)
    }
    
    protected fun showToast(messageResId: Int) {
        showToast(messageResId, Toast.LENGTH_SHORT)
    }
    
    protected fun showError(message: String) {
        showToast("错误：$message")
    }
    
    protected fun showNetworkError() {
        showToast("网络错误，请检查网络连接")
    }
}
