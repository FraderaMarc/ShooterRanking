package com.marcfradera.shooterranking.ui.vm

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.marcfradera.shooterranking.data.AccountRepository
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val repository: AccountRepository = AccountRepository()
) : ViewModel() {

    var currentEmail by mutableStateOf(
        repository.currentUserEmail()
    )
        private set

    var loading by mutableStateOf(false)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    var deletionRequestSent by mutableStateOf(false)
        private set

    fun requestAccountDeletion(language: String) {
        viewModelScope.launch {
            error = null
            deletionRequestSent = false
            loading = true

            try {
                repository.requestAccountDeletion(language)
                deletionRequestSent = true
            } catch (e: Exception) {
                error = e.message
            } finally {
                loading = false
            }
        }
    }

    fun clearDeletionRequestSent() {
        deletionRequestSent = false
    }

    fun signOut(onDone: () -> Unit) {
        error = null
        loading = true

        try {
            repository.signOut()
            currentEmail = ""
            onDone()
        } catch (e: Exception) {
            error = e.message
        } finally {
            loading = false
        }
    }
}
