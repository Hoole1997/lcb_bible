package com.mobile.bible.kjv.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.mobile.bible.kjv.R

data class ListenLearnItem(
    val title: String,
    val subtitle: String,
    val duration: String,
    val coverRes: Int
)

class ListenLearnAdapter : RecyclerView.Adapter<ListenLearnAdapter.ListenVH>() {
    private val items = mutableListOf<ListenLearnItem>()

    fun submit(newItems: List<ListenLearnItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListenVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_listen_learn_card, parent, false)
        return ListenVH(v)
    }

    override fun onBindViewHolder(holder: ListenVH, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.subtitle.text = item.subtitle
        holder.duration.text = item.duration
        val d = holder.itemView.resources.displayMetrics.density
        Glide.with(holder.itemView)
            .load(item.coverRes)
            .transform(CenterCrop(), RoundedCorners((12 * d).toInt()))
            .into(holder.cover)
        val params = holder.itemView.layoutParams as ViewGroup.MarginLayoutParams
        params.marginStart = if (position == 0) (16 * d).toInt() else 0
        params.marginEnd = if (position == items.size - 1) (16 * d).toInt() else (12 * d).toInt()
        holder.itemView.layoutParams = params
    }

    override fun getItemCount(): Int = items.size

    class ListenVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cover: ImageView = itemView.findViewById(R.id.cover)
        val icon: ImageView = itemView.findViewById(R.id.icon_listen)
        val title: TextView = itemView.findViewById(R.id.title)
        val subtitle: TextView = itemView.findViewById(R.id.subtitle)
        val duration: TextView = itemView.findViewById(R.id.badge_duration)
    }
}
