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

sealed class PlayerWallListItem {
    data class Card(
        val itemKey: String,
        val userName: String,
        val content: String,
        val timeLeft: String,
        val likeCount: Int,
        val isLiked: Boolean
    ) : PlayerWallListItem()

    data class Title(
        val text: String,
        val canSticky: Boolean
    ) : PlayerWallListItem()

    data class PrayerItem(
        val name: String,
        val content: String
    ) : PlayerWallListItem()
}

class PlayerWallAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<PlayerWallListItem>()
    private var onCardLikeClick: ((PlayerWallListItem.Card) -> Unit)? = null
    private var onTitleStickyClick: (() -> Unit)? = null

    fun submitItems(newItems: List<PlayerWallListItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    fun setOnCardLikeClickListener(listener: (PlayerWallListItem.Card) -> Unit) {
        onCardLikeClick = listener
    }

    fun setOnTitleStickyClickListener(listener: () -> Unit) {
        onTitleStickyClick = listener
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is PlayerWallListItem.Card -> VIEW_TYPE_CARD
        is PlayerWallListItem.Title -> VIEW_TYPE_TITLE
        is PlayerWallListItem.PrayerItem -> VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_CARD -> CardVH(inflater.inflate(R.layout.item_player_wall_card, parent, false))
            VIEW_TYPE_TITLE -> TitleVH(inflater.inflate(R.layout.item_player_wall_title, parent, false))
            VIEW_TYPE_ITEM -> ItemVH(inflater.inflate(R.layout.item_player_wall_item, parent, false))
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is CardVH -> {
                val item = items[position] as PlayerWallListItem.Card
                holder.userName.text = item.userName
                holder.content.text = item.content
                holder.timeLeft.text = item.timeLeft
                holder.likeCount.text = item.likeCount.toString()
                holder.likeIcon.setImageResource(if (item.isLiked) R.drawable.svg_liked else R.drawable.svg_like)
                holder.likeIcon.isEnabled = !item.isLiked
                holder.likeIcon.setOnClickListener {
                    if (!item.isLiked) {
                        onCardLikeClick?.invoke(item)
                    }
                }
                val density = holder.itemView.resources.displayMetrics.density
                val radius = (16 * density).toInt()
                Glide.with(holder.itemView)
                    .load(R.mipmap.img_player_card)
                    .transform(CenterCrop(), RoundedCorners(radius))
                    .into(holder.bg)
            }
            is TitleVH -> {
                val item = items[position] as PlayerWallListItem.Title
                holder.title.text = item.text
                val stickyVisibility = if (item.canSticky) View.VISIBLE else View.GONE
                holder.stickyPost.visibility = stickyVisibility
                holder.stickyMore.visibility = stickyVisibility
                holder.stickyPost.setOnClickListener {
                    if (item.canSticky) {
                        onTitleStickyClick?.invoke()
                    }
                }
            }
            is ItemVH -> {
                val item = items[position] as PlayerWallListItem.PrayerItem
                holder.name.text = item.name
                holder.content.text = item.content
            }
        }
        val layoutParams = holder.itemView.layoutParams
        if (layoutParams is ViewGroup.MarginLayoutParams) {
            val density = holder.itemView.resources.displayMetrics.density
            val extraBottom = (48 * density).toInt()
            if (position == itemCount - 1) {
                layoutParams.bottomMargin = extraBottom
            } else {
                layoutParams.bottomMargin = 0
            }
            holder.itemView.layoutParams = layoutParams
        }
    }

    override fun getItemCount(): Int = items.size

    class CardVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userName: TextView = itemView.findViewById(R.id.tv_user_name)
        val content: TextView = itemView.findViewById(R.id.tv_prayer_content)
        val timeLeft: TextView = itemView.findViewById(R.id.tv_time_left)
        val likeCount: TextView = itemView.findViewById(R.id.tv_like_count)
        val likeIcon: ImageView = itemView.findViewById(R.id.icon_like)
        val bg: ImageView = itemView.findViewById(R.id.image_bg)
    }

    class TitleVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tv_title)
        val stickyPost: TextView = itemView.findViewById(R.id.tv_sticky_post)
        val stickyMore: ImageView = itemView.findViewById(R.id.icon_more)
    }

    class ItemVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tv_name)
        val content: TextView = itemView.findViewById(R.id.tv_content)
    }

    companion object {
        private const val VIEW_TYPE_CARD = 1
        private const val VIEW_TYPE_TITLE = 2
        private const val VIEW_TYPE_ITEM = 3
    }
}
