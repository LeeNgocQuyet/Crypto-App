package com.project.cryptoapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.cryptoapp.domain.model.CryptoHistory
import com.project.cryptoapp.domain.model.DigitalSignature
import com.project.cryptoapp.domain.model.OperationStatus
import com.project.cryptoapp.domain.model.OperationType
import com.project.cryptoapp.domain.usecase.ClearHistoryUseCase
import com.project.cryptoapp.domain.usecase.DecryptMessageUseCase
import com.project.cryptoapp.domain.usecase.DeleteHistoryByIdUseCase
import com.project.cryptoapp.domain.usecase.EncryptMessageUseCase
import com.project.cryptoapp.domain.usecase.GenerateKeyPairUseCase
import com.project.cryptoapp.domain.usecase.GetHistoryUseCase
import com.project.cryptoapp.domain.usecase.SaveHistoryUseCase
import com.project.cryptoapp.domain.usecase.SignMessageUseCase
import com.project.cryptoapp.domain.usecase.VerifySignatureUseCase
import com.project.cryptoapp.util.AppSettingsStore
import com.project.cryptoapp.util.CryptoSessionStore
import com.project.cryptoapp.util.toDisplayString
import com.project.cryptoapp.util.validateCipherText
import com.project.cryptoapp.util.validatePayload
import com.project.cryptoapp.util.validatePrivateKey
import com.project.cryptoapp.util.validatePublicKey
import com.project.cryptoapp.util.validateSignaturePart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class KeyGenerationViewModel(
    private val generateKeyPairUseCase: GenerateKeyPairUseCase,
    private val saveHistoryUseCase: SaveHistoryUseCase,
    private val sessionStore: CryptoSessionStore,
    private val settingsStore: AppSettingsStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(KeyGenerationUiState())
    val uiState: StateFlow<KeyGenerationUiState> = _uiState.asStateFlow()

    fun generateKeyPair() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            runCatching { generateKeyPairUseCase() }
                .onSuccess { keyPair ->
                    val publicKey = keyPair.publicKey.toDisplayString()
                    sessionStore.setKeyPair(
                        privateKey = keyPair.privateKey,
                        publicKey = publicKey,
                    )
                    _uiState.update {
                        it.copy(
                            privateKey = keyPair.privateKey,
                            publicKey = publicKey,
                            isLoading = false,
                            successMessage = "Key pair generated",
                        )
                    }
                    saveHistory(OperationType.KEY_GENERATION, "Generate ECC-512 key pair", publicKey)
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Key generation failed")
                    }
                }
        }
    }

    private suspend fun saveHistory(type: OperationType, input: String, output: String) {
        if (!settingsStore.state.value.saveHistory) return
        saveHistoryUseCase(
            CryptoHistory(
                operationType = type,
                inputText = input,
                outputText = output,
                status = OperationStatus.SUCCESS,
                timestamp = System.currentTimeMillis(),
            ),
        )
    }
}

class EncryptViewModel(
    private val encryptMessageUseCase: EncryptMessageUseCase,
    private val saveHistoryUseCase: SaveHistoryUseCase,
    private val sessionStore: CryptoSessionStore,
    private val settingsStore: AppSettingsStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EncryptUiState())
    val uiState: StateFlow<EncryptUiState> = _uiState.asStateFlow()

    fun onPlaintextChange(value: String) = _uiState.update { it.copy(plaintext = value) }
    fun onPublicKeyChange(value: String) = _uiState.update { it.copy(publicKey = value) }

    init {
        val publicKey = sessionStore.state.value.publicKey
        if (publicKey.isNotBlank()) {
            _uiState.update { it.copy(publicKey = publicKey) }
        }
    }

    fun useLatestPublicKey() {
        val publicKey = sessionStore.state.value.publicKey
        if (publicKey.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Generate a key pair first") }
        } else {
            _uiState.update { it.copy(publicKey = publicKey, errorMessage = null, successMessage = "Latest public key loaded") }
        }
    }

    fun encrypt() {
        val state = _uiState.value
        val validationError = when {
            else -> validatePayload("Plaintext", state.plaintext, settingsStore.state.value.defaultEncoding)
                ?: validatePublicKey(state.publicKey)
        }
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            runCatching { encryptMessageUseCase(state.plaintext, state.publicKey).toDisplayString() }
                .onSuccess { output ->
                    sessionStore.setCipherText(output)
                    _uiState.update {
                        it.copy(cipherText = output, isLoading = false, successMessage = "Message encrypted")
                    }
                    saveHistory(OperationType.ENCRYPT, state.plaintext, output, OperationStatus.SUCCESS)
                }
                .onFailure { error ->
                    val message = error.message ?: "Encryption failed"
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                    saveHistory(OperationType.ENCRYPT, state.plaintext, message, OperationStatus.FAILED)
                }
        }
    }

    private suspend fun saveHistory(
        type: OperationType,
        input: String,
        output: String,
        status: OperationStatus,
    ) {
        if (!settingsStore.state.value.saveHistory) return
        saveHistoryUseCase(CryptoHistory(operationType = type, inputText = input, outputText = output, status = status, timestamp = System.currentTimeMillis()))
    }
}

