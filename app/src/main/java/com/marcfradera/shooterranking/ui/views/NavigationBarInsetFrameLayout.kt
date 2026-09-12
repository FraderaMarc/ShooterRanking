package com.marcfradera.shooterranking.ui.views

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Mantiene el banner por encima de la zona de navegación del sistema.
 *
 * El espacio inferior es dinámico:
 * - navegación con botones: reserva exactamente la altura indicada por Android;
 * - navegación por gestos: reserva únicamente su zona segura;
 * - sin inset inferior: no añade ningún hueco.
 */
class NavigationBarInsetFrameLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(
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

            val rootInsets =
                ViewCompat.getRootWindowInsets(
                    view.rootView
                ) ?: insets

            val navigationBarBottom =
                rootInsets
                    .getInsets(
                        WindowInsetsCompat.Type.navigationBars()
                    )
                    .bottom

            view.setPadding(
                initialPaddingLeft,
                initialPaddingTop,
                initialPaddingRight,
                initialPaddingBottom +
                        navigationBarBottom
            )

            /*
             * No consumimos los insets: solo protegemos el banner.
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
