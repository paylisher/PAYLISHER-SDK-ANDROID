package com.paylisher.android.notification.iam

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.setMargins
import com.paylisher.android.R
import com.paylisher.android.notification.ButtonBlock
import com.paylisher.android.notification.InAppButtonGroupType
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

class InAppMessagingModal(context: Context) : FrameLayout(context) {

    init {
        // Inflate the base layout (it can be an empty layout that gets populated dynamically)
        LayoutInflater.from(context).inflate(R.layout.inapp_message_modal, this, true)
        setBackgroundColor(Color.TRANSPARENT)
        layoutParams = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT
        )
    }

    // ✅ TODO: issue -> top | center | bottom not work -> from InAppMessageHelper
    fun configureLayout(model: InAppMessagingLayout, defaultLang: String, activity: Activity) {

        // Create the root layout
        val rootLayout = RelativeLayout(context).apply {
            layoutParams =
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT).apply {
//                setMargins(0, 72, 0, 0) // 2 * raw px
//                setPadding(0, 72, 0, 0) // 2 * raw px
                }
//            gravity = Gravity.TOP

            clipChildren = false
            clipToPadding = false
            clipToOutline = false

//            elevation = 4f

//            val backgroundDrawable = GradientDrawable().apply {
//                setColor(Color.RED)
//            }
//            background = backgroundDrawable
        }

        // For close button's left/right -> over other views
        val parentLayout = RelativeLayout(context).apply {
            layoutParams =
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    gravity = when (model.blocks.align) {
                        "top" -> Gravity.TOP
                        "center" -> Gravity.CENTER
                        "bottom" -> Gravity.BOTTOM
                        else -> Gravity.TOP // Default to top
                    }
//                    setMargins(0, 72, 0, 0) // 2 * raw px
//
//                    clipChildren = false
//                    clipToPadding = false
                    clipToOutline = true
                }
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
                                val maskedBitmap =
                                    Utils.applyRoundedCornerMask(scaledBitmap, maskRadius)

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
        }