class DecryptViewModel(
    private val decryptMessageUseCase: DecryptMessageUseCase,
    private val saveHistoryUseCase: SaveHistoryUseCase,
    private val sessionStore: CryptoSessionStore,
    private val settingsStore: AppSettingsStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DecryptUiState())
    val uiState: StateFlow<DecryptUiState> = _uiState.asStateFlow()

    fun onCipherTextChange(value: String) = _uiState.update { it.copy(cipherText = value) }
    fun onPrivateKeyChange(value: String) = _uiState.update { it.copy(privateKey = value) }

    init {
        val session = sessionStore.state.value
        _uiState.update {
            it.copy(
                cipherText = session.cipherText,
                privateKey = session.privateKey,
            )
        }
    }

    fun useLatestPrivateKey() {
        val privateKey = sessionStore.state.value.privateKey
        if (privateKey.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Generate a key pair first") }
        } else {
            _uiState.update { it.copy(privateKey = privateKey, errorMessage = null, successMessage = "Latest private key loaded") }
        }
    }

    fun useLatestCipherText() {
        val cipherText = sessionStore.state.value.cipherText
        if (cipherText.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Encrypt a message first") }
        } else {
            _uiState.update { it.copy(cipherText = cipherText, errorMessage = null, successMessage = "Latest ciphertext loaded") }
        }
    }

    fun decrypt() {
        val state = _uiState.value
        val validationError = validateCipherText(state.cipherText) ?: validatePrivateKey(state.privateKey)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            runCatching { decryptMessageUseCase(state.cipherText, state.privateKey) }
                .onSuccess { output ->
                    _uiState.update {
                        it.copy(plaintext = output, isLoading = false, successMessage = "Ciphertext decrypted")
                    }
                    saveHistory(OperationType.DECRYPT, state.cipherText, output, OperationStatus.SUCCESS)
                }
                .onFailure { error ->
                    val message = error.message ?: "Decryption failed"
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                    saveHistory(OperationType.DECRYPT, state.cipherText, message, OperationStatus.FAILED)
                }
        }
    }

    private suspend fun saveHistory(type: OperationType, input: String, output: String, status: OperationStatus) {
        if (!settingsStore.state.value.saveHistory) return
        saveHistoryUseCase(CryptoHistory(operationType = type, inputText = input, outputText = output, status = status, timestamp = System.currentTimeMillis()))
    }
}

class SignViewModel(
    private val signMessageUseCase: SignMessageUseCase,
    private val saveHistoryUseCase: SaveHistoryUseCase,
    private val sessionStore: CryptoSessionStore,
    private val settingsStore: AppSettingsStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignUiState())
    val uiState: StateFlow<SignUiState> = _uiState.asStateFlow()

    fun onMessageChange(value: String) = _uiState.update { it.copy(message = value) }
    fun onPrivateKeyChange(value: String) = _uiState.update { it.copy(privateKey = value) }

    init {
        val privateKey = sessionStore.state.value.privateKey
        if (privateKey.isNotBlank()) {
            _uiState.update { it.copy(privateKey = privateKey) }
        }
    }

    fun useLatestPrivateKey() {
        val privateKey = sessionStore.state.value.privateKey
        if (privateKey.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Generate a key pair first") }
        } else {
            _uiState.update { it.copy(privateKey = privateKey, errorMessage = null, successMessage = "Latest private key loaded") }
        }
    }

    fun sign() {
        val state = _uiState.value
        val validationError = when {
            else -> validatePayload("Message", state.message, settingsStore.state.value.defaultEncoding)
                ?: validatePrivateKey(state.privateKey)
        }
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            runCatching { signMessageUseCase(state.message, state.privateKey) }
                .onSuccess { signature ->
                    val output = signature.toDisplayString()
                    sessionStore.setSignature(signature.r, signature.s)
                    _uiState.update {
                        it.copy(signature = output, isLoading = false, successMessage = "Message signed")
                    }
                    saveHistory(OperationType.SIGN, state.message, output, OperationStatus.SUCCESS)
                }
                .onFailure { error ->
                    val message = error.message ?: "Signing failed"
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                    saveHistory(OperationType.SIGN, state.message, message, OperationStatus.FAILED)
                }
        }
    }

    private suspend fun saveHistory(type: OperationType, input: String, output: String, status: OperationStatus) {
        if (!settingsStore.state.value.saveHistory) return
        saveHistoryUseCase(CryptoHistory(operationType = type, inputText = input, outputText = output, status = status, timestamp = System.currentTimeMillis()))
    }
}

