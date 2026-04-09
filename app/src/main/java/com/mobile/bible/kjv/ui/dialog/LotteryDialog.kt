package com.mobile.bible.kjv.ui.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.mobile.bible.kjv.R

enum class LotteryType {
    COIN,
    ERASER,
    DELAY,
    COIN_MORE,
    ERASER_DELAY,
    COIN_DOUBLE,
    ERASER_DOUBLE,
    DELAY_DOUBLE,
    ERASER_DELAY_DOUBLE,
    COIN_MORE_DOUBLE
}

class LotteryDialog : DialogFragment() {

    private var listener: OnLotteryListener? = null
    private var lotteryType: LotteryType = LotteryType.COIN
    private var amount: Int = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.dialog_gift_unlock, container, false)

        val cancelButton = view.findViewById<ImageView>(R.id.button_cancel)
        val confirmButton = view.findViewById<TextView>(R.id.button_confirm)
        val claim2xButton = view.findViewById<View>(R.id.button_claim_2x)
        val messageTextView = view.findViewById<TextView>(R.id.text_message)
        val messageCoinDoubleTextView = view.findViewById<TextView>(R.id.text_message_coin_double)
        val iconImageView = view.findViewById<ImageView>(R.id.image_coins)
        val eraserDelayLayout = view.findViewById<View>(R.id.layout_eraser_delay)
        val coinDoubleLayout = view.findViewById<View>(R.id.layout_coin_double)
        val textCoinSmallAmount = view.findViewById<TextView>(R.id.text_coin_small_amount)
        val textCoinLargeAmount = view.findViewById<TextView>(R.id.text_coin_large_amount)
        val imageCoinSmall = view.findViewById<ImageView>(R.id.image_coin_small)
        val imageCoinLarge = view.findViewById<ImageView>(R.id.image_coin_large)
        val eraserDelayDoubleLayout = view.findViewById<View>(R.id.layout_eraser_delay_double)
        val textEraserDelaySmallAmount = view.findViewById<TextView>(R.id.text_eraser_delay_small_amount)
        val textEraserDelayLargeAmount = view.findViewById<TextView>(R.id.text_eraser_delay_large_amount)

        updateIcon(iconImageView, eraserDelayLayout, coinDoubleLayout, textCoinSmallAmount, textCoinLargeAmount, imageCoinSmall, imageCoinLarge, eraserDelayDoubleLayout, textEraserDelaySmallAmount, textEraserDelayLargeAmount)

        if (lotteryType == LotteryType.COIN_DOUBLE || lotteryType == LotteryType.ERASER_DOUBLE || lotteryType == LotteryType.DELAY_DOUBLE || lotteryType == LotteryType.ERASER_DELAY_DOUBLE || lotteryType == LotteryType.COIN_MORE_DOUBLE) {
            claim2xButton.visibility = View.GONE
            messageTextView.visibility = View.GONE
            messageCoinDoubleTextView.visibility = View.VISIBLE
            messageCoinDoubleTextView.text = getString(R.string.lottery_coin_double_message_prefix) + getString(R.string.lottery_coin_double_message_suffix)
        } else {
            messageCoinDoubleTextView.visibility = View.GONE
            updateMessage(messageTextView)
        }

        cancelButton.setOnClickListener {
            listener?.onDismiss()
            dismiss()
        }

        confirmButton.setOnClickListener {
            listener?.onConfirm(amount)
            dismiss()
        }

        claim2xButton.setOnClickListener {
            listener?.onClaim2x(amount * 2)
            dismiss()
        }

        return view
    }

    private fun updateIcon(
        imageView: ImageView,
        eraserDelayLayout: View,
        coinDoubleLayout: View,
        textCoinSmallAmount: TextView,
        textCoinLargeAmount: TextView,
        imageCoinSmall: ImageView,
        imageCoinLarge: ImageView,
        eraserDelayDoubleLayout: View,
        textEraserDelaySmallAmount: TextView,
        textEraserDelayLargeAmount: TextView
    ) {
        imageView.visibility = View.GONE
        eraserDelayLayout.visibility = View.GONE
        coinDoubleLayout.visibility = View.GONE
        eraserDelayDoubleLayout.visibility = View.GONE

        when (lotteryType) {
            LotteryType.ERASER_DELAY -> {
                eraserDelayLayout.visibility = View.VISIBLE
            }
            LotteryType.COIN_DOUBLE -> {
                coinDoubleLayout.visibility = View.VISIBLE
                imageCoinSmall.setImageResource(R.mipmap.img_lottery_coins)
                imageCoinLarge.setImageResource(R.mipmap.img_lottery_coins)
                textCoinSmallAmount.text = (amount / 2).toString()
                textCoinLargeAmount.text = amount.toString()
            }
            LotteryType.COIN_MORE_DOUBLE -> {
                coinDoubleLayout.visibility = View.VISIBLE
                imageCoinSmall.setImageResource(R.mipmap.img_lottery_coins)
                imageCoinLarge.setImageResource(R.mipmap.img_lottery_coins)
                textCoinSmallAmount.text = (amount / 2).toString()
                textCoinLargeAmount.text = amount.toString()
            }
            LotteryType.ERASER_DOUBLE -> {
                coinDoubleLayout.visibility = View.VISIBLE
                imageCoinSmall.setImageResource(R.mipmap.img_lottery_eraser)
                imageCoinLarge.setImageResource(R.mipmap.img_lottery_eraser)
                textCoinSmallAmount.text = "x${amount / 2}"
                textCoinLargeAmount.text = "x$amount"
            }
            LotteryType.DELAY_DOUBLE -> {
                coinDoubleLayout.visibility = View.VISIBLE
                imageCoinSmall.setImageResource(R.mipmap.img_lottery_delay)
                imageCoinLarge.setImageResource(R.mipmap.img_lottery_delay)
                textCoinSmallAmount.text = "x${amount / 2}"
                textCoinLargeAmount.text = "x$amount"
            }
            LotteryType.ERASER_DELAY_DOUBLE -> {
                eraserDelayDoubleLayout.visibility = View.VISIBLE
                textEraserDelaySmallAmount.text = "x${amount / 2}"
                textEraserDelayLargeAmount.text = "x$amount"
            }
            else -> {
                imageView.visibility = View.VISIBLE
                val iconRes = when (lotteryType) {
                    LotteryType.COIN -> R.mipmap.img_lottery_coins
                    LotteryType.ERASER -> R.mipmap.img_lottery_eraser
                    LotteryType.DELAY -> R.mipmap.img_lottery_delay
                    LotteryType.COIN_MORE -> R.mipmap.img_lottery_coins
                    else -> R.mipmap.img_lottery_coins
                }
                imageView.setImageResource(iconRes)
            }
        }
    }

    private fun updateMessage(textView: TextView) {
        val (prefix, amountText, suffix) = when (lotteryType) {
            LotteryType.COIN -> Triple(
                getString(R.string.lottery_message_prefix),
                " $amount ",
                getString(R.string.lottery_message_suffix)
            )
            LotteryType.ERASER -> Triple(
                getString(R.string.lottery_eraser_message_prefix),
                " X$amount",
                getString(R.string.lottery_eraser_message_suffix)
            )
            LotteryType.DELAY -> Triple(
                getString(R.string.lottery_delay_message_prefix),
                " X$amount",
                getString(R.string.lottery_delay_message_suffix)
            )
            LotteryType.COIN_MORE -> Triple(
                getString(R.string.lottery_coin_more_message_prefix),
                " $amount ",
                getString(R.string.lottery_coin_more_message_suffix)
            )
            LotteryType.COIN_DOUBLE, LotteryType.ERASER_DOUBLE, LotteryType.DELAY_DOUBLE, LotteryType.ERASER_DELAY_DOUBLE, LotteryType.COIN_MORE_DOUBLE -> Triple(
                getString(R.string.lottery_coin_double_message_prefix),
                "",
                getString(R.string.lottery_coin_double_message_suffix)
            )
            LotteryType.ERASER_DELAY -> {
                val prefix = getString(R.string.lottery_eraser_delay_message_prefix)
                val amount1 = " X$amount "
                val middle = getString(R.string.lottery_eraser_delay_message_middle)
                val amount2 = " X$amount "
                val suffix = getString(R.string.lottery_eraser_delay_message_suffix)
                val fullText = "$prefix$amount1$middle$amount2$suffix"

                val spannable = SpannableString(fullText)
                val redColor = ForegroundColorSpan(Color.parseColor("#F4241E"))
                val redColor2 = ForegroundColorSpan(Color.parseColor("#F4241E"))
                spannable.setSpan(redColor, prefix.length, prefix.length + amount1.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                val secondStart = prefix.length + amount1.length + middle.length
                spannable.setSpan(redColor2, secondStart, secondStart + amount2.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                textView.text = spannable
                return
            }
        }
        val fullText = "$prefix$amountText$suffix"

        val spannable = SpannableString(fullText)
        val startIndex = prefix.length
        val endIndex = startIndex + amountText.length
        spannable.setSpan(
            ForegroundColorSpan(Color.parseColor("#F4241E")),
            startIndex,
            endIndex,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )
        textView.text = spannable
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.let { window ->
            window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            val metrics = resources.displayMetrics
            val marginPx = (30 * metrics.density).toInt()
            val width = metrics.widthPixels - marginPx * 2
            window.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        listener = null
    }

    fun setAmount(amount: Int) {
        this.amount = amount
    }

    fun setLotteryType(type: LotteryType) {
        this.lotteryType = type
    }

    fun setOnLotteryListener(listener: OnLotteryListener?) {
        this.listener = listener
    }

    interface OnLotteryListener {
        fun onConfirm(coinAmount: Int)
        fun onClaim2x(coinAmount: Int)
        fun onDismiss()
    }

    companion object {
        fun show(activity: FragmentActivity, type: LotteryType = LotteryType.COIN, amount: Int = 100, listener: OnLotteryListener? = null) {
            val dialog = LotteryDialog()
            dialog.lotteryType = type
            dialog.amount = amount
            dialog.listener = listener
            val fm = activity.supportFragmentManager
            val existing = fm.findFragmentByTag("LotteryDialog")
            if (existing is DialogFragment) {
                existing.dismissAllowingStateLoss()
            }
            fm.beginTransaction()
                .add(dialog, "LotteryDialog")
                .commitAllowingStateLoss()
        }

        fun showCoin(activity: FragmentActivity, coinAmount: Int, listener: OnLotteryListener? = null) {
            show(activity, LotteryType.COIN, coinAmount, listener)
        }

        fun showEraser(activity: FragmentActivity, eraserCount: Int, listener: OnLotteryListener? = null) {
            show(activity, LotteryType.ERASER, eraserCount, listener)
        }

        fun showDelay(activity: FragmentActivity, delayCount: Int, listener: OnLotteryListener? = null) {
            show(activity, LotteryType.DELAY, delayCount, listener)
        }

        fun showCoinMore(activity: FragmentActivity, coinAmount: Int, listener: OnLotteryListener? = null) {
            show(activity, LotteryType.COIN_MORE, coinAmount, listener)
        }

        fun showEraserDelay(activity: FragmentActivity, count: Int, listener: OnLotteryListener? = null) {
            show(activity, LotteryType.ERASER_DELAY, count, listener)
        }

        fun showCoinDouble(activity: FragmentActivity, doubledAmount: Int, listener: OnLotteryListener? = null) {
            show(activity, LotteryType.COIN_DOUBLE, doubledAmount, listener)
        }

        fun showEraserDouble(activity: FragmentActivity, doubledAmount: Int, listener: OnLotteryListener? = null) {
            show(activity, LotteryType.ERASER_DOUBLE, doubledAmount, listener)
        }

        fun showDelayDouble(activity: FragmentActivity, doubledAmount: Int, listener: OnLotteryListener? = null) {
            show(activity, LotteryType.DELAY_DOUBLE, doubledAmount, listener)
        }

        fun showEraserDelayDouble(activity: FragmentActivity, doubledAmount: Int, listener: OnLotteryListener? = null) {
            show(activity, LotteryType.ERASER_DELAY_DOUBLE, doubledAmount, listener)
        }

        fun showCoinMoreDouble(activity: FragmentActivity, doubledAmount: Int, listener: OnLotteryListener? = null) {
            show(activity, LotteryType.COIN_MORE_DOUBLE, doubledAmount, listener)
        }
    }
}
