package com.mobile.bible.kjv.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "my_prayers")
data class MyPrayerEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val username: String,
    val content: String,
    val visibility: String,
    val createdAt: Long
)
