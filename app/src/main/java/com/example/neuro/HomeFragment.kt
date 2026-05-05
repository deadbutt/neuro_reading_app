package com.example.neuro

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class HomeFragment : Fragment() {

    private lateinit var flHomeContent: FrameLayout
    private lateinit var tvTabRecommend: TextView
    private lateinit var tvTabHot: TextView
    private lateinit var tvTabLatest: TextView
    private lateinit var vIndicator: View
    private var currentTab: Int = 0
    private var indicatorInitialized = false

    private val fragments = mutableMapOf<Int, Fragment>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        flHomeContent = view.findViewById(R.id.fl_home_content)
        tvTabRecommend = view.findViewById(R.id.tv_tab_recommend)
        tvTabHot = view.findViewById(R.id.tv_tab_hot)
        tvTabLatest = view.findViewById(R.id.tv_tab_latest)
        vIndicator = view.findViewById(R.id.v_indicator)

        setupTabs()
        setupSearchBar(view)
        switchTab(0)
    }

    private fun setupTabs() {
        val tabs = listOf(tvTabRecommend, tvTabHot, tvTabLatest)

        tabs.forEachIndexed { index, tv ->
            tv.setOnClickListener {
                if (currentTab != index) {
                    switchTab(index)
                }
            }
        }
    }

    private fun switchTab(index: Int) {
        currentTab = index
        val tabs = listOf(tvTabRecommend, tvTabHot, tvTabLatest)

        for ((i, tv) in tabs.withIndex()) {
            if (i == index) {
                tv.setTextColor(requireContext().getColor(R.color.primary_red))
                tv.setTypeface(null, Typeface.BOLD)
            } else {
                tv.setTextColor(requireContext().getColor(R.color.tab_inactive))
                tv.setTypeface(null, Typeface.NORMAL)
            }
        }

        // 移动下划线到选中的 Tab
        moveIndicatorToTab(index)

        val fragment = fragments.getOrPut(index) {
            BookListFragment.newInstance(
                when (index) {
                    0 -> BookListFragment.TYPE_RECOMMEND
                    1 -> BookListFragment.TYPE_HOT
                    2 -> BookListFragment.TYPE_LATEST
                    else -> BookListFragment.TYPE_RECOMMEND
                }
            )
        }

        childFragmentManager.beginTransaction()
            .replace(R.id.fl_home_content, fragment)
            .commitAllowingStateLoss()
    }

    private fun moveIndicatorToTab(index: Int) {
        val tabs = listOf(tvTabRecommend, tvTabHot, tvTabLatest)
        val targetTab = tabs.getOrNull(index) ?: return

        vIndicator.post {
            if (!isAdded || vIndicator.parent == null) return@post

            val tabWidth = targetTab.width.toFloat()
            val indicatorWidth = vIndicator.width.toFloat()
            val tabLeft = targetTab.x

            val targetX = tabLeft + (tabWidth - indicatorWidth) / 2f

            if (!indicatorInitialized) {
                vIndicator.translationX = targetX
                indicatorInitialized = true
            } else {
                vIndicator.animate()
                    .translationX(targetX)
                    .setDuration(200)
                    .start()
            }
        }
    }

    private fun setupSearchBar(view: View) {
        view.findViewById<View>(R.id.et_search).setOnClickListener {
            (requireActivity() as MainActivity).navigateToSearch()
        }
    }
}
