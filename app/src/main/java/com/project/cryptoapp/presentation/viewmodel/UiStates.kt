package com.project.cryptoapp.presentation.viewmodel

import com.project.cryptoapp.domain.model.CryptoHistory

data class KeyGenerationUiState(
    val privateKey: String = "",
    val publicKey: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

data class EncryptUiState(
    val plaintext: String = "",
    val publicKey: String = "",
    val aad: String = "",
    val outputFormat: String = "Pretty JSON",
    val cipherText: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

data class DecryptUiState(
    val cipherText: String = "",
    val privateKey: String = "",
    val aad: String = "",
    val plaintext: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

data class SignUiState(
    val message: String = "",
    val privateKey: String = "",
    val signature: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

data class VerifyUiState(
    val message: String = "",
    val publicKey: String = "",
    val signatureR: String = "",
    val signatureS: String = "",
    val isValid: Boolean? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

data class HistoryUiState(
    val history: List<CryptoHistory> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

data class SettingsUiState(
    val enableHistory: Boolean = true,
    val defaultEncoding: String = "Text",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)
