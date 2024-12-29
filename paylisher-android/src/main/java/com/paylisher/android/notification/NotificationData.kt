package com.paylisher.android.notification

data class NotificationPushData(
    val title: Map<String, String>?,
    val message: Map<String, String>?,

    val imageUrl: String?,
    val iconUrl: String?,

    val silent: Boolean,
    val action: String?,
//    val deepLink: String?,

    val buttons: List<PushButton>?,

    val defaultLang: String?,
    val condition: Condition,
    val geofence: Geofence?
)

data class PushButton(
    val label: Map<String, String>,
    val action: String,
)

data class Geofence(
    val trigger: String?,

    val latitude: Double?,
    val longitude: Double?,
    val radius: Float?,
    val geofenceId: String?
)

data class NotificationInAppData(
    val native: InAppNative?,

    val condition: Condition,

    val defaultLang: String?,
    val layoutType: String,
    val layouts: List<InAppMessagingLayout>?,
)

data class InAppNative(
    val title: Map<String, String>?,
    val message: Map<String, String>?,

    val imageUrl: String?,

    val actionText: String?,
    val actionUrl: String?,
)

data class Condition(
    val target: String?,        // if null -> MainActivity
    val displayTime: Long?,     // Indicate the display time
    val expireDate: Long?,      // Expiry timestamp
    val delay: Int  // minutes
)

enum class InAppLayoutType(val value: String) {
    NATIVE("native"),

    BANNER("banner"),
    MODAL("modal"),
    FULLSCREEN("fullscreen"),
    MODAL_CAROUSEL("modal-carousel"),
    FULLSCREEN_CAROUSEL("fullscreen-carousel");

    companion object {
        fun fromValue(value: String): InAppLayoutType {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown layout type: $value")
        }
    }
}

data class InAppMessagingLayout(
    val style: InAppStyle,
    val close: InAppCloseButton,

    val extra: InAppExtra,
    val blocks: InAppLayout
) {
    data class InAppStyle(
        val navigationalArrows: Boolean? = null, // for modal-s

        val radius: Int, // Corner Radius
        val bgColor: String? = null,

        val bgImage: String? = null,
        val bgImageMask: Boolean,
        val bgImageColor: String,

        val verticalPosition: String? = null, // top center bottom
        val horizontalPosition: String? = null // left center right
    )

    data class InAppCloseButton(
        val active: Boolean, // Display Close Button if checked
        val type: String,    // icon text
        val position: String, // left right
        val text: CloseText? = null,
        val icon: CloseIcon? = null
    )

    data class CloseText(
//        val label: String,  // Text Label
        val label: Map<String, String>,  // Text Label
        val fontSize: String,   // Text Size (px)
        val color: String   // Text Color #001122
    )

    data class CloseIcon(
        val color: String,  // Icon Color #001122
        val style: String   // Icon Style: Filled, Outlined, Basic
    )
}

data class InAppLayout(
    val align: String, // top
    val order: List<InAppLayoutBlock> // Ordered list of layout blocks
)

// **********************************************
data class InAppExtra(
    val banner: Banner? = null,
    val overlay: Overlay? = null,
    val transition: String? = null
) {
    data class Banner(
        val action: String,  // URL or deep link
        val duration: Int    // Display duration in seconds
    )

    data class Overlay(
        val action: String,  // Close or no action
        val color: String    // Hex color code
    )
}

enum class InAppTransitionType(val value: String) {
    RightToLeft("right-to-left"),
    LeftToRight("left-to-right"),
    TopToBottom("top-to-bottom"),
    BottomToTop("bottom-to-top"),
    None("no-transition");

    companion object {
        fun fromValue(value: String): InAppTransitionType {
            return InAppTransitionType.entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown layout type: $value")
        }
    }
}

// **********************************************
// Layout Block Types
sealed class InAppLayoutBlock {
    abstract val order: Int

    data class SpacerBlock(
        val type: String = "spacer",  // Block type
        override val order: Int,      // Determines position in the modal

        val verticalSpacing: String, // def: 16px
        val fillAvailableSpacing: Boolean
    ) : InAppLayoutBlock()

    data class ImageBlock(
        val type: String = "image",  // Block type
        override val order: Int,     // Determines position in the modal

        val url: String,             // Image URL
        val action: String? = null,  // Optional action

        val alt: String? = null,     // Accessibility text
        val radius: Int,             // Corner radius
        val margin: Int              // Margin
    ) : InAppLayoutBlock()

    data class TextBlock(
        val type: String = "text",   // Block type
        override val order: Int, // Determines position in the modal

//        val content: String,         // Text content
        val content: Map<String, String>,         // Text content
        val action: String,          // Action to trigger

        val fontFamily: String,      // Font family
        val fontWeight: String,         // Font weight
        val fontSize: String,           // Font size
        val underscore: Boolean,     // Underline text
        val italic: Boolean,         // Italicize text
        val color: String,           // Hex color code

        val textAlignment: String, // Text alignment
        val horizontalMargin: Int    // Horizontal margin
    ) : InAppLayoutBlock()

    data class ButtonGroupBlock(
        val type: String = "buttonGroup", // Block type
        override val order: Int,          // Determines position in the modal

        val buttonGroupType: String,      // InAppButtonGroupType
        val buttons: List<ButtonBlock>    // List of buttons in this group
    ) : InAppLayoutBlock()
}

enum class InAppButtonGroupType(val value: String) {
    SingleVertical("single-vertical"),         // One button centered vertically
    DoubleVertical("double-vertical"),         // Two buttons stacked vertically (e.g., top and bottom)

    DoubleHorizontal("double-horizontal"),     // Two buttons side by side (e.g., left and right)

    SingleCompactVertical("single-compact-vertical"),   // One short button vertically aligned
    DoubleCompactVertical("double-compact-vertical");   // Two short buttons stacked vertically (top and bottom)

    companion object {
        fun fromValue(value: String): InAppButtonGroupType {
            return entries.find { it.value == value }
                ?: throw IllegalArgumentException("Unknown layout type: $value")
        }
    }
}

// **********************************************
// Button Block
data class ButtonBlock(
//    val label: String,               // Button label
    val label: Map<String, String>,               // Button label
    val action: String,              // Action to trigger

    val fontFamily: String,      // Font family
    val fontWeight: String,         // Font weight
    val fontSize: String,           // Font size

    val underscore: Boolean,     // Underline text
    val italic: Boolean,         // Italicize text

    val textColor: String,           // Button text color
    val backgroundColor: String,     // Button background color
    val borderColor: String,     // Button background color

    val borderRadius: Int,            // Button corner radius

    val horizontalSize: String,      // large, medium, small  def: large (full vertical-one) (full half - horizontal)
    val verticalSize: String,       // large, medium, small  def: small

    val buttonPosition: String,       // left, centered, right
    val margin: Int,            // def 16
)
