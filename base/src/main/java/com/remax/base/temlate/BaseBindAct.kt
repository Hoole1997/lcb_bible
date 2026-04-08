package com.remax.base.temlate

import android.view.View
import androidx.viewbinding.ViewBinding
import com.example.base.extention.inflateBindingWithGeneric

abstract class BaseBindAct<VB : ViewBinding> : BaseAct() {

    override fun xGetIdXml(): Int = 0

    lateinit var selfBindView: VB

    override fun prepareBindView(): View? {
        selfBindView = inflateBindingWithGeneric(layoutInflater)
        return selfBindView.root
    }
}