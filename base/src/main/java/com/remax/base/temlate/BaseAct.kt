package com.remax.base.temlate

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import com.blankj.utilcode.util.LanguageUtils
import com.example.base.extention.appendNavigationBarPaddingBottom
import com.example.base.extention.edgeToEdge
import com.remax.base.report.DataReportManager

abstract class BaseAct : AppCompatActivity() {


    abstract fun xGetIdXml(): Int

    abstract fun xLoadViewFromXml(savedInstanceState: Bundle?)

    open fun prepareData() {

    }

    open var isSaveInstance = true

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        isSaveInstance = true
    }

    override fun onResume() {
        isSaveInstance = false
        super.onResume()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        isSaveInstance = false

        super.onCreate(savedInstanceState)

        edgeToEdge()

        supportActionBar?.hide()

        prepareBindView()?.let {
            setContentView(it)
        } ?: run {
            setContentView(xGetIdXml())
        }
        runTemp(savedInstanceState)
    }


    private fun runTemp(savedInstanceState: Bundle?) {

        //函数抽取
        xLoadViewFromXml(savedInstanceState)
        prepareData()

    }

    override fun onStart() {
        super.onStart()
        DataReportManager.reportDataByName("ThinkingData","pageView",mapOf(
            "pageName" to this::class.java.simpleName,
            "pageEvent" to "onStart"))
    }

    override fun onStop() {
        super.onStop()
        DataReportManager.reportDataByName("ThinkingData","pageView",mapOf(
            "pageName" to this::class.java.simpleName,
            "pageEvent" to "onStop"))
    }


    open fun prepareBindView(): View? {
        return null
    }

    fun replaceFragment(layoutId: Int, fragment: Fragment?) {
        if (fragment == null) return
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.replace(layoutId, fragment)
        transaction.setMaxLifecycle(fragment, Lifecycle.State.RESUMED)
            .commitAllowingStateLoss()
    }

    open fun showFragment(layoutId: Int, fragment: Fragment?) {
        if (fragment == null) return
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        if (!fragment.isAdded) {
            transaction.add(layoutId, fragment)
        } else {
            transaction.show(fragment)
        }
        transaction.setMaxLifecycle(fragment, Lifecycle.State.RESUMED)
            .commitAllowingStateLoss()
    }

    open fun showFragment(fragment: Fragment?) {
        if (fragment == null) return
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.show(fragment)
            .setMaxLifecycle(fragment, Lifecycle.State.RESUMED)
            .commitAllowingStateLoss()
    }

    open fun hideFragment(fragment: Fragment?) {
        if (fragment == null) return
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.hide(fragment)
            .setMaxLifecycle(fragment, Lifecycle.State.STARTED)
            .commitAllowingStateLoss()
    }


    open fun removeFragment(fragment: Fragment?) {
        if (fragment == null || !fragment.isAdded) return
        val fragmentManager = supportFragmentManager
        val transaction = fragmentManager.beginTransaction()
        transaction.remove(fragment).commitAllowingStateLoss()
    }

    override fun attachBaseContext(newBase: Context?) {
        super.attachBaseContext(LanguageUtils.attachBaseContext(newBase))

    }


}