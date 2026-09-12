package com.marcfradera.shooterranking.legal

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.ads.AdsConsentManager

object LegalDocuments {

    const val TERMS_VERSION = "1.0"
    const val PRIVACY_VERSION = "1.0"

    fun showLegalMenu(context: Context) {

        val activity =
            context as? Activity

        val adsConsentManager =
            activity?.let {
                AdsConsentManager(it)
            }

        val showAdPrivacyOptions =
            adsConsentManager
                ?.isPrivacyOptionsRequired() ==
                    true

        val items =
            buildList {

                add(
                    context.getString(
                        R.string.privacy_policy
                    )
                )

                add(
                    context.getString(
                        R.string.terms_and_conditions
                    )
                )

                if (showAdPrivacyOptions) {

                    add(
                        context.getString(
                            R.string.ad_privacy_options
                        )
                    )
                }
            }.toTypedArray()

        MaterialAlertDialogBuilder(context)
            .setTitle(
                R.string.legal_information
            )
            .setItems(items) {
                    _,
                    which ->

                when (which) {

                    0 ->
                        showPrivacyPolicy(
                            context
                        )

                    1 ->
                        showTerms(
                            context
                        )

                    2 ->
                        adsConsentManager
                            ?.showPrivacyOptions {
                                    errorMessage ->

                                if (
                                    !errorMessage
                                        .isNullOrBlank()
                                ) {

                                    Toast
                                        .makeText(
                                            context,
                                            errorMessage,
                                            Toast.LENGTH_LONG
                                        )
                                        .show()
                                }
                            }
                }
            }
            .setNegativeButton(
                R.string.cancel,
                null
            )
            .show()
    }

    fun showPrivacyPolicy(
        context: Context
    ) =
        showDocument(
            context,
            R.string.privacy_policy,
            R.string.privacy_policy_body
        )

    fun showTerms(
        context: Context
    ) =
        showDocument(
            context,
            R.string.terms_and_conditions,
            R.string.terms_conditions_body
        )

    private fun showDocument(
        context: Context,
        titleRes: Int,
        bodyRes: Int
    ) {

        MaterialAlertDialogBuilder(context)
            .setTitle(
                titleRes
            )
            .setMessage(
                context.getString(
                    bodyRes
                )
            )
            .setPositiveButton(
                R.string.close,
                null
            )
            .show()
    }
}
