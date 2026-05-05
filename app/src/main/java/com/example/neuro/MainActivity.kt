package com.example.neuro

import android.graphics.PorterDuff
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

class MainActivity : AppCompatActivity() {

    private var currentNavIndex = 0

    private val navIds = listOf(
        R.id.ll_nav_home,
        R.id.ll_nav_reading,
        R.id.ll_nav_mine
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            showFragment(HomeFragment())
        }

        supportFragmentManager.addOnBackStackChangedListener {
            val isSearch = supportFragmentManager.backStackEntryCount > 0
            findViewById<View>(R.id.ll_bottom_nav).visibility = if (isSearch) View.GONE else View.VISIBLE
        }

        setupBottomNav()
    }

    fun navigateToSearch() {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, SearchFragment())
            .addToBackStack("search")
            .commit()
    }

    private fun setupBottomNav() {
        navIds.forEachIndexed { index, id ->
            findViewById<View>(id).setOnClickListener {
                if (currentNavIndex != index) {
                    selectNavTab(index)
                }
            }
        }
    }

    private fun selectNavTab(index: Int) {
        currentNavIndex = index

        val navIconIds = listOf(R.id.iv_nav_home, R.id.iv_nav_reading, R.id.iv_nav_mine)
        val navTextIds = listOf(R.id.tv_nav_home, R.id.tv_nav_reading, R.id.tv_nav_mine)

        navIconIds.forEachIndexed { i, iconId ->
            val icon = findViewById<ImageView>(iconId)
            val text = findViewById<TextView>(navTextIds[i])
            val isActive = i == index

            val tintColor = if (isActive) R.color.primary_red else R.color.bottom_nav_inactive
            icon.setColorFilter(ContextCompat.getColor(this, tintColor), PorterDuff.Mode.SRC_IN)

            if (isActive) {
                text.setTextColor(ContextCompat.getColor(this, R.color.primary_red))
                text.setTypeface(text.typeface, Typeface.BOLD)
            } else {
                text.setTextColor(ContextCompat.getColor(this, R.color.bottom_nav_inactive))
                text.setTypeface(text.typeface, Typeface.NORMAL)
            }
        }

        val fragment: Fragment = when (index) {
            1 -> BookshelfFragment()
            2 -> ProfileFragment()
            else -> HomeFragment()
        }
        showFragment(fragment)
    }

    private fun showFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
