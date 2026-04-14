package com.mobile.bible.kjv.ui.adapter

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.LottieAnimationView
import com.mobile.bible.kjv.R
import com.remax.base.utils.LottieUtils

enum class StudyLevelSide { LEFT, RIGHT }

data class StudyLevelUiModel(
    val level: Int,
    val side: StudyLevelSide,
    val unlocked: Boolean,
    val isCurrent: Boolean,
    val iconRes: Int? = null,
    val label: String? = null,
    val showGift: Boolean = false
)

class StudyLevelAdapter(
    private val onLevelClick: ((StudyLevelUiModel) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = mutableListOf<StudyLevelUiModel>()

    fun submit(levels: List<StudyLevelUiModel>) {
        items.clear()
        items.addAll(levels)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position].side) {
            StudyLevelSide.LEFT -> VIEW_TYPE_LEFT
            StudyLevelSide.RIGHT -> VIEW_TYPE_RIGHT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_LEFT -> LeftVH(inflater.inflate(R.layout.item_study_level_left, parent, false))
            VIEW_TYPE_RIGHT -> RightVH(inflater.inflate(R.layout.item_study_level_right, parent, false))
            else -> throw IllegalArgumentException("Unknown viewType: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is LeftVH -> bindLevel(holder.itemView, item)
            is RightVH -> bindLevel(holder.itemView, item)
        }
    }

    private fun bindLevel(root: View, item: StudyLevelUiModel) {
        val iconView = root.findViewById<ImageView>(R.id.level_icon)
        val numberView = root.findViewById<TextView>(R.id.level_number)
        val giftTagView = root.findViewById<ImageView>(R.id.gift_tag)
        val passTagView = root.findViewById<ImageView>(R.id.pass_tag)
        val lottieView = root.findViewById<LottieAnimationView>(R.id.lottie_view)

        setViewSizeDp(iconView, 42f, 42f)

        giftTagView.visibility = if (item.showGift) View.VISIBLE else View.INVISIBLE

        when {
            !item.unlocked && !item.isCurrent -> {
                // 未解锁关卡（有礼物时 level_icon 用礼物锁图标，尺寸 22×24dp）
                giftTagView.setImageResource(R.mipmap.img_gift_dark)
                passTagView.visibility = View.INVISIBLE
                numberView.visibility = View.INVISIBLE
                iconView.visibility = View.VISIBLE
                lottieView.visibility = View.INVISIBLE
                if (item.showGift) {
                    iconView.setImageResource(R.drawable.svg_gift_lock)
                    setViewSizeDp(iconView, 22f, 24f)
                } else {
                    iconView.setImageResource(R.drawable.svg_level_locked)
                }
            }
            item.isCurrent -> {
                // 当前进行中的关卡
                if (item.showGift) {
                    // 展示lottie动画
                    lottieView.visibility = View.VISIBLE
                    iconView.visibility = View.INVISIBLE
                    numberView.visibility = View.INVISIBLE
                    passTagView.visibility = View.INVISIBLE
                    giftTagView.visibility = View.INVISIBLE
                    
                    // Set images folder first to prevent "You must set an images folder" error
                    lottieView.setImageAssetsFolder("lottie/gift/images/")
                    
                    // 手动处理图片加载，解决路径解析问题
                    lottieView.setImageAssetDelegate { asset ->
                        try {
                            val fileName = asset.fileName
                            val dirName = asset.dirName
                            val path = "lottie/gift/$dirName$fileName"
                            val inputStream = root.context.assets.open(path)
                            android.graphics.BitmapFactory.decodeStream(inputStream)
                        } catch (e: Exception) {
                            null
                        }
                    }

                    lottieView.setAnimation("lottie/gift/data.json")
                    lottieView.repeatCount = -1
                    lottieView.playAnimation()
                } else {
                    passTagView.visibility = View.INVISIBLE
                    iconView.visibility = View.INVISIBLE
                    numberView.visibility = View.VISIBLE
                    lottieView.visibility = View.INVISIBLE
                    numberView.text = item.label ?: item.level.toString()
                    numberView.background = root.context.getDrawable(R.drawable.bg_study_level_current)
                }
            }
            else -> {
                // 已通过的关卡
                giftTagView.setImageResource(R.mipmap.img_gift_dark)
                iconView.visibility = View.INVISIBLE
                numberView.visibility = View.VISIBLE
                passTagView.visibility = View.VISIBLE
                lottieView.visibility = View.INVISIBLE
                numberView.text = ""
                if (item.iconRes != null) {
                    numberView.background = root.context.getDrawable(item.iconRes)
                } else {
                    numberView.background = root.context.getDrawable(R.drawable.bg_study_level_unlocked)
                }
            }
        }

        val clickListener = View.OnClickListener { onLevelClick?.invoke(item) }
        lottieView.setOnClickListener(clickListener)
        iconView.setOnClickListener(clickListener)
        numberView.setOnClickListener(clickListener)
    }

    class LeftVH(itemView: View) : RecyclerView.ViewHolder(itemView)
    class RightVH(itemView: View) : RecyclerView.ViewHolder(itemView)

    companion object {
        private const val VIEW_TYPE_LEFT = 1
        private const val VIEW_TYPE_RIGHT = 2

        private fun setViewSizeDp(view: View, widthDp: Float, heightDp: Float) {
            val dm = view.context.resources.displayMetrics
            view.layoutParams = view.layoutParams.apply {
                width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, widthDp, dm).toInt()
                height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, heightDp, dm).toInt()
            }
        }
    }
}
