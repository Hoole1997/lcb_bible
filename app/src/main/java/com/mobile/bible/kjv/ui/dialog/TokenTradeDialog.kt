package com.mobile.bible.kjv.ui.dialog

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.mobile.bible.kjv.R
import java.util.Locale

class TokenTradeDialog : DialogFragment() {

    enum class PropType(val imageRes: Int, val labelRes: Int) {
        DELAY(R.mipmap.img_gift_delay, R.string.prop_delay),
        ERASER(R.mipmap.img_eraser, R.string.prop_eraser)
    }

    private var listener: OnTokenTradeListener? = null
    private var onDismissListener: (() -> Unit)? = null
    private var onDismissWithQuantityListener: ((Int) -> Unit)? = null
    private var propType: PropType = PropType.ERASER
    private var quantity = 1
    private var coinCost = 25
    private var maxQuantity = 10
    private var minQuantity = 1

    private var quantityTextView: TextView? = null
    private var exchangeInfoTextView: TextView? = null
    private var imageView: ImageView? = null
    private var labelTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isCancelable = true
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.dialog_token_trade, container, false)

        val cancelButton = view.findViewById<ImageView>(R.id.button_cancel)
        val subtractButton = view.findViewById<ImageView>(R.id.button_subtract)
        val addButton = view.findViewById<ImageView>(R.id.button_add)
        val confirmButton = view.findViewById<TextView>(R.id.button_confirm)
        quantityTextView = view.findViewById(R.id.text_quantity)
        exchangeInfoTextView = view.findViewById(R.id.text_exchange_info)
        imageView = view.findViewById(R.id.image_delay)
        labelTextView = view.findViewById(R.id.text_delay_label)

        imageView?.setImageResource(propType.imageRes)
        labelTextView?.setText(propType.labelRes)

        cancelButton.setOnClickListener { dismiss() }

        subtractButton.setOnClickListener {
            if (quantity > minQuantity) {
                quantity--
                updateQuantityDisplay()
            }
        }

        addButton.setOnClickListener {
            if (quantity < maxQuantity) {
                quantity++
                updateQuantityDisplay()
            }
        }

        confirmButton.setOnClickListener {
            listener?.onConfirmExchange(quantity, quantity * coinCost)
            dismiss()
        }

        updateQuantityDisplay()

        return view
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
        quantityTextView = null
        exchangeInfoTextView = null
        imageView = null
        labelTextView = null
        onDismissWithQuantityListener?.invoke(quantity)
        onDismissListener?.invoke()
        listener = null
        onDismissListener = null
        onDismissWithQuantityListener = null
    }

    private fun updateQuantityDisplay() {
        quantityTextView?.text = String.format(Locale.getDefault(), "%02d", quantity)
        exchangeInfoTextView?.text = String.format(Locale.getDefault(), "Exchange for %d coins", quantity * coinCost)
    }

    fun setOnTokenTradeListener(listener: OnTokenTradeListener?) {
        this.listener = listener
    }

    fun setCoinCost(cost: Int) {
        this.coinCost = cost
    }

    fun setMaxQuantity(max: Int) {
        this.maxQuantity = max
    }

    fun setOnDismissListener(listener: () -> Unit) {
        this.onDismissListener = listener
    }

    fun setOnDismissWithQuantityListener(listener: (Int) -> Unit) {
        this.onDismissWithQuantityListener = listener
    }

    interface OnTokenTradeListener {
        fun onConfirmExchange(quantity: Int, totalCost: Int)
    }

    companion object {
        fun show(activity: FragmentActivity) {
            TokenTradeDialog().show(activity.supportFragmentManager, "TokenTradeDialog")
        }

        fun show(activity: FragmentActivity, listener: OnTokenTradeListener) {
            val dialog = TokenTradeDialog()
            dialog.listener = listener
            dialog.show(activity.supportFragmentManager, "TokenTradeDialog")
        }

        fun show(activity: FragmentActivity, coinCost: Int, maxQuantity: Int, listener: OnTokenTradeListener) {
            val dialog = TokenTradeDialog()
            dialog.coinCost = coinCost
            dialog.maxQuantity = maxQuantity
            dialog.listener = listener
            dialog.show(activity.supportFragmentManager, "TokenTradeDialog")
        }

        fun show(activity: FragmentActivity, coinCost: Int, maxQuantity: Int, listener: OnTokenTradeListener, onDismiss: () -> Unit) {
            val dialog = TokenTradeDialog()
            dialog.coinCost = coinCost
            dialog.maxQuantity = maxQuantity
            dialog.listener = listener
            dialog.onDismissListener = onDismiss
            dialog.show(activity.supportFragmentManager, "TokenTradeDialog")
        }

        fun show(activity: FragmentActivity, propType: PropType, coinCost: Int, maxQuantity: Int, listener: OnTokenTradeListener, onDismiss: () -> Unit) {
            val dialog = TokenTradeDialog()
            dialog.propType = propType
            dialog.coinCost = coinCost
            dialog.maxQuantity = maxQuantity
            dialog.listener = listener
            dialog.onDismissListener = onDismiss
            dialog.show(activity.supportFragmentManager, "TokenTradeDialog")
        }

        fun show(
            activity: FragmentActivity,
            propType: PropType,
            coinCost: Int,
            maxQuantity: Int,
            listener: OnTokenTradeListener,
            onDismissWithQuantity: (Int) -> Unit
        ) {
            val dialog = TokenTradeDialog()
            dialog.propType = propType
            dialog.coinCost = coinCost
            dialog.maxQuantity = maxQuantity
            dialog.listener = listener
            dialog.onDismissWithQuantityListener = onDismissWithQuantity
            dialog.show(activity.supportFragmentManager, "TokenTradeDialog")
        }
    }
}
