package com.marcfradera.shooterranking.ui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import java.util.Locale

/**
 * Mantiene el formato visual del título equivalente al TopAppBar
 * usado en las pantallas de estadísticas.
 *
 * TemporadasFragment y EquipsFragment siguen enviando actualmente
 * el texto en mayúsculas. Esta View normaliza únicamente ese título
 * para no tocar la lógica de ninguno de los dos fragments.
 */
class StatsStyleTitleTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int =
        android.R.attr.textViewStyle
) : AppCompatTextView(
    context,
    attrs,
    defStyleAttr
) {

    override fun setText(
        text: CharSequence?,
        type: TextView.BufferType?
    ) {

        super.setText(
            normalizeAllCapsTitle(
                text
            ),
            type
        )
    }

    private fun normalizeAllCapsTitle(
        value: CharSequence?
    ): CharSequence? {

        if (value == null) {
            return null
        }

        val original =
            value.toString()

        val letters =
            original.filter {
                it.isLetter()
            }

        /*
         * Si el texto no está enteramente en mayúsculas,
         * se deja exactamente como llega.
         */
        if (
            letters.isEmpty() ||
            letters.any {
                it.isLowerCase()
            }
        ) {
            return value
        }

        val locale =
            currentLocale()

        val lower =
            original.lowercase(
                locale
            )

        return lower.replaceFirstChar {
                firstCharacter ->

            if (
                firstCharacter.isLowerCase()
            ) {

                firstCharacter.titlecase(
                    locale
                )

            } else {

                firstCharacter.toString()
            }
        }
    }

    private fun currentLocale(): Locale {

        val locales =
            resources
                .configuration
                .locales

        return if (
            !locales.isEmpty
        ) {
            locales[0]
        } else {
            Locale.getDefault()
        }
    }
}
