package com.mobile.bible.kjv.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import android.content.Intent
import android.net.Uri
import com.mobile.bible.kjv.R
import androidx.core.graphics.toColorInt
import com.mobile.bible.kjv.ui.vm.PrayerWallItem

sealed class TodayItem {
    data object Fixed : TodayItem()
    data class TodaysJourney(val items: List<JourneyItem>) : TodayItem()
    data object NewsReadingPlans : TodayItem()
    data object ListenLearn : TodayItem()
}

class TodayAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = mutableListOf<TodayItem>()
    var onJourneyCardExpanded: ((View) -> Unit)? = null
    var onPrayerWallBadgeClick: (() -> Unit)? = null
    
    // 存储每日经文数据
    private var dailyVerseText: String? = null
    private var dailyVersePosition: String? = null
    private var prayerWallItems: List<PrayerWallItem> = emptyList()

    fun submitItems(newItems: List<TodayItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    
    /**
     * 更新每日经文显示
     */
    fun updateDailyVerse(text: String, position: String) {
        dailyVerseText = text
        dailyVersePosition = position
        // 找到Fixed item的位置并更新
        val fixedIndex = items.indexOfFirst { it is TodayItem.Fixed }
        if (fixedIndex >= 0) {
            notifyItemChanged(fixedIndex)
        }
    }

    fun updatePrayerWallItems(items: List<PrayerWallItem>) {
        prayerWallItems = items
        val newsIndex = this.items.indexOfFirst { it is TodayItem.NewsReadingPlans }
        if (newsIndex >= 0) {
            notifyItemChanged(newsIndex)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is TodayItem.Fixed -> VIEW_TYPE_FIXED
        is TodayItem.TodaysJourney -> VIEW_TYPE_TODAYS_JOURNEY
        is TodayItem.NewsReadingPlans -> VIEW_TYPE_NEWS_READING_PLANS
        is TodayItem.ListenLearn -> VIEW_TYPE_LISTEN_LEARN
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_FIXED -> FixedVH(inflater.inflate(R.layout.item_today_fixed, parent, false))
            VIEW_TYPE_TODAYS_JOURNEY -> TodaysJourneyVH(inflater.inflate(R.layout.item_today_journey, parent, false))
            VIEW_TYPE_NEWS_READING_PLANS -> NewsReadingPlansVH(inflater.inflate(R.layout.item_news_reading_plans, parent, false))
            VIEW_TYPE_LISTEN_LEARN -> ListenLearnVH(inflater.inflate(R.layout.item_listen_learn, parent, false))
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is FixedVH -> {
                val bg = holder.itemView.findViewById<ImageView>(R.id.today_fix_bg)
                val info = holder.itemView.findViewById<View>(R.id.today_fixed_info)
                val verse = holder.itemView.findViewById<TextView>(R.id.info_verse)
                val versePosition = holder.itemView.findViewById<TextView>(R.id.info_verse_position)
                bg.setImageResource(R.mipmap.img_today_appbar)
                info.setBackgroundResource(R.drawable.bg_bible_today_fixed_info)
                verse.setTextColor("#633800".toColorInt())
                
                // 显示每日经文
                dailyVerseText?.let { verse.text = it }
                dailyVersePosition?.let { versePosition.text = it }
                
                holder.itemView.findViewById<View>(R.id.icon_verse_share)?.setOnClickListener { v ->
                    val packageName = "com.kjv.bible.audio.verse.read.study.tool"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName"))
                    intent.setPackage("com.android.vending")
                    try {
                        v.context.startActivity(intent)
                    } catch (e: android.content.ActivityNotFoundException) {
                        v.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$packageName")))
                    }
                }
            }
            is TodaysJourneyVH -> {
                val title = holder.itemView.findViewById<TextView>(R.id.journey_title)
                val progressbar = holder.itemView.findViewById<ProgressBar>(R.id.progressbar)
                val progressValue = holder.itemView.findViewById<TextView>(R.id.progress_value)
                val progress = holder.itemView.findViewById<TextView>(R.id.journey_progress)
                val rcy = holder.itemView.findViewById<RecyclerView>(R.id.journey_rcy)
                title.text = "Today's Journey"
                
                // 更新进度显示
                val journeyItems = (items[position] as TodayItem.TodaysJourney).items
                val total = journeyItems.size
                val current = journeyItems.count { it.status == JourneyStatus.DONE }
                progress.text = "($current/$total)"
                val percent = if (total > 0) (current.toFloat() / total * 100).toInt() else 0
                progressbar.progress = percent
                progressValue.text = "$percent%"
                
                rcy.layoutManager = LinearLayoutManager(holder.itemView.context)
                val adapter = JourneyAdapter()
                adapter.onCardExpanded = onJourneyCardExpanded
                rcy.adapter = adapter
                // 使用传入的数据
                adapter.submit(journeyItems)
            }
            is NewsReadingPlansVH -> {
                val title = holder.itemView.findViewById<TextView>(R.id.title)
                val more = holder.itemView.findViewById<TextView>(R.id.more)
                val icMore = holder.itemView.findViewById<ImageView>(R.id.ic_more)
                val rcy = holder.itemView.findViewById<RecyclerView>(R.id.rcy)
                title.text = holder.itemView.context.getString(R.string.today_prayer_wall_title)
                more.text = holder.itemView.context.getString(R.string.today_view_more)
                more.setOnClickListener {
                    onPrayerWallBadgeClick?.invoke()
                }
                icMore.setOnClickListener {
                    onPrayerWallBadgeClick?.invoke()
                }
                rcy.layoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
                val adapter = ReadingPlansAdapter()
                rcy.adapter = adapter
                adapter.onBadgeClick = {
                    onPrayerWallBadgeClick?.invoke()
                }
                adapter.submit(
                    prayerWallItems.map { prayer ->
                        ReadingPlanItem(
                            planName = prayer.username,
                            planTitle = prayer.content,
                            planDesc = "",
                            badgeText = holder.itemView.context.getString(R.string.today_view_more),
                            badgeType = PlanBadge.PRIMARY
                        )
                    }
                )
            }
            is ListenLearnVH -> {
                val title = holder.itemView.findViewById<TextView>(R.id.title)
                val more = holder.itemView.findViewById<TextView>(R.id.more)
                val rcy = holder.itemView.findViewById<RecyclerView>(R.id.rcy)
                title.text = "Listen & Learn"
                more.text = "More"
                rcy.layoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
                val adapter = ListenLearnAdapter()
                rcy.adapter = adapter
                adapter.submit(
                    listOf(
                        ListenLearnItem(
                            "The Sermon ……",
                            "The Sermon Series - Ep. 12",
                            "28 Min · Audio",
                            R.mipmap.img_today_journey_item_a
                        ),
                        ListenLearnItem(
                            "Morning Devotional",
                            "Daily Devotion - Ep. 35",
                            "15 Min · Audio",
                            R.mipmap.img_today_journey_item_b
                        ),
                        ListenLearnItem(
                            "Guided Prayer",
                            "Prayer Sessions - Ep. 9",
                            "12 Min · Audio",
                            R.mipmap.img_today_journey_item_c
                        )
                    )
                )
            }
            else -> {}
        }
    }

    class FixedVH(itemView: View) : RecyclerView.ViewHolder(itemView)
    class TodaysJourneyVH(itemView: View) : RecyclerView.ViewHolder(itemView)
    class NewsReadingPlansVH(itemView: View) : RecyclerView.ViewHolder(itemView)
    class ListenLearnVH(itemView: View) : RecyclerView.ViewHolder(itemView)

    companion object {
        private const val VIEW_TYPE_FIXED = 1
        private const val VIEW_TYPE_TODAYS_JOURNEY = 2
        private const val VIEW_TYPE_NEWS_READING_PLANS = 3
        private const val VIEW_TYPE_LISTEN_LEARN = 4
    }


}
