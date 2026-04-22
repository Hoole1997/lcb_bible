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
    val planName: String? = null,
    val planTitle: String? = null,
    val planDesc: String? = null,
    val badgeText: String? = null,
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
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_prayer_wall_card, parent, false)
        return PlanVH(v)
    }

    override fun onBindViewHolder(holder: PlanVH, position: Int) {
        val item = items[position]
        holder.username.text = item.planName.orEmpty()
        holder.content.text = item.planTitle.orEmpty()
        holder.viewMore.setOnClickListener {
            onBadgeClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size

    class PlanVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val avatar: ImageView = itemView.findViewById(R.id.iv_avatar)
        val username: TextView = itemView.findViewById(R.id.tv_username)
        val content: TextView = itemView.findViewById(R.id.tv_content)
        val viewMore: TextView = itemView.findViewById(R.id.btn_view_more)
    }

    companion object {
        private const val MAX_VISIBLE_ITEMS = 6
    }
}