//        clipChildren = false
//        clipToPadding = false
//        clipToOutline = false

        // TODO: model.blocks.align -> "top" | "center" | "bottom"

        // Modal Container (LinearLayout)
        val modalContainer = LinearLayout(context).apply {
            layoutParams = RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
//                addRule(RelativeLayout.ALIGN_PARENT_TOP)
                gravity = when (model.blocks.align) {
                    "top" -> Gravity.TOP
                    "center" -> Gravity.CENTER
                    "bottom" -> Gravity.BOTTOM
                    else -> Gravity.TOP // Default to top
                }
            }
            orientation = LinearLayout.VERTICAL
            setPadding(0, 0, 0, 0)
            elevation = 5f
        }

        // ✅ TODO: model.blocks.order
        model.blocks.order
            .sortedBy { it.order } // ✅ Sort the blocks by their `order` property
            .forEach { block ->
                when (block) {
                    is InAppLayoutBlock.ImageBlock -> {
                        val imageView = addImageView(block)
                        modalContainer.addView(imageView)
                    }

                    is InAppLayoutBlock.TextBlock -> {
                        // Add the banner text
                        val bannerText = addText(block, activity, defaultLang)
                        modalContainer.addView(bannerText)
                    }

                    is InAppLayoutBlock.ButtonGroupBlock -> {
                        // Map `block.buttonGroupType` to the `InAppButtonGroupType` enum
                        val buttonGroupType = when (block.buttonGroupType) {
                            "single-vertical" -> InAppButtonGroupType.SingleVertical
                            "double-vertical" -> InAppButtonGroupType.DoubleVertical
                            "double-horizontal" -> InAppButtonGroupType.DoubleHorizontal
                            "single-compact-vertical" -> InAppButtonGroupType.SingleCompactVertical
                            "double-compact-vertical" -> InAppButtonGroupType.DoubleCompactVertical
                            else -> throw IllegalArgumentException("Unknown button group type: ${block.buttonGroupType}")
                        }

                        // TODO: Use `buttonGroupType` to create or render buttons
                        val buttonGroup =
                            addButtonGroup(block, buttonGroupType, defaultLang, activity)
                        modalContainer.addView(buttonGroup)
                    }

                    is InAppLayoutBlock.SpacerBlock -> {
                        val spacerView = addSpacer(block)
                        modalContainer.addView(spacerView)
                    }
                }
            }

        // Add modal container to parent layout first
        parentLayout.addView(modalContainer)

        // ✅ Close Button (Sequence is matter!)
        var closeButton: View? = null
        if (model.close.active) {
            when (model.close.position) {
                // 2 * raw px
                "outside-right", "outside-left" -> rootLayout.setPadding(0, 72, 0, 0)
                else -> rootLayout.setPadding(0, 0, 0, 0) // Default
            }

            // Handle close button
            closeButton = when (model.close.type) {
//                "icon" -> model.close.icon?.let { btnClose(it, "outside-right") }
                "icon" -> model.close.icon?.let { btnClose(it, model.close.position) }
                "text" -> model.close.text?.let { btnClose(it, model.close.position, defaultLang) }
                else -> null
            }

            closeButton?.let { rootLayout.addView(it) }

            // when style is "filled" -> result is circle with chosen color
            if (model.close.type == "icon" && model.close.icon != null && model.close.icon.style == "filled") {
                closeButton =
                    btnClose(CloseIcon(color = "#FFFFFF", style = "outlined"), model.close.position)
                rootLayout.addView(closeButton)
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

        // Finally, add parentLayout to rootLayout to ensure it's stacked above the close button
        rootLayout.addView(parentLayout)

        // Add root layout to the InAppMessagingModal (FrameLayout)
        removeAllViews()

        addView(rootLayout)
        setBackgroundColor(Color.TRANSPARENT)
    }

    private fun addSpacer(block: InAppLayoutBlock.SpacerBlock): View {
        val pxValue = block.verticalSpacing.removeSuffix("px").toFloatOrNull() ?: 0f

        return View(context).apply {
            layoutParams = if (block.fillAvailableSpacing) {
                // X TODO: Fill available space logic
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    0,
                )
            } else {
                // Set fixed height based on verticalSpacing
                LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    pxValue.toInt() // raw pixel
//                    pxValue.pxToDp() // dp is too short
                )
            }
        }
    }

    private fun addButtonGroup(
        block: InAppLayoutBlock.ButtonGroupBlock,
        buttonGroupType: InAppButtonGroupType,
        defaultLang: String, activity: Activity
    ): LinearLayout {
        val layout = LinearLayout(context).apply {
            orientation = when (buttonGroupType) {
                InAppButtonGroupType.SingleVertical,
                InAppButtonGroupType.SingleCompactVertical,
                InAppButtonGroupType.DoubleVertical,
                InAppButtonGroupType.DoubleCompactVertical -> LinearLayout.VERTICAL

                InAppButtonGroupType.DoubleHorizontal -> LinearLayout.HORIZONTAL
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        when (buttonGroupType) {
            InAppButtonGroupType.SingleVertical,
            InAppButtonGroupType.SingleCompactVertical -> {
                if (block.buttons.isNotEmpty()) {
                    val buttonView =
                        addButton(block.buttons.first(), defaultLang, layout, activity)
                    layout.addView(buttonView)
                }
            }

            InAppButtonGroupType.DoubleVertical,
            InAppButtonGroupType.DoubleCompactVertical -> {
                block.buttons.forEach { button ->
                    val buttonView = addButton(button, defaultLang, layout, activity)
                    layout.addView(buttonView)
                }
            }

            InAppButtonGroupType.DoubleHorizontal -> {
                //*left*  *right*   button
                // small + small
                // small + medium
                // small + large
                // medium + small
                // medium + medium
                // medium + large
                // large + small
                // large + medium
                // large + large

                block.buttons.forEach { button ->
                    val buttonView =
                        addButtonHorizontal(
                            button,
                            defaultLang,
                            layout,
                            activity,
                            block.buttons,
                        )
                    layout.addView(buttonView)
                }
                layout.gravity = Gravity.CENTER
            }
        }

        return layout
    }

    private fun addButtonHorizontal(
        btn: ButtonBlock,
        defaultLang: String,
        layout: LinearLayout,
        activity: Activity,
        buttons: List<ButtonBlock>,
    ): Button {
        val pxValue = btn.fontSize.removeSuffix("px").toFloatOrNull() ?: 14f

        return Button(context).apply {
            // Set text and styles
            text = btn.label.localize(defaultLang)

            textSize = pxValue  // using raw pixel -> sp is too small
//            textSize = pxValue.pxToSp()

            setTextColor(Color.parseColor(btn.textColor))
            setBackgroundColor(Color.parseColor(btn.backgroundColor))

            // Set font family
            if (btn.fontFamily.isNotEmpty()) {
                typeface = Typeface.create(btn.fontFamily, Typeface.NORMAL)
            }

            // Apply text style (bold, italic, underline)
            paint.isUnderlineText = btn.underscore
            typeface = when {
                btn.fontWeight.equals("bold_italic", ignoreCase = true) -> Typeface.create(
                    typeface,
                    Typeface.BOLD_ITALIC
                )

                btn.fontWeight.equals("bold", ignoreCase = true) && btn.italic -> Typeface.create(
                    typeface,
                    Typeface.BOLD_ITALIC
                )

                btn.fontWeight.equals("bold", ignoreCase = true) -> Typeface.create(
                    typeface,
                    Typeface.BOLD
                )

                btn.fontWeight.equals(
                    "italic",
                    ignoreCase = true
                ) || btn.italic -> Typeface.create(typeface, Typeface.ITALIC)

                else -> typeface // Default typeface
            }

            // Set underline if specified
            paintFlags = if (btn.underscore) {
                paintFlags or Paint.UNDERLINE_TEXT_FLAG
            } else {
                paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
            }

            // Disable all caps transformation
            isAllCaps = false

            // Set corner radius with a background drawable
            background = GradientDrawable().apply {
                cornerRadius = btn.borderRadius.toFloat()
                setColor(Color.parseColor(btn.backgroundColor))
                setStroke(3, Color.parseColor(btn.borderColor)) // Example border thickness: 2dp
            }

            val margins = buttons.sumOf { x -> x.margin }

            val left = buttons.first()
            val right = buttons.last()

            // Set button layout and position
            val screenWidth = layout.resources.displayMetrics.widthPixels
//            screenWidth = (screenWidth / 2) - (2 * margins) - 5 // perfect for large & large

            val large = (screenWidth / 2) - (2 * margins) - 5
            val medium = (screenWidth / 3) - (2 * margins)
            val small = (screenWidth / 4) - (2 * margins)

            var horizontalLength = (screenWidth / 2);
            if (left.horizontalSize == "large" && right.horizontalSize == "large") {
                horizontalLength = large
            } else if (left.horizontalSize == "medium" && right.horizontalSize == "medium") {
                horizontalLength = medium
            } else if (left.horizontalSize == "small" && right.horizontalSize == "small") {
                horizontalLength = small
            } else {
                horizontalLength = when (btn.horizontalSize.lowercase()) {
                    "large" -> large + (2 * margins)
                    "medium" -> medium + (2 * margins)
                    "small" -> small + (2 * margins)
                    else -> large + (2 * margins)
                }
            }

//            Log.d("FCM", "left: ${left.horizontalSize} right: ${right.horizontalSize}")
//            Log.d("FCM", "index: $index length: $horizontalLength")
//            Log.d("FCM", "margins: ${(2 * margins)}")

            layoutParams = LinearLayout.LayoutParams(
                horizontalLength,
//                when (btn.horizontalSize.lowercase()) {
//                    "large" -> large
//                    "medium" -> medium // Half of the parent width
//                    "small" -> small // One-third of the parent width
//                    else -> large
//                },
                when (btn.verticalSize.lowercase()) {
                    "large" -> (72).dpToPx()
                    "medium" -> (60).dpToPx()
                    "small" -> (48).dpToPx()
                    else -> (48).dpToPx()
                }
            ).apply {
                setMargins(btn.margin)
                gravity = Gravity.CENTER // btn pos: centered fixed for horizontal
            }

            if (btn.action.isNotEmpty()) {
                setOnClickListener {
//                    Log.e("btn", "action: ${btn.action}") // To verify the action value
                    when {
                        btn.action == "dismiss" -> {
                            InAppMessageHelper.dialog?.dismiss()
                            InAppMessageHelper.dismissEvent()
                        }
                        btn.action.startsWith("copy") -> {
                            val textToCopy = btn.action.removePrefix("copy:").trim()
                            if (textToCopy.isNotEmpty()) {
                                // Use a background thread for clipboard operations
                                Thread {
                                    try {
                                        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Copied Text", textToCopy)
                                        clipboard.setPrimaryClip(clip)

                                        // Post a toast on the main thread
                                        Handler(Looper.getMainLooper()).post {
                                            Toast.makeText(activity, "Copied to clipboard: $textToCopy", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("CopyAction", "Error while copying: ${e.message}", e)
                                        Handler(Looper.getMainLooper()).post {
                                            Toast.makeText(activity, "Failed to copy text.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }.start()
                            } else {
                                Toast.makeText(activity, "No text to copy.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        else -> {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(btn.action))
                            activity.startActivity(browserIntent)

                            InAppMessageHelper.dialog?.dismiss()
                            InAppMessageHelper.dismissEvent("intent")
                        }
                    }
                }
            }
        }
    }

    private fun addButton(
        btn: ButtonBlock,
        defaultLang: String,
        layout: LinearLayout,
        activity: Activity
    ): Button {
        val pxValue = btn.fontSize.removeSuffix("px").toFloatOrNull() ?: 14f

        return Button(context).apply {
            // Set text and styles
            text = btn.label.localize(defaultLang)

            textSize = pxValue  // using raw pixel -> sp is too small
//            textSize = pxValue.pxToSp()

            setTextColor(Color.parseColor(btn.textColor))
            setBackgroundColor(Color.parseColor(btn.backgroundColor))

            // Set font family
            if (btn.fontFamily.isNotEmpty()) {
                typeface = Typeface.create(btn.fontFamily, Typeface.NORMAL)
            }

            // Apply text style (bold, italic, underline)
            paint.isUnderlineText = btn.underscore
            typeface = when {
                btn.fontWeight.equals("bold_italic", ignoreCase = true) -> Typeface.create(
                    typeface,
                    Typeface.BOLD_ITALIC
                )

                btn.fontWeight.equals("bold", ignoreCase = true) && btn.italic -> Typeface.create(
                    typeface,
                    Typeface.BOLD_ITALIC
                )

                btn.fontWeight.equals("bold", ignoreCase = true) -> Typeface.create(
                    typeface,
                    Typeface.BOLD
                )

                btn.fontWeight.equals(
                    "italic",
                    ignoreCase = true
                ) || btn.italic -> Typeface.create(typeface, Typeface.ITALIC)

                else -> typeface // Default typeface
            }

            // Set underline if specified
            paintFlags = if (btn.underscore) {
                paintFlags or Paint.UNDERLINE_TEXT_FLAG
            } else {
                paintFlags and Paint.UNDERLINE_TEXT_FLAG.inv()
            }

            // Disable all caps transformation
            isAllCaps = false

            // Set corner radius with a background drawable
            background = GradientDrawable().apply {
                cornerRadius = btn.borderRadius.toFloat()
                setColor(Color.parseColor(btn.backgroundColor))
                setStroke(3, Color.parseColor(btn.borderColor)) // Example border thickness: 2dp
            }

            // Set button layout and position
            layoutParams = LinearLayout.LayoutParams(
                when (btn.horizontalSize.lowercase()) {
                    "large" -> LinearLayout.LayoutParams.MATCH_PARENT
                    "medium" -> (layout.resources.displayMetrics.widthPixels / 2) // Half of the parent width
                    "small" -> (layout.resources.displayMetrics.widthPixels / 3) // One-third of the parent width
                    else -> LinearLayout.LayoutParams.MATCH_PARENT
                },
                when (btn.verticalSize.lowercase()) {
                    "large" -> (72).dpToPx()
                    "medium" -> (60).dpToPx()
                    "small" -> (48).dpToPx()
                    else -> (48).dpToPx()
                }
            ).apply {
                setMargins(btn.margin)
                gravity = when (btn.buttonPosition) {
                    "left" -> Gravity.START
                    "centered" -> Gravity.CENTER
                    "right" -> Gravity.END
                    else -> Gravity.CENTER
                }
            }

            if (btn.action.isNotEmpty()) {
                setOnClickListener {
//                    Log.e("btn", "action: ${btn.action}") // To verify the action value
                    when {
                        btn.action == "dismiss" -> {
                            InAppMessageHelper.dialog?.dismiss()
                            InAppMessageHelper.dismissEvent()
                        }
                        btn.action.startsWith("copy") -> {
                            val textToCopy = btn.action.removePrefix("copy:").trim()
                            if (textToCopy.isNotEmpty()) {
                                // Use a background thread for clipboard operations
                                Thread {
                                    try {
                                        val clipboard = activity.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        val clip = ClipData.newPlainText("Copied Text", textToCopy)
                                        clipboard.setPrimaryClip(clip)

                                        // Post a toast on the main thread
                                        Handler(Looper.getMainLooper()).post {
                                            Toast.makeText(activity, "Copied to clipboard: $textToCopy", Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (e: Exception) {
                                        Log.e("CopyAction", "Error while copying: ${e.message}", e)
                                        Handler(Looper.getMainLooper()).post {
                                            Toast.makeText(activity, "Failed to copy text.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }.start()
                            } else {
                                Toast.makeText(activity, "No text to copy.", Toast.LENGTH_SHORT).show()
                            }
                        }
                        else -> {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(btn.action))
                            activity.startActivity(browserIntent)

                            InAppMessageHelper.dialog?.dismiss()
                            InAppMessageHelper.dismissEvent("intent")
                        }
                    }
                }
            }
        }
    }

    private fun addText(
        data: InAppLayoutBlock.TextBlock,
        activity: Activity,
        defaultLang: String
    ): TextView {
        val pxValue = data.fontSize.removeSuffix("px").toFloatOrNull() ?: 16f

        return TextView(context).apply {
            id = View.generateViewId()
            layoutParams = RelativeLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
            ).apply {
                addRule(RelativeLayout.CENTER_VERTICAL) // Center text vertically
                //Set horizontal margin -> in pixels (px)
                setMargins(
                    data.horizontalMargin,
                    0,
                    data.horizontalMargin,
                    0,
                )
            }

            // Set text properties
            text = data.content.localize(defaultLang)
            textSize = pxValue  // using raw pixel -> sp is too small
//            textSize =pxValue.pxToSp() // Fallback to 16sp if textSize is invalid
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
            textSize = pxValue  // raw pixel
//            textSize = pxValue.pxToSp() // sp is too small

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

            // Elevation to ensure the button appears above other views
            elevation = 10f
        }
    }

    private fun btnClose(icon: CloseIcon, position: String = "right"): ImageButton {
//        println("FCM btnClose position $position") // TODO: left, right, outside-right, outside-left

        return ImageButton(context).apply {
            id = View.generateViewId()

            // position: left right modals -> outside-right, outside-left

            // Set the size of the button
            layoutParams = RelativeLayout.LayoutParams(36.dpToPx(), 36.dpToPx()).apply {

                // Handle button position based on the "position" parameter
                when (position) {
                    "right" -> {
                        addRule(RelativeLayout.ALIGN_PARENT_END)  // Align to the right
                    }

                    "left" -> {
                        addRule(RelativeLayout.ALIGN_PARENT_START)  // Align to the left
                    }

                    "outside-right" -> {
                        addRule(RelativeLayout.ALIGN_PARENT_END)  // Align to the right
                        // Move outside of the parent by adding margin
                        topMargin = (-36).dpToPx()  // Half the button width to move outside
                        rightMargin = (-5).dpToPx()  // Half the button width to move outside
                    }

                    "outside-left" -> {
                        addRule(RelativeLayout.ALIGN_PARENT_START)  // Align to the left
                        // Move outside of the parent by adding margin
                        topMargin = (-26).dpToPx()  // Half the button width to move outside
                        leftMargin = (-5).dpToPx()  // Half the button width to move outside
                    }

                    else -> {
                        // Default behavior: position at top-right
                        addRule(RelativeLayout.ALIGN_PARENT_END)
                    }
                }

                addRule(RelativeLayout.ALIGN_PARENT_TOP)  // Always align to the top
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

            // Elevation to ensure the button appears above other views
            elevation = 10f
        }
    }

    private fun addImageView(image: InAppLayoutBlock.ImageBlock): ImageView {
        val imageView = ImageView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (200).dpToPx() // Height of the image
            ).apply {
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
            image.url?.let { loadImage(it) }
        }

        return imageView
    }

//    fun pxToDp(px: Float): Float {
//        val density = context.resources.displayMetrics.density
//        return px / density
//    }
//
//    fun pxToSp(px: Float, context: Context): Float {
//        val configuration = context.resources.configuration
//        val scaledDensity = configuration.fontScale * context.resources.displayMetrics.density
//        return px / scaledDensity
//    }

    // Not correct
//    private fun Float.pxToSp(): Float {
//        val configuration = context.resources.configuration
//        val scaledDensity = configuration.fontScale * context.resources.displayMetrics.density
//        return this / scaledDensity
//    }

    private fun Float.pxToSp(): Float {
        return this / context.resources.displayMetrics.scaledDensity
    }

    //    private fun Int.pxToDp(): Int = (this / context.resources.displayMetrics.density).toInt()
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