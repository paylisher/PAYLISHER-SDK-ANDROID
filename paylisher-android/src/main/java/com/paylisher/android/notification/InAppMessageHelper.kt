package com.paylisher.android.notification

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import com.paylisher.android.R
import java.io.IOException
import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.setMargins
import androidx.viewpager2.widget.ViewPager2
import com.paylisher.Paylisher
import com.paylisher.android.notification.helpers.InAppCarouselFullscreenAdapter
import com.paylisher.android.notification.helpers.InAppCarouselModalAdapter
import com.paylisher.android.notification.helpers.InAppLocalize.Companion.localize
import com.paylisher.android.notification.iam.InAppMessagingBanner
import com.paylisher.android.notification.iam.InAppMessagingFullscreen
import com.paylisher.android.notification.iam.InAppMessagingModal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import java.util.Locale

class InAppMessageHelper {

    companion object {
        private const val TAG = "FCM | InApp"
        var dialog: AlertDialog? = null

        fun dismissEvent(via: String = "closeButton") {
            Paylisher.capture(
                "inappMessageClose",
                properties = mapOf("via" to via)
            )
        }
    }

    fun showCustomInAppMessageBanner(activity: Activity, data: NotificationInAppData) {
        activity.runOnUiThread {
            val inAppView = InAppMessagingBanner(activity)

            // Configure the custom InAppMessagingBanner layout
            data.layouts?.let {
                data.defaultLang?.let { lang ->
                    inAppView.configureLayout(it.first(), lang, activity)
                }
            }

            // gravity START ******************************
            val rootLayout = FrameLayout(activity)

            // Create and set up the overlay view
            val overlayView = View(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            rootLayout.addView(overlayView)

            // ✅ TODO: verticalPosition -> top | center | bottom
            val inAppParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = when (data.layouts?.first()?.style?.verticalPosition) {
                    "top" -> Gravity.TOP
                    "center" -> Gravity.CENTER
                    "bottom" -> Gravity.BOTTOM
                    else -> Gravity.TOP // Default to top if no value is provided
                }
            }

            rootLayout.addView(inAppView, inAppParams)
            // gravity END ********************************

            // Create and show the dialog
            val dialogBuilder = AlertDialog.Builder(activity)
                .setView(rootLayout)
                .setCancelable(true)

            val alertDialog = dialogBuilder.create()
            // ✅ TODO: issue -> there is white color behind.. if root corner rounded
            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            alertDialog.show()

            // Set cancelable based on layout configuration
            data.layouts?.let {
                val layout = it.first()

                // TODO: After vertical positioning -> NOT WORKING
                dialogBuilder.setCancelable(layout.close.active.not())

                if (layout.extra.banner != null) {
                    val banner = layout.extra.banner

//                    println("FCM, extra dur:${banner.duration}")
//                    println("FCM, extra action:${banner.action}")

                    // ✅TODO: action -> url, deeplink
                    if (banner.action.isNotEmpty()) {
                        inAppView.setOnClickListener {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(banner.action))
                            activity.startActivity(browserIntent)
                            alertDialog?.dismiss()
                        }
                    }

                    // ✅ TODO: duration: number; // second -> display duration
                    if (banner.duration > 0) {
                        // Convert seconds to milliseconds
                        val durationMillis = banner.duration * 1000L
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (alertDialog.isShowing) {
                                alertDialog.dismiss()
                                Log.d(TAG, "Dialog dismissed after ${banner.duration} seconds")
                            }
                        }, durationMillis)
                    }
                }

            }

            overlayView.setOnClickListener {
                if (alertDialog?.isShowing == true) {
                    alertDialog.dismiss()
                    Log.d("FCM Banner", "Dialog dismissed via Overlay.")
                }
            }

            // Update the global reference
            dialog = alertDialog

