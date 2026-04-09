package com.mobile.bible.kjv.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.mobile.bible.kjv.data.repository.KjvRepository
import com.mobile.bible.kjv.debug.DebugDateTimeOverride
import com.mobile.bible.kjv.ui.vm.TimeSlot
import com.mobile.bible.kjv.ui.vm.SpecialDayCalculator
import com.mobile.bible.kjv.R
import kotlinx.coroutines.launch
import com.mobile.bible.kjv.ui.dialog.AnswerFailedDialog
import com.mobile.bible.kjv.ui.dialog.AnswerRecoveryDialog
import com.mobile.bible.kjv.ui.dialog.AnswerPauseDialog
import com.mobile.bible.kjv.ui.dialog.FloatingWindowBottomDialog
import com.mobile.bible.kjv.ui.dialog.NotificationPermissionDialog
import com.mobile.bible.kjv.ui.dialog.LotteryDialog
import com.mobile.bible.kjv.ui.dialog.TokenTradeDialog
import com.mobile.bible.kjv.ui.dialog.DeletePrayerDialog
import com.mobile.bible.kjv.ui.dialog.PrayerEditMenuBottomDialog

import androidx.core.view.WindowCompat
import com.mobile.bible.kjv.noti.LocalNotificationService
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DebugActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportFragmentManager.setFragmentResultListener(DeletePrayerDialog.REQUEST_KEY, this) { _, bundle ->
            val confirmed = bundle.getBoolean(DeletePrayerDialog.RESULT_CONFIRMED, false)
            Toast.makeText(
                this,
                if (confirmed) "删除确认：Ok" else "删除确认：Cancel",
                Toast.LENGTH_SHORT
            ).show()
        }
        supportFragmentManager.setFragmentResultListener(PrayerEditMenuBottomDialog.REQUEST_KEY, this) { _, bundle ->
            val action = bundle.getString(PrayerEditMenuBottomDialog.RESULT_ACTION).orEmpty()
            Toast.makeText(this, "编辑菜单点击：$action", Toast.LENGTH_SHORT).show()
        }
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val scrollView = ScrollView(this)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER

        val tv = TextView(this)
        tv.text = "Debug 页面"
        tv.textSize = 20f
        tv.gravity = Gravity.CENTER

        val btn = Button(this)
        btn.text = "显示弹窗"
        btn.setOnClickListener { FloatingWindowBottomDialog.show(this) }

        val btnNotify = Button(this)
        btnNotify.text = "显示通知权限弹窗"
        btnNotify.setOnClickListener { NotificationPermissionDialog.show(this) }

        val btnOverlay = Button(this)
        btnOverlay.text = "跳转设置并显示透明层"
        btnOverlay.setOnClickListener {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivity(intent)
            Handler(Looper.getMainLooper()).postDelayed({
                val overlay = Intent(this, GuideFloatingWindowActivity::class.java)
                    .addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                                Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                                Intent.FLAG_ACTIVITY_NO_HISTORY or
                                Intent.FLAG_ACTIVITY_NO_ANIMATION
                    )
                startActivity(overlay)
            }, 100)
        }

        val btnAnswerFailed = Button(this)
        btnAnswerFailed.text = "显示答题失败弹窗"
        btnAnswerFailed.setOnClickListener { AnswerFailedDialog.show(this) }

        val btnAnswerRecovery = Button(this)
        btnAnswerRecovery.text = "显示答题恢复弹窗"
        btnAnswerRecovery.setOnClickListener {
            AnswerRecoveryDialog.show(this, object : AnswerRecoveryDialog.OnTimeoutListener {
                override fun onRecoveryTimeout() {
                }
            })
        }

        val btnAnswerPause = Button(this)
        btnAnswerPause.text = "显示答题暂停弹窗"
        btnAnswerPause.setOnClickListener { AnswerPauseDialog.show(this) }

        val btnAnswerFailedActivity = Button(this)
        btnAnswerFailedActivity.text = "答题失败页面"
        btnAnswerFailedActivity.setOnClickListener {
            startActivity(Intent(this, AnswerFailedActivity::class.java))
        }

        val btnLevelFailedActivity = Button(this)
        btnLevelFailedActivity.text = "关卡失败页面"
        btnLevelFailedActivity.setOnClickListener {
            startActivity(
                Intent(this, LevelFailedActivity::class.java)
                    .putExtra(LevelFailedActivity.EXTRA_COIN, 326)
                    .putExtra(
                        LevelFailedActivity.EXTRA_QUESTION,
                        "On the ________day, God createdman in his own image."
                    )
            )
        }

        val btnTokenTrade = Button(this)
        btnTokenTrade.text = "显示Token兑换弹窗"
        btnTokenTrade.setOnClickListener {
            TokenTradeDialog.show(this, object : TokenTradeDialog.OnTokenTradeListener {
                override fun onConfirmExchange(quantity: Int, totalCost: Int) {
                    android.widget.Toast.makeText(this@DebugActivity, "兑换 $quantity 个，花费 $totalCost coins", android.widget.Toast.LENGTH_SHORT).show()
                }
            })
        }

        val btnDeletePrayer = Button(this)
        btnDeletePrayer.text = "显示删除祷告弹窗"
        btnDeletePrayer.setOnClickListener { DeletePrayerDialog.show(this) }

        val btnPrayerEditMenu = Button(this)
        btnPrayerEditMenu.text = "显示祷告编辑菜单弹窗"
        btnPrayerEditMenu.setOnClickListener {
            PrayerEditMenuBottomDialog.show(
                this,
                pinText = getString(R.string.prayer_edit_menu_pin_via_ad),
                visibleText = getString(R.string.prayer_edit_menu_visible_default)
            )
        }

        val btnSimulateNotiCatchUp = Button(this)
        btnSimulateNotiCatchUp.text = "模拟当前窗口补发通知"
        btnSimulateNotiCatchUp.setOnClickListener {
            val success = LocalNotificationService.debugForceShowCurrentWindow(this)
            if (success) {
                Toast.makeText(this, "已强制发送通知", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "发送失败，请检查通知权限是否开启", Toast.LENGTH_SHORT).show()
            }
        }

        layout.addView(tv)
        layout.addView(btn)
        layout.addView(btnNotify)
        layout.addView(btnOverlay)
        layout.addView(btnAnswerFailed)
        layout.addView(btnAnswerRecovery)
        layout.addView(btnAnswerPause)
        layout.addView(btnAnswerFailedActivity)
        layout.addView(btnLevelFailedActivity)
        layout.addView(btnTokenTrade)
        layout.addView(btnDeletePrayer)
        layout.addView(btnPrayerEditMenu)
        layout.addView(btnSimulateNotiCatchUp)

        val btnGiftUnlock = Button(this)
        btnGiftUnlock.text = "显示礼物解锁弹窗"
        btnGiftUnlock.setOnClickListener { LotteryDialog.showCoin(this, 100) }

        val btnLotteryEraser = Button(this)
        btnLotteryEraser.text = "显示橡皮擦抽奖弹窗"
        btnLotteryEraser.setOnClickListener { LotteryDialog.showEraser(this, 2) }
        layout.addView(btnLotteryEraser)

        val btnLotteryDelay = Button(this)
        btnLotteryDelay.text = "显示时间道具抽奖弹窗"
        btnLotteryDelay.setOnClickListener { LotteryDialog.showDelay(this, 2) }
        layout.addView(btnLotteryDelay)
        layout.addView(btnGiftUnlock)

        val btnLotteryCoinMore = Button(this)
        btnLotteryCoinMore.text = "显示学习金币奖励弹窗"
        btnLotteryCoinMore.setOnClickListener { LotteryDialog.showCoinMore(this, 150) }
        layout.addView(btnLotteryCoinMore)

        val btnLotteryEraserDelay = Button(this)
        btnLotteryEraserDelay.text = "显示橡皮擦+时间道具弹窗"
        btnLotteryEraserDelay.setOnClickListener { LotteryDialog.showEraserDelay(this, 2) }
        layout.addView(btnLotteryEraserDelay)

        val btnLotteryCoinDouble = Button(this)
        btnLotteryCoinDouble.text = "显示双倍金币弹窗"
        btnLotteryCoinDouble.setOnClickListener { LotteryDialog.showCoinDouble(this, 200) }
        layout.addView(btnLotteryCoinDouble)

        val btnLotteryEraserDouble = Button(this)
        btnLotteryEraserDouble.text = "显示双倍橡皮擦弹窗"
        btnLotteryEraserDouble.setOnClickListener { LotteryDialog.showEraserDouble(this, 4) }
        layout.addView(btnLotteryEraserDouble)

        val btnLotteryDelayDouble = Button(this)
        btnLotteryDelayDouble.text = "显示双倍时间道具弹窗"
        btnLotteryDelayDouble.setOnClickListener { LotteryDialog.showDelayDouble(this, 4) }
        layout.addView(btnLotteryDelayDouble)

        val btnLotteryEraserDelayDouble = Button(this)
        btnLotteryEraserDelayDouble.text = "显示双倍橡皮擦+时间道具弹窗"
        btnLotteryEraserDelayDouble.setOnClickListener { LotteryDialog.showEraserDelayDouble(this, 4) }
        layout.addView(btnLotteryEraserDelayDouble)

        val btnLotteryCoinMoreDouble = Button(this)
        btnLotteryCoinMoreDouble.text = "显示双倍额外金币弹窗(150→300)"
        btnLotteryCoinMoreDouble.setOnClickListener { LotteryDialog.showCoinMoreDouble(this, 300) }
        layout.addView(btnLotteryCoinMoreDouble)

        val etLevel = EditText(this).apply {
            hint = "输入关卡数字"
            inputType = InputType.TYPE_CLASS_NUMBER
            gravity = Gravity.CENTER
        }
        layout.addView(etLevel)

        val btnJumpLevel = Button(this)
        btnJumpLevel.text = "设置已通过关卡"
        btnJumpLevel.setOnClickListener {
            val level = etLevel.text.toString().toIntOrNull()
            if (level != null && level > 0) {
                val repo = KjvRepository(this)
                lifecycleScope.launch {
                    repo.setHighestPassedLevel(level)
                    Toast.makeText(this@DebugActivity, "已将关卡 $level 及之前设为已通过", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "请输入有效的关卡数字", Toast.LENGTH_SHORT).show()
            }
        }
        layout.addView(btnJumpLevel)

        val divider = TextView(this).apply {
            text = "---------------- 特殊日联调 ----------------"
            gravity = Gravity.CENTER
        }
        layout.addView(divider)

        val tvSpecialTitle = TextView(this).apply {
            text = "Today Fixed 特殊日调试"
            gravity = Gravity.CENTER
        }
        layout.addView(tvSpecialTitle)

        val etMockDate = EditText(this).apply {
            hint = "输入模拟日期 yyyy-MM-dd"
            inputType = InputType.TYPE_CLASS_DATETIME
            gravity = Gravity.CENTER
            setText(formatDate(Calendar.getInstance()))
        }
        layout.addView(etMockDate)

        val tvSlotLabel = TextView(this).apply {
            text = "选择时段"
            gravity = Gravity.CENTER
        }
        layout.addView(tvSlotLabel)

        val slotLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val btnMorning = Button(this).apply { text = "Morning" }
        val btnEvening = Button(this).apply { text = "Evening" }
        slotLayout.addView(btnMorning)
        slotLayout.addView(btnEvening)
        layout.addView(slotLayout)

        var selectedSlot = TimeSlot.MORNING
        updateSlotButtons(btnMorning, btnEvening, selectedSlot)
        btnMorning.setOnClickListener {
            selectedSlot = TimeSlot.MORNING
            updateSlotButtons(btnMorning, btnEvening, selectedSlot)
        }
        btnEvening.setOnClickListener {
            selectedSlot = TimeSlot.EVENING
            updateSlotButtons(btnMorning, btnEvening, selectedSlot)
        }

        val tvMockStatus = TextView(this).apply {
            gravity = Gravity.CENTER
        }
        updateMockStatus(tvMockStatus)
        layout.addView(tvMockStatus)

        val btnApplyMock = Button(this).apply { text = "应用模拟日期/时段" }
        btnApplyMock.setOnClickListener {
            val input = etMockDate.text?.toString().orEmpty().trim()
            val calendar = parseDateToCalendar(input)
            if (calendar == null) {
                Toast.makeText(this, "日期格式错误，请输入 yyyy-MM-dd", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            DebugDateTimeOverride.set(calendar, selectedSlot)
            updateMockStatus(tvMockStatus)
            Toast.makeText(this, "已应用模拟：${formatDate(calendar)} / ${selectedSlot.name}", Toast.LENGTH_SHORT).show()
        }
        layout.addView(btnApplyMock)

        val btnClearMock = Button(this).apply { text = "清除模拟设置" }
        btnClearMock.setOnClickListener {
            DebugDateTimeOverride.clear()
            updateMockStatus(tvMockStatus)
            Toast.makeText(this, "已清除模拟设置", Toast.LENGTH_SHORT).show()
        }
        layout.addView(btnClearMock)

        val btnJumpToday = Button(this).apply { text = "跳转 Today 页面验证" }
        btnJumpToday.setOnClickListener {
            val intent = Intent(this, KjvHomeActivity::class.java).apply {
                putExtra(KjvHomeActivity.EXTRA_START_TAB, KjvHomeActivity.Tab.TODAY.name)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
        }
        layout.addView(btnJumpToday)

        val tvQuickJumpTitle = TextView(this).apply {
            text = "一键定位到特殊日（中文）"
            gravity = Gravity.CENTER
        }
        layout.addView(tvQuickJumpTitle)

        val etYear = EditText(this).apply {
            hint = "输入年份（用于可变节日）"
            inputType = InputType.TYPE_CLASS_NUMBER
            gravity = Gravity.CENTER
            setText(Calendar.getInstance().get(Calendar.YEAR).toString())
        }
        layout.addView(etYear)

        fun applySpecialDayAndJump(calendar: Calendar, label: String) {
            etMockDate.setText(formatDate(calendar))
            DebugDateTimeOverride.set(calendar, selectedSlot)
            updateMockStatus(tvMockStatus)
            Toast.makeText(
                this,
                "已定位：$label ${formatDate(calendar)} / ${selectedSlot.name}",
                Toast.LENGTH_SHORT
            ).show()
            btnJumpToday.performClick()
        }

        fun getSelectedYearOrNull(): Int? {
            val year = etYear.text?.toString()?.trim()?.toIntOrNull()
            if (year == null || year !in 1900..2100) {
                Toast.makeText(this, "请输入 1900-2100 的有效年份", Toast.LENGTH_SHORT).show()
                return null
            }
            return year
        }

        val btnJumpSunday = Button(this).apply { text = "定位到 主日（每周日）" }
        btnJumpSunday.setOnClickListener {
            val year = getSelectedYearOrNull() ?: return@setOnClickListener
            val calendar = findFirstSundayOfYear(year)
            applySpecialDayAndJump(calendar, "主日")
        }
        layout.addView(btnJumpSunday)

        val btnJumpChristmas = Button(this).apply { text = "定位到 圣诞节（12月25日）" }
        btnJumpChristmas.setOnClickListener {
            val year = getSelectedYearOrNull() ?: return@setOnClickListener
            applySpecialDayAndJump(newDate(year, Calendar.DECEMBER, 25), "圣诞节")
        }
        layout.addView(btnJumpChristmas)

        val btnJumpEpiphany = Button(this).apply { text = "定位到 主显节（1月6日）" }
        btnJumpEpiphany.setOnClickListener {
            val year = getSelectedYearOrNull() ?: return@setOnClickListener
            applySpecialDayAndJump(newDate(year, Calendar.JANUARY, 6), "主显节")
        }
        layout.addView(btnJumpEpiphany)

        val btnJumpPaulDay = Button(this).apply { text = "定位到 保罗日（1月25日）" }
        btnJumpPaulDay.setOnClickListener {
            val year = getSelectedYearOrNull() ?: return@setOnClickListener
            applySpecialDayAndJump(newDate(year, Calendar.JANUARY, 25), "保罗日")
        }
        layout.addView(btnJumpPaulDay)

        val btnJumpEaster = Button(this).apply { text = "定位到 复活节" }
        btnJumpEaster.setOnClickListener {
            val year = getSelectedYearOrNull() ?: return@setOnClickListener
            applySpecialDayAndJump(SpecialDayCalculator.calculateEaster(year), "复活节")
        }
        layout.addView(btnJumpEaster)

        val btnJumpGoodFriday = Button(this).apply { text = "定位到 受难日（复活节前周五）" }
        btnJumpGoodFriday.setOnClickListener {
            val year = getSelectedYearOrNull() ?: return@setOnClickListener
            val calendar = SpecialDayCalculator.calculateEaster(year).apply { add(Calendar.DAY_OF_YEAR, -2) }
            applySpecialDayAndJump(calendar, "受难日")
        }
        layout.addView(btnJumpGoodFriday)

        val btnJumpPalmSunday = Button(this).apply { text = "定位到 棕枝主日（复活节前周日）" }
        btnJumpPalmSunday.setOnClickListener {
            val year = getSelectedYearOrNull() ?: return@setOnClickListener
            val calendar = SpecialDayCalculator.calculateEaster(year).apply { add(Calendar.DAY_OF_YEAR, -7) }
            applySpecialDayAndJump(calendar, "棕枝主日")
        }
        layout.addView(btnJumpPalmSunday)

        val btnJumpAscension = Button(this).apply { text = "定位到 升天节（复活节后40天）" }
        btnJumpAscension.setOnClickListener {
            val year = getSelectedYearOrNull() ?: return@setOnClickListener
            val calendar = SpecialDayCalculator.calculateEaster(year).apply { add(Calendar.DAY_OF_YEAR, 39) }
            applySpecialDayAndJump(calendar, "升天节")
        }
        layout.addView(btnJumpAscension)

        val btnJumpPentecost = Button(this).apply { text = "定位到 五旬节（复活节后50天）" }
        btnJumpPentecost.setOnClickListener {
            val year = getSelectedYearOrNull() ?: return@setOnClickListener
            val calendar = SpecialDayCalculator.calculateEaster(year).apply { add(Calendar.DAY_OF_YEAR, 49) }
            applySpecialDayAndJump(calendar, "五旬节")
        }
        layout.addView(btnJumpPentecost)

        val btnJumpThanksgiving = Button(this).apply { text = "定位到 感恩节（11月第4个周四）" }
        btnJumpThanksgiving.setOnClickListener {
            val year = getSelectedYearOrNull() ?: return@setOnClickListener
            val calendar = calculateNthWeekday(year, Calendar.NOVEMBER, Calendar.THURSDAY, 4)
            applySpecialDayAndJump(calendar, "感恩节")
        }
        layout.addView(btnJumpThanksgiving)

        scrollView.addView(layout)
        setContentView(scrollView)
    }

    private fun newDate(year: Int, month: Int, dayOfMonth: Int): Calendar {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun calculateNthWeekday(year: Int, month: Int, dayOfWeek: Int, nthWeek: Int): Calendar {
        val cal = newDate(year, month, 1)
        while (cal.get(Calendar.DAY_OF_WEEK) != dayOfWeek) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        cal.add(Calendar.WEEK_OF_MONTH, nthWeek - 1)
        return cal
    }

    private fun findFirstSundayOfYear(year: Int): Calendar {
        val cal = newDate(year, Calendar.JANUARY, 1)
        while (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) {
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        return cal
    }

    private fun parseDateToCalendar(input: String): Calendar? {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
            isLenient = false
        }
        val date = runCatching { formatter.parse(input) }.getOrNull() ?: return null
        return Calendar.getInstance().apply {
            time = date
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    private fun formatDate(calendar: Calendar): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(calendar.time)
    }

    private fun updateSlotButtons(btnMorning: Button, btnEvening: Button, selected: TimeSlot) {
        btnMorning.isEnabled = selected != TimeSlot.MORNING
        btnEvening.isEnabled = selected != TimeSlot.EVENING
    }

    private fun updateMockStatus(statusView: TextView) {
        val state = DebugDateTimeOverride.get()
        statusView.text = if (state == null) {
            "当前模拟：未启用（使用系统时间）"
        } else {
            "当前模拟：${formatDate(state.calendar)} / ${state.timeSlot.name}"
        }
    }
}
