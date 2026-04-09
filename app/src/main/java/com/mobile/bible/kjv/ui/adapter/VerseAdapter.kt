package com.mobile.bible.kjv.ui.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mobile.bible.kjv.R

sealed class VerseListItem {
    data class Title(val text: String, val verses: List<Verse>) : VerseListItem()
    data class Verse(val index: Int, val text: String) : VerseListItem()
}

class VerseAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = mutableListOf<VerseListItem>()
    private var verseTextSizeSp: Float = 16f
    private var currentReadingVerseIndex: Int = -1

    fun submit(newItems: List<VerseListItem>) {
        items.clear()
        newItems.forEach { item ->
            when (item) {
                is VerseListItem.Title -> {
                    items.add(item)
                    items.addAll(item.verses)
                }
                is VerseListItem.Verse -> items.add(item)
            }
        }
        currentReadingVerseIndex = -1
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is VerseListItem.Title -> VIEW_TYPE_TITLE
        is VerseListItem.Verse -> VIEW_TYPE_VERSE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_TITLE -> TitleVH(inflater.inflate(R.layout.item_verse_title, parent, false))
            VIEW_TYPE_VERSE -> VerseVH(inflater.inflate(R.layout.item_verse, parent, false))
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is TitleVH -> {
                val item = items[position] as VerseListItem.Title
                holder.title.text = item.text
            }
            is VerseVH -> {
                val item = items[position] as VerseListItem.Verse
                holder.content.text = "${item.index}. ${item.text}"
                holder.content.setTextSize(TypedValue.COMPLEX_UNIT_SP, verseTextSizeSp)
                val isReading = position == currentReadingVerseIndex + 1
                holder.content.setTextColor(if (isReading) COLOR_READING else COLOR_NORMAL)
                // Always reset to a deterministic typeface to avoid recycled bold style leakage.
                holder.content.typeface = if (isReading) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
        }
    }

    override fun getItemCount(): Int = items.size

    class TitleVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.tv_title)
    }

    class VerseVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val content: TextView = itemView.findViewById(R.id.tv_content)
    }

    fun setVerseTextSize(sizeSp: Int) {
        verseTextSizeSp = sizeSp.toFloat()
        notifyDataSetChanged()
    }

    fun setCurrentReadingVerseIndex(index: Int) {
        val safeIndex = index.coerceAtLeast(-1)
        if (currentReadingVerseIndex == safeIndex) return

        val oldAdapterPos = currentReadingVerseIndex + 1
        val newAdapterPos = safeIndex + 1
        currentReadingVerseIndex = safeIndex

        if (oldAdapterPos in 1 until itemCount) {
            notifyItemChanged(oldAdapterPos)
        }
        if (newAdapterPos in 1 until itemCount) {
            notifyItemChanged(newAdapterPos)
        }
    }

    companion object {
        private const val VIEW_TYPE_TITLE = 1
        private const val VIEW_TYPE_VERSE = 2
        private val COLOR_NORMAL = Color.parseColor("#333333")
        private val COLOR_READING = Color.parseColor("#DF9C67")
    }
}