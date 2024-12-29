package com.paylisher.android.notification.iam

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.core.view.setMargins
import com.paylisher.android.R
import com.paylisher.android.notification.InAppLayoutBlock
import com.paylisher.android.notification.InAppMessageHelper
import com.paylisher.android.notification.InAppMessagingLayout
import com.paylisher.android.notification.InAppMessagingLayout.CloseIcon
import com.paylisher.android.notification.InAppMessagingLayout.CloseText
import com.paylisher.android.notification.helpers.InAppLocalize.Companion.localize
import com.paylisher.android.notification.helpers.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.URL

class InAppMessagingBanner(context: Context) : FrameLayout(context) {

    init {
        // Inflate the base layout (it can be an empty layout that gets populated dynamically)
        LayoutInflater.from(context).inflate(R.layout.inapp_message_banner, this, true)
    }

    // TODO: model.blocks.align
    // ✅ TODO: issue -> top | center | bottom not work -> from InAppMessageHelper
    fun configureLayout(model: InAppMessagingLayout, defaultLang: String, activity: Activity) {
        // Create the parent layout (LinearLayout for horizontal alignment)
        val parentLayout = RelativeLayout(context).apply {
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                height = 360
                gravity = when (model.style.verticalPosition) {
                    "top" -> Gravity.TOP
                    "center" -> Gravity.CENTER
                    "bottom" -> Gravity.BOTTOM
                    else -> Gravity.TOP // Default to top
                }
            }
            setPadding(0, 0, 0, 0)
            elevation = 4f

            val backgroundDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = model.style.radius.toFloat() // Rounded corners
                model.style.bgColor?.let { setColor(Color.parseColor(it)) }
            }
            background = backgroundDrawable

