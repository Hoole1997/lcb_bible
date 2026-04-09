package com.mobile.bible.kjv.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 圣经经文表
 */
@Entity(
    tableName = "verses",
    foreignKeys = [
        ForeignKey(
            entity = BookEntity::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["bookId"]),
        Index(value = ["bookId", "chapter"]),
        Index(value = ["bookId", "chapter", "verse"])
    ]
)
data class VerseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val bookId: Int,                // 书籍ID
    val chapter: Int,               // 章节号
    val verse: Int,                 // 经文号
    val text: String                // 经文内容
)
