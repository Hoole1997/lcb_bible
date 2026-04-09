package com.mobile.bible.kjv.ui.dialog

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.animation.ValueAnimator
import android.animation.AnimatorListenerAdapter
import androidx.fragment.app.FragmentActivity
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mobile.bible.kjv.data.entity.BookEntity
import com.mobile.bible.kjv.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class ChooseChapterBottomDialog : BottomSheetDialogFragment() {

    private var books: List<BookEntity> = emptyList()
    private var selectedBookId: Int = 1
    private var selectedChapter: Int = 1

    override fun getTheme(): Int {
        return R.style.BottomSheetDialog
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.dialog_choose_chapter_bottom_sheet, container, false)
        val rcy = view.findViewById<RecyclerView>(R.id.rcy_chapters)
        rcy.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
        val adapter = ChapterGroupAdapter(
            selectedBookId = selectedBookId,
            selectedChapter = selectedChapter,
            onChapterClick = { result ->
                parentFragmentManager.setFragmentResult(
                    "choose_chapter",
                    bundleOf("book_id" to result.first, "book_name" to result.second, "chapter" to result.third)
                )
                dismiss()
            }
        )
        rcy.adapter = adapter
        adapter.submit(books.map { book ->
            ChapterGroup(
                bookId = book.id,
                name = book.name,
                chaptersCount = book.chapters,
                expanded = book.id == selectedBookId
            )
        })
        return view
    }

    override fun onStart() {
        super.onStart()
        val d = dialog as? BottomSheetDialog ?: return
        d.dismissWithAnimation = true
        d.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        d.findViewById<android.widget.FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)?.setBackgroundColor(Color.TRANSPARENT)
        val topOffsetPx = (88 * resources.displayMetrics.density).toInt()
        val bottomSheet = d.findViewById<android.widget.FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
        if (bottomSheet != null) {
            val windowHeight = resources.displayMetrics.heightPixels
            val params = bottomSheet.layoutParams
            params.height = (windowHeight - topOffsetPx).coerceAtLeast(0)
            bottomSheet.layoutParams = params
            bottomSheet.requestLayout()
        }
        val behavior = d.behavior
        behavior.isHideable = true
        behavior.skipCollapsed = true
        behavior.state = BottomSheetBehavior.STATE_EXPANDED
        behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                if (newState == BottomSheetBehavior.STATE_HIDDEN) {
                    dismissAllowingStateLoss()
                }
            }
            override fun onSlide(bottomSheet: View, slideOffset: Float) {}
        })
    }

    companion object {
        fun show(activity: FragmentActivity, books: List<BookEntity>, selectedBookId: Int = 1, selectedChapter: Int = 1) {
            ChooseChapterBottomDialog().apply {
                this.books = books
                this.selectedBookId = selectedBookId
                this.selectedChapter = selectedChapter
            }.show(activity.supportFragmentManager, "ChooseChapterBottomDialog")
        }
    }
}

private data class ChapterGroup(val bookId: Int, val name: String, val chaptersCount: Int, var expanded: Boolean = false)

