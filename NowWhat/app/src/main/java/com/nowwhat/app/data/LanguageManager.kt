package com.nowwhat.app.data

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import com.nowwhat.app.model.AppLanguage
import java.util.*

object LanguageManager {

    /**
     * Convert language code string to Locale
     */
    fun getLocale(languageCode: String): Locale {
        return when (languageCode) {
            "en" -> Locale.ENGLISH
            "iw", "he" -> Locale("iw") // Hebrew uses "iw" in Android
            "ru" -> Locale("ru")
            else -> Locale.ENGLISH
        }
    }

    /**
     * Convert AppLanguage enum to Locale
     */
    fun getLocale(language: AppLanguage): Locale {
        return when (language) {
            AppLanguage.English -> Locale.ENGLISH
            AppLanguage.Hebrew -> Locale("iw") // Hebrew uses "iw" in Android
            AppLanguage.Russian -> Locale("ru")
        }
    }

    /**
     * Get language code from AppLanguage enum
     */
    fun getLanguageCode(language: AppLanguage): String {
        return when (language) {
            AppLanguage.English -> "en"
            AppLanguage.Hebrew -> "iw"
            AppLanguage.Russian -> "ru"
        }
    }

    /**
     * Get AppLanguage enum from language code string
     */
    fun getAppLanguage(languageCode: String): AppLanguage {
        return when (languageCode) {
            "en" -> AppLanguage.English
            "iw", "he" -> AppLanguage.Hebrew
            "ru" -> AppLanguage.Russian
            else -> AppLanguage.English
        }
    }

    /**
     * Apply locale to context and return updated context
     */
    fun setLocale(context: Context, locale: Locale): Context {
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        // Set layout direction for RTL languages
        if (locale.language == "iw" || locale.language == "he" || locale.language == "ar") {
            config.setLayoutDirection(locale)
        }

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            context.createConfigurationContext(config)
        } else {
            @Suppress("DEPRECATION")
            context.resources.updateConfiguration(config, context.resources.displayMetrics)
            context
        }
    }

    /**
     * Apply AppLanguage enum to context
     */
    fun setLocale(context: Context, language: AppLanguage): Context {
        val locale = getLocale(language)
        return setLocale(context, locale)
    }
}