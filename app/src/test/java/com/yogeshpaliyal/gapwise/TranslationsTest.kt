package com.yogeshpaliyal.gapwise

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

/**
 * Formats every string and plural in every shipped language, with the same arguments the English
 * text expects, so a broken placeholder in a translation fails here instead of crashing the app.
 */
@RunWith(RobolectricTestRunner::class)
class TranslationsTest {
    private val languages = listOf("hi", "es", "pt-BR", "de", "fr", "id", "ja")
    private val app = ApplicationProvider.getApplicationContext<Context>()

    private fun inLocale(tag: String): Context =
        app.createConfigurationContext(Configuration(app.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) })

    private val placeholder = Regex("%(\\d+\\$)?([sd])")

    /** Arguments matching the English text: a string for %s, a number for %d. */
    private fun argsFor(english: String): Array<Any> =
        placeholder.findAll(english)
            .map { (it.groupValues[1].removeSuffix("$").toIntOrNull() ?: 1) to it.groupValues[2] }
            .distinct().sortedBy { it.first }
            .map { if (it.second == "d") 3 else "x" }.toList().toTypedArray()

    private fun placeholders(text: String) = placeholder.findAll(text).map { it.value }.toSet()

    private val strings = R.string::class.java.fields.map { it.name to it.getInt(null) }
    private val plurals = R.plurals::class.java.fields.map { it.name to it.getInt(null) }

    @Test
    fun everyLanguageFormatsEveryString() {
        val en = inLocale("en")
        for (tag in languages) {
            val ctx = inLocale(tag)
            for ((name, id) in strings) {
                val english = en.resources.getString(id)
                val text = ctx.resources.getString(id)
                assertEquals("$tag/$name placeholders", placeholders(english), placeholders(text))
                ctx.getString(id, *argsFor(english))
            }
            for ((name, id) in plurals) {
                val english = en.resources.getQuantityString(id, 3)
                for (n in listOf(0, 1, 2, 5, 21)) {
                    ctx.resources.getQuantityString(id, n, *argsFor(english).map { if (it is Int) n else it }.toTypedArray())
                }
                assertNotEquals("$tag/$name is empty", "", ctx.resources.getQuantityString(id, 3, 3))
            }
        }
    }

    @Test
    fun translationsAreActuallyUsed() {
        assertEquals("नया इवेंट", inLocale("hi").getString(R.string.new_event))
        assertEquals("Neues Ereignis", inLocale("de").getString(R.string.new_event))
        assertEquals("新しい項目", inLocale("ja").getString(R.string.new_event))
        assertEquals("Acara baru", inLocale("id").getString(R.string.new_event))
        assertEquals("Novo evento", inLocale("pt-BR").getString(R.string.new_event))
    }
}
