package com.marcfradera.shooterranking.data

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestore
import com.marcfradera.shooterranking.R
import com.marcfradera.shooterranking.localization.AppLanguageManager
import kotlinx.coroutines.tasks.await
import java.util.Locale

class AccountRepository {

    private val auth
        get() = FirebaseProvider.auth

    private val db: FirebaseFirestore
        get() = FirebaseProvider.firestore

    fun currentUserEmail(): String =
        auth.currentUser?.email.orEmpty()

    suspend fun deleteAccount(password: String) {
        if (password.isBlank()) {
            throw IllegalArgumentException(
                AppLanguageManager.text(
                    R.string.account_delete_password_required
                )
            )
        }

        val user = auth.currentUser
            ?: throw IllegalStateException(
                AppLanguageManager.text(
                    R.string.error_no_authenticated_user
                )
            )

        val email = user.email
            ?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException(
                AppLanguageManager.text(
                    R.string.account_delete_failed
                )
            )

        try {
            val credential =
                EmailAuthProvider.getCredential(
                    email,
                    password
                )

            user.reauthenticate(
                credential
            ).await()

            deleteUserData(
                user.uid
            )

            user.delete().await()
        } catch (e: IllegalArgumentException) {
            throw e
        } catch (e: FirebaseNetworkException) {
            throw IllegalStateException(
                AppLanguageManager.text(
                    R.string.error_no_internet
                )
            )
        } catch (e: FirebaseAuthException) {
            val raw =
                e.message.orEmpty()

            val invalidPassword =
                e.errorCode == "ERROR_WRONG_PASSWORD" ||
                    e.errorCode == "ERROR_INVALID_CREDENTIAL" ||
                    raw.contains(
                        "INVALID_LOGIN_CREDENTIALS",
                        ignoreCase = true
                    ) ||
                    raw.contains(
                        "password is invalid",
                        ignoreCase = true
                    )

            if (invalidPassword) {
                throw IllegalStateException(
                    AppLanguageManager.text(
                        R.string.account_delete_wrong_password
                    )
                )
            }

            throw IllegalStateException(
                AppLanguageManager.text(
                    R.string.account_delete_failed
                )
            )
        } catch (_: Exception) {
            throw IllegalStateException(
                AppLanguageManager.text(
                    R.string.account_delete_failed
                )
            )
        }
    }

    private suspend fun deleteUserData(
        uid: String
    ) {
        val userRef =
            db.collection("users")
                .document(uid)

        val userSnapshot =
            userRef
                .get()
                .await()

        val usernameDocumentId =
            userSnapshot
                .getString("usernameLower")
                ?.takeIf { it.isNotBlank() }
                ?: userSnapshot
                    .getString("username")
                    ?.trim()
                    ?.lowercase(Locale.ROOT)
                    ?.takeIf { it.isNotBlank() }

        // These are the user-owned collections used by Shooter Ranking.
        // Delete them while the account is still authenticated so the
        // existing Firestore rules continue protecting all operations.
        listOf(
            "sessions",
            "jugadors",
            "equips",
            "temporades"
        ).forEach { collectionName ->
            deleteOwnedDocuments(
                collectionName = collectionName,
                uid = uid
            )
        }

        if (usernameDocumentId != null) {
            db.collection("usernames")
                .document(usernameDocumentId)
                .delete()
                .await()
        }

        userRef
            .delete()
            .await()
    }

    private suspend fun deleteOwnedDocuments(
        collectionName: String,
        uid: String
    ) {
        while (true) {
            val documents =
                db.collection(collectionName)
                    .whereEqualTo(
                        "userId",
                        uid
                    )
                    .limit(400)
                    .get()
                    .await()
                    .documents

            if (documents.isEmpty()) {
                return
            }

            val batch =
                db.batch()

            documents.forEach { document ->
                batch.delete(
                    document.reference
                )
            }

            batch.commit().await()
        }
    }

    fun signOut() {
        auth.signOut()
    }
}
