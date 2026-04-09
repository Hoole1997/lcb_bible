package com.mobile.bible.kjv.prefs

import com.remax.base.ext.KvBoolDelegate

object AudioSettings {
    var isMusicEnabled: Boolean by KvBoolDelegate("audio_music_enabled", true)
    var isSoundEnabled: Boolean by KvBoolDelegate("audio_sound_enabled", true)
}
