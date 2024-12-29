package com.paylisher.android.notification.helpers

import java.util.Locale

class InAppLocalize {

    companion object {

//        fun localize(text: Map<String, String>, default: String = "en"): String {
//            // Get the current system language
//            val currentLocale = Locale.getDefault().language
//
//            // Attempt to fetch the localized text using the current locale
//            val localizedText = text[currentLocale]
//
//            // Fallback to the default language if the current locale is not found
//            return localizedText ?: text[default] ?: "Translation not available"
//        }

        fun Map<String, String>.localize(default: String? = "en"): String {
            val currentLocale = Locale.getDefault().language
            return this[currentLocale]
                ?: this[default]
                ?: this.values.firstOrNull()
                ?: "Translation not available"
        }
    }

}