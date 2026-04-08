package com.remax.base.temlate

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewbinding.ViewBinding
import com.example.base.extention.inflateBindingWithGeneric

abstract class BaseBindFrag<VB : ViewBinding> : BaseFrag() {

    override fun xmlIdView() = 0

    private var _binding: VB? = null
    val selfBindView: VB? get() = _binding

    val mViewBindNoNull: VB get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = inflateBindingWithGeneric(inflater, container, false)
        return selfBindView?.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun isCreated(): Boolean {
        return _binding != null
    }

}