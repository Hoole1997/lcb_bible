package com.mobile.bible.kjv.ui.vm

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mobile.bible.kjv.data.KjvDataPopulator
import com.mobile.bible.kjv.data.KjvDatabase
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class KjvSplashViewModel(application: Application) : AndroidViewModel(application) {

    private val _navigateToHome = MutableSharedFlow<Unit>(replay = 1)
    val navigateToHome: SharedFlow<Unit> = _navigateToHome

    init {
        viewModelScope.launch {
            // 同时执行：1. 等待最少3秒  2. 初始化数据库
            val minDelayJob = async { delay(3000) }
            val dbInitJob = async { initializeDatabase() }
            
            // 等待两个任务都完成
            minDelayJob.await()
            dbInitJob.await()
            
            _navigateToHome.emit(Unit)
        }
    }
    
    private suspend fun initializeDatabase() {
        try {
            val context = getApplication<Application>()
            val database = KjvDatabase.getInstance(context)
            
            // 检查数据库是否已经初始化
            val bookCount = database.bookDao().getBookCount()
            if (bookCount == 0) {
                Log.d("KjvSplashViewModel", "数据库为空，开始初始化...")
                KjvDataPopulator.populateDatabase(context, database)
                Log.d("KjvSplashViewModel", "数据库初始化完成")
            } else {
                Log.d("KjvSplashViewModel", "数据库已存在 $bookCount 本书，跳过初始化")
            }
        } catch (e: Exception) {
            Log.e("KjvSplashViewModel", "数据库初始化失败", e)
        }
    }
}
