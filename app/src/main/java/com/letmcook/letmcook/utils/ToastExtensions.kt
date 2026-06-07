package com.letmcook.letmcook.utils

import android.app.Activity
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
    ERROR,
    INFO
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
        rootLayout.removeView(existingToast)
    }

    val toastView = inflater.inflate(R.layout.custom_toast, rootLayout, false)
    val tvMessage = toastView.findViewById<TextView>(R.id.tvToastMessage)
    val ivIcon = toastView.findViewById<ImageView>(R.id.ivToastIcon)

    tvMessage.text = message

    when (type) {
        ToastType.SUCCESS -> {
            ivIcon.setImageResource(R.drawable.ic_check)
            ivIcon.visibility = View.VISIBLE
        }
        ToastType.ERROR -> {
            ivIcon.setImageResource(R.drawable.ic_error)
            ivIcon.visibility = View.VISIBLE
        }
        ToastType.INFO -> {
            ivIcon.visibility = View.GONE
        }
    }

    val params = FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        bottomMargin = (96 * resources.displayMetrics.density).toInt() // Float above bottom navigation
    }
    toastView.layoutParams = params

    rootLayout.addView(toastView)

    // Smooth animation: fade in and slide up slightly
    toastView.alpha = 0f
    toastView.translationY = 50f
    
    toastView.animate()
        .alpha(1f)
        .translationY(0f)
        .setDuration(250)
        .withEndAction {
            // Stay on screen for 2.5 seconds
            toastView.postDelayed({
                toastView.animate()
                    .alpha(0f)
                    .translationY(-30f)
                    .setDuration(250)
                    .withEndAction {
                        rootLayout.removeView(toastView)
                    }
                    .start()
            }, 2500)
        }
        .start()
}