class VerifyViewModel(
    private val verifySignatureUseCase: VerifySignatureUseCase,
    private val saveHistoryUseCase: SaveHistoryUseCase,
    private val sessionStore: CryptoSessionStore,
    private val settingsStore: AppSettingsStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(VerifyUiState())
    val uiState: StateFlow<VerifyUiState> = _uiState.asStateFlow()

    fun onMessageChange(value: String) = _uiState.update { it.copy(message = value) }
    fun onPublicKeyChange(value: String) = _uiState.update { it.copy(publicKey = value) }
    fun onSignatureRChange(value: String) = _uiState.update { it.copy(signatureR = value) }
    fun onSignatureSChange(value: String) = _uiState.update { it.copy(signatureS = value) }

    init {
        val session = sessionStore.state.value
        _uiState.update {
            it.copy(
                publicKey = session.publicKey,
                signatureR = session.signatureR,
                signatureS = session.signatureS,
            )
        }
    }

    fun useLatestPublicKey() {
        val publicKey = sessionStore.state.value.publicKey
        if (publicKey.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Generate a key pair first") }
        } else {
            _uiState.update { it.copy(publicKey = publicKey, errorMessage = null, successMessage = "Latest public key loaded") }
        }
    }

    fun useLatestSignature() {
        val session = sessionStore.state.value
        if (session.signatureR.isBlank() || session.signatureS.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Sign a message first") }
        } else {
            _uiState.update {
                it.copy(
                    signatureR = session.signatureR,
                    signatureS = session.signatureS,
                    errorMessage = null,
                    successMessage = "Latest signature loaded",
                )
            }
        }
    }

    fun verify() {
        val state = _uiState.value
        val validationError = when {
            else -> validatePayload("Message", state.message, settingsStore.state.value.defaultEncoding)
                ?: validatePublicKey(state.publicKey)
                ?: validateSignaturePart("r", state.signatureR)
                ?: validateSignaturePart("s", state.signatureS)
        }
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val signature = DigitalSignature(state.signatureR, state.signatureS)
            runCatching { verifySignatureUseCase(state.message, state.publicKey, signature) }
                .onSuccess { isValid ->
                    val output = if (isValid) "Signature is valid" else "Signature is invalid"
                    _uiState.update {
                        it.copy(
                            isValid = isValid,
                            isLoading = false,
                            successMessage = output,
                        )
                    }
                    saveHistory(OperationType.VERIFY, state.message, output, OperationStatus.SUCCESS)
                }
                .onFailure { error ->
                    val message = error.message ?: "Verification failed"
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                    saveHistory(OperationType.VERIFY, state.message, message, OperationStatus.FAILED)
                }
        }
    }

    private suspend fun saveHistory(type: OperationType, input: String, output: String, status: OperationStatus) {
        if (!settingsStore.state.value.saveHistory) return
        saveHistoryUseCase(CryptoHistory(operationType = type, inputText = input, outputText = output, status = status, timestamp = System.currentTimeMillis()))
    }
}

class HistoryViewModel(
    getHistoryUseCase: GetHistoryUseCase,
    private val deleteHistoryByIdUseCase: DeleteHistoryByIdUseCase,
    private val clearHistoryUseCase: ClearHistoryUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            getHistoryUseCase()
                .catch { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Unable to load history")
                    }
                }
                .collect { history ->
                    _uiState.update { it.copy(history = history, isLoading = false, errorMessage = null) }
                }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            runCatching { clearHistoryUseCase() }
                .onSuccess {
                    _uiState.update { it.copy(successMessage = "History cleared", errorMessage = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "Unable to clear history") }
                }
        }
    }

    fun deleteHistoryById(id: Long) {
        viewModelScope.launch {
            runCatching { deleteHistoryByIdUseCase(id) }
                .onSuccess {
                    _uiState.update { it.copy(successMessage = "History item deleted", errorMessage = null) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "Unable to delete history item") }
                }
        }
    }

}

class SettingsViewModel(
    private val clearHistoryUseCase: ClearHistoryUseCase,
    private val settingsStore: AppSettingsStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        SettingsUiState(
            enableHistory = settingsStore.state.value.saveHistory,
            defaultEncoding = settingsStore.state.value.defaultEncoding,
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setHistoryEnabled(enabled: Boolean) {
        settingsStore.setSaveHistory(enabled)
        _uiState.update { it.copy(enableHistory = enabled, successMessage = "Settings updated") }
    }

    fun setDefaultEncoding(encoding: String) {
        settingsStore.setDefaultEncoding(encoding)
        _uiState.update { it.copy(defaultEncoding = encoding, successMessage = "Default encoding set to $encoding") }
    }

    fun clearHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            runCatching { clearHistoryUseCase() }
                .onSuccess {
                    _uiState.update {
                        it.copy(isLoading = false, successMessage = "History cleared")
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Unable to clear history")
                    }
                }
        }
    }

    fun resetAppData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            runCatching {
                clearHistoryUseCase()
                settingsStore.setDefaultEncoding("Text")
                settingsStore.setSaveHistory(true)
            }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            defaultEncoding = "Text",
                            enableHistory = true,
                            isLoading = false,
                            successMessage = "App data reset",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.message ?: "Unable to reset app data")
                    }
                }
        }
    }
}
