package com.marcfradera.shooterranking.localization

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.legal.LegalDocuments

object AppSettingsDialogs {

    fun showSettings(
        context: Context,
        onLogout: () -> Unit
    ) {
        val items =
            arrayOf(
                AppLanguageManager.text(
                    context,
                    R.string.change_language
                ),
                AppLanguageManager.text(
                    context,
                    R.string.legal_information
                ),
                AppLanguageManager.text(
                    context,
                    R.string.logout
                )
            )

        MaterialAlertDialogBuilder(
            context
        )
            .setTitle(
                R.string.settings
            )
            .setItems(
                items
            ) { _, which ->

                when (which) {

                    0 -> {
                        showLanguagePicker(
                            context
                        )
                    }

                    1 -> {
                        LegalDocuments
                            .showLegalMenu(
                                context
                            )
                    }

                    2 -> {
                        onLogout()
                    }
                }
            }
            .setNegativeButton(
                R.string.cancel,
                null
            )
            .show()
    }

    fun showLanguagePicker(
        context: Context
    ) {
        val languages =
            AppLanguageManager
                .supportedLanguages

        val selectedIndex =
            AppLanguageManager
                .selectedLanguageIndex(
                    context
                )

        val adapter =
            LanguagePickerAdapter(
                context = context,
                languageTags =
                    languages.map {
                        it.tag
                    },
                selectedIndex =
                    selectedIndex
            )

        MaterialAlertDialogBuilder(
            context
        )
            .setTitle(
                R.string.select_language
            )
            .setAdapter(
                adapter
            ) { dialog, which ->

                val selected =
                    languages.getOrNull(
                        which
                    )

                if (selected != null) {

                    dialog.dismiss()

                    if (
                        selected.tag !=
                        AppLanguageManager
                            .currentLanguageTag(
                                context
                            )
                    ) {
                        AppLanguageManager
                            .setLanguage(
                                selected.tag
                            )
                    }
                }
            }
            .setNegativeButton(
                R.string.cancel,
                null
            )
            .show()
    }

    private class LanguagePickerAdapter(
        private val context: Context,
        private val languageTags: List<String>,
        private val selectedIndex: Int
    ) : BaseAdapter() {

        override fun getCount(): Int =
            languageTags.size

        override fun getItem(
            position: Int
        ): String =
            languageTags[position]

        override fun getItemId(
            position: Int
        ): Long =
            position.toLong()

        override fun getView(
            position: Int,
            convertView: View?,
            parent: ViewGroup?
        ): View {

            val languageTag =
                getItem(
                    position
                )

            val row =
                LinearLayout(
                    context
                ).apply {

                    orientation =
                        LinearLayout.HORIZONTAL

                    gravity =
                        Gravity.CENTER_VERTICAL

                    setPadding(
                        dp(20),
                        dp(10),
                        dp(12),
                        dp(10)
                    )

                    minimumHeight =
                        dp(54)
                }

            val flagView =
                LanguageFlagView(
                    context
                ).apply {

                    tagValue =
                        languageTag

                    layoutParams =
                        LinearLayout.LayoutParams(
                            dp(34),
                            dp(22)
                        ).apply {

                            marginEnd =
                                dp(14)
                        }
                }

            val languageName =
                TextView(
                    context
                ).apply {

                    text =
                        languageDisplayName(
                            languageTag
                        )

                    textSize =
                        16f

                    gravity =
                        Gravity.CENTER_VERTICAL

                    layoutParams =
                        LinearLayout.LayoutParams(
                            0,
                            ViewGroup.LayoutParams.WRAP_CONTENT,
                            1f
                        )
                }

            val selectedRadio =
                RadioButton(
                    context
                ).apply {

                    isChecked =
                        position ==
                                selectedIndex

                    isClickable =
                        false

                    isFocusable =
                        false
                }

            row.addView(
                flagView
            )

            row.addView(
                languageName
            )

            row.addView(
                selectedRadio
            )

            return row
        }

        private fun languageDisplayName(
            languageTag: String
        ): String =
            when (languageTag) {

                "ca" ->
                    AppLanguageManager.text(
                        context,
                        R.string.language_catalan
                    )

                "en" ->
                    AppLanguageManager.text(
                        context,
                        R.string.language_english
                    )

                "fr" ->
                    AppLanguageManager.text(
                        context,
                        R.string.language_french
                    )

                else ->
                    AppLanguageManager.text(
                        context,
                        R.string.language_spanish
                    )
            }

        private fun dp(
            value: Int
        ): Int =
            (
                    value *
                            context.resources
                                .displayMetrics
                                .density
                    )
                .toInt()
    }

    private class LanguageFlagView(
        context: Context
    ) : View(
        context
    ) {

        var tagValue: String =
            "es"
            set(value) {

                field =
                    value

                invalidate()
            }

        private val paint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            )

