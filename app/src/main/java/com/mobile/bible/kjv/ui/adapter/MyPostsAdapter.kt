package com.mobile.bible.kjv.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mobile.bible.kjv.R

data class MyPostListItem(
    val id: Long,
    val username: String,
    val content: String,
    val createdAtText: String,
    val visibilityText: String,
    val visibilityScope: String
)

class MyPostsAdapter : RecyclerView.Adapter<MyPostsAdapter.MyPostViewHolder>() {

    private val items = mutableListOf<MyPostListItem>()
    private var onMoreClick: ((MyPostListItem) -> Unit)? = null
    private var onStickyClick: ((MyPostListItem) -> Unit)? = null

    fun submitItems(newItems: List<MyPostListItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setOnMoreClickListener(listener: (MyPostListItem) -> Unit) {
        onMoreClick = listener
    }

    fun setOnStickyClickListener(listener: (MyPostListItem) -> Unit) {
        onStickyClick = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyPostViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_my_post, parent, false)
        return MyPostViewHolder(view)
    }

    override fun onBindViewHolder(holder: MyPostViewHolder, position: Int) {
        val item = items[position]
        holder.name.text = item.username
        holder.content.text = item.content
        holder.createdAt.text = item.createdAtText
        holder.visibility.text = item.visibilityText
        holder.iconMore.setOnClickListener {
            onMoreClick?.invoke(item)
        }
        holder.layoutSticky.setOnClickListener {
            onStickyClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = items.size

    class MyPostViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_name)
        val content: TextView = itemView.findViewById(R.id.tv_content)
        val createdAt: TextView = itemView.findViewById(R.id.tv_created_at)
        val visibility: TextView = itemView.findViewById(R.id.tv_visibility)
        val iconMore: ImageView = itemView.findViewById(R.id.icon_more)
        val layoutSticky: View = itemView.findViewById(R.id.layout_sticky)
    }
}
