package com.example.neuro

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.CompoundButton
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ReaderSettingsActivity : AppCompatActivity() {

    companion object {
        fun start(context: Context) {
            context.startActivity(Intent(context, ReaderSettingsActivity::class.java))
        }
    }

    private lateinit var tvPreview: TextView
    private var currentFontId = R.id.ll_font_default
    private var currentBgId = R.id.fl_bg_day
    private var currentFlipId = R.id.tv_flip_sim

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reader_settings)

        tvPreview = findViewById(R.id.tv_preview_text)

        findViewById<View>(R.id.iv_settings_back).setOnClickListener { finish() }
        findViewById<View>(R.id.tv_reset_default).setOnClickListener { resetDefaults() }

        setupFontSelection()
        setupFontSize()
        setupBackgroundColor()
        setupFlipMode()
        setupSwitches()
    }

    private fun setupFontSelection() {
        val fontOptions = listOf(
            R.id.ll_font_default,
            R.id.ll_font_siyuan,
            R.id.ll_font_kaiti,
            R.id.ll_font_fangsong
        )
        fontOptions.forEach { id ->
            findViewById<View>(id).setOnClickListener {
                selectFont(id)
            }
        }
    }

    private fun selectFont(id: Int) {
        updateFontUI(currentFontId, false)
        currentFontId = id
        updateFontUI(id, true)
    }

    private fun updateFontUI(id: Int, selected: Boolean) {
        val view = findViewById<View>(id)
        val bg = if (selected) R.drawable.bg_rs_font_option_selected else R.drawable.bg_rs_font_option
        view.setBackgroundResource(bg)

        if (view is android.widget.LinearLayout) {
            for (i in 0 until view.childCount) {
                val child = view.getChildAt(i)
                if (child is TextView) {
                    child.setTextColor(if (selected) getColor(R.color.primary_red) else getColor(R.color.text_primary))
                }
            }
        }
    }

    private fun setupFontSize() {
        findViewById<View>(R.id.btn_size_small).setOnClickListener {
            val sb = findViewById<SeekBar>(R.id.sb_font_size)
            sb.progress = (sb.progress - 1).coerceAtLeast(0)
            updatePreviewSize(sb.progress)
        }
        findViewById<View>(R.id.btn_size_large).setOnClickListener {
            val sb = findViewById<SeekBar>(R.id.sb_font_size)
            sb.progress = (sb.progress + 1).coerceAtMost(sb.max)
            updatePreviewSize(sb.progress)
        }
        findViewById<SeekBar>(R.id.sb_font_size).setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                    updatePreviewSize(progress)
                }
                override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                override fun onStopTrackingTouch(seekBar: SeekBar?) {}
            }
        )
    }

    private fun updatePreviewSize(progress: Int) {
        val size = 14f + progress * 1f
        tvPreview.textSize = size
    }

    private fun setupBackgroundColor() {
        val bgOptions = listOf(
            R.id.fl_bg_day,
            R.id.fl_bg_night,
            R.id.fl_bg_eye,
            R.id.fl_bg_parchment
        )
        bgOptions.forEach { id ->
            findViewById<View>(id).setOnClickListener {
                selectBackground(id)
            }
        }
    }

    private fun selectBackground(id: Int) {
        val colorMap = mapOf(
            R.id.fl_bg_day to Color.WHITE,
            R.id.fl_bg_night to Color.parseColor("#1A1A1A"),
            R.id.fl_bg_eye to Color.parseColor("#C8E6C0"),
            R.id.fl_bg_parchment to Color.parseColor("#F5E6C8")
        )
        tvPreview.setBackgroundColor(colorMap[id] ?: Color.WHITE)
        tvPreview.setTextColor(
            if (id == R.id.fl_bg_night) Color.parseColor("#D0D0D0") else getColor(R.color.reader_text)
        )
        currentBgId = id
    }

    private fun setupFlipMode() {
        val flipOptions = listOf(R.id.tv_flip_sim, R.id.tv_flip_cover, R.id.tv_flip_slide, R.id.tv_flip_none)
        flipOptions.forEach { id ->
            findViewById<TextView>(id).setOnClickListener {
                selectFlip(id)
            }
        }
    }

    private fun selectFlip(id: Int) {
        val flipOptions = listOf(R.id.tv_flip_sim, R.id.tv_flip_cover, R.id.tv_flip_slide, R.id.tv_flip_none)
        flipOptions.forEach { fid ->
            val tv = findViewById<TextView>(fid)
            if (fid == id) {
                tv.setBackgroundResource(R.drawable.bg_rs_flip_option_selected)
                tv.setTextColor(getColor(R.color.primary_red))
            } else {
                tv.setBackgroundResource(R.drawable.bg_rs_flip_option)
                tv.setTextColor(getColor(R.color.text_secondary))
            }
        }
        currentFlipId = id
    }

    private fun setupSwitches() {
        findViewById<CompoundButton>(R.id.sw_brightness).setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this, "亮度跟随: $isChecked", Toast.LENGTH_SHORT).show()
        }
        findViewById<CompoundButton>(R.id.sw_volume).setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this, "音量翻页: $isChecked", Toast.LENGTH_SHORT).show()
        }
        findViewById<CompoundButton>(R.id.sw_progress).setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this, "显示进度: $isChecked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetDefaults() {
        selectFont(R.id.ll_font_default)
        findViewById<SeekBar>(R.id.sb_font_size).progress = 3
        updatePreviewSize(3)
        selectBackground(R.id.fl_bg_day)
        selectFlip(R.id.tv_flip_sim)
        findViewById<CompoundButton>(R.id.sw_brightness).isChecked = true
        findViewById<CompoundButton>(R.id.sw_volume).isChecked = true
        findViewById<CompoundButton>(R.id.sw_progress).isChecked = true
        Toast.makeText(this, "已恢复默认设置", Toast.LENGTH_SHORT).show()
    }
}