        private val borderPaint =
            Paint(
                Paint.ANTI_ALIAS_FLAG
            ).apply {

                style =
                    Paint.Style.STROKE

                strokeWidth =
                    resources
                        .displayMetrics
                        .density

                color =
                    Color.argb(
                        90,
                        0,
                        0,
                        0
                    )
            }

        override fun onDraw(
            canvas: Canvas
        ) {

            super.onDraw(
                canvas
            )

            val widthValue =
                width.toFloat()

            val heightValue =
                height.toFloat()

            if (
                widthValue <= 0f ||
                heightValue <= 0f
            ) {
                return
            }

            paint.style =
                Paint.Style.FILL

            when (
                tagValue
            ) {

                "ca" -> {

                    paint.color =
                        Color.rgb(
                            255,
                            213,
                            79
                        )

                    canvas.drawRect(
                        0f,
                        0f,
                        widthValue,
                        heightValue,
                        paint
                    )

                    val stripeHeight =
                        heightValue /
                                9f

                    paint.color =
                        Color.rgb(
                            211,
                            47,
                            47
                        )

                    listOf(
                        1,
                        3,
                        5,
                        7
                    ).forEach {
                            stripe ->

                        canvas.drawRect(
                            0f,
                            stripeHeight *
                                    stripe,
                            widthValue,
                            stripeHeight *
                                    (stripe + 1),
                            paint
                        )
                    }
                }

                "en" -> {

                    paint.color =
                        Color.rgb(
                            1,
                            33,
                            105
                        )

                    canvas.drawRect(
                        0f,
                        0f,
                        widthValue,
                        heightValue,
                        paint
                    )

                    paint.style =
                        Paint.Style.STROKE

                    paint.strokeCap =
                        Paint.Cap.SQUARE

                    paint.color =
                        Color.WHITE

                    paint.strokeWidth =
                        heightValue *
                                0.20f

                    canvas.drawLine(
                        0f,
                        0f,
                        widthValue,
                        heightValue,
                        paint
                    )

                    canvas.drawLine(
                        widthValue,
                        0f,
                        0f,
                        heightValue,
                        paint
                    )

                    paint.color =
                        Color.rgb(
                            200,
                            16,
                            46
                        )

                    paint.strokeWidth =
                        heightValue *
                                0.09f

                    canvas.drawLine(
                        0f,
                        0f,
                        widthValue,
                        heightValue,
                        paint
                    )

                    canvas.drawLine(
                        widthValue,
                        0f,
                        0f,
                        heightValue,
                        paint
                    )

                    paint.style =
                        Paint.Style.FILL

                    paint.color =
                        Color.WHITE

                    canvas.drawRect(
                        widthValue *
                                0.40f,
                        0f,
                        widthValue *
                                0.60f,
                        heightValue,
                        paint
                    )

                    canvas.drawRect(
                        0f,
                        heightValue *
                                0.34f,
                        widthValue,
                        heightValue *
                                0.66f,
                        paint
                    )

                    paint.color =
                        Color.rgb(
                            200,
                            16,
                            46
                        )

                    canvas.drawRect(
                        widthValue *
                                0.455f,
                        0f,
                        widthValue *
                                0.545f,
                        heightValue,
                        paint
                    )

                    canvas.drawRect(
                        0f,
                        heightValue *
                                0.42f,
                        widthValue,
                        heightValue *
                                0.58f,
                        paint
                    )
                }

                "fr" -> {

                    val third =
                        widthValue /
                                3f

                    paint.color =
                        Color.rgb(
                            0,
                            85,
                            164
                        )

                    canvas.drawRect(
                        0f,
                        0f,
                        third,
                        heightValue,
                        paint
                    )

                    paint.color =
                        Color.WHITE

                    canvas.drawRect(
                        third,
                        0f,
                        third *
                                2f,
                        heightValue,
                        paint
                    )

                    paint.color =
                        Color.rgb(
                            239,
                            65,
                            53
                        )

                    canvas.drawRect(
                        third *
                                2f,
                        0f,
                        widthValue,
                        heightValue,
                        paint
                    )
                }

                else -> {

                    paint.color =
                        Color.rgb(
                            170,
                            21,
                            27
                        )

                    canvas.drawRect(
                        0f,
                        0f,
                        widthValue,
                        heightValue,
                        paint
                    )

                    paint.color =
                        Color.rgb(
                            241,
                            191,
                            0
                        )

                    canvas.drawRect(
                        0f,
                        heightValue *
                                0.25f,
                        widthValue,
                        heightValue *
                                0.75f,
                        paint
                    )
                }
            }

            canvas.drawRect(
                0.5f,
                0.5f,
                widthValue -
                        0.5f,
                heightValue -
                        0.5f,
                borderPaint
            )
        }
    }
}