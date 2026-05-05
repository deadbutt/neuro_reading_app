package com.example.neuro

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.neuro.api.RetrofitClient
import com.example.neuro.api.model.ArticleChapterMeta
import com.example.neuro.api.model.ChapterContentResponse
import kotlinx.coroutines.launch

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

    private lateinit var articleId: String
    private var currentChapterIndex: Int = 0
    private var articleTitle: String = ""

    private var isBarsVisible = false
    private var isNightMode = false
    private var fontSize = 16f

    private var chapters: List<ArticleChapterMeta> = emptyList()

    // 已加载的章节数据
    private val loadedChapters = mutableMapOf<Int, ChapterContentResponse>()
    private var isLoadingNext = false
    private var isLoadingPrev = false

    private lateinit var rvReaderBody: RecyclerView
    private lateinit var tvReaderProgress: TextView
    private lateinit var sbReaderProgress: SeekBar
    private lateinit var rvChapters: RecyclerView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var paragraphAdapter: ParagraphAdapter

    private lateinit var llTopBar: LinearLayout
    private lateinit var llBottomBar: LinearLayout
    private lateinit var vTouchOverlay: View
    private lateinit var tvTopTitle: TextView
    private lateinit var tvFontSizePreview: TextView
    private lateinit var sbBrightness: SeekBar
    private lateinit var ivNightModeIcon: ImageView
    private lateinit var tvNightModeLabel: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader)

        articleId = intent.getStringExtra(EXTRA_ARTICLE_ID) ?: ""
        currentChapterIndex = intent.getIntExtra(EXTRA_CHAPTER_INDEX, 0)
        articleTitle = intent.getStringExtra(EXTRA_ARTICLE_TITLE) ?: ""

        if (articleId.isEmpty()) {
            Toast.makeText(this, "参数错误", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        initViews()
        setupTouchInteraction()
        setupTopBar()
        setupBottomBar()
        setupSideDrawer()
        setupReaderRecyclerView()
        setupBrightnessControl()

        loadChapterContent(currentChapterIndex)
        loadArticleChapters()
    }

    private fun initViews() {
        rvReaderBody = findViewById(R.id.rv_reader_body)
        tvReaderProgress = findViewById(R.id.tv_reader_progress)
        sbReaderProgress = findViewById(R.id.sb_reader_progress)
        rvChapters = findViewById(R.id.rv_chapters)
        drawerLayout = findViewById(R.id.dl_reader)

        llTopBar = findViewById(R.id.ll_reader_top_bar)
        llBottomBar = findViewById(R.id.ll_reader_bottom_bar)
        vTouchOverlay = findViewById(R.id.v_touch_overlay)
        tvTopTitle = findViewById(R.id.tv_reader_top_title)
        tvFontSizePreview = findViewById(R.id.tv_font_size_preview)
        sbBrightness = findViewById(R.id.sb_brightness)
        ivNightModeIcon = findViewById(R.id.iv_night_mode_icon)
        tvNightModeLabel = findViewById(R.id.tv_night_mode_label)
    }

    private fun setupReaderRecyclerView() {
        paragraphAdapter = ParagraphAdapter()
        val layoutManager = LinearLayoutManager(this)
        rvReaderBody.layoutManager = layoutManager
        rvReaderBody.adapter = paragraphAdapter

        rvReaderBody.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val firstVisible = layoutManager.findFirstVisibleItemPosition()
                val lastVisible = layoutManager.findLastVisibleItemPosition()
                val total = paragraphAdapter.itemCount

                // 根据当前可见位置更新 currentChapterIndex
                updateCurrentChapterByScroll(firstVisible)

                // 向下滑动到底，加载下一章
                if (dy > 0 && lastVisible >= total - 5 && !isLoadingNext) {
                    val lastChapterId = findLastChapterId()
                    val lastIndex = loadedChapters.entries.find { it.value.chapterId == lastChapterId }?.key
                    val nextIndex = lastIndex?.let { chapters.getOrNull(it + 1)?.index }
                    if (nextIndex != null && !loadedChapters.containsKey(nextIndex)) {
                        loadNextChapter(nextIndex)
                    }
                }

                // 向上滑动到顶，加载上一章
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

        // 从当前可见位置往前找最近的章节标题
        var chapterIndex = currentChapterIndex
        for (i in firstVisiblePosition downTo 0) {
            val item = items[i]
            if (item is ReaderItem.ChapterHeader) {
                // 找到对应的章节索引
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

    private fun loadChapterContent(chapterIndex: Int) {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getChapterContent(articleId, chapterIndex)
                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = response.body()?.data
                    data?.let {
                        loadedChapters[chapterIndex] = it
                        currentChapterIndex = chapterIndex
                        displayAllChapters()
                        updateTopTitle()
                    }
                } else {
                    Toast.makeText(this@ReaderActivity, "加载章节失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@ReaderActivity, "网络错误：${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadNextChapter(chapterIndex: Int) {
        if (isLoadingNext) return
        isLoadingNext = true

        // 添加加载中提示
        val lastChapterId = findLastChapterId()
        paragraphAdapter.appendItems(listOf(ReaderItem.Loading(lastChapterId)))

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getChapterContent(articleId, chapterIndex)
                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = response.body()?.data
                    data?.let {
                        loadedChapters[chapterIndex] = it
                        paragraphAdapter.removeLoading()
                        appendChapterToAdapter(it, chapterIndex)
                    }
                } else {
                    paragraphAdapter.removeLoading()
                }
            } catch (e: Exception) {
                paragraphAdapter.removeLoading()
            }
            isLoadingNext = false
        }
    }

    private fun loadPrevChapter(chapterIndex: Int) {
        if (isLoadingPrev) return
        isLoadingPrev = true

        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getChapterContent(articleId, chapterIndex)
                if (response.isSuccessful && response.body()?.code == 0) {
                    val data = response.body()?.data
                    data?.let {
                        loadedChapters[chapterIndex] = it
                        prependChapterToAdapter(it, chapterIndex)
                    }
                }
            } catch (e: Exception) {
                // 静默失败
            }
            isLoadingPrev = false
        }
    }

    private fun chapterToItems(content: ChapterContentResponse, chapterIndex: Int): List<ReaderItem> {
        val items = mutableListOf<ReaderItem>()

        // 章节标题
        items.add(ReaderItem.ChapterHeader(
            chapterId = content.chapterId,
            title = content.title,
            chapterNumber = chapterIndex + 1
        ))

        // 段落内容
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
        // 滚动到当前章节开头
        rvReaderBody.scrollToPosition(0)
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
        tvTopTitle.text = currentContent?.title ?: ""
    }

    private fun loadArticleChapters() {
        lifecycleScope.launch {
            try {
                val response = RetrofitClient.apiService.getArticleDetail(articleId)
                if (response.isSuccessful && response.body()?.code == 0) {
                    chapters = response.body()?.data?.chapters ?: emptyList()
                    setupChapterList()
                    updateProgress()
                }
            } catch (e: Exception) {
                // 静默失败
            }
        }
    }

    private fun updateProgress() {
        if (chapters.isNotEmpty()) {
            val progress = ((currentChapterIndex + 1) * 100 / chapters.size)
            sbReaderProgress.progress = progress
            tvReaderProgress.text = "$progress%"
        }
    }

    private fun setupChapterList() {
        val chapterItems = chapters.mapIndexed { index, chapter ->
            ChapterItem(
                name = chapter.title,
                isCurrent = index == currentChapterIndex
            )
        }

        rvChapters.layoutManager = LinearLayoutManager(this)
        rvChapters.adapter = ChapterAdapter(chapterItems) { _, position ->
            val targetIndex = chapters.getOrNull(position)?.index ?: position
            if (targetIndex != currentChapterIndex) {
                jumpToChapter(targetIndex)
            }
            drawerLayout.closeDrawer(GravityCompat.END)
            if (isBarsVisible) toggleBars()
        }
    }

    private fun jumpToChapter(chapterIndex: Int) {
        // 清空已加载数据，重新加载目标章节
        loadedChapters.clear()
        currentChapterIndex = chapterIndex
        loadChapterContent(chapterIndex)
        updateChapterListSelection(chapterIndex)
    }

    private fun updateChapterListSelection(newPosition: Int) {
        val adapter = rvChapters.adapter as? ChapterAdapter ?: return
        val newList = chapters.mapIndexed { index, chapter ->
            ChapterItem(
                name = chapter.title,
                isCurrent = index == newPosition
            )
        }
        adapter.updateData(newList)
    }

    private fun goToPrevChapter() {
        val currentContent = loadedChapters[currentChapterIndex]
        val prevIndex = currentContent?.prevChapterId
        if (prevIndex != null && prevIndex >= 0) {
            jumpToChapter(prevIndex)
        } else {
            Toast.makeText(this, "已经是第一章了", Toast.LENGTH_SHORT).show()
        }
    }

    private fun goToNextChapter() {
        val currentContent = loadedChapters[currentChapterIndex]
        val nextIndex = currentContent?.nextChapterId
        if (nextIndex != null) {
            jumpToChapter(nextIndex)
        } else {
            Toast.makeText(this, "已经是最后一章了", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupTouchInteraction() {
        vTouchOverlay.setOnClickListener {
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
        llTopBar.visibility = View.VISIBLE
        llBottomBar.visibility = View.VISIBLE

        llTopBar.translationY = -llTopBar.height.toFloat()
        llBottomBar.translationY = llBottomBar.height.toFloat()

        llTopBar.animate()
            .translationY(0f)
            .setDuration(250)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()

        llBottomBar.animate()
            .translationY(0f)
            .setDuration(250)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .start()
    }

    private fun hideBars() {
        llTopBar.animate()
            .translationY(-llTopBar.height.toFloat())
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                llTopBar.visibility = View.GONE
            }
            .start()

        llBottomBar.animate()
            .translationY(llBottomBar.height.toFloat())
            .setDuration(200)
            .setInterpolator(AccelerateDecelerateInterpolator())
            .withEndAction {
                llBottomBar.visibility = View.GONE
            }
            .start()
    }

    private fun setupTopBar() {
        findViewById<View>(R.id.iv_reader_back).setOnClickListener { finish() }
        findViewById<View>(R.id.iv_reader_toc).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }
        findViewById<View>(R.id.iv_reader_more).setOnClickListener {
            Toast.makeText(this, "更多功能", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomBar() {
        findViewById<LinearLayout>(R.id.ll_btn_catalog).setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        findViewById<LinearLayout>(R.id.ll_btn_night_mode).setOnClickListener {
            toggleNightMode()
        }

        findViewById<LinearLayout>(R.id.ll_btn_settings).setOnClickListener {
            Toast.makeText(this, "阅读设置", Toast.LENGTH_SHORT).show()
        }

        findViewById<LinearLayout>(R.id.ll_btn_prev).setOnClickListener {
            goToPrevChapter()
        }

        findViewById<LinearLayout>(R.id.ll_btn_next).setOnClickListener {
            goToNextChapter()
        }

        findViewById<View>(R.id.btn_font_small).setOnClickListener {
            fontSize = (fontSize - 2).coerceAtLeast(12f)
            updateFontSize()
        }
        findViewById<View>(R.id.btn_font_large).setOnClickListener {
            fontSize = (fontSize + 2).coerceAtMost(24f)
            updateFontSize()
        }

        sbReaderProgress.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    if (fromUser) {
                        tvReaderProgress.text = "$progress%"
                    }
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    val progress = seekBar?.progress ?: 0
                    val targetPosition = progress * (chapters.size - 1) / 100
                    val targetIndex = chapters.getOrNull(targetPosition)?.index ?: targetPosition
                    if (targetIndex in chapters.map { it.index }) {
                        jumpToChapter(targetIndex)
                    }
                }
            }
        )
    }

    private fun setupBrightnessControl() {
        val brightness = Settings.System.getInt(contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        sbBrightness.progress = brightness * 100 / 255

        sbBrightness.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
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
        val contentLayout = findViewById<View>(R.id.fl_reader_content)
        if (isNightMode) {
            contentLayout.setBackgroundColor(Color.parseColor("#1A1A1A"))
            ivNightModeIcon.setImageResource(R.drawable.ic_reader_day)
            tvNightModeLabel.text = "日间"
        } else {
            contentLayout.setBackgroundColor(Color.parseColor("#F5E6C8"))
            ivNightModeIcon.setImageResource(R.drawable.ic_reader_night)
            tvNightModeLabel.text = "夜间"
        }
        paragraphAdapter.notifyDataSetChanged()
    }

    private fun updateFontSize() {
        tvFontSizePreview.text = fontSize.toInt().toString()
        paragraphAdapter.notifyDataSetChanged()
    }

    private fun setupSideDrawer() {
        findViewById<View>(R.id.iv_drawer_close).setOnClickListener {
            drawerLayout.closeDrawer(GravityCompat.END)
        }
    }

    override fun onBackPressed() {
        when {
            drawerLayout.isDrawerOpen(GravityCompat.END) -> {
                drawerLayout.closeDrawer(GravityCompat.END)
            }
            isBarsVisible -> {
                toggleBars()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }
}