//            synchronized(this) {
//                dialog = dialogBuilder.create()
//                dialog?.show()
//            }

            Paylisher.capture(
                "inappMessageRead",
                properties = mapOf("type" to "Banner")
            )

            Log.d(
                TAG,
                "In-App Banner sent! ${Locale.getDefault().language}-${Locale.getDefault().country}"
            )
        }
    }

    fun showCustomInAppMessageModal(activity: Activity, data: NotificationInAppData) {
        activity.runOnUiThread {
            val inAppView = InAppMessagingModal(activity)

            // Configure the custom InAppMessagingBanner layout
            data.layouts?.let {
                data.defaultLang?.let { lang ->
                    inAppView.configureLayout(it.first(), lang, activity)
                }
            }

            // gravity START ******************************
            val rootLayout = FrameLayout(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
//                    clipToOutline = false
//                    clipChildren = false
//                    clipToPadding = false
//                    setPadding(0,0,0,0)
//                    setPadding(-50)
                }
            }

            // ✅ TODO: overlay -> color: hex
            // Create and set up the overlay view
            val overlayView = View(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
//                    gravity = Gravity.CENTER // Ensure it covers the full screen
//                    clipToOutline = false
                }

                val colorHex =
                    data.layouts?.first()?.extra?.overlay?.color ?: "#808080" // Default to gray
                try {
                    setBackgroundColor(Color.parseColor(colorHex))
                } catch (e: IllegalArgumentException) {
                    println("FCM, invalid overlay color: $colorHex. Falling back to default gray.")
                    setBackgroundColor(Color.GRAY) // Fallback color
                }
                alpha = 0.15f // Set transparency directly
            }
            rootLayout.addView(overlayView)

            // ✅ TODO: verticalPosition -> top | center | bottom
            val inAppParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
//                gravity = Gravity.TOP
//                gravity = Gravity.CENTER
//                gravity = Gravity.BOTTOM
                gravity = when (data.layouts?.first()?.style?.verticalPosition) {
                    "top" -> Gravity.TOP
                    "center" -> Gravity.CENTER
                    "bottom" -> Gravity.BOTTOM
                    else -> Gravity.CENTER // Default to top if no value is provided
                }
                setMargins(36) // 2 * raw px
            }

            rootLayout.addView(inAppView, inAppParams)
            // gravity END ********************************

            // Create the dialog
            val dialogBuilder = AlertDialog.Builder(activity)
                .setView(rootLayout)
                .setCancelable(true) // Set the dialog to be cancellable

            val alertDialog = dialogBuilder.create()

            // ✅ TODO: issue -> there is white color behind.. if root corner rounded
