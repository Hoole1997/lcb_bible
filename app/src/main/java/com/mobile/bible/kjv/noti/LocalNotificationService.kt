package com.mobile.bible.kjv.noti

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.mobile.bible.kjv.utils.AppLifecycleObserver
import com.mobile.bible.kjv.ui.activity.KjvHomeActivity
import com.mobile.bible.kjv.R
import org.json.JSONObject
import java.util.Calendar
import java.util.Random
import java.util.concurrent.TimeUnit
import kotlin.math.absoluteValue

object LocalNotificationService {

    private const val PREF_NAME = "local_notification_service"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_START_DATE = "start_date"
    private const val KEY_LAST_MORNING_DELIVERED_DAY = "last_morning_delivered_day"
    private const val KEY_LAST_EVENING_DELIVERED_DAY = "last_evening_delivered_day"
    private const val KEY_PENDING_MORNING_DAY = "pending_morning_day"
    private const val KEY_PENDING_EVENING_DAY = "pending_evening_day"
    private const val KEY_SHUFFLE_CYCLE_INDEX = "shuffle_cycle_index"
    private const val KEY_SHUFFLED_ORDER = "shuffled_order"
    private const val KEY_SHUFFLE_SEED_BASE = "shuffle_seed_base"
    private const val DEFAULT_MORNING_HOUR = 7
    private const val DEFAULT_MORNING_MINUTE = 0
    private const val DEFAULT_EVENING_HOUR = 18
    private const val DEFAULT_EVENING_MINUTE = 0
    private const val CHANNEL_ID = "prayer_daily_channel"
    private const val ACTION_TRIGGER = "com.mobile.bible.kjv.noti.LOCAL_NOTIFICATION_TRIGGER"
    internal const val EXTRA_SLOT = "extra_slot"
    internal const val SLOT_MORNING = "morning"
    internal const val SLOT_EVENING = "evening"
    internal const val REQUEST_CODE_MORNING = 41001
    internal const val REQUEST_CODE_EVENING = 41002
    private const val NOTIFICATION_ID_MORNING = 42001
    private const val NOTIFICATION_ID_EVENING = 42002
    private const val KEEP_ALIVE_WORK_NAME = "local_noti_keep_alive_work"
    private const val DAY_MILLIS = 24L * 60 * 60 * 1000

