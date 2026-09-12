package com.marcfradera.shooterranking.ui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.LinearLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Añade únicamente el espacio real ocupado por la barra de estado.
 *
 * No usa un margen fijo: si el dispositivo no necesita inset superior,
 * no se añade espacio adicional.
 */
class StatusBarInsetLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(
    context,
    attrs,
    defStyleAttr
) {

    private val initialPaddingLeft = paddingLeft
    private val initialPaddingTop = paddingTop
    private val initialPaddingRight = paddingRight
    private val initialPaddingBottom = paddingBottom

    init {

        ViewCompat.setOnApplyWindowInsetsListener(
            this
        ) {
                view,
                insets ->

            /*
             * Leemos los insets de la ventana raíz para que funcione
             * también aunque un contenedor padre gestione sus propios
             * fitsSystemWindows.
             */
            val rootInsets =
                ViewCompat.getRootWindowInsets(
                    view.rootView
                ) ?: insets

            val statusBarTop =
                rootInsets
                    .getInsets(
                        WindowInsetsCompat.Type.statusBars()
                    )
                    .top

            view.setPadding(
                initialPaddingLeft,
                initialPaddingTop + statusBarTop,
                initialPaddingRight,
                initialPaddingBottom
            )

            /*
             * No consumimos los insets para no modificar el
             * comportamiento de ninguna otra pantalla.
             */
            insets
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        ViewCompat.requestApplyInsets(
            this
        )
    }
}
