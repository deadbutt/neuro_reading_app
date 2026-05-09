package com.example.neuro.util

import android.content.Context
import android.widget.Toast

fun Context.showToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun Context.showToast(messageResId: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, messageResId, duration).show()
}