private class ChapterGroupAdapter(
    private val selectedBookId: Int,
    private val selectedChapter: Int,
    private val onChapterClick: (Triple<Int, String, Int>) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val groups = mutableListOf<ChapterGroup>()

    fun submit(newGroups: List<ChapterGroup>) {
        groups.clear()
        groups.addAll(newGroups)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = VIEW_TYPE_GROUP

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return GroupVH(inflater.inflate(R.layout.item_chapter_group, parent, false), selectedBookId, selectedChapter, onChapterClick)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val group = groups[position]
        (holder as GroupVH).bind(group, selectedBookId, selectedChapter)
    }

    override fun getItemCount(): Int = groups.size

    class GroupVH(
        itemView: View,
        private val selectedBookId: Int,
        private val selectedChapter: Int,
        private val onChapterClick: (Triple<Int, String, Int>) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.tv_group_name)
        private val toggleIcon: ImageView = itemView.findViewById(R.id.iv_expand_collapse)
        private val grid: RecyclerView = itemView.findViewById(R.id.rcy_chapter_grid)

        fun bind(group: ChapterGroup, selectedBookId: Int, selectedChapter: Int) {
            title.text = group.name
            toggleIcon.setImageResource(if (group.expanded) R.drawable.svg_group_expand else R.drawable.svg_group_collapse)

            val isSelectedBook = group.bookId == selectedBookId
            // 每次 bind 都重新创建 adapter，避免 ViewHolder 复用时闭包捕获错误的 bookId
            grid.layoutManager = GridLayoutManager(itemView.context, 6)
            grid.adapter = ChapterNumberAdapter(
                chapterCount = group.chaptersCount,
                selectedChapter = if (isSelectedBook) selectedChapter else -1,
                onChapterClick = { number -> onChapterClick(Triple(group.bookId, group.name, number)) }
            )

            if (group.expanded) {
                grid.visibility = View.VISIBLE
                val lp = grid.layoutParams
                lp.height = ViewGroup.LayoutParams.WRAP_CONTENT
                grid.layoutParams = lp
            } else {
                grid.visibility = View.GONE
                val lp = grid.layoutParams
                lp.height = 0
                grid.layoutParams = lp
            }

            val clickListener = View.OnClickListener {
                val expanding = !group.expanded
                group.expanded = expanding
                toggleIcon.setImageResource(if (group.expanded) R.drawable.svg_group_expand else R.drawable.svg_group_collapse)
                if (expanding) {
                    expandGridAnimated()
                } else {
                    collapseGridAnimated()
                }
            }
            toggleIcon.setOnClickListener(clickListener)
            itemView.setOnClickListener(clickListener)
        }

        private fun expandGridAnimated() {
            grid.visibility = View.VISIBLE
            val parentWidth = (itemView as? ViewGroup)?.width ?: grid.width
            val widthSpec = View.MeasureSpec.makeMeasureSpec(parentWidth, View.MeasureSpec.EXACTLY)
            val heightSpec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            grid.measure(widthSpec, heightSpec)
            val targetHeight = grid.measuredHeight
            val lp = grid.layoutParams
            lp.height = 0
            grid.layoutParams = lp
            val animator = ValueAnimator.ofInt(0, targetHeight)
            animator.duration = 200
            animator.addUpdateListener {
                val h = it.animatedValue as Int
                val lp2 = grid.layoutParams
                lp2.height = h
                grid.layoutParams = lp2
            }
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    val lp2 = grid.layoutParams
                    lp2.height = ViewGroup.LayoutParams.WRAP_CONTENT
                    grid.layoutParams = lp2
                }
            })
            animator.start()
        }

        private fun collapseGridAnimated() {
            val startHeight = grid.height
            if (startHeight == 0) {
                grid.visibility = View.GONE
                return
            }
            val animator = ValueAnimator.ofInt(startHeight, 0)
            animator.duration = 200
            animator.addUpdateListener {
                val h = it.animatedValue as Int
                val lp2 = grid.layoutParams
                lp2.height = h
                grid.layoutParams = lp2
            }
            animator.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    grid.visibility = View.GONE
                }
            })
            animator.start()
        }
    }

    companion object {
        private const val VIEW_TYPE_GROUP = 1
    }
}

private class ChapterNumberAdapter(
    private val chapterCount: Int,
    private val selectedChapter: Int = -1,
    private val onChapterClick: (Int) -> Unit
) : RecyclerView.Adapter<ChapterNumberAdapter.VH>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_chapter_chip, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val number = position + 1
        holder.text.text = number.toString()
        val isSelected = number == selectedChapter
        if (isSelected) {
            holder.text.setBackgroundResource(R.drawable.bg_chapter_chip_selected)
            holder.text.setTextColor(Color.WHITE)
        } else {
            holder.text.setBackgroundResource(R.drawable.bg_chapter_chip)
            holder.text.setTextColor(Color.parseColor("#333333"))
        }
        holder.itemView.setOnClickListener { onChapterClick(number) }
    }

    override fun getItemCount(): Int = chapterCount

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val text: TextView = itemView.findViewById(R.id.tv_chip)
    }
}
