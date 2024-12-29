package com.paylisher.android.replay

import com.paylisher.PaylisherExperimental

@PaylisherExperimental
public class PaylisherSessionReplayConfig
    @JvmOverloads
    constructor(
        /**
         * Enable masking of all text input fields
         * Defaults to true
         * This isn't supported if using Jetpack Compose views, use with caution
         */
        @PaylisherExperimental
        public var maskAllTextInputs: Boolean = true,
        /**
         * Enable masking of all images to a placeholder
         * Defaults to true
         * This isn't supported if using Jetpack Compose views, use with caution
         */
        @PaylisherExperimental
        public var maskAllImages: Boolean = true,
        /**
         * Enable capturing of logcat as console events
         * Defaults to true
         */
        @PaylisherExperimental
        public var captureLogcat: Boolean = true,
        /**
         * Converts custom Drawable to Bitmap
         * By default Paylisher tries to convert the Drawable to Bitmap, the supported types are
         * BitmapDrawable, ColorDrawable, GradientDrawable, InsetDrawable, LayerDrawable, RippleDrawable
         */
        @PaylisherExperimental
        public var drawableConverter: PaylisherDrawableConverter? = null,
        /**
         * By default Session replay will capture all the views on the screen as a wireframe,
         * By enabling this option, Paylisher will capture the screenshot of the screen.
         * The screenshot may contain sensitive information, use with caution.
         */
        @PaylisherExperimental
        public var screenshot: Boolean = false,
        /**
         * Deboucer delay used to reduce the number of snapshots captured and reduce performance impact
         * This is used for capturing the view as a wireframe or screenshot
         * The lower the number more snapshots will be captured but higher the performance impact
         * Defaults to 500ms
         */
        @PaylisherExperimental
        public var debouncerDelayMs: Long = 500,
    )
