package com.mobile.bible.kjv.ui.adapter

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import android.graphics.Matrix
import android.graphics.drawable.Drawable
import android.widget.LinearLayout
import android.util.TypedValue
import android.view.Gravity
import com.mobile.bible.kjv.ui.activity.VersePlayerActivity
import com.mobile.bible.kjv.ui.activity.VerseReadActivity
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.target.Target
import com.mobile.bible.kjv.R
import android.widget.Toast
import android.app.Activity
import androidx.fragment.app.FragmentActivity
import com.android.common.bill.ads.AdResult
import com.android.common.bill.ads.ext.AdShowExt
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import net.corekit.core.report.ReportDataManager

enum class JourneyStatus { START, DONE }

data class JourneyItem(
    val stepId: Int,
    val title: String,
    val subtitle: String,
    val status: JourneyStatus,
    val bgRes: Int,
    val tags: List<String> = emptyList(),
    val expanded: Boolean = false,
    val quoteText: String = "",
    val verseReference: String = ""
)

class JourneyAdapter : RecyclerView.Adapter<JourneyAdapter.JourneyVH>() {
    private val items = mutableListOf<JourneyItem>()
    private var recyclerView: RecyclerView? = null
    var onCardExpanded: ((View) -> Unit)? = null

    fun submit(newItems: List<JourneyItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): JourneyVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_today_journey_item, parent, false)
        return JourneyVH(v)
    }

    override fun onBindViewHolder(holder: JourneyVH, position: Int) {
        val item = items[position]
        val (displayTitle, durationText) = splitTitleAndDuration(item.title)
        holder.title.text = displayTitle
        holder.subtitle.text = displayTitle
        holder.chapter.text = item.subtitle
        holder.timeText.text = durationText
        holder.timeBadge.visibility = if (durationText.isBlank()) View.GONE else View.VISIBLE
        holder.doneBadge.visibility = if (item.status == JourneyStatus.DONE) View.VISIBLE else View.GONE
        holder.expandArrow.setImageResource(
            if (item.expanded) R.drawable.svg_coll_arrow else R.drawable.svg_expand_arrow
        )
        holder.title.visibility = if (item.expanded) View.GONE else View.VISIBLE
        holder.chapter.visibility = if (item.subtitle.isBlank()) View.GONE else View.VISIBLE
        Glide.with(holder.itemView)
            .load(item.bgRes)
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable?>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    applyFitWidthMatrix(holder.bg, holder.bg.width, holder.root.layoutParams.height)
                    return false
                }

            })
            .into(holder.bg)
        val density = holder.itemView.resources.displayMetrics.density
        val heightPx = if (item.expanded) (206 * density).toInt() else (122 * density).toInt()
        val lp = holder.root.layoutParams
        lp.height = heightPx
        holder.root.layoutParams = lp
        holder.expandPanel.visibility = if (item.expanded) View.VISIBLE else View.GONE
        holder.bg.post {
            applyFitWidthMatrix(holder.bg, holder.bg.width, holder.root.layoutParams.height)
        }
        val tagsLayout = holder.chapterTags
        tagsLayout.removeAllViews()
        val density2 = holder.itemView.resources.displayMetrics.density
        val padH = (6 * density2).toInt()
        val padV = (4 * density2).toInt()
        val marginEnd = (5 * density2).toInt()
        if (item.tags.isEmpty()) {
            tagsLayout.visibility = View.GONE
        } else {
            tagsLayout.visibility = View.VISIBLE
            for (t in item.tags) {
                val tv = TextView(holder.itemView.context)
                tv.text = t
                tv.setTextColor(0xFFFFFFFF.toInt())
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11f)
                tv.setPadding(padH, padV, padH, padV)
                tv.gravity = Gravity.CENTER
                tv.setBackgroundResource(R.drawable.bg_journey_tag)
                val lpTag = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                lpTag.setMargins(0, 0, marginEnd, 0)
                tv.layoutParams = lpTag
                tagsLayout.addView(tv)
            }
        }
        holder.root.requestLayout()
        holder.root.setOnClickListener {
            val cardNumber = cardNumberForStepId(item.stepId)
            ReportDataManager.reportData("Card_Click", mapOf("cardNumber" to cardNumber))

            val p = holder.bindingAdapterPosition
            if (p != RecyclerView.NO_POSITION) {
                val cur = items[p]
                val expanding = !cur.expanded
                if (expanding) {
                    for (i in items.indices) {
                        if (i != p && items[i].expanded) {
                            items[i] = items[i].copy(expanded = false)
                            val vh = recyclerView?.findViewHolderForAdapterPosition(i) as? JourneyVH
                            if (vh != null) {
                                vh.expandArrow.setImageResource(R.drawable.svg_expand_arrow)
                                vh.title.visibility = View.VISIBLE
                                vh.expandPanel.visibility = View.INVISIBLE
                                val d2 = vh.itemView.resources.displayMetrics.density
                                val startH2 = vh.root.layoutParams.height
                                val endH2 = (122 * d2).toInt()
                                val anim2 = ValueAnimator.ofInt(startH2, endH2)
                                anim2.duration = 200
                                anim2.addUpdateListener {
                                    val h2 = it.animatedValue as Int
                                    val lp2 = vh.root.layoutParams
                                    lp2.height = h2
                                    vh.root.layoutParams = lp2
                                    applyFitWidthMatrix(vh.bg, vh.bg.width, h2)
                                    vh.root.requestLayout()
                                }
                                anim2.addListener(object : AnimatorListenerAdapter() {
                                    override fun onAnimationEnd(animation: Animator) {
                                        vh.expandPanel.visibility = View.GONE
                                    }
                                })
                                anim2.start()
                            }
                        }
                    }
                }
                items[p] = cur.copy(expanded = expanding)
                holder.expandArrow.setImageResource(
                    if (expanding) R.drawable.svg_coll_arrow else R.drawable.svg_expand_arrow
                )
                holder.title.visibility = if (expanding) View.GONE else View.VISIBLE
                if (!expanding) holder.expandPanel.visibility = View.INVISIBLE
                val d = holder.itemView.resources.displayMetrics.density
                val startH = holder.root.layoutParams.height
                val endH = if (expanding) (206 * d).toInt() else (122 * d).toInt()
                if (expanding) holder.expandPanel.visibility = View.VISIBLE
                val anim = ValueAnimator.ofInt(startH, endH)
                anim.duration = 200
                anim.addUpdateListener {
                    val h = it.animatedValue as Int
                    val lp2 = holder.root.layoutParams
                    lp2.height = h
                    holder.root.layoutParams = lp2
                    applyFitWidthMatrix(holder.bg, holder.bg.width, h)
                    holder.root.requestLayout()
                }
                anim.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (!expanding) holder.expandPanel.visibility = View.GONE
                        if (expanding) {
                            holder.itemView.post {
                                onCardExpanded?.invoke(holder.itemView)
                            }
                        }
                    }
                })
                anim.start()
            }
        }
        holder.listen.setOnClickListener {
            val cardNumber = cardNumberForStepId(item.stepId)
            ReportDataManager.reportData("Listen_Click", mapOf("cardNumber" to cardNumber))

            val context = holder.root.context
            if (context is Activity) {
                // Show loading
                // Toast.makeText(context, "Loading Ad...", Toast.LENGTH_SHORT).show()
                val navigateAction = {
                    val intent = Intent(context, VersePlayerActivity::class.java)
                    intent.putExtra(VersePlayerActivity.EXTRA_STEP_ID, item.stepId)
                    intent.putExtra("extra_quote_text", item.quoteText)
                    intent.putExtra("extra_track_title", item.title)
                    intent.putExtra("extra_verse_reference", item.verseReference)
                    intent.putExtra(VersePlayerActivity.EXTRA_TRACK_TASK_COMPLETION, true)
                    context.startActivity(intent)
                    context.overridePendingTransition(R.anim.activity_scale_alpha_enter, R.anim.activity_no_change)
                }

                if (context is FragmentActivity) {
                    MainScope().launch {
                        var rewardEarned = false
                        val result = AdShowExt.showRewardedAd(
                            activity = context,
                            onRewardEarned = { rewardEarned = true },
                            competeWithInterstitial = false
                        )
                        if (rewardEarned) {
                            navigateAction()
                        } else if (result is AdResult.Failure) {
                            navigateAction()
                        } else {
                            Toast.makeText(context, "You need to watch the ad to proceed.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    navigateAction()
                }
            } else {
                 val intent = Intent(holder.root.context, VersePlayerActivity::class.java)
                 intent.putExtra(VersePlayerActivity.EXTRA_STEP_ID, item.stepId)
                 intent.putExtra("extra_quote_text", item.quoteText)
                 intent.putExtra("extra_track_title", item.title)
                 intent.putExtra("extra_verse_reference", item.verseReference)
                 holder.root.context.startActivity(intent)
                 (holder.root.context as? Activity)?.overridePendingTransition(R.anim.activity_scale_alpha_enter, R.anim.activity_no_change)
            }
        }
        holder.read.setOnClickListener {
            val cardNumber = cardNumberForStepId(item.stepId)
            ReportDataManager.reportData("Read_Click", mapOf("cardNumber" to cardNumber))

            val context = holder.root.context
            val intent = Intent(context, VerseReadActivity::class.java)
            intent.putExtra(VerseReadActivity.EXTRA_STEP_ID, item.stepId)
            intent.putExtra("extra_quote_text", item.quoteText)
            intent.putExtra("extra_track_title", item.title)
            intent.putExtra("extra_verse_reference", item.verseReference)
            context.startActivity(intent)
            (context as? Activity)?.overridePendingTransition(R.anim.activity_scale_alpha_enter, R.anim.activity_no_change)
        }
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        if (this.recyclerView === recyclerView) this.recyclerView = null
    }

    override fun getItemCount(): Int = items.size

    private fun cardNumberForStepId(stepId: Int): Int {
        return ((stepId - 1).mod(3)) + 1
    }

    class JourneyVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val bg: ImageView = itemView.findViewById(R.id.image_bg)
        val timeBadge: View = itemView.findViewById(R.id.time_badge)
        val timeText: TextView = itemView.findViewById(R.id.time_text)
        val doneBadge: View = itemView.findViewById(R.id.done_badge)
        val expandArrow: ImageView = itemView.findViewById(R.id.expand_arrow)
        val title: TextView = itemView.findViewById(R.id.title)
        val subtitle: TextView = itemView.findViewById(R.id.subtitle)
        val chapter: TextView = itemView.findViewById(R.id.chapter)
        val root: View = itemView.findViewById(R.id.journey_item_root)
        val expandPanel: View = itemView.findViewById(R.id.expand_panel)
        val listen: View = itemView.findViewById(R.id.listen)
        val read: LinearLayout = itemView.findViewById(R.id.read)
        val chapterTags: LinearLayout = itemView.findViewById(R.id.chapter_tags)
    }

    private fun applyFitWidthMatrix(v: ImageView, viewWidth: Int, viewHeight: Int) {
        val d = v.drawable ?: return
        val dw = d.intrinsicWidth
        val dh = d.intrinsicHeight
        if (dw <= 0 || dh <= 0 || viewWidth <= 0 || viewHeight <= 0) return
        val sw = viewWidth.toFloat() / dw.toFloat()
        val sh = viewHeight.toFloat() / dh.toFloat()
        val s = if (sh > sw) sh else sw
        val scaledW = dw * s
        val scaledH = dh * s
        val dx = (viewWidth - scaledW) / 2f
        val dy = (viewHeight - scaledH) / 2f
        val m = Matrix()
        m.setScale(s, s)
        m.postTranslate(dx, dy)
        v.scaleType = ImageView.ScaleType.MATRIX
        v.imageMatrix = m
    }

    private fun splitTitleAndDuration(rawTitle: String): Pair<String, String> {
        val match = TITLE_DURATION_REGEX.matchEntire(rawTitle.trim()) ?: return rawTitle to ""
        return match.groupValues[1].trim() to match.groupValues[2].trim()
    }

    companion object {
        private val TITLE_DURATION_REGEX = Regex("^(.*)\\s*\\(([^()]*)\\)$")
    }
}
