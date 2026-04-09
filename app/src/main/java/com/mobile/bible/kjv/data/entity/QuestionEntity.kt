package com.mobile.bible.kjv.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 答题问题表
 */
@Entity(
    tableName = "questions",
    foreignKeys = [
        ForeignKey(
            entity = LevelEntity::class,
            parentColumns = ["level"],
            childColumns = ["level"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("level")]
)
data class QuestionEntity(
    @PrimaryKey
    val questionId: String,         // 问题ID (如 L1Q1)
    val level: Int,                 // 所属关卡
    val questionText: String,       // 问题文本
    val optionA: String,            // 选项A
    val optionB: String,            // 选项B
    val optionC: String,            // 选项C
    val optionD: String,            // 选项D
    val answerIndex: String,        // 正确答案索引 (A/B/C/D)
    val reference: String           // 经文引用
)
