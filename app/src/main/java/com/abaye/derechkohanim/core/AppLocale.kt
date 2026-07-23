package com.abaye.derechkohanim.core

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.abaye.derechkohanim.data.model.Language

/**
 * Per-app language, independent of the device language (spec 14.3). On Android 13 and above
 * AppCompat forwards this to the platform LocaleManager; below that it recreates the
 * activity itself, which is why MainActivity extends AppCompatActivity.
 *
 * Layout direction follows the locale automatically, so nothing here touches direction.
 */
object AppLocale {

    fun apply(language: Language) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }

    fun current(): Language {
        val tag = AppCompatDelegate.getApplicationLocales().toLanguageTags()
        return if (tag.startsWith("en")) Language.EN else Language.HE
    }
}
