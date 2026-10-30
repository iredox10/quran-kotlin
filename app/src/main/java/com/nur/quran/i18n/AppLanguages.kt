package com.nur.quran.i18n

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

data class AppLanguage(val code: String, val nativeName: String, val englishName: String, val isRtl: Boolean = false)

/** App interface language is independent of the Quran text and selected translation. */
object AppLanguages {
    private const val PREFERENCES = "app_language"
    private const val LANGUAGE = "language_tag"
    val supported = listOf(
        AppLanguage("", "System default", "System default"),
        AppLanguage("en", "English", "English"),
        AppLanguage("ar", "العربية", "Arabic", isRtl = true),
        AppLanguage("fr", "Français", "French"),
        AppLanguage("es", "Español", "Spanish"),
        AppLanguage("pt", "Português", "Portuguese"),
        AppLanguage("de", "Deutsch", "German"),
        AppLanguage("it", "Italiano", "Italian"),
        AppLanguage("ru", "Русский", "Russian"),
        AppLanguage("zh-Hans", "简体中文", "Chinese (Simplified)"),
        AppLanguage("ja", "日本語", "Japanese"),
        AppLanguage("ko", "한국어", "Korean"),
        AppLanguage("hi", "हिन्दी", "Hindi"),
        AppLanguage("bn", "বাংলা", "Bengali"),
        AppLanguage("ur", "اردو", "Urdu", isRtl = true),
        AppLanguage("id", "Bahasa Indonesia", "Indonesian"),
        AppLanguage("ms", "Bahasa Melayu", "Malay"),
        AppLanguage("tr", "Türkçe", "Turkish"),
        AppLanguage("fa", "فارسی", "Persian", isRtl = true),
        AppLanguage("sw", "Kiswahili", "Swahili"),
        AppLanguage("ha", "Hausa", "Hausa"),
        AppLanguage("yo", "Yorùbá", "Yoruba")
    )

    fun selectedCode(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
            val locale = locales?.takeIf { !it.isEmpty }?.get(0) ?: return ""
            return supportedCodeFor(locale)
        }
        return context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString(LANGUAGE, "").orEmpty()
            .takeIf { code -> supported.any { it.code == code } }.orEmpty()
    }

    internal fun supportedCodeFor(locale: Locale): String =
        supported.firstOrNull { it.code.equals(locale.toLanguageTag(), ignoreCase = true) }?.code
            ?: supported.firstOrNull { it.code.isNotEmpty() && Locale.forLanguageTag(it.code).language == locale.language }?.code.orEmpty()

    fun wrap(context: Context): Context {
        // Android 13 applies localized resources itself. Keep process date/number
        // formatters aligned too; their default Locale can otherwise remain English.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val applicationLocales = context.getSystemService(LocaleManager::class.java)?.applicationLocales
            val locale = applicationLocales?.takeIf { !it.isEmpty }?.get(0)
                ?: context.resources.configuration.locales[0]
            Locale.setDefault(locale)
            return context
        }
        val code = selectedCode(context)
        if (code.isBlank()) {
            Locale.setDefault(context.resources.configuration.locales[0])
            return context
        }
        val locale = Locale.forLanguageTag(code)
        Locale.setDefault(locale)
        val configuration = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLocales(LocaleList(locale))
            setLayoutDirection(locale)
        }
        return context.createConfigurationContext(configuration)
    }

    fun setLanguage(context: Context, code: String) {
        require(supported.any { it.code == code }) { "Unsupported app language" }
        if (selectedCode(context) == code) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(code)
        } else {
            context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().putString(LANGUAGE, code).commit()
            findActivity(context)?.recreate()
        }
    }

    private tailrec fun findActivity(context: Context?): Activity? = when (context) {
        is Activity -> context
        is ContextWrapper -> findActivity(context.baseContext)
        else -> null
    }
}
