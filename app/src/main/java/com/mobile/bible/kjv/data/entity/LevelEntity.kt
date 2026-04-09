package com.mobile.bible.kjv.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 答题关卡表
 */
@Entity(tableName = "levels")
data class LevelEntity(
    @PrimaryKey
    val level: Int,                 // 关卡编号
    val theme: String               // 关卡主题
)
