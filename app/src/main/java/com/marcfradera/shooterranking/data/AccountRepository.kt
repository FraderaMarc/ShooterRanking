package com.marcfradera.shooterranking.data

import com.google.firebase.functions.FirebaseFunctions
import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.localization.AppLanguageManager
import kotlinx.coroutines.tasks.await

class AccountRepository {

    private val auth
        get() = FirebaseProvider.auth

    private val functions: FirebaseFunctions
        get() = FirebaseFunctions.getInstance(
            FirebaseProvider.app,
            "europe-west1"
        )

    fun currentUserEmail(): String =
        auth.currentUser?.email.orEmpty()

    suspend fun requestAccountDeletion(language: String) {
        val user = auth.currentUser
            ?: throw IllegalStateException(
                AppLanguageManager.text(
                    R.string.error_no_authenticated_user
                )
            )

        try {
            user.reload().await()

            if (!user.isEmailVerified) {
                throw IllegalStateException(
                    AppLanguageManager.text(
                        R.string.account_delete_email_must_be_verified
                    )
                )
            }

            val normalizedLanguage = language
                .lowercase()
                .takeIf { it in setOf("es", "ca", "en", "fr") }
                ?: "es"

            functions
                .getHttpsCallable("requestAccountDeletion")
                .call(
                    mapOf(
                        "language" to normalizedLanguage
                    )
                )
                .await()
        } catch (e: IllegalStateException) {
            throw e
        } catch (_: Exception) {
            throw IllegalStateException(
                AppLanguageManager.text(
                    R.string.account_delete_request_failed
                )
            )
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
