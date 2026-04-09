package com.mobile.bible.kjv.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mobile.bible.kjv.R

enum class PlanBadge { PRIMARY, FREE, TODAY }

data class ReadingPlanItem(
    val planName: String,
    val planTitle: String,
    val planDesc: String,
    val badgeText: String,
    val badgeType: PlanBadge
)

class ReadingPlansAdapter : RecyclerView.Adapter<ReadingPlansAdapter.PlanVH>() {
    private val items = mutableListOf<ReadingPlanItem>()
    var onBadgeClick: ((ReadingPlanItem) -> Unit)? = null

    fun submit(newItems: List<ReadingPlanItem>) {
        items.clear()
        items.addAll(newItems.take(MAX_VISIBLE_ITEMS))
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlanVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_news_reading_plan_card, parent, false)
        return PlanVH(v)
    }

    override fun onBindViewHolder(holder: PlanVH, position: Int) {
        val item = items[position]
        holder.planName.text = item.planName
        holder.planTitle.text = item.planTitle
        holder.planDesc.text = item.planDesc
        holder.badge.text = item.badgeText
        val d = holder.itemView.resources.displayMetrics.density
        val params = holder.itemView.layoutParams as ViewGroup.MarginLayoutParams
        params.marginStart = if (position == 0) (16 * d).toInt() else 0
        params.marginEnd = if (position == items.size - 1) (16 * d).toInt() else (12 * d).toInt()
        holder.itemView.layoutParams = params
        when (position % 3) {
            0 -> {
                holder.badge.setBackgroundResource(R.drawable.bg_plan_btn_red)
                holder.planIcon.setImageResource(R.drawable.bg_plan_avatar_red)
            }
            1 -> {
                holder.badge.setBackgroundResource(R.drawable.bg_plan_btn_orange)
                holder.planIcon.setImageResource(R.drawable.bg_plan_avatar_orange)
            }
            else -> {
                holder.badge.setBackgroundResource(R.drawable.bg_plan_btn_green)
                holder.planIcon.setImageResource(R.drawable.bg_plan_avatar_green)
            }
        }
        holder.badge.setOnClickListener {
            onBadgeClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size

    class PlanVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val planIcon: ImageView = itemView.findViewById(R.id.icon_plan)
        val planName: TextView = itemView.findViewById(R.id.plan_name)
        val planTitle: TextView = itemView.findViewById(R.id.plan_title)
        val planDesc: TextView = itemView.findViewById(R.id.plan_desc)
        val badge: TextView = itemView.findViewById(R.id.badge)
    }

    companion object {
        private const val MAX_VISIBLE_ITEMS = 6
    }
}
