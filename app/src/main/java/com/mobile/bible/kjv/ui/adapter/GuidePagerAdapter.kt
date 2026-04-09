package com.mobile.bible.kjv.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.mobile.bible.kjv.R
import net.corekit.core.report.ReportDataManager

data class GuidePage(
    val page: Int,
    val imageResId: Int
)

class GuidePagerAdapter(
    private val pages: List<GuidePage>,
    private val onNext: (Int) -> Unit,
    private val onSkip: (Int) -> Unit
) : RecyclerView.Adapter<GuidePagerAdapter.VH>() {

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val image: ImageView = itemView.findViewById(R.id.bgImage)
        val guideAMsgLayout: View = itemView.findViewById(R.id.guide_a_msg_layout)
        val guideBMsgLayout: View = itemView.findViewById(R.id.guide_b_msg_layout)
        val guideCMsgLayout: View = itemView.findViewById(R.id.guide_c_msg_layout)
        val guideDMsgLayout: View = itemView.findViewById(R.id.guide_d_msg_layout)

        val skipA: TextView = itemView.findViewById(R.id.guide_a_skip)
        val skipB: TextView = itemView.findViewById(R.id.guide_b_skip)
        val skipC: TextView = itemView.findViewById(R.id.guide_c_skip)
        val skipD: TextView = itemView.findViewById(R.id.guide_d_skip)

        val nextA: TextView = itemView.findViewById(R.id.guide_a_btn_next)
        val nextB: TextView = itemView.findViewById(R.id.guide_b_btn_next)
        val nextC: TextView = itemView.findViewById(R.id.guide_c_btn_next)
        val nextD: TextView = itemView.findViewById(R.id.guide_d_btn_next)

        val baseTopMarginA: Int = (skipA.layoutParams as ViewGroup.MarginLayoutParams).topMargin
        val baseTopMarginB: Int = (skipB.layoutParams as ViewGroup.MarginLayoutParams).topMargin
        val baseTopMarginC: Int = (skipC.layoutParams as ViewGroup.MarginLayoutParams).topMargin
        val baseTopMarginD: Int = (skipD.layoutParams as ViewGroup.MarginLayoutParams).topMargin
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_guide_page, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = pages.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val page = pages[position]
        holder.image.setImageResource(page.imageResId)

        holder.guideAMsgLayout.visibility = View.GONE
        holder.guideBMsgLayout.visibility = View.GONE
        holder.guideCMsgLayout.visibility = View.GONE
        holder.guideDMsgLayout.visibility = View.GONE

        val statusTop = getStatusBarHeight(holder.itemView)
        setMarginTop(holder.skipA, holder.baseTopMarginA + statusTop)
        setMarginTop(holder.skipB, holder.baseTopMarginB + statusTop)
        setMarginTop(holder.skipC, holder.baseTopMarginC + statusTop)
        setMarginTop(holder.skipD, holder.baseTopMarginD + statusTop)

        holder.skipA.setOnClickListener {
            ReportDataManager.reportData("Guide_Skip", emptyMap())
            onSkip(position)
        }
        holder.skipB.setOnClickListener { onSkip(position) }
        holder.skipC.setOnClickListener { onSkip(position) }
        holder.skipD.setOnClickListener { onSkip(position) }

        holder.nextA.setOnClickListener {
            ReportDataManager.reportData("Guide_Continue", emptyMap())
            onNext(position)
        }
        holder.nextB.setOnClickListener { onNext(position) }
        holder.nextC.setOnClickListener { onNext(position) }
        holder.nextD.setOnClickListener { onNext(position) }

        when (page.page) {
            0 -> holder.guideAMsgLayout.visibility = View.VISIBLE
            1 -> holder.guideBMsgLayout.visibility = View.VISIBLE
            2 -> holder.guideCMsgLayout.visibility = View.VISIBLE
            3 -> holder.guideDMsgLayout.visibility = View.VISIBLE
        }
    }

    private fun getStatusBarHeight(view: View): Int {
        val insets = ViewCompat.getRootWindowInsets(view)
        val insetTop = insets?.getInsets(WindowInsetsCompat.Type.statusBars())?.top ?: 0
        if (insetTop > 0) return insetTop
        val resId = view.resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resId > 0) view.resources.getDimensionPixelSize(resId) else 0
    }

    private fun setMarginTop(v: View, top: Int) {
        val lp = v.layoutParams as ViewGroup.MarginLayoutParams
        if (lp.topMargin != top) {
            lp.topMargin = top
            v.layoutParams = lp
        }
    }

}
