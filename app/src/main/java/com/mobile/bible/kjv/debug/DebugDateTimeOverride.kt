package com.mobile.bible.kjv.debug

import com.mobile.bible.kjv.ui.vm.TimeSlot
import java.util.Calendar

/**
 * Debug-only runtime override for today date/time slot.
 * Stored in memory only and cleared when process restarts.
 */
object DebugDateTimeOverride {
    @Volatile
    private var state: State? = null

    data class State(
        val calendar: Calendar,
        val timeSlot: TimeSlot
    )

    fun set(calendar: Calendar, timeSlot: TimeSlot) {
        // Keep a defensive copy to avoid external mutation.
        state = State((calendar.clone() as Calendar), timeSlot)
    }

    fun clear() {
        state = null
    }

    fun get(): State? {
        val current = state ?: return null
        return State((current.calendar.clone() as Calendar), current.timeSlot)
    }
}
