package com.rokid.transit.ui

import android.content.Intent
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
import com.rokid.transit.databinding.ActivityTransitResultBinding
import com.rokid.transit.util.FormatUtil

class TransitResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransitResultBinding
    private var selectedIndex = 0
    private val planViews = mutableListOf<View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(6815872)
        binding = ActivityTransitResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val plans = TransitDataHolder.plans
        binding.tvDestName.text = "→ ${TransitDataHolder.destName}"

        for ((index, plan) in plans.withIndex()) {
            val card = createPlanCard(plan, index)
            binding.plansContainer.addView(card)
            planViews.add(card)
        }

        updateSelection()
    }

    private fun createPlanCard(plan: TransitPlan, index: Int): LinearLayout {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 16, 20, 16)
            background = resources.getDrawable(R.drawable.bg_plan_card, null)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { bottomMargin = 12 }
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val label = TextView(this).apply {
            text = "方案${index + 1}"
            textSize = 10f
            setTextColor(resources.getColor(R.color.green_normal_80, null))
        }
        headerRow.addView(label)

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
        }
        headerRow.addView(spacer)

        val time = TextView(this).apply {
            text = FormatUtil.formatDuration(plan.cost.duration)
            textSize = 12f
            setTextColor(resources.getColor(R.color.green_normal, null))
            setTypeface(null, Typeface.BOLD)
        }
        headerRow.addView(time)

        card.addView(headerRow)

        val lineRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 10 }
        }

        var first = true
        for (seg in plan.segments) {
            if (seg.type == SegmentType.WALK) continue
            val info = seg.transitInfo ?: continue

            if (!first) {
                val arrow = TextView(this).apply {
                    text = " → "
                    textSize = 8f
                    setTextColor(resources.getColor(R.color.green_normal_60, null))
                }
                lineRow.addView(arrow)
            }

            val tag = TextView(this).apply {
                text = FormatUtil.shortLineName(info.lineName)
                textSize = 8f
                setTextColor(Color.WHITE)
                setPadding(12, 4, 12, 4)
                background = GradientDrawable().apply {
                    cornerRadius = 6f
                    setColor(Color.parseColor(info.lineColor))
                }
                gravity = Gravity.CENTER
            }
            lineRow.addView(tag, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { marginEnd = 6 })

            first = false
        }
        card.addView(lineRow)

        val infoRow = TextView(this).apply {
            text = "¥${plan.cost.transitFee}  |  步行${FormatUtil.formatDistance(plan.cost.walkDistance)}"
            textSize = 8f
            setTextColor(resources.getColor(R.color.green_normal_60, null))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 8 }
        }
        card.addView(infoRow)

        return card
    }

    private fun updateSelection() {
        for ((i, view) in planViews.withIndex()) {
            view.setBackgroundResource(
                if (i == selectedIndex) R.drawable.bg_selected else R.drawable.bg_plan_card
            )
        }
        val selectedView = planViews.getOrNull(selectedIndex) ?: return
        binding.scrollPlans.post {
            binding.scrollPlans.smoothScrollTo(0, selectedView.top - 20)
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (selectedIndex < planViews.size - 1) {
                    selectedIndex++
                    updateSelection()
                } else {
                    openDetail()
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (selectedIndex > 0) {
                    selectedIndex--
                    updateSelection()
                } else {
                    finish()
                }
                return true
            }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                openDetail()
                return true
            }
            KeyEvent.KEYCODE_BACK -> {
                finish()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun openDetail() {
        TransitDataHolder.selectedPlanIndex = selectedIndex
        startActivity(Intent(this, TransitDetailActivity::class.java))
    }
}