//            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.RED))
            alertDialog.window?.let {
                // Set the background of the window to transparent (if you want transparency behind)
                it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                // Set the background color of the entire dialog window
                it.decorView.setBackgroundColor(Color.TRANSPARENT) // This sets the entire window's background to blue

                // Remove padding/margin from the window to make sure it covers the entire screen
                it.attributes = it.attributes.apply {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = ViewGroup.LayoutParams.MATCH_PARENT

//                    gravity = Gravity.CENTER // Set gravity to TOP to prevent unexpected centering behavior
//                    horizontalMargin=-10f
//                    verticalMargin=-10f
                }
            }

            // Set cancelable based on layout configuration
            data.layouts?.let {
                val layout = it.first()

                // ✅ TODO: handle transition type
                layout.extra.transition?.let { transition ->
//                    println("FCM, extra transition:${transition}")

                    // Map transition enum value to the corresponding animation style
                    val transitionStyle = when (transition) {
                        InAppTransitionType.LeftToRight.value -> R.style.LeftToRight
                        InAppTransitionType.RightToLeft.value -> R.style.RightToLeft
                        InAppTransitionType.TopToBottom.value -> R.style.TopToBottom
                        InAppTransitionType.BottomToTop.value -> R.style.BottomToTop
                        else -> null  // Default: no-transition or handle fallback
                    }

                    // Apply transition style if valid
                    transitionStyle?.let {

                        alertDialog.window?.apply {
                            setWindowAnimations(it) // Apply the selected transition style
                        }
                    } ?: run {
                        // Optionally handle no valid transition
                        alertDialog.window?.apply {
                            setWindowAnimations(0) // No animation or default animation
                        }
                    }
                }

                if (layout.extra.overlay != null) {
                    val overlay = layout.extra.overlay

                    // ✅ TODO: overlay -> action: close, no action
                    // TODO: After vertical positioning -> NOT WORKING
                    // Set the dialog's cancelable based on overlay action
                    when (overlay.action) {
                        "close" -> {
                            // Close the dialog when overlay is clicked
                            alertDialog.setCancelable(true)
                            alertDialog.setCanceledOnTouchOutside(true) // Allows dismiss on touch outside
                        }

                        "no-action" -> {
                            // Prevent dialog dismissal on overlay click
                            alertDialog.setCancelable(false)
                            alertDialog.setCanceledOnTouchOutside(false)
                        }

                        else -> {
                            alertDialog.setCancelable(true)
                            alertDialog.setCanceledOnTouchOutside(true)
                            println("FCM, unknown overlay action: ${overlay.action}")
                        }
                    }

                    if (overlay.action != "no-action") {
                        overlayView.setOnClickListener {
                            alertDialog.dismiss()
                        }
                        // ✅ TODO: custom overlay -> action: close, no action
                        // causes -> Input dispatching timed out (863bc1a com.paylisher.android.sample/com.paylisher.android.sample.MainActivity (server) is not responding. Waited 5011ms for MotionEvent).
//                        overlayView.setOnTouchListener { view, event ->
//                            if (event.action == MotionEvent.ACTION_DOWN) {
//                                val dialogBounds = Rect()
//                                inAppView.getGlobalVisibleRect(dialogBounds) // Get global bounds of the dialog content
//
////                                Log.d("DialogBounds", "Bounds: $dialogBounds")
////                                Log.d("TouchEvent", "Touch: (${event.rawX}, ${event.rawY})")
//
//                                // Check if the touch is outside the dialog bounds
//                                if (!dialogBounds.contains(
//                                        event.rawX.toInt(),
//                                        event.rawY.toInt()
//                                    )
//                                ) {
////                                    view.post { alertDialog.dismiss() } // Thread-safe dismissal
//                                    alertDialog.dismiss() // Safe dismissal
//
//                                    view.performClick() // Accessibility compliance
//                                    return@setOnTouchListener true
//                                }
//                            }
//                            false // Pass through other events
//                        }
                    }
                }
            }

            alertDialog.show()

            // Update the global reference
            dialog = alertDialog

            Paylisher.capture(
                "inappMessageRead",
                properties = mapOf("type" to "Modal")
            )

            Log.d(
                TAG,
                "In-App Modal sent! ${Locale.getDefault().language}-${Locale.getDefault().country}"
            )
        }
    }

    // Based on showCustomInAppMessageModal
    fun showCustomInAppMessageModalCarousel(activity: Activity, data: NotificationInAppData) {
        activity.runOnUiThread {
            // Root layout
            val rootLayout = FrameLayout(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            // Overlay view
            val overlayView = View(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                val colorHex = data.layouts?.first()?.extra?.overlay?.color ?: "#808080"
                try {
                    setBackgroundColor(Color.parseColor(colorHex))
                } catch (e: IllegalArgumentException) {
                    println("FCM, invalid overlay color: $colorHex. Falling back to default gray.")
                    setBackgroundColor(Color.GRAY)
                }
                alpha = 0.15f
            }
            rootLayout.addView(overlayView)

            val carouselHolder = FrameLayout(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
            // ************************************************
            // ViewPager2 setup
            val viewPager = ViewPager2(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                    gravity = when (data.layouts?.first()?.style?.verticalPosition) {
                        "top" -> Gravity.TOP
                        "center" -> Gravity.CENTER
                        "bottom" -> Gravity.BOTTOM
                        else -> Gravity.CENTER
                    }
                    setMargins(36) // Optional margin
                }
            }

            // Set up the adapter
            val layouts = data.layouts ?: emptyList()
            viewPager.adapter = InAppCarouselModalAdapter(activity, layouts, data.defaultLang)

            carouselHolder.addView(viewPager)
            // ************************************************

            val indicatorLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
//                    bottomMargin = 16 // Adjust the margin as needed
                    setMargins(10)

//                    background = GradientDrawable().apply {
//                        setColor(Color.WHITE)
//                    }
                }
            }

            // Add the indicator layout to the root layout
            carouselHolder.addView(indicatorLayout)
            rootLayout.addView(carouselHolder)

            // ✅todo:  layouts[0].style.navigationalArrows
            // Check if navigational arrows are enabled
            val showArrows = layouts.firstOrNull()?.style?.navigationalArrows == true

            if (showArrows) {
                // Left arrow
                val leftArrow = ImageView(activity).apply {
                    setImageResource(R.drawable.ic_arrow_left) // Replace with your drawable resource
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER_VERTICAL or Gravity.START
                        setMargins(16) // Optional margin
                    }
                    visibility = View.GONE // Initially hidden if on the first page
                }

                // Right arrow
                val rightArrow = ImageView(activity).apply {
                    setImageResource(R.drawable.ic_arrow_right) // Replace with your drawable resource
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER_VERTICAL or Gravity.END
                        setMargins(16) // Optional margin
                    }
                }

                // Add arrows to the carousel holder
                carouselHolder.addView(leftArrow)
                carouselHolder.addView(rightArrow)

                // Handle arrow clicks
                leftArrow.setOnClickListener {
                    val currentItem = viewPager.currentItem
                    if (currentItem > 0) {
                        viewPager.setCurrentItem(currentItem - 1, true)
                    }
                }

                rightArrow.setOnClickListener {
                    val currentItem = viewPager.currentItem
                    if (currentItem < layouts.size - 1) {
                        viewPager.setCurrentItem(currentItem + 1, true)
                    }
                }

                // Update arrow visibility based on page changes
                viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        leftArrow.visibility = if (position > 0) View.VISIBLE else View.GONE
                        rightArrow.visibility =
                            if (position < layouts.size - 1) View.VISIBLE else View.GONE
                        updateIndicators(
                            position,
                            layouts.size,
                            indicatorLayout,
                            activity
                        ) // Keep indicator updates
                    }
                })
            } else {
                // Update arrow visibility based on page changes
                viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        updateIndicators(
                            position,
                            layouts.size,
                            indicatorLayout,
                            activity
                        ) // Keep indicator updates
                    }
                })
            }

            // Dialog setup
            val dialogBuilder = AlertDialog.Builder(activity)
                .setView(rootLayout)
                .setCancelable(true)

            val alertDialog = dialogBuilder.create()

