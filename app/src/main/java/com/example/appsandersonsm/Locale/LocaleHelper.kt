package com.example.appsandersonsm.Locale

import android.content.Context
import java.util.Locale

object LocaleHelper {
    private const val PREFS_NAME = "AppPreferences"
    private const val KEY_LANGUAGE = "language"

    fun setLocale(context: Context): Context {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val language = prefs.getString(KEY_LANGUAGE, Locale.getDefault().language) ?: Locale.getDefault().language
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }
}
