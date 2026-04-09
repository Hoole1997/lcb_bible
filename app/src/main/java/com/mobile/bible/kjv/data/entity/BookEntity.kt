package com.mobile.bible.kjv.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 圣经书籍目录表
 */
@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey
    val id: Int,                    // 书籍ID (1-66)
    val testament: String,          // 新约/旧约 (OT/NT)
    val name: String,               // 书籍名称
    val chapters: Int               // 章节数量
)