//            alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            // ✅ TODO: issue -> there is white color behind.. if root corner rounded
            alertDialog.window?.let {
                // Set the background of the window to transparent (if you want transparency behind)
                it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                // Set the background color of the entire dialog window
                it.decorView.setBackgroundColor(Color.TRANSPARENT) // This sets the entire window's background to blue

                // Remove padding/margin from the window to make sure it covers the entire screen
                it.attributes = it.attributes.apply {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                }
            }

            // Set cancelable based on layout configuration
            data.layouts?.let {
                val layout = it.first()

                // ✅ TODO: handle transition type
                layout.extra.transition?.let { transition ->
//                    println("FCM, extra transition:${transition}")

                    // Map transition enum value to the corresponding animation style
                    val transitionStyle = when (transition) {
                        InAppTransitionType.LeftToRight.value -> R.style.LeftToRight
                        InAppTransitionType.RightToLeft.value -> R.style.RightToLeft
                        InAppTransitionType.TopToBottom.value -> R.style.TopToBottom
                        InAppTransitionType.BottomToTop.value -> R.style.BottomToTop
                        else -> null  // Default: no-transition or handle fallback
                    }

                    // Apply transition style if valid
                    transitionStyle?.let {

                        alertDialog.window?.apply {
                            setWindowAnimations(it) // Apply the selected transition style
                        }
                    } ?: run {
                        // Optionally handle no valid transition
                        alertDialog.window?.apply {
                            setWindowAnimations(0) // No animation or default animation
                        }
                    }
                }

                if (layout.extra.overlay != null) {
                    val overlay = layout.extra.overlay

                    // ✅ TODO: overlay -> action: close, no action
                    // TODO: After vertical positioning -> NOT WORKING
                    // Set the dialog's cancelable based on overlay action
                    when (overlay.action) {
                        "close" -> {
                            // Close the dialog when overlay is clicked
                            alertDialog.setCancelable(true)
                            alertDialog.setCanceledOnTouchOutside(true) // Allows dismiss on touch outside
                        }

                        "no-action" -> {
                            // Prevent dialog dismissal on overlay click
                            alertDialog.setCancelable(false)
                            alertDialog.setCanceledOnTouchOutside(false)
                        }

                        else -> {
                            alertDialog.setCancelable(true)
                            alertDialog.setCanceledOnTouchOutside(true)
                            println("FCM, unknown overlay action: ${overlay.action}")
                        }
                    }

                    if (overlay.action != "no-action") {
                        overlayView.setOnClickListener {
                            alertDialog.dismiss()
                        }
                    }
                }
            }

            alertDialog.show()

            // Update the global reference
            dialog = alertDialog

            Paylisher.capture(
                "inappMessageRead",
                properties = mapOf("type" to "ModalCarousel")
            )

            Log.d(
                TAG,
                "In-App Modal Carousel sent! ${Locale.getDefault().language}-${Locale.getDefault().country}"
            )
        }
    }

    fun showCustomInAppMessageFullscreen(activity: Activity, data: NotificationInAppData) {
        activity.runOnUiThread {
            val inAppView = InAppMessagingFullscreen(activity)

            // Configure the custom InAppMessagingBanner layout
            data.layouts?.let {
                data.defaultLang?.let { lang ->
                    inAppView.configureLayout(it.first(), lang, activity)
                }
            }

            // gravity START ******************************
            val rootLayout = FrameLayout(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
                }
            }

            // ✅
            // TODO: verticalPosition -> NONE
            val inAppParams = FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            ).apply {
//                gravity = Gravity.TOP
//                gravity = Gravity.CENTER
//                gravity = Gravity.BOTTOM
//                gravity = when (data.layouts?.first()?.style?.verticalPosition) {
//                    "top" -> Gravity.TOP
//                    "center" -> Gravity.CENTER
//                    "bottom" -> Gravity.BOTTOM
//                    else -> Gravity.CENTER // Default to top if no value is provided
//                }
//                setMargins(0) // 2 * raw px
            }

            rootLayout.addView(inAppView, inAppParams)
            // gravity END ********************************

            // Create the dialog
            val dialogBuilder = AlertDialog.Builder(activity)
                .setView(rootLayout)
                .setCancelable(true) // Set the dialog to be cancellable

            val alertDialog = dialogBuilder.create()

            // ✅ TODO: issue -> there is white color behind.. if root corner rounded
            alertDialog.window?.let {
                // Set the background of the window to transparent (if you want transparency behind)
                it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                // Set the background color of the entire dialog window
                it.decorView.setBackgroundColor(Color.TRANSPARENT) // This sets the entire window's background to blue

                // Remove padding/margin from the window to make sure it covers the entire screen
                it.attributes = it.attributes.apply {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                }
            }

            // Set cancelable based on layout configuration
            data.layouts?.let {
                val layout = it.first()

                // ✅ TODO: handle transition type
                layout.extra.transition?.let { transition ->
//                    println("FCM, extra transition:${transition}")

                    // Map transition enum value to the corresponding animation style
                    val transitionStyle = when (transition) {
                        InAppTransitionType.LeftToRight.value -> R.style.LeftToRight
                        InAppTransitionType.RightToLeft.value -> R.style.RightToLeft
                        InAppTransitionType.TopToBottom.value -> R.style.TopToBottom
                        InAppTransitionType.BottomToTop.value -> R.style.BottomToTop
                        else -> null  // Default: no-transition or handle fallback
                    }

                    // Apply transition style if valid
                    transitionStyle?.let {
                        alertDialog.window?.apply {
                            setWindowAnimations(it) // Apply the selected transition style
                        }
                    } ?: run {
                        // Optionally handle no valid transition
                        alertDialog.window?.apply {
                            setWindowAnimations(0) // No animation or default animation
                        }
                    }
                }
            }

            rootLayout.setOnClickListener {
                alertDialog.dismiss()
            }

            alertDialog.show()

            // Update the global reference
            dialog = alertDialog

            Paylisher.capture(
                "inappMessageRead",
                properties = mapOf("type" to "Fullscreen")
            )

            Log.d(
                TAG,
                "In-App Fullscreen sent! ${Locale.getDefault().language}-${Locale.getDefault().country}"
            )
        }
    }

    // Based on showCustomInAppMessageFullscreen
    fun showCustomInAppMessageFullscreenCarousel(activity: Activity, data: NotificationInAppData) {
        activity.runOnUiThread {
            // Root layout
            val rootLayout = FrameLayout(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }

            val carouselHolder = FrameLayout(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
            // ************************************************
            // ViewPager2 setup
            val viewPager = ViewPager2(activity).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                ).apply {
//                    gravity = when (data.layouts?.first()?.style?.verticalPosition) {
//                        "top" -> Gravity.TOP
//                        "center" -> Gravity.CENTER
//                        "bottom" -> Gravity.BOTTOM
//                        else -> Gravity.CENTER
//                    }
//                    setMargins(36) // Optional margin
                }
            }

            // Set up the adapter
            val layouts = data.layouts ?: emptyList()
            viewPager.adapter = InAppCarouselFullscreenAdapter(activity, layouts, data.defaultLang)

            carouselHolder.addView(viewPager)
            // ************************************************

            val indicatorLayout = LinearLayout(activity).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
//                    bottomMargin = 16 // Adjust the margin as needed
                    setMargins(10)

//                    background = GradientDrawable().apply {
//                        setColor(Color.WHITE)
//                    }
                }
            }

            // Add the indicator layout to the root layout
            carouselHolder.addView(indicatorLayout)
            rootLayout.addView(carouselHolder)

            // ✅todo:  layouts[0].style.navigationalArrows
            // Check if navigational arrows are enabled
            val showArrows = layouts.firstOrNull()?.style?.navigationalArrows == true

            if (showArrows) {
                // Left arrow
                val leftArrow = ImageView(activity).apply {
                    setImageResource(R.drawable.ic_arrow_left) // Replace with your drawable resource
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER_VERTICAL or Gravity.START
                        setMargins(16) // Optional margin
                    }
                    visibility = View.GONE // Initially hidden if on the first page
                }

                // Right arrow
                val rightArrow = ImageView(activity).apply {
                    setImageResource(R.drawable.ic_arrow_right) // Replace with your drawable resource
                    layoutParams = FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    ).apply {
                        gravity = Gravity.CENTER_VERTICAL or Gravity.END
                        setMargins(16) // Optional margin
                    }
                }

                // Add arrows to the carousel holder
                carouselHolder.addView(leftArrow)
                carouselHolder.addView(rightArrow)

                // Handle arrow clicks
                leftArrow.setOnClickListener {
                    val currentItem = viewPager.currentItem
                    if (currentItem > 0) {
                        viewPager.setCurrentItem(currentItem - 1, true)
                    }
                }

                rightArrow.setOnClickListener {
                    val currentItem = viewPager.currentItem
                    if (currentItem < layouts.size - 1) {
                        viewPager.setCurrentItem(currentItem + 1, true)
                    }
                }

                // Update arrow visibility based on page changes
                viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        leftArrow.visibility = if (position > 0) View.VISIBLE else View.GONE
                        rightArrow.visibility =
                            if (position < layouts.size - 1) View.VISIBLE else View.GONE
                        updateIndicators(
                            position,
                            layouts.size,
                            indicatorLayout,
                            activity
                        ) // Keep indicator updates
                    }
                })
            } else {
                // Update arrow visibility based on page changes
                viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        super.onPageSelected(position)
                        updateIndicators(
                            position,
                            layouts.size,
                            indicatorLayout,
                            activity
                        ) // Keep indicator updates
                    }
                })
            }

            // Dialog setup
            val dialogBuilder = AlertDialog.Builder(activity)
                .setView(rootLayout)
                .setCancelable(true)

            val alertDialog = dialogBuilder.create()

            // ✅ TODO: issue -> there is white color behind.. if root corner rounded
            alertDialog.window?.let {
                // Set the background of the window to transparent (if you want transparency behind)
                it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

                // Set the background color of the entire dialog window
                it.decorView.setBackgroundColor(Color.TRANSPARENT) // This sets the entire window's background to blue

                // Remove padding/margin from the window to make sure it covers the entire screen
                it.attributes = it.attributes.apply {
                    width = ViewGroup.LayoutParams.MATCH_PARENT
                    height = ViewGroup.LayoutParams.MATCH_PARENT
                }
            }

            // Set cancelable based on layout configuration
            data.layouts?.let {
                val layout = it.first()

                // ✅ TODO: handle transition type
                layout.extra.transition?.let { transition ->
//                    println("FCM, extra transition:${transition}")

                    // Map transition enum value to the corresponding animation style
                    val transitionStyle = when (transition) {
                        InAppTransitionType.LeftToRight.value -> R.style.LeftToRight
                        InAppTransitionType.RightToLeft.value -> R.style.RightToLeft
                        InAppTransitionType.TopToBottom.value -> R.style.TopToBottom
                        InAppTransitionType.BottomToTop.value -> R.style.BottomToTop
                        else -> null  // Default: no-transition or handle fallback
                    }

                    // Apply transition style if valid
                    transitionStyle?.let {
                        alertDialog.window?.apply {
                            setWindowAnimations(it) // Apply the selected transition style
                        }
                    } ?: run {
                        // Optionally handle no valid transition
                        alertDialog.window?.apply {
                            setWindowAnimations(0) // No animation or default animation
                        }
                    }
                }
            }

            rootLayout.setOnClickListener {
                alertDialog.dismiss()
            }

            alertDialog.show()

            // Update the global reference
            dialog = alertDialog

            Paylisher.capture(
                "inappMessageRead",
                properties = mapOf("type" to "FullscreenCarousel")
            )

            Log.d(
                TAG,
                "In-App Fullscreen Carousel sent! ${Locale.getDefault().language}-${Locale.getDefault().country}"
            )
        }
    }

    // NATIVE
    fun showCustomInAppMessage(activity: Activity, data: InAppNative, defaultLang: String?) {
        activity.runOnUiThread {
            val dialogView =
                LayoutInflater.from(activity).inflate(R.layout.dialog_in_app_message, null)
            val dialogBuilder = AlertDialog.Builder(activity)
                .setView(dialogView)
                .setCancelable(true)

            val dialog = dialogBuilder.create()

            val title = data.title?.localize(defaultLang) ?: "Title"
            val message = data.message?.localize(defaultLang) ?: "Body"

            dialogView.findViewById<TextView>(R.id.messageTitle).text = title
            dialogView.findViewById<TextView>(R.id.messageBody).text = message

            // Load image if imageUrl is available, else hide the ImageView
            val imageView = dialogView.findViewById<ImageView>(R.id.messageImage)
            if (!data.imageUrl.isNullOrEmpty()) {
                imageView.visibility = View.VISIBLE
                imageView.loadImage(data.imageUrl)
            } else {
                imageView.visibility = View.GONE
            }

            // Configure action button
            val actionButton = dialogView.findViewById<Button>(R.id.actionButton)

            if (!data.actionUrl.isNullOrEmpty()) {
                actionButton.text = data.actionText ?: "Yes"
                actionButton.visibility = View.VISIBLE
                actionButton.setOnClickListener {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(data.actionUrl))
                    activity.startActivity(browserIntent)
                    dialog.dismiss()
                }
            } else {
                actionButton.visibility = View.GONE
            }

            val closeButton = dialogView.findViewById<ImageButton>(R.id.closeButton)
            closeButton.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()

            Paylisher.capture(
                "inappMessageRead",
                properties = mapOf("type" to "Native")
            )

            Log.d(TAG, "In-App sent!")
        }
    }

    private fun updateIndicators(
        currentPosition: Int,
        totalItems: Int,
        indicatorLayout: LinearLayout,
        context: Context
    ) {
        // Clear existing indicators
        indicatorLayout.removeAllViews()

        for (i in 0 until totalItems) {
            val indicator = View(context).apply {
                layoutParams = LinearLayout.LayoutParams(16, 16).apply {
                    setMargins(8, 0, 8, 0)
                }
                background = ContextCompat.getDrawable(
                    context,
                    if (i == currentPosition) R.drawable.active_dot else R.drawable.inactive_dot
                )
            }
            indicatorLayout.addView(indicator)
        }
    }

    private fun ImageView.loadImage(url: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val bitmap = withContext(Dispatchers.IO) { loadBitmapFromUrl(url) }
            bitmap?.let { this@loadImage.setImageBitmap(it) }
        }
    }

    private fun loadBitmapFromUrl(url: String): Bitmap? {
        return try {
            BitmapFactory.decodeStream(URL(url).openStream())
        } catch (e: IOException) {
            Log.e("InAppMessageHelper", "Error loading image from URL: $url", e)
            null
        }
    }
}

