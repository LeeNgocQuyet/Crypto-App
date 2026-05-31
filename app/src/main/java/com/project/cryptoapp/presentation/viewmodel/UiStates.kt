package com.project.cryptoapp.presentation.viewmodel

import com.project.cryptoapp.domain.model.CryptoHistory
import com.project.cryptoapp.domain.model.ECCurveParams

data class KeyGenerationUiState(
    val privateKey: String = "",
    val publicKey: String = "",
    val hasProtectedKey: Boolean = false,
    val keyExportPayload: String = "",
    val identityLabel: String = "",
    val identityProofPayload: String = "",
    val publicKeyAuthResult: String = "",
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

data class FileToolsUiState(
    val publicKey: String = "",
    val privateKey: String = "",
    val encryptedFilePayload: String = "",
    val decryptedFileBytes: ByteArray? = null,
    val fileSignaturePayload: String = "",
    val verificationResult: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as FileToolsUiState
        return publicKey == other.publicKey &&
            privateKey == other.privateKey &&
            encryptedFilePayload == other.encryptedFilePayload &&
            decryptedFileBytes.contentEqualsNullable(other.decryptedFileBytes) &&
            fileSignaturePayload == other.fileSignaturePayload &&
            verificationResult == other.verificationResult &&
            isLoading == other.isLoading &&
            errorMessage == other.errorMessage &&
            successMessage == other.successMessage
    }

    override fun hashCode(): Int {
        var result = publicKey.hashCode()
        result = 31 * result + privateKey.hashCode()
        result = 31 * result + encryptedFilePayload.hashCode()
        result = 31 * result + (decryptedFileBytes?.contentHashCode() ?: 0)
        result = 31 * result + fileSignaturePayload.hashCode()
        result = 31 * result + verificationResult.hashCode()
        result = 31 * result + isLoading.hashCode()
        result = 31 * result + (errorMessage?.hashCode() ?: 0)
        result = 31 * result + (successMessage?.hashCode() ?: 0)
        return result
    }
}

data class HistoryUiState(
    val history: List<CryptoHistory> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

private fun ByteArray?.contentEqualsNullable(other: ByteArray?): Boolean =
    when {
        this == null && other == null -> true
        this == null || other == null -> false
        else -> contentEquals(other)
    }

data class SettingsUiState(
    val enableHistory: Boolean = true,
    val defaultEncoding: String = "Text",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

data class CurveParametersUiState(
    val params: ECCurveParams,
    val status: String,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
)
