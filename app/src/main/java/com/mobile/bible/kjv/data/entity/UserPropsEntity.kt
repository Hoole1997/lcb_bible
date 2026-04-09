package com.mobile.bible.kjv.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 用户道具和金币表
 */
@Entity(tableName = "user_props")
data class UserPropsEntity(
    @PrimaryKey
    val id: Int = 1,                    // 单用户，固定ID为1
    val coins: Int = 0,                 // 金币数量，默认0
    val timePropCount: Int = 0,         // 时间道具数量（增加答题时长20s）
    val eraserPropCount: Int = 0,       // 橡皮擦道具数量（去掉一个错误答案）
    val highestPassedLevel: Int = 0     // 最高通过关卡
) {
    companion object {
        const val PROP_PRICE = 25       // 道具售价25金币
        const val TIME_PROP_SECONDS = 20 // 时间道具增加20秒
    }
}
