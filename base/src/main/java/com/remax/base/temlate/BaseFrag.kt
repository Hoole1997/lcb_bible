package com.remax.base.temlate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

abstract class BaseFrag : Fragment() {
    abstract fun xmlIdView(): Int

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(xmlIdView(), container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //函数抽取
        onReadView(savedInstanceState)

    }


    /** 初始化view */
    abstract fun onReadView(savedInstanceState: Bundle?)

    override fun onResume() {
        super.onResume()
        loadDelay()
    }

    private var inflated = false

    private fun loadDelay() {
        if (inflated) {
            return
        }
        inflated = true
        viewOnInitForDelay()
    }

    open fun viewOnInitForDelay() {
    }

}