package com.mobile.bible.kjv.data.model

import com.google.gson.annotations.SerializedName

data class DevotionalDay(
    @SerializedName("index") val index: Int,
    @SerializedName("steps") val steps: List<DevotionalStep>
)

data class DevotionalStep(
    @SerializedName("step_id") val stepId: Int,
    @SerializedName("entry_title") val entryTitle: String,
    @SerializedName("entry_duration") val entryDuration: String,
    @SerializedName("content") val content: DevotionalContent
)

data class DevotionalContent(
    @SerializedName("verse_reference") val verseReference: String? = null,
    @SerializedName("verse_text") val verseText: String? = null,
    @SerializedName("keywords") val keywords: List<String>? = null,
    @SerializedName("title") val title: String? = null,
    @SerializedName("text") val text: String? = null
)
