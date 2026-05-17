package com.example.neuro

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.SeekBar
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.neuro.api.model.ArticleChapterMeta
import com.example.neuro.api.model.ChapterContentResponse
import com.example.neuro.base.UiState
import com.example.neuro.databinding.ActivityReaderBinding
import com.example.neuro.viewmodel.ReaderViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReaderActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ARTICLE_ID = "article_id"
        private const val EXTRA_CHAPTER_INDEX = "chapter_index"
        private const val EXTRA_ARTICLE_TITLE = "article_title"

        fun start(context: android.content.Context, articleId: String, chapterIndex: Int, articleTitle: String) {
            context.startActivity(Intent(context, ReaderActivity::class.java).apply {
                putExtra(EXTRA_ARTICLE_ID, articleId)
                putExtra(EXTRA_CHAPTER_INDEX, chapterIndex)
                putExtra(EXTRA_ARTICLE_TITLE, articleTitle)
            })
        }
    }

    private lateinit var binding: ActivityReaderBinding
    private val viewModel: ReaderViewModel by viewModels()

    private lateinit var articleId: String
    private var currentChapterIndex: Int = 0
    private var articleTitle: String = ""

    private var isBarsVisible = false
    private var isNightMode = false
    private var fontSize = 16f

    private var chapters: List<ArticleChapterMeta> = emptyList()
    private var totalWordCount: Int = 0
    private val loadedChapters = mutableMapOf<Int, ChapterContentResponse>()
    private var isLoadingNext = false
    private var isLoadingPrev = false
    private var pendingNextChapterIndex: Int? = null
    private var pendingPrevChapterIndex: Int? = null
    private var currentScrollPosition = 0

    private lateinit var paragraphAdapter: ParagraphAdapter
    private var autoSaveJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReaderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        articleId = intent.getStringExtra(EXTRA_ARTICLE_ID) ?: ""
        currentChapterIndex = intent.getIntExtra(EXTRA_CHAPTER_INDEX, 0)
        articleTitle = intent.getStringExtra(EXTRA_ARTICLE_TITLE) ?: ""

        if (articleId.isEmpty()) {
            Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupTouchInteraction()
        setupTopBar()
        setupBottomBar()
        setupSideDrawer()
        setupReaderRecyclerView()
        setupBrightnessControl()
        observeViewModel()
        startAutoSaveProgress()

        viewModel.loadArticleMeta(articleId)
    }

    private fun startAutoSaveProgress() {
        autoSaveJob = lifecycleScope.launch {
            while (true) {
                delay(30000)
                if (::articleId.isInitialized && articleId.isNotEmpty() && chapters.isNotEmpty()) {
                    val progress = binding.sbReaderProgress.progress
                    viewModel.saveProgress(articleId, currentChapterIndex, progress, currentScrollPosition)
                }
            }
        }
    }

    private fun doSaveProgress() {
        if (!::articleId.isInitialized || articleId.isEmpty() || chapters.isEmpty()) return
        val items = paragraphAdapter.currentItems.filter { it !is ReaderItem.Loading }
        val totalItems = items.size
        val layoutManager = binding.rvReaderBody.layoutManager as? LinearLayoutManager
        val firstVisible = layoutManager?.findFirstVisibleItemPosition() ?: 0
        val lastVisible = layoutManager?.findLastVisibleItemPosition() ?: firstVisible

        val visibleItems = paragraphAdapter.currentItems
        var filteredFirstVisible = 0
        var visibleCount = 0
        for (i in visibleItems.indices) {
            if (visibleItems[i] !is ReaderItem.Loading) {
                if (i <= firstVisible) {
                    filteredFirstVisible = visibleCount
                }
                visibleCount++
            }
        }

        val progress = if (totalItems > 0) {
            if (lastVisible >= paragraphAdapter.itemCount - 1 && paragraphAdapter.currentItems.lastOrNull() !is ReaderItem.Loading) {
                100
            } else {
                ((filteredFirstVisible + 1) * 100 / totalItems).coerceIn(0, 100)
            }
        } else {
            (currentChapterIndex + 1) * 100 / chapters.size
        }
        viewModel.saveProgress(articleId, currentChapterIndex, progress, currentScrollPosition)
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is UiState.Error -> {
                                onChapterLoadFailed()
                                Toast.makeText(this@ReaderActivity, state.message, Toast.LENGTH_SHORT).show()
                            }
                            else -> {}
                        }
                    }
                }

                launch {
                    viewModel.chapterContent.collect { content ->
                        content ?: return@collect

                        val nextIdx = pendingNextChapterIndex
                        val prevIdx = pendingPrevChapterIndex

                        when {
                            nextIdx != null -> {
                                paragraphAdapter.removeLoading()
                                loadedChapters[nextIdx] = content
                                appendChapterToAdapter(content, nextIdx)
                                pendingNextChapterIndex = null
                                isLoadingNext = false
                            }
                            prevIdx != null -> {
                                paragraphAdapter.removeLoading()
                                loadedChapters[prevIdx] = content
                                val prevItemCount = chapterToItems(content, prevIdx).size
                                prependChapterToAdapter(content, prevIdx)
                                binding.rvReaderBody.post {
                                    val layoutManager = binding.rvReaderBody.layoutManager as? LinearLayoutManager
                                    val currentFirst = layoutManager?.findFirstVisibleItemPosition() ?: 0
                                    binding.rvReaderBody.scrollToPosition(currentFirst + prevItemCount)
                                }
                                pendingPrevChapterIndex = null
                                isLoadingPrev = false
                            }
                            else -> {
                                loadedChapters[currentChapterIndex] = content
                                displayAllChapters()
                                updateTopTitle()
                            }
                        }
                    }
                }

                launch {
                    viewModel.chapters.collect { chapterList ->
                        if (chapterList.isNotEmpty()) {
                            chapters = chapterList
                            setupChapterList()
                            if (chapterList.size == 1) {
                                viewModel.loadChapter(articleId, currentChapterIndex)
                            } else {
                                viewModel.loadAllChapters(articleId)
                            }
                        }
                    }
                }

                launch {
                    viewModel.allChapterContents.collect { allContents ->
                        if (allContents.isNotEmpty()) {
                            loadedChapters.clear()
                            loadedChapters.putAll(allContents)
                            displayAllChapters()
                            updateTopTitle()
                        }
                    }
                }

                launch {
                    viewModel.totalWordCount.collect { count ->
                        totalWordCount = count
                    }
                }
            }
        }
    }

    private fun onChapterLoadFailed() {
        paragraphAdapter.removeLoading()
        isLoadingNext = false
        isLoadingPrev = false
        pendingNextChapterIndex = null
        pendingPrevChapterIndex = null
    }

    private fun setupReaderRecyclerView() {
        paragraphAdapter = ParagraphAdapter()
        val layoutManager = LinearLayoutManager(this)
        binding.rvReaderBody.layoutManager = layoutManager
        binding.rvReaderBody.adapter = paragraphAdapter

        binding.rvReaderBody.addOnScrollListener(object : androidx.recyclerview.widget.RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: androidx.recyclerview.widget.RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val total = paragraphAdapter.itemCount

                currentScrollPosition = firstVisible

                updateCurrentChapterByScroll(firstVisible)

                if (isBarsVisible) {
                    updateProgress()
                }

                if (dy > 0 && lastVisible >= total - 5 && !isLoadingNext) {
                    val lastChapterId = findLastChapterId()
                    val lastIndex = loadedChapters.entries.find { it.value.chapterId == lastChapterId }?.key
                    val nextIndex = lastIndex?.let { chapters.getOrNull(it + 1)?.index }
                    if (nextIndex != null && !loadedChapters.containsKey(nextIndex)) {
                        loadNextChapter(nextIndex)
                    }
                }

                if (dy < 0 && firstVisible <= 3 && !isLoadingPrev) {
                    val firstChapterId = findFirstChapterId()
                    val firstIndex = loadedChapters.entries.find { it.value.chapterId == firstChapterId }?.key
                    val prevIndex = firstIndex?.let { chapters.getOrNull(it - 1)?.index }
                    if (prevIndex != null && prevIndex >= 0 && !loadedChapters.containsKey(prevIndex)) {
                        loadPrevChapter(prevIndex)
                    }
                }
            }
        })
    }

    private fun updateCurrentChapterByScroll(firstVisiblePosition: Int) {
        val items = paragraphAdapter.currentItems
        if (items.isEmpty() || firstVisiblePosition < 0 || firstVisiblePosition >= items.size) return

        var chapterIndex = currentChapterIndex
        for (i in firstVisiblePosition downTo 0) {
            val item = items[i]
            if (item is ReaderItem.ChapterHeader) {
                val idx = loadedChapters.entries.find { it.value.chapterId == item.chapterId }?.key
                if (idx != null) {
                    chapterIndex = idx
                    break
                }
            }
        }

        if (chapterIndex != currentChapterIndex) {
            currentChapterIndex = chapterIndex
            updateTopTitle()
            updateProgress()
            updateChapterListSelection(chapterIndex)
        }
    }

    private fun findFirstChapterId(): String {
        return paragraphAdapter.currentItems.firstOrNull { it is ReaderItem.ChapterHeader }?.chapterId ?: ""
    }

    private fun findLastChapterId(): String {
        return paragraphAdapter.currentItems.lastOrNull { it is ReaderItem.ChapterHeader }?.chapterId ?: ""
    }

    private fun loadNextChapter(chapterIndex: Int) {
        if (isLoadingNext) return
        isLoadingNext = true
        pendingNextChapterIndex = chapterIndex

        val lastChapterId = findLastChapterId()
        paragraphAdapter.appendItems(listOf(ReaderItem.Loading(lastChapterId)))

        viewModel.loadChapter(articleId, chapterIndex)
    }

    private fun loadPrevChapter(chapterIndex: Int) {
        if (isLoadingPrev) return
        isLoadingPrev = true
        pendingPrevChapterIndex = chapterIndex

        val firstChapterId = findFirstChapterId()
        paragraphAdapter.prependItems(listOf(ReaderItem.Loading(firstChapterId)))

        viewModel.loadChapter(articleId, chapterIndex)
    }

    private fun chapterToItems(content: ChapterContentResponse, chapterIndex: Int): List<ReaderItem> {
        val items = mutableListOf<ReaderItem>()

        items.add(ReaderItem.ChapterHeader(
            chapterId = content.chapterId,
            title = content.title,
            chapterNumber = chapterIndex + 1
        ))

        val paragraphs = if (content.paragraphs.isNotEmpty()) {
            content.paragraphs
        } else {
            content.content.split("\n")
        }

        paragraphs.forEachIndexed { index, text ->
            if (text.isNotBlank()) {
                items.add(ReaderItem.Paragraph(
                    chapterId = content.chapterId,
                    text = text,
                    index = index
                ))
            }
        }

        return items
    }

    private fun displayAllChapters() {
        val sortedIndices = loadedChapters.keys.sorted()
        val allItems = mutableListOf<ReaderItem>()
        sortedIndices.forEach { index ->
            loadedChapters[index]?.let {
                allItems.addAll(chapterToItems(it, index))
            }
        }
        paragraphAdapter.updateData(allItems)
        binding.rvReaderBody.scrollToPosition(0)
    }

    private fun appendChapterToAdapter(content: ChapterContentResponse, chapterIndex: Int) {
        val items = chapterToItems(content, chapterIndex)
        paragraphAdapter.appendItems(items)
    }

    private fun prependChapterToAdapter(content: ChapterContentResponse, chapterIndex: Int) {
        val items = chapterToItems(content, chapterIndex)
        paragraphAdapter.prependItems(items)
    }

    private fun updateTopTitle() {
        val currentContent = loadedChapters[currentChapterIndex]
        binding.tvReaderTopTitle.text = currentContent?.title ?: ""
    }

    private fun updateProgress() {
        val items = paragraphAdapter.currentItems.filter { it !is ReaderItem.Loading }
        val totalItems = items.size
        if (totalItems <= 0) return

        val layoutManager = binding.rvReaderBody.layoutManager as? LinearLayoutManager
        val firstVisible = layoutManager?.findFirstVisibleItemPosition() ?: 0
        val lastVisible = layoutManager?.findLastVisibleItemPosition() ?: firstVisible

        // 将RecyclerView的位置映射到过滤后的items位置
        val visibleItems = paragraphAdapter.currentItems
        var filteredFirstVisible = 0
        var visibleCount = 0
        for (i in visibleItems.indices) {
            if (visibleItems[i] !is ReaderItem.Loading) {
                if (i <= firstVisible) {
                    filteredFirstVisible = visibleCount
                }
                visibleCount++
            }
        }

        val progress = if (lastVisible >= paragraphAdapter.itemCount - 1) {
            100
        } else {
            ((filteredFirstVisible + 1) * 100 / totalItems).coerceIn(0, 100)
        }
        binding.sbReaderProgress.progress = progress
        binding.tvReaderProgress.text = "$progress%"
    }

    private fun setupChapterList() {
        val chapterItems = chapters.mapIndexed { index, chapter ->
            ChapterItem(
                name = chapter.title,
                isCurrent = index == currentChapterIndex
            )
        }

        binding.rvChapters.layoutManager = LinearLayoutManager(this)
        binding.rvChapters.adapter = ChapterAdapter(chapterItems) { _, position ->
            val targetIndex = chapters.getOrNull(position)?.index ?: position
            if (targetIndex != currentChapterIndex) {
                jumpToChapter(targetIndex)
            }
            binding.dlReader.closeDrawer(GravityCompat.END)
            if (isBarsVisible) toggleBars()
        }
    }

    private fun jumpToChapter(chapterIndex: Int) {
        if (loadedChapters.containsKey(chapterIndex)) {
            // 如果章节已加载，直接滚动到该章节
            scrollToChapter(chapterIndex)
        } else {
            // 如果章节未加载，清空并加载该章节
            loadedChapters.clear()
            pendingNextChapterIndex = null
            pendingPrevChapterIndex = null
            currentChapterIndex = chapterIndex
            viewModel.loadChapter(articleId, chapterIndex)
            updateChapterListSelection(chapterIndex)
        }
    }

    private fun scrollToChapter(chapterIndex: Int) {
        val targetChapterId = chapters.getOrNull(chapterIndex)?.chapterId ?: return
        val items = paragraphAdapter.currentItems
        var targetPosition = -1
        for (i in items.indices) {
            val item = items[i]
            if (item is ReaderItem.ChapterHeader && item.chapterId == targetChapterId) {
                targetPosition = i
                break
            }
        }
        if (targetPosition >= 0) {
            binding.rvReaderBody.scrollToPosition(targetPosition)
            currentChapterIndex = chapterIndex
            updateTopTitle()
            updateChapterListSelection(chapterIndex)
        }
    }

    private fun updateChapterListSelection(newPosition: Int) {
        val adapter = binding.rvChapters.adapter as? ChapterAdapter ?: return
        val newList = chapters.mapIndexed { index, chapter ->
            ChapterItem(
                name = chapter.title,
                isCurrent = index == newPosition
            )
        }
        adapter.updateData(newList)
    }

    private fun goToPrevChapter() {
        if (currentChapterIndex > 0) {
            jumpToChapter(currentChapterIndex - 1)
        } else {
            Toast.makeText(this, "已经是第一章了", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goToNextChapter() {
        if (currentChapterIndex < chapters.size - 1) {
            jumpToChapter(currentChapterIndex + 1)
        } else {
            Toast.makeText(this, "已经是最后一章了", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTouchInteraction() {
        binding.vTouchOverlay.setOnClickListener {
            toggleBars()
        }
    }

    private fun toggleBars() {
        isBarsVisible = !isBarsVisible
        if (isBarsVisible) {
            showBars()
        } else {
            hideBars()
        }
    }

    private fun showBars() {
        binding.llReaderTopBar.visibility = View.VISIBLE
        binding.llReaderBottomBar.visibility = View.VISIBLE

        binding.llReaderTopBar.translationY = -binding.llReaderTopBar.height.toFloat()
        binding.llReaderBottomBar.translationY = binding.llReaderBottomBar.height.toFloat()

        binding.llReaderTopBar.animate()
            .translationY(0f)
            .setDuration(250)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        binding.llReaderBottomBar.animate()
            .translationY(0f)
            .setDuration(250)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        updateProgress()
    }

    private fun hideBars() {
        binding.llReaderTopBar.animate()
            .translationY(-binding.llReaderTopBar.height.toFloat())
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                binding.llReaderTopBar.visibility = View.GONE
            }
            .start()

        binding.llReaderBottomBar.animate()
            .translationY(binding.llReaderBottomBar.height.toFloat())
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                binding.llReaderBottomBar.visibility = View.GONE
            }
            .start()
    }

    private fun setupTopBar() {
        binding.ivReaderBack.setOnClickListener { finish() }
        binding.ivReaderToc.setOnClickListener {
            binding.dlReader.openDrawer(GravityCompat.END)
        }
        binding.ivReaderMore.setOnClickListener {
            Toast.makeText(this, "更多功能", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomBar() {
        binding.llBtnCatalog.setOnClickListener {
            binding.dlReader.openDrawer(GravityCompat.END)
        }

        binding.llBtnNightMode.setOnClickListener {
            toggleNightMode()
        }

        binding.llBtnSettings.setOnClickListener {
            Toast.makeText(this, "阅读设置", Toast.LENGTH_SHORT).show()
        }

        binding.llBtnPrev.setOnClickListener {
            goToPrevChapter()
        }

        binding.llBtnNext.setOnClickListener {
            goToNextChapter()
        }

        binding.btnFontSmall.setOnClickListener {
            fontSize = (fontSize - 2).coerceAtLeast(12f)
            updateFontSize()
        }
        binding.btnFontLarge.setOnClickListener {
            fontSize = (fontSize + 2).coerceAtMost(24f)
            updateFontSize()
        }

        binding.sbReaderProgress.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        binding.tvReaderProgress.text = "$progress%"
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    val progress = seekBar?.progress ?: 0
                    val totalItems = paragraphAdapter.itemCount
                    if (totalItems > 0) {
                        val targetPosition = progress * totalItems / 100
                        binding.rvReaderBody.scrollToPosition(targetPosition.coerceIn(0, totalItems - 1))
                    }
                }
            }
        )
    }

    private fun setupBrightnessControl() {
        val brightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        binding.sbBrightness.progress = brightness * 100 / 255

        binding.sbBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val lp = window.attributes
                    lp.screenBrightness = progress / 100f
                    window.attributes = lp
                }
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun toggleNightMode() {
        isNightMode = !isNightMode
        if (isNightMode) {
            binding.flReaderContent.setBackgroundColor(Color.parseColor("#1A1A1A"))
            binding.ivNightModeIcon.setImageResource(R.drawable.ic_reader_day)
            binding.tvNightModeLabel.text = "日间"
        } else {
            binding.flReaderContent.setBackgroundColor(Color.parseColor("#F5E6C8"))
            binding.ivNightModeIcon.setImageResource(R.drawable.ic_reader_night)
            binding.tvNightModeLabel.text = "夜间"
        }
        paragraphAdapter.notifyDataSetChanged()
    }

    private fun updateFontSize() {
        binding.tvFontSizePreview.text = fontSize.toInt().toString()
        paragraphAdapter.notifyDataSetChanged()
    }

    private fun setupSideDrawer() {
        binding.ivDrawerClose.setOnClickListener {
            binding.dlReader.closeDrawer(GravityCompat.END)
        }
    }

    override fun onBackPressed() {
        when {
            binding.dlReader.isDrawerOpen(GravityCompat.END) -> {
                binding.dlReader.closeDrawer(GravityCompat.END)
            }
            isBarsVisible -> {
                toggleBars()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        doSaveProgress()
    }

    override fun onStop() {
        super.onStop()
        doSaveProgress()
    }

    override fun onDestroy() {
        super.onDestroy()
        autoSaveJob?.cancel()
    }
}
