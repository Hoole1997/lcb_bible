package com.mobile.bible.kjv.prefs

import com.remax.base.ext.KvIntDelegate
import com.remax.base.ext.KvLongDelegate
import com.remax.base.ext.KvStringDelegate

object PlayerWallSettings {
    // 当天首次启动时记录日期，避免同一天重复洗牌
    var lastShuffleDate: Long by KvLongDelegate("player_wall_last_shuffle_date", 0L)

    // 洗牌后的顺序（存原始列表索引，逗号分隔）
    var shuffledOrder: String? by KvStringDelegate("player_wall_shuffled_order", "")

    // 当前13条窗口的起始索引
    var windowStartIndex: Int by KvIntDelegate("player_wall_window_start_index", 0)

    // 激励广告置顶后的祈祷ID，供祈祷墙优先展示在窗口首位
    var stickyPrayerId: Long by KvLongDelegate("player_wall_sticky_prayer_id", 0L)
}
