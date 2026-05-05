package com.example.neuro

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop

class ArticleDetailActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_DATE = "extra_date"
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_URL = "extra_url"
        private const val HEADER_BG_URL = "https://placehold.co/800x400/1a1a2e/ffffff?text=Article"
        private const val AVATAR_URL = "https://placehold.co/200x200/1a1a2e/ffffff?text=Avatar"
        private const val BILIBILI_URL = "https://space.bilibili.com/"
    }

    private var articleUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_article_detail)

        loadHeaderImage()

        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val date = intent.getStringExtra(EXTRA_DATE) ?: ""
        val category = intent.getStringExtra(EXTRA_CATEGORY) ?: ""
        articleUrl = intent.getStringExtra(EXTRA_URL)

        findViewById<TextView>(R.id.detail_title).text = title
        findViewById<TextView>(R.id.detail_date).text = date
        findViewById<TextView>(R.id.detail_category).text = category

        loadAuthorAvatar()
        setupWebView()
        setupCommentSection()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadHeaderImage() {
        Glide.with(this)
            .load(HEADER_BG_URL)
            .centerCrop()
            .into(findViewById<ImageView>(R.id.detail_header_bg))

        Glide.with(this)
            .load(AVATAR_URL)
            .transform(CircleCrop())
            .into(findViewById<ImageView>(R.id.detail_author_avatar))
    }

    private fun setupWebView() {
        val webView = findViewById<WebView>(R.id.detail_webview)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = true
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                findViewById<View>(R.id.detail_webview).visibility = View.VISIBLE
            }

            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                request?.url?.let { uri ->
                    if (uri.scheme == "http" || uri.scheme == "https") {
                        if (uri.host?.contains("book.neurosama.chat") == true) {
                            return false
                        }
                        try {
                            startActivity(Intent(Intent.ACTION_VIEW, uri))
                        } catch (_: Exception) {}
                        return true
                    }
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                injectReadingStyle(view)
            }
        }

        articleUrl?.let { url ->
            webView.loadUrl(url)
        } ?: run {
            webView.loadData(
                "<html><body style='text-align:center;padding-top:80px;color:#999;font-size:16px;'>暂无文章链接</body></html>",
                "text/html", "UTF-8"
            )
        }
    }

    private fun injectReadingStyle(webView: WebView?) {
        val css = """
            javascript:(function() {
                var style = document.createElement('style');
                style.innerHTML = `
                    body { padding: 8px 12px !important; line-height: 1.8 !important; word-break: break-word !important; }
                    img { max-width: 100% !important; height: auto !important; }
                    pre, code { max-width: 100% !important; overflow-x: auto !important; white-space: pre-wrap !important; word-wrap: break-word !important; }
                    #page-header, #footer, #rightside, #sidebar, #nav, #site-info, #scroll-down, .js-pjax { display: none !important; }
                    #body-wrap { padding-top: 0 !important; }
                    .layout { max-width: 100% !important; padding: 0 !important; }
                `;
                document.head.appendChild(style);
            })();
        """.trimIndent().replace("\n", " ")
        webView?.loadUrl(css)
    }

    private fun setupCommentSection() {
        findViewById<View>(R.id.card_comments_section).setOnClickListener {
            articleUrl?.let { url ->
                val commentUrl = "$url#comments"
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(commentUrl)))
                } catch (_: Exception) {
                    Toast.makeText(this, "无法打开评论页面", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun loadAuthorAvatar() {
        findViewById<TextView>(R.id.detail_author_link_hint).setOnClickListener {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(BILIBILI_URL)))
            } catch (_: Exception) {}
        }
    }
}
