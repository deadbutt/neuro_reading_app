package com.example.neuro.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.neuro.util.showToast

abstract class BaseFragment : Fragment() {
    
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(getLayoutId(), container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        initData()
        setupListeners()
    }
    
    protected abstract fun getLayoutId(): Int
    
    protected open fun initViews(view: View) {}
    
    protected open fun initData() {}
    
    protected open fun setupListeners() {}
    
    protected fun showToast(message: String) {
        requireContext().showToast(message)
    }
    
    protected fun showToast(messageResId: Int) {
        requireContext().showToast(messageResId)
    }
    
    protected fun showError(message: String) {
        showToast("错误：$message")
    }
    
    protected fun showNetworkError() {
        showToast("网络错误，请检查网络连接")
    }
}
