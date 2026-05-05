package com.example.neuro

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class ProfileActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ProfileActivity::class.java))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        findViewById<View>(R.id.btn_login).setOnClickListener {
            Toast.makeText(this, "登录功能开发中", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.ll_menu_history).setOnClickListener {
            Toast.makeText(this, "阅读历史", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.ll_menu_cache).setOnClickListener {
            Toast.makeText(this, "离线缓存", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.ll_menu_rewards).setOnClickListener {
            Toast.makeText(this, "福利中心", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.ll_menu_help).setOnClickListener {
            Toast.makeText(this, "帮助反馈", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.ll_menu_settings).setOnClickListener {
            startActivity(Intent(this, ReaderSettingsActivity::class.java))
        }

        val tbNightMode = findViewById<CompoundButton>(R.id.tb_night_mode)
        tbNightMode.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            AppCompatDelegate.setDefaultNightMode(mode)
        }

        setupBottomNav()
    }

    private fun setupBottomNav() {
        findViewById<View>(R.id.ll_nav_home).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            })
        }
        findViewById<View>(R.id.ll_nav_bookstore).setOnClickListener {
            Toast.makeText(this, "书城即将上线", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.ll_nav_bookshelf).setOnClickListener {
            startActivity(Intent(this, BookshelfActivity::class.java))
        }
    }
}
