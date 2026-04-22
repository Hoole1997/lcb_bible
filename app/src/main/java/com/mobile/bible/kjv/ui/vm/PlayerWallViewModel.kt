package com.mobile.bible.kjv.ui.vm

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.bible.kjv.data.entity.MyPrayerEntity
import com.mobile.bible.kjv.data.repository.KjvRepository
import com.mobile.bible.kjv.prefs.PlayerWallSettings
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

data class PrayerWallItem(
    val username: String,
    val content: String,
    val likes: Int,
    val localPrayerId: Long? = null
)

private data class PrayerWallAssetItem(
    @SerializedName("username") val username: String? = null,
    @SerializedName("content") val content: String? = null,
    @SerializedName("likes") val likes: Int? = 0
)

data class PrayerWallWindow(
    val items: List<PrayerWallItem>,
    val startIndex: Int,
    val windowSize: Int,
    val intervalSeconds: Int
)

class PlayerWallViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val WINDOW_SIZE = 13
        const val SHIFT_INTERVAL_SECONDS = 119
        private const val ASSET_PATH = "prayer/prayer_wall_items.json"
        private const val LOCAL_INSERT_START_POSITION = WINDOW_SIZE - 1
    }

    private val gson = Gson()
    private val repository = KjvRepository(application)
    private var sourceItems: List<PrayerWallItem> = emptyList()
    private var shuffledItems: List<PrayerWallItem> = emptyList()
    private var timelineItems: MutableList<PrayerWallItem> = mutableListOf()
    private var latestLocalPrayers: List<MyPrayerEntity> = emptyList()
    private val pendingDeletedLocalPrayerIds = mutableSetOf<Long>()

    private data class LocalPrayerInsertion(
        val prayer: MyPrayerEntity,
        val position: Int,
        val isSticky: Boolean
    )

    private val _windowState = MutableStateFlow(
        PrayerWallWindow(
            items = emptyList(),
            startIndex = 0,
            windowSize = WINDOW_SIZE,
            intervalSeconds = SHIFT_INTERVAL_SECONDS
        )
    )
    val windowState: StateFlow<PrayerWallWindow> = _windowState.asStateFlow()

    init {
        ensureDataReady()
        observeLocalPrayers()
    }

    /**
     * 对外入口：确保已加载并可获取窗口数据
     */
    fun ensureDataReady() {
        if (sourceItems.isEmpty()) {
            sourceItems = readItemsFromAsset()
        }
        if (sourceItems.isEmpty()) {
            _windowState.value = _windowState.value.copy(items = emptyList(), startIndex = 0)
            return
        }

        restoreOrShuffleForToday()
        emitCurrentWindow()
    }

    /**
     * 对外入口：获取当前窗口（13条）
     */
    fun getCurrentWindowItems(): List<PrayerWallItem> {
        ensureDataReady()
        return _windowState.value.items
    }

    /**
     * 对外入口：每119秒调用一次，窗口向下移动1条
     * 当剩余不足13条时，重新乱序并从头继续
     */
    fun shiftWindowByOne(): List<PrayerWallItem> {
        ensureDataReady()
        if (timelineItems.isEmpty()) return emptyList()
        expireCurrentStickyPrayerIfNeeded()

        val nextStart = PlayerWallSettings.windowStartIndex + 1
        val needReshuffle = sourceItems.size < WINDOW_SIZE || nextStart + WINDOW_SIZE > timelineItems.size
        if (needReshuffle) {
            reshuffleAndResetWindow()
        } else {
            PlayerWallSettings.windowStartIndex = nextStart
        }

        // 每个切换周期都按发布时间重算本地祈祷位次，并清理超时数据
        rebuildTimelineItems()
        emitCurrentWindow()
        return _windowState.value.items
    }

    /**
     * 对外入口：手动强制重置（可用于下拉刷新等）
     */
    fun reshuffleNow(): List<PrayerWallItem> {
        ensureDataReady()
        if (sourceItems.isEmpty()) return emptyList()
        reshuffleAndResetWindow()
        rebuildTimelineItems()
        emitCurrentWindow()
        return _windowState.value.items
    }

    suspend fun stickyFirstWaitingLocalPrayer(): Boolean {
        val waitingLocalPrayerId = _windowState.value.items
            .drop(1)
            .firstOrNull { it.localPrayerId != null }
            ?.localPrayerId
            ?: return false
        val updated = withContext(Dispatchers.IO) {
            repository.stickyMyPrayerToTop(waitingLocalPrayerId)
        }
        if (updated) {
            ensureDataReady()
            emitCurrentWindow()
        }
        return updated
    }

    private fun readItemsFromAsset(): List<PrayerWallItem> {
        return try {
            val json = getApplication<Application>().assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
            val type = object : TypeToken<List<PrayerWallAssetItem>>() {}.type
            gson.fromJson<List<PrayerWallAssetItem>>(json, type)
                .orEmpty()
                .mapNotNull { item ->
                    val username = item.username?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    val content = item.content?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                    PrayerWallItem(
                        username = username,
                        content = content,
                        likes = item.likes ?: 0
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun restoreOrShuffleForToday() {
        val todayStart = getTodayStartMillis()
        val shouldShuffleToday = PlayerWallSettings.lastShuffleDate != todayStart

        if (shouldShuffleToday) {
            reshuffleAndResetWindow(todayStart)
            rebuildTimelineItems()
            return
        }

        val restored = restoreShuffledItems()
        if (!restored) {
            reshuffleAndResetWindow(todayStart)
        } else {
            PlayerWallSettings.windowStartIndex =
                PlayerWallSettings.windowStartIndex.coerceIn(0, maxStartIndex(shuffledItems.size))
        }
        rebuildTimelineItems()
    }

    private fun restoreShuffledItems(): Boolean {
        val raw = PlayerWallSettings.shuffledOrder.orEmpty()
        if (raw.isBlank()) return false

        val order = raw.split(",")
            .mapNotNull { it.toIntOrNull() }
            .distinct()

        if (order.size != sourceItems.size) return false
        if (order.any { it !in sourceItems.indices }) return false

        shuffledItems = order.map { idx -> sourceItems[idx] }
        return shuffledItems.isNotEmpty()
    }

    private fun reshuffleAndResetWindow(shuffleDate: Long = getTodayStartMillis()) {
        val order = sourceItems.indices.shuffled()
        shuffledItems = order.map { idx -> sourceItems[idx] }

        PlayerWallSettings.lastShuffleDate = shuffleDate
        PlayerWallSettings.shuffledOrder = order.joinToString(",")
        PlayerWallSettings.windowStartIndex = 0
    }

    private fun emitCurrentWindow() {
        if (timelineItems.isEmpty()) {
            _windowState.value = _windowState.value.copy(items = emptyList(), startIndex = 0)
            return
        }

        val start = PlayerWallSettings.windowStartIndex.coerceIn(0, maxStartIndex(timelineItems.size))
        PlayerWallSettings.windowStartIndex = start
        val end = (start + WINDOW_SIZE).coerceAtMost(timelineItems.size)
        val window = timelineItems.subList(start, end)

        _windowState.value = PrayerWallWindow(
            items = window,
            startIndex = start,
            windowSize = WINDOW_SIZE,
            intervalSeconds = SHIFT_INTERVAL_SECONDS
        )
    }

    private fun maxStartIndex(size: Int): Int {
        if (size <= WINDOW_SIZE) return 0
        return size - WINDOW_SIZE
    }

    private fun observeLocalPrayers() {
        viewModelScope.launch {
            repository.getMyPrayersFlow().collect { prayers ->
                latestLocalPrayers = prayers.sortedBy { it.createdAt }
                val prayerIds = latestLocalPrayers.map { it.id }.toSet()
                pendingDeletedLocalPrayerIds.retainAll(prayerIds)
                ensureDataReady()
                emitCurrentWindow()
            }
        }
    }

    private fun rebuildTimelineItems() {
        timelineItems = shuffledItems.toMutableList()
        val now = System.currentTimeMillis()
        val stickyPrayerId = PlayerWallSettings.stickyPrayerId.takeIf { it > 0L }
        var stickyPrayerStillActive = false
        val activeLocalPrayers = latestLocalPrayers.mapNotNull { prayer ->
            if (pendingDeletedLocalPrayerIds.contains(prayer.id)) {
                return@mapNotNull null
            }
            val position = calculateLocalPrayerPosition(prayer.createdAt, now)
            if (position < 0) {
                deleteLocalPrayerAsync(prayer.id)
                null
            } else {
                val isSticky = stickyPrayerId != null && prayer.id == stickyPrayerId
                if (isSticky) {
                    stickyPrayerStillActive = true
                }
                LocalPrayerInsertion(
                    prayer = prayer,
                    position = if (isSticky) 0 else position,
                    isSticky = isSticky
                )
            }
        }.sortedWith(
            compareBy<LocalPrayerInsertion> { it.position }
                // 相同位置时让 sticky 后插入，以便最终位于最前
                .thenBy { if (it.isSticky) 1 else 0 }
                .thenByDescending { it.prayer.createdAt }
        )

        if (stickyPrayerId != null && !stickyPrayerStillActive) {
            PlayerWallSettings.stickyPrayerId = 0L
        }

        activeLocalPrayers.forEach { insertion ->
            val insertIndex =
                (PlayerWallSettings.windowStartIndex + insertion.position).coerceIn(0, timelineItems.size)
            timelineItems.add(insertIndex, insertion.prayer.toPrayerWallItem())
        }
        PlayerWallSettings.windowStartIndex = PlayerWallSettings.windowStartIndex.coerceIn(0, maxStartIndex(timelineItems.size))
    }

    private fun deleteLocalPrayerAsync(prayerId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteMyPrayerById(prayerId)
        }
    }

    private fun expireCurrentStickyPrayerIfNeeded() {
        val stickyPrayerId = PlayerWallSettings.stickyPrayerId.takeIf { it > 0L } ?: return
        val currentTopPrayerId = _windowState.value.items.firstOrNull()?.localPrayerId ?: return
        if (currentTopPrayerId != stickyPrayerId) return
        pendingDeletedLocalPrayerIds.add(stickyPrayerId)
        PlayerWallSettings.stickyPrayerId = 0L
        deleteLocalPrayerAsync(stickyPrayerId)
    }

    private fun calculateLocalPrayerPosition(createdAt: Long, now: Long): Int {
        val elapsedMs = (now - createdAt).coerceAtLeast(0L)
        val elapsedWindows = (elapsedMs / (SHIFT_INTERVAL_SECONDS * 1000L)).toInt()
        return LOCAL_INSERT_START_POSITION - elapsedWindows
    }

    private fun MyPrayerEntity.toPrayerWallItem(): PrayerWallItem {
        return PrayerWallItem(
            username = username,
            content = content,
            likes = 0,
            localPrayerId = id
        )
    }

    private fun getTodayStartMillis(): Long {
        return Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
