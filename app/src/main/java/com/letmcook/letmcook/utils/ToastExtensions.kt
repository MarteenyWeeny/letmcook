package com.letmcook.letmcook.utils

import android.app.Activity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.letmcook.letmcook.R

enum class ToastType {
    SUCCESS,
    INFO,
    WARNING,
    ERROR
}

fun Fragment.showCustomToast(message: String, type: ToastType = ToastType.SUCCESS) {
    activity?.showCustomToast(message, type)
}

fun Activity.showCustomToast(message: String, type: ToastType = ToastType.SUCCESS) {
    val rootLayout = findViewById<FrameLayout>(android.R.id.content) ?: return
    val inflater = LayoutInflater.from(this)
    
    // Remove existing toast first to prevent overlay stack
    val existingToast = rootLayout.findViewById<View>(R.id.custom_toast_container)
    if (existingToast != null) {
        existingToast.animate().cancel()
        rootLayout.removeView(existingToast)
    }

    val toastView = inflater.inflate(R.layout.custom_toast, rootLayout, false)
    val tvTitle = toastView.findViewById<TextView>(R.id.tvToastTitle)
    val tvMessage = toastView.findViewById<TextView>(R.id.tvToastMessage)
    val ivIcon = toastView.findViewById<ImageView>(R.id.ivToastIcon)
    val flIconBg = toastView.findViewById<View>(R.id.flToastIconBg)
    val btnClose = toastView.findViewById<View>(R.id.btnToastClose)

    tvMessage.text = message

    // Config style values based on ToastType
    val (titleText, bgColor, strokeColor, iconCircleColor, iconRes) = when (type) {
        ToastType.SUCCESS -> Triple(
            "Congratulations!",
            Color.parseColor("#EBF9F1"),
            Color.parseColor("#85E3B2")
        ) to Pair(
            Color.parseColor("#2E7D32"),
            R.drawable.ic_check
        )
        ToastType.INFO -> Triple(
            "Did you know?",
            Color.parseColor("#EBF3FC"),
            Color.parseColor("#8FBEF7")
        ) to Pair(
            Color.parseColor("#1976D2"),
            R.drawable.ic_info
        )
        ToastType.WARNING -> Triple(
            "Warning!",
            Color.parseColor("#FFF9EB"),
            Color.parseColor("#FFE194")
        ) to Pair(
            Color.parseColor("#FFA000"),
            R.drawable.ic_error
        )
        ToastType.ERROR -> Triple(
            "Something went wrong!",
            Color.parseColor("#FCECEF"),
            Color.parseColor("#F7A8B8")
        ) to Pair(
            Color.parseColor("#D32F2F"),
            R.drawable.ic_cross
        )
    }.let { (t, p) -> Quintuple(t.first, t.second, t.third, p.first, p.second) }

    tvTitle.text = titleText

    // Apply transparent colored background and border stroke
    val bgDrawable = toastView.background.mutate() as? GradientDrawable
    bgDrawable?.let {
        it.setColor(bgColor)
        it.setStroke((1.5 * resources.displayMetrics.density).toInt(), strokeColor)
    }

    // Apply color to circular icon background
    val circleDrawable = flIconBg.background.mutate() as? GradientDrawable
    circleDrawable?.setColor(iconCircleColor)

    // Set the status icon
    ivIcon.setImageResource(iconRes)

    // Set layout params for top banner placement
    val params = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        topMargin = (64 * resources.displayMetrics.density).toInt()
    }
    toastView.layoutParams = params

    // Setup close button click action
    btnClose.setOnClickListener {
        toastView.animate().cancel()
        toastView.animate()
            .alpha(0f)
            .translationY(-120f)
            .setDuration(250)
            .withEndAction {
                rootLayout.removeView(toastView)
            }
            .start()
    }

    rootLayout.addView(toastView)

    // Smooth animation: slide down from top & fade in
    toastView.alpha = 0f
    toastView.translationY = -120f
    
    toastView.animate()
        .alpha(1f)
        .translationY(0f)
        .setDuration(300)
        .withEndAction {
            // Stay for 3 seconds, then slide up & fade out
            toastView.postDelayed({
                toastView.animate()
                    .alpha(0f)
                    .translationY(-120f)
                    .setDuration(250)
                    .withEndAction {
                        rootLayout.removeView(toastView)
                    }
                    .start()
            }, 3000)
        }
        .start()
}

// Simple Quintuple helper data class
private data class Quintuple<out A, out B, out C, out D, out E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
