package com.example.neuro

import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.neuro.databinding.FragmentHomeBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var currentTab: Int = 0
    private var indicatorInitialized = false
    private var lastClickTime = 0L
    private val clickCooldown = 300L

    private val fragments = mutableMapOf<Int, Fragment>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTabs()
        setupSearchBar()
        switchTab(0)
    }

    private fun setupTabs() {
        val tabs = listOf(binding.tvTabRecommend, binding.tvTabHot, binding.tvTabLatest)

        tabs.forEachIndexed { index, tv ->
            tv.setOnClickListener {
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastClickTime < clickCooldown) {
                    return@setOnClickListener
                }
                lastClickTime = currentTime

                if (currentTab != index) {
                    switchTab(index)
                }
            }
        }
    }

    private fun switchTab(index: Int) {
        currentTab = index
        val tabs = listOf(binding.tvTabRecommend, binding.tvTabHot, binding.tvTabLatest)

        for ((i, tv) in tabs.withIndex()) {
            if (i == index) {
                tv.setTextColor(requireContext().getColor(R.color.primary_red))
                tv.setTypeface(null, Typeface.BOLD)
            } else {
                tv.setTextColor(requireContext().getColor(R.color.tab_inactive))
                tv.setTypeface(null, Typeface.NORMAL)
            }
        }

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

        val transaction = childFragmentManager.beginTransaction()

        fragments.values.forEach { f ->
            if (f != fragment && f.isAdded) {
                transaction.hide(f)
            }
        }

        if (fragment.isAdded) {
            transaction.show(fragment)
        } else {
            transaction.add(R.id.fl_home_content, fragment)
        }

        transaction.commitAllowingStateLoss()
    }

    private fun moveIndicatorToTab(index: Int) {
        val tabs = listOf(binding.tvTabRecommend, binding.tvTabHot, binding.tvTabLatest)
        val targetTab = tabs.getOrNull(index) ?: return

        binding.vIndicator.post {
            if (!isAdded || binding.vIndicator.parent == null) return@post

            val tabWidth = targetTab.width.toFloat()
            val indicatorWidth = binding.vIndicator.width.toFloat()
            val tabLeft = targetTab.x

            val targetX = tabLeft + (tabWidth - indicatorWidth) / 2f

            if (!indicatorInitialized) {
                binding.vIndicator.translationX = targetX
                indicatorInitialized = true
            } else {
                binding.vIndicator.animate()
                    .translationX(targetX)
                    .setDuration(200)
                    .start()
            }
        }
    }

    private fun setupSearchBar() {
        binding.etSearch.setOnClickListener {
            (requireActivity() as MainActivity).navigateToSearch()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
