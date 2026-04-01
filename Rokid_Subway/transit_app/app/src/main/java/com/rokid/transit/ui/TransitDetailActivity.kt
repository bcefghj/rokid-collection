package com.rokid.transit.ui

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.rokid.transit.R
import com.rokid.transit.data.SegmentType
import com.rokid.transit.data.TransitPlan
import com.rokid.transit.databinding.ActivityTransitDetailBinding
import com.rokid.transit.util.FormatUtil

class TransitDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransitDetailBinding
    private var scrollY = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(6815872)
        binding = ActivityTransitDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val plan = TransitDataHolder.plans.getOrNull(TransitDataHolder.selectedPlanIndex) ?: run {
            finish()
            return
        }

        renderSummary(plan)
        renderDetail(plan)
    }

    private fun renderSummary(plan: TransitPlan) {
        binding.tvSummary.text = "${FormatUtil.formatDuration(plan.cost.duration)}  ¥${plan.cost.transitFee}  步行${FormatUtil.formatDistance(plan.cost.walkDistance)}"
    }

    private fun renderDetail(plan: TransitPlan) {
        val c = binding.detailContainer
        c.removeAllViews()

        for ((index, segment) in plan.segments.withIndex()) {
            when (segment.type) {
                SegmentType.WALK -> {
                    val info = segment.walkInfo ?: continue
                    addRow(c, "●", "#888888",
                        "步行 ${FormatUtil.formatDistance(info.distance)}",
                        "约${FormatUtil.formatDuration(info.duration)}", null)
                }
                SegmentType.SUBWAY, SegmentType.BUS -> {
                    val info = segment.transitInfo ?: continue
                    val color = info.lineColor
                    val shortName = FormatUtil.shortLineName(info.lineName)

                    addLineHeader(c, shortName, color)
                    addRow(c, "▶", color, "上车  ${info.departureStop}", null, color)

                    if (info.direction.isNotEmpty()) {
                        addRow(c, "│", color, "方向: ${info.direction}", null, null)
                    }

                    addRow(c, "│", color,
                        "${info.stationCount}站 · ${FormatUtil.formatDuration(info.duration)}", null, null)

                    if (info.viaStops.isNotEmpty()) {
                        val stops = info.viaStops.joinToString(" → ")
                        addRow(c, "│", color, "途经: $stops", null, null)
                    }

                    addRow(c, "■", color, "下车  ${info.arrivalStop}", null, color)
                }
            }
        }

        addRow(c, "●", "#00FF88", "到达 ${TransitDataHolder.destName}", null, "#00FF88")
    }

    private fun addLineHeader(container: LinearLayout, name: String, colorHex: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 14, 0, 6)
        }

        val tag = TextView(this).apply {
            text = "  $name  "
            textSize = 10f
            setTextColor(Color.WHITE)
            setTypeface(null, Typeface.BOLD)
            setPadding(16, 6, 16, 6)
            background = GradientDrawable().apply {
                cornerRadius = 8f
                setColor(Color.parseColor(colorHex))
            }
        }
        row.addView(tag)
        container.addView(row)
    }

    private fun addRow(
        container: LinearLayout,
        icon: String, iconColor: String,
        mainText: String, subText: String?,
        textColor: String?
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.TOP
            setPadding(0, 4, 0, 4)
        }

        val iconView = TextView(this).apply {
            text = icon
            textSize = 8f
            setTextColor(Color.parseColor(iconColor))
            layoutParams = LinearLayout.LayoutParams(30, LinearLayout.LayoutParams.WRAP_CONTENT)
            gravity = Gravity.CENTER
        }
        row.addView(iconView)

        val textCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = 8
            }
        }

        val mainView = TextView(this).apply {
            text = mainText
            textSize = 9f
            val c = if (textColor != null) Color.parseColor(textColor) else resources.getColor(R.color.green_normal_80, null)
            setTextColor(c)
            if (textColor != null) setTypeface(null, Typeface.BOLD)
            setLineSpacing(4f, 1f)
        }
        textCol.addView(mainView)

        if (subText != null) {
            val subView = TextView(this).apply {
                text = subText
                textSize = 7f
                setTextColor(resources.getColor(R.color.green_normal_60, null))
            }
            textCol.addView(subView)
        }

        row.addView(textCol)
        container.addView(row)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                binding.scrollDetail.smoothScrollBy(0, 150)
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (binding.scrollDetail.scrollY <= 0) {
                    finish()
                } else {
                    binding.scrollDetail.smoothScrollBy(0, -150)
                }
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                finish()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