            // ✅ TODO: Handle background image
            try {
                // Load the background image if URL is present
                if (model.style.bgImage != null) {
                    CoroutineScope(Dispatchers.Main).launch {
                        var layeredBackground: Drawable = GradientDrawable().apply {
                            shape = GradientDrawable.RECTANGLE
                            cornerRadius = model.style.radius.toFloat() // Rounded corners
                            setColor(Color.parseColor(model.style.bgImageColor))
                        }

                        val bitmap = withContext(Dispatchers.IO) {
                            loadBitmapFromUrl(model.style.bgImage)
                        }

                        if (bitmap != null) {
                            val scaledBitmap =
                                Bitmap.createScaledBitmap(bitmap, width, height, true)
                            var bitmapDrawable = BitmapDrawable(context.resources, scaledBitmap)

                            // ?? TODO: bgImageMask,
                            // Mask -> for rounded corner ?
                            if (model.style.bgImageMask) {
                                val maskRadius = model.style.radius.toFloat()
                                val maskedBitmap = Utils.applyRoundedCornerMask(scaledBitmap, maskRadius)

                                bitmapDrawable = BitmapDrawable(context.resources, maskedBitmap)
                            }

                            // Apply the mask and background together
                            layeredBackground =
                                LayerDrawable(arrayOf(layeredBackground, bitmapDrawable)).apply {
                                    // You can add extra layers if necessary
                                }
                        }

                        // Apply the final layered background
                        background = layeredBackground
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                background = backgroundDrawable // Fallback to solid color
            }

            // Enable clipping for the corner radius
            clipToOutline = true
        }

        // TODO: model.blocks.align -> "top" | "center" | "bottom" -> NOT WORKING
        val childLayout = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            )
            gravity = when (model.blocks.align) {
                "top" -> Gravity.TOP
                "center" -> Gravity.CENTER
                "bottom" -> Gravity.BOTTOM
                else -> Gravity.START
            }
        }

        // ✅ TODO: model.blocks.order
        model.blocks.order
            .sortedBy { it.order } // ✅ Sort the blocks by their `order` property
            .forEach { block ->
//                println("FCM block order  ${block.order}")
                when (block) {
                    is InAppLayoutBlock.ImageBlock -> {
                        // Add the left image (3/7 width)
                        val imageView = addImageView(block, 3f)
                        childLayout.addView(imageView)
                    }

                    is InAppLayoutBlock.TextBlock -> {
                        // Add the banner text
                        val bannerText = addText(block, activity, defaultLang)

                        // Create the right container (RelativeLayout for text and close button)
                        val textContainer = RelativeLayout(context).apply {
                            layoutParams =
                                LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT).apply {
                                    weight = 4f
                                }
                            setPadding(16, 0, 16, 0)
                        }
                        textContainer.addView(bannerText)

                        // Add the right container to the parent layout
                        childLayout.addView(textContainer)
                    }

                    is InAppLayoutBlock.ButtonGroupBlock -> {}
                    is InAppLayoutBlock.SpacerBlock -> {}
                }
            }

        parentLayout.addView(childLayout)

        // ✅ Close Button (Sequence is matter!)
        var closeButton: View? = null
        if (model.close.active) {

            if (model.close.type == "icon") {
                // Add the close button at the top-right
                closeButton = model.close.icon?.let { btnClose(it, model.close.position) }

                // TESTS
//                closeButton = btnClose(CloseIcon(color = "#0000FF", style = "outlined"), "left")
//                closeButton = btnClose(CloseIcon(color = "#0000FF", style = "filled"), "right")

                parentLayout.addView(closeButton)

                if (model.close.icon != null && model.close.icon.style == "filled") {
                    // when style is "filled" -> result is circle with chosen color... to fix it :p
                    closeButton = btnClose(
                        CloseIcon(color = "#FFFFFF", style = "outlined"),
                        model.close.position
                    )
                    parentLayout.addView(closeButton)
                }
            } else if (model.close.type == "text") {
                closeButton =
                    model.close.text?.let { btnClose(it, model.close.position, defaultLang) };

                // TESTS
//            val closeText = CloseText(label = "Close", size = "16", color = "#222222")
//            closeButton = btnClose(closeText, "left")

                parentLayout.addView(closeButton)
            }

            // ✅ Access the close button and set its listener from outside
            closeButton?.setOnClickListener {
//                println("FCM closeButton: ${closeButton.id}, dialog: ${InAppMessageHelper.dialog != null}")

                if (InAppMessageHelper.dialog?.isShowing == true) {
                    InAppMessageHelper.dialog?.dismiss()
                    InAppMessageHelper.dismissEvent()
                    Log.d("FCM", "Dialog dismissed via closeButton.")
                }
            }
        }

        // Ensure views are not already attached to a parent
        removeAllViews()
        addView(parentLayout)
    }

    private fun addText(
        data: InAppLayoutBlock.TextBlock,
        activity: Activity,
        defaultLang: String
    ): TextView {
        return TextView(context).apply {
            id = View.generateViewId()
            layoutParams = RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.CENTER_VERTICAL) // Center text vertically
                setMargins(
                    data.horizontalMargin.dpToPx(),
                    0,
                    data.horizontalMargin.dpToPx(),
                    0
                ) // Set horizontal margin
            }

            // Set text properties
            text = data.content.localize(defaultLang)
            textSize = data.fontSize.toFloatOrNull() ?: 16f // default -> 16sp
            setTextColor(Color.parseColor(data.color)) // Convert hex color to Color
            maxLines = 3
            ellipsize = TextUtils.TruncateAt.END

            // Set font family
            if (data.fontFamily.isNotEmpty()) {
                typeface = Typeface.create(data.fontFamily, Typeface.NORMAL)
            }

            // Apply text style (bold, italic, underline)
            paint.isUnderlineText = data.underscore
            typeface = when {
                data.fontWeight.equals("bold_italic", ignoreCase = true) -> Typeface.create(
                    typeface,
                    Typeface.BOLD_ITALIC
                )

                data.fontWeight.equals("bold", ignoreCase = true) && data.italic -> Typeface.create(
                    typeface,
                    Typeface.BOLD_ITALIC
                )

                data.fontWeight.equals("bold", ignoreCase = true) -> Typeface.create(
                    typeface,
                    Typeface.BOLD
                )

                data.fontWeight.equals(
                    "italic",
                    ignoreCase = true
                ) || data.italic -> Typeface.create(typeface, Typeface.ITALIC)

                else -> typeface // Default typeface
            }

            // Set text alignment
            gravity = when (data.textAlignment.lowercase()) {
                "left" -> Gravity.START
                "center" -> Gravity.CENTER
                "right" -> Gravity.END
                else -> Gravity.START // Default to left alignment
            }

            if (data.action.isNotEmpty()) {
                setOnClickListener {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(data.action))
                    activity.startActivity(browserIntent)

                    InAppMessageHelper.dialog?.dismiss()
                    InAppMessageHelper.dismissEvent("intent")
                }
            }
        }
    }

    private fun btnClose(data: CloseText, position: String = "right", defaultLang: String): Button {
        val pxValue = data.fontSize.removeSuffix("px").toFloatOrNull() ?: 14f

        return Button(context).apply {
            id = View.generateViewId()

            // Set the button text
            text = data.label.localize(defaultLang)

            // Set the text size (convert string size to float, with a fallback)
            textSize = pxValue  // using raw pixel -> sp is too small
//            textSize = pxValue.pxToSp()

            // Disable all caps transformation
            isAllCaps = false

            // Set the text color
            setTextColor(Color.parseColor(data.color))

            // Set the size and position of the button
            layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (position == "right") {
                    addRule(RelativeLayout.ALIGN_PARENT_END)
                } else if (position == "left") {
                    addRule(RelativeLayout.ALIGN_PARENT_START)
                }
                addRule(RelativeLayout.ALIGN_PARENT_TOP)
            }

            // Optional: Set additional styles like background
            setBackgroundColor(Color.TRANSPARENT) // Or apply a drawable for styling
        }
    }

    private fun btnClose(icon: CloseIcon, position: String = "right"): ImageButton {
        return ImageButton(context).apply {
            id = View.generateViewId()

            // Set the size of the button
            layoutParams = RelativeLayout.LayoutParams(36.dpToPx(), 36.dpToPx()).apply {
                // Set the position of the button
                if (position == "right") {
                    addRule(RelativeLayout.ALIGN_PARENT_END)  // Align to the right
                } else if (position == "left") {
                    addRule(RelativeLayout.ALIGN_PARENT_START)  // Align to the left
                }
                addRule(RelativeLayout.ALIGN_PARENT_TOP)  // Align to the top
            }
            setBackgroundColor(Color.TRANSPARENT)

            // Apply the appropriate icon based on the style
            val iconResource = when (icon.style) {
                "filled" -> R.drawable.ic_close_filled
                "outlined" -> R.drawable.ic_close_outline
                "basic" -> R.drawable.ic_close // default to basic icon
                else -> R.drawable.ic_close // fallback to basic if style is not recognized
            }

            setImageResource(iconResource)

            // Apply the tint color from `icon.color`
            icon.color.let { colorString ->
                val color = Color.parseColor(colorString) // Convert hex to color
                imageTintList = ColorStateList.valueOf(color) // Apply the tint
                imageTintMode = PorterDuff.Mode.SRC_IN // Ensure the tint blends correctly
            }
        }
    }

    private fun addImageView(image: InAppLayoutBlock.ImageBlock, w: Float = 3f): ImageView {
        // Add the left image (3/7 width)
        val imageView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(0, LayoutParams.MATCH_PARENT).apply {
                weight = w
                setMargins(image.margin)
            }
            contentDescription = image.alt
            scaleType = ImageView.ScaleType.CENTER_CROP

            // Set rounded corners with GradientDrawable
            val roundedDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = image.radius.toFloat()
            }

            background = roundedDrawable // Apply background for rounded corners

            // Enable clipping of the ImageView content to match the outline
            clipToOutline = true
            outlineProvider = ViewOutlineProvider.BACKGROUND // Use background's outline

            // Load image using your custom loadImage function
            loadImage(image.url)
        }

        return imageView;
    }

    private fun Float.pxToSp(): Float {
        return this / context.resources.displayMetrics.scaledDensity
    }

    private fun Float.pxToDp(): Int = (this / context.resources.displayMetrics.density).toInt()

    // Extension function for converting dp to pixels
    private fun Int.dpToPx(): Int = (this * context.resources.displayMetrics.density).toInt()

    // Extension function to load image into ImageView using coroutines
    private fun ImageView.loadImage(url: String) {
        CoroutineScope(Dispatchers.Main).launch {
            val bitmap = withContext(Dispatchers.IO) { loadBitmapFromUrl(url) }
            bitmap?.let { this@loadImage.setImageBitmap(it) }
        }
    }

    // Helper function to load the bitmap from URL
    private fun loadBitmapFromUrl(url: String): Bitmap? {
        return try {
            BitmapFactory.decodeStream(URL(url).openStream())
        } catch (e: IOException) {
            Log.e("InAppMessageHelper", "Error loading image from URL: $url", e)
            null
        }
    }
}