    fun bootstrap(context: Context) {
        val appContext = context.applicationContext
        createChannel(appContext)
        val prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_ENABLED)) {
            prefs.edit().putBoolean(KEY_ENABLED, true).apply()
        }
        if (isEnabled(appContext)) {
            ensureStartDate(appContext)
            ensureScheduled(appContext)
            scheduleKeepAlive(appContext)
        }
    }

    fun start(context: Context) {
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putBoolean(KEY_ENABLED, true)
            .apply()
        ensureStartDate(appContext)
        createChannel(appContext)
        ensureScheduled(appContext)
        scheduleKeepAlive(appContext)
    }

    fun stop(context: Context) {
        val appContext = context.applicationContext
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, false)
            .apply()
        cancelSlot(appContext, SLOT_MORNING)
        cancelSlot(appContext, SLOT_EVENING)
        WorkManager.getInstance(appContext).cancelUniqueWork(KEEP_ALIVE_WORK_NAME)
    }

    fun ensureScheduled(context: Context) {
        val appContext = context.applicationContext
        if (!isEnabled(appContext)) return
        scheduleSlot(
            appContext,
            slot = SLOT_MORNING,
            hour = DEFAULT_MORNING_HOUR,
            minute = DEFAULT_MORNING_MINUTE
        )
        scheduleSlot(
            appContext,
            slot = SLOT_EVENING,
            hour = DEFAULT_EVENING_HOUR,
            minute = DEFAULT_EVENING_MINUTE
        )
    }

    internal fun onAlarmTriggered(context: Context, slot: String?) {
        val appContext = context.applicationContext
        if (!isEnabled(appContext)) return
        if (slot != SLOT_MORNING && slot != SLOT_EVENING) return
        createChannel(appContext)
        evaluateAndDispatch(appContext, triggerSlot = slot)
        if (slot == SLOT_MORNING) {
            scheduleSlot(appContext, slot, DEFAULT_MORNING_HOUR, DEFAULT_MORNING_MINUTE)
        } else {
            scheduleSlot(appContext, slot, DEFAULT_EVENING_HOUR, DEFAULT_EVENING_MINUTE)
        }
    }

    internal fun onKeepAliveTick(context: Context) {
        val appContext = context.applicationContext
        if (!isEnabled(appContext)) return
        ensureScheduled(appContext)
        evaluateAndDispatch(appContext, triggerSlot = null)
    }

    internal fun onSystemRestored(context: Context) {
        val appContext = context.applicationContext
        if (!isEnabled(appContext)) return
        ensureScheduled(appContext)
        evaluateAndDispatch(appContext, triggerSlot = null)
    }

    internal fun onAppMovedToBackground(context: Context) {
        val appContext = context.applicationContext
        if (!isEnabled(appContext)) return
        evaluateAndDispatch(appContext, triggerSlot = null)
    }

    fun debugForceShowCurrentWindow(context: Context): Boolean {
        val appContext = context.applicationContext
        createChannel(appContext)
        val now = System.currentTimeMillis()
        val window = currentWindow(now)
        val debugNotificationId = (now % Int.MAX_VALUE).toInt().absoluteValue
        return postNotification(
            context = appContext,
            slot = window.slot,
            targetDayToken = window.targetDayToken,
            notificationIdOverride = debugNotificationId
        )
    }

    private fun evaluateAndDispatch(context: Context, triggerSlot: String?) {
        val now = System.currentTimeMillis()
        val currentDayToken = dayToken(now)
        if (triggerSlot == SLOT_MORNING || triggerSlot == SLOT_EVENING) {
            tryDispatch(
                context = context,
                slot = triggerSlot,
                targetDayToken = currentDayToken
            )
        }

        val window = currentWindow(now)
        tryDispatch(
            context = context,
            slot = window.slot,
            targetDayToken = window.targetDayToken
        )
    }

    private fun tryDispatch(context: Context, slot: String, targetDayToken: Int) {
        if (hasDelivered(slot, targetDayToken, context)) {
            clearPending(slot, context)
            return
        }
        if (AppLifecycleObserver.isAppInForeground()) {
            markPending(slot, targetDayToken, context)
            return
        }
        if (postNotification(context, slot, targetDayToken)) {
            markDelivered(slot, targetDayToken, context)
            clearPending(slot, context)
        } else {
            markPending(slot, targetDayToken, context)
        }
    }

    private fun postNotification(
        context: Context,
        slot: String,
        targetDayToken: Int,
        notificationIdOverride: Int? = null
    ): Boolean {
        if (!canPostNotification(context)) return false
        val verse = PrayerVerseRepository.getVerseForDay(context, slot, targetDayToken)
        val title = if (slot == SLOT_MORNING) {
            context.getString(R.string.notification_prayer_morning_title)
        } else {
            context.getString(R.string.notification_prayer_evening_title)
        }
        val verseText = verse?.verse?.ifBlank { null } ?: context.getString(R.string.notification_prayer_default_body)
        val chapterText = verse?.chapter?.ifBlank { null }.orEmpty()
        val actionText = if (slot == SLOT_MORNING) {
            context.getString(R.string.notification_prayer_action_morning)
        } else {
            context.getString(R.string.notification_prayer_action_evening)
        }

        val openIntent = Intent(context, KjvHomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            43001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentTitle(title)
            .setContentText(verseText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomBigContentView(
                buildBigRemoteView(
                    context = context,
                    verseText = verseText,
                    chapterText = chapterText,
                    actionText = actionText,
                    clickIntent = openPendingIntent
                )
            )
            .setCustomContentView(
                buildMediumRemoteView(
                    context = context,
                    verseText = verseText,
                    actionText = actionText,
                    clickIntent = openPendingIntent
                )
            )
            .setCustomHeadsUpContentView(
                buildSmallRemoteView(
                    context = context,
                    verseText = verseText,
                    chipText = context.getString(R.string.notification_prayer_action_short),
                    clickIntent = openPendingIntent
                )
            )

        try {
            val notificationId = notificationIdOverride ?: if (slot == SLOT_MORNING) {
                NOTIFICATION_ID_MORNING
            } else {
                NOTIFICATION_ID_EVENING
            }
            NotificationManagerCompat.from(context).notify(notificationId, builder.build())
            return true
        } catch (_: SecurityException) {
            // Notification permission may be revoked while app is running.
            return false
        }
    }

    private fun buildBigRemoteView(
        context: Context,
        verseText: String,
        chapterText: String,
        actionText: String,
        clickIntent: PendingIntent
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.noti_prayer_big).apply {
            setTextViewText(R.id.noti_big_verse, verseText)
            setTextViewText(R.id.noti_big_chapter, chapterText)
            setTextViewText(R.id.noti_big_action, actionText)
            setOnClickPendingIntent(R.id.noti_big_root, clickIntent)
            setOnClickPendingIntent(R.id.noti_big_action, clickIntent)
        }
    }

    private fun buildMediumRemoteView(
        context: Context,
        verseText: String,
        actionText: String,
        clickIntent: PendingIntent
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.noti_prayer_medium).apply {
            setTextViewText(R.id.noti_medium_verse, verseText)
            setTextViewText(R.id.noti_medium_action, actionText)
            setOnClickPendingIntent(R.id.noti_medium_root, clickIntent)
            setOnClickPendingIntent(R.id.noti_medium_action, clickIntent)
        }
    }

    private fun buildSmallRemoteView(
        context: Context,
        verseText: String,
        chipText: String,
        clickIntent: PendingIntent
    ): RemoteViews {
        return RemoteViews(context.packageName, R.layout.noti_prayer_small).apply {
            setTextViewText(R.id.noti_small_verse, verseText)
            setTextViewText(R.id.noti_small_chip, chipText)
            setOnClickPendingIntent(R.id.noti_small_root, clickIntent)
            setOnClickPendingIntent(R.id.noti_small_chip, clickIntent)
        }
    }

    private fun canPostNotification(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun scheduleSlot(context: Context, slot: String, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = alarmPendingIntent(context, slot)
        val triggerAtMillis = computeNextTrigger(hour, minute)
        alarmManager.cancel(pendingIntent)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }
    }

    private fun cancelSlot(context: Context, slot: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = alarmPendingIntent(context, slot)
        alarmManager.cancel(pendingIntent)
    }

    private fun alarmPendingIntent(context: Context, slot: String): PendingIntent {
        val requestCode = if (slot == SLOT_MORNING) REQUEST_CODE_MORNING else REQUEST_CODE_EVENING
        val intent = Intent(context, LocalNotiAlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER
            putExtra(EXTRA_SLOT, slot)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun computeNextTrigger(hour: Int, minute: Int): Long {
        val now = Calendar.getInstance()
        val trigger = Calendar.getInstance().apply {
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            if (before(now)) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        return trigger.timeInMillis
    }

    private fun scheduleKeepAlive(context: Context) {
        val work = PeriodicWorkRequestBuilder<LocalNotiKeepAliveWorker>(6, TimeUnit.HOURS)
            .setInitialDelay(20, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            KEEP_ALIVE_WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            work
        )
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_prayer_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_prayer_channel_desc)
        }
        manager.createNotificationChannel(channel)
    }

    private fun ensureStartDate(context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        if (prefs.getLong(KEY_START_DATE, 0L) > 0L) return
        prefs.edit().putLong(KEY_START_DATE, todayStartMillis()).apply()
    }

    private fun todayStartMillis(): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    private fun startDate(context: Context): Long {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getLong(KEY_START_DATE, todayStartMillis())
    }

    private fun hasDelivered(slot: String, dayToken: Int, context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = if (slot == SLOT_MORNING) KEY_LAST_MORNING_DELIVERED_DAY else KEY_LAST_EVENING_DELIVERED_DAY
        return prefs.getInt(key, -1) == dayToken
    }

    private fun markDelivered(slot: String, dayToken: Int, context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = if (slot == SLOT_MORNING) KEY_LAST_MORNING_DELIVERED_DAY else KEY_LAST_EVENING_DELIVERED_DAY
        prefs.edit().putInt(key, dayToken).apply()
    }

    private fun markPending(slot: String, dayToken: Int, context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = if (slot == SLOT_MORNING) KEY_PENDING_MORNING_DAY else KEY_PENDING_EVENING_DAY
        prefs.edit().putInt(key, dayToken).apply()
    }

    private fun clearPending(slot: String, context: Context) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val key = if (slot == SLOT_MORNING) KEY_PENDING_MORNING_DAY else KEY_PENDING_EVENING_DAY
        prefs.edit().remove(key).apply()
    }

    private fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, true)
    }

    private data class ActiveWindow(
        val slot: String,
        val targetDayToken: Int
    )

    private fun currentWindow(nowMillis: Long): ActiveWindow {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val hm = hour * 60 + minute
        return if (hm in (7 * 60)..(17 * 60 + 59)) {
            ActiveWindow(SLOT_MORNING, dayToken(nowMillis))
        } else {
            val target = if (hm >= 18 * 60) {
                dayToken(nowMillis)
            } else {
                dayToken(nowMillis - DAY_MILLIS)
            }
            ActiveWindow(SLOT_EVENING, target)
        }
    }

    private fun dayToken(timeMillis: Long): Int {
        val cal = Calendar.getInstance().apply { timeInMillis = timeMillis }
        return cal.get(Calendar.YEAR) * 1000 + cal.get(Calendar.DAY_OF_YEAR)
    }

    private object PrayerVerseRepository {
        private const val ASSET_FILE_PATH = "noti/prayer_verses_30days.json"
        @Volatile
        private var cached: List<DayVerses>? = null

        fun getVerseForDay(context: Context, slot: String, targetDayToken: Int): VerseItem? {
            val data = load(context)
            if (data.isEmpty()) return null
            val targetDayStart = dayStartMillisFromToken(targetDayToken)
            val daysPassed = ((targetDayStart - LocalNotificationService.startDate(context)) / LocalNotificationService.DAY_MILLIS)
                .toInt()
                .coerceAtLeast(0)
            val order = shuffledOrder(context, data.size, daysPassed)
            val index = order[daysPassed % data.size]
            val dayVerses = data[index.coerceIn(0, data.lastIndex)]
            return if (slot == SLOT_MORNING) dayVerses.morning else dayVerses.evening
        }

        private fun shuffledOrder(context: Context, size: Int, daysPassed: Int): IntArray {
            if (size <= 1) return IntArray(size) { it }
            val cycleIndex = daysPassed / size
            val prefs = context.getSharedPreferences(LocalNotificationService.PREF_NAME, Context.MODE_PRIVATE)
            val cachedCycle = prefs.getInt(LocalNotificationService.KEY_SHUFFLE_CYCLE_INDEX, -1)
            val cachedOrder = parseOrder(
                prefs.getString(LocalNotificationService.KEY_SHUFFLED_ORDER, "") ?: "",
                size
            )
            if (cachedCycle == cycleIndex && cachedOrder != null) {
                return cachedOrder
            }
            val seedBase = prefs.getLong(LocalNotificationService.KEY_SHUFFLE_SEED_BASE, 0L).let { existing ->
                if (existing != 0L) {
                    existing
                } else {
                    val generated = Random().nextLong()
                    prefs.edit().putLong(LocalNotificationService.KEY_SHUFFLE_SEED_BASE, generated).apply()
                    generated
                }
            }
            val newOrder = IntArray(size) { it }
            val random = Random(seedBase + cycleIndex)
            for (i in newOrder.lastIndex downTo 1) {
                val j = random.nextInt(i + 1)
                val temp = newOrder[i]
                newOrder[i] = newOrder[j]
                newOrder[j] = temp
            }
            prefs.edit()
                .putInt(LocalNotificationService.KEY_SHUFFLE_CYCLE_INDEX, cycleIndex)
                .putString(LocalNotificationService.KEY_SHUFFLED_ORDER, newOrder.joinToString(","))
                .apply()
            return newOrder
        }

        private fun parseOrder(raw: String, size: Int): IntArray? {
            if (raw.isBlank()) return null
            val parts = raw.split(",")
            if (parts.size != size) return null
            val parsed = IntArray(size)
            val seen = BooleanArray(size)
            for (i in parts.indices) {
                val value = parts[i].toIntOrNull() ?: return null
                if (value !in 0 until size) return null
                if (seen[value]) return null
                seen[value] = true
                parsed[i] = value
            }
            return parsed
        }

        private fun load(context: Context): List<DayVerses> {
            val snapshot = cached
            if (snapshot != null) return snapshot
            synchronized(this) {
                val second = cached
                if (second != null) return second
                val parsed = runCatching {
                    val json = context.assets.open(ASSET_FILE_PATH).bufferedReader().use { it.readText() }
                    parseJson(json)
                }.getOrElse { emptyList() }
                cached = parsed
                return parsed
            }
        }

        private fun parseJson(raw: String): List<DayVerses> {
            val root = JSONObject(raw)
            val arr = root.optJSONArray("prayer_verses_30days") ?: return emptyList()
            val result = ArrayList<DayVerses>(arr.length())
            for (i in 0 until arr.length()) {
                val item = arr.optJSONObject(i) ?: continue
                val day = item.optInt("day", i + 1)
                val morning = item.optJSONObject("morning")
                val evening = item.optJSONObject("evening")
                result += DayVerses(
                    day = day,
                    morning = VerseItem(
                        verse = morning?.optString("verse").orEmpty(),
                        chapter = morning?.optString("chapter").orEmpty(),
                        version = morning?.optString("version").orEmpty()
                    ),
                    evening = VerseItem(
                        verse = evening?.optString("verse").orEmpty(),
                        chapter = evening?.optString("chapter").orEmpty(),
                        version = evening?.optString("version").orEmpty()
                    )
                )
            }
            return result
        }

        private fun dayStartMillisFromToken(token: Int): Long {
            val year = token / 1000
            val dayOfYear = token % 1000
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.DAY_OF_YEAR, dayOfYear)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }
    }

    private data class DayVerses(
        val day: Int,
        val morning: VerseItem,
        val evening: VerseItem
    )

    private data class VerseItem(
        val verse: String,
        val chapter: String,
        val version: String
    )
}
