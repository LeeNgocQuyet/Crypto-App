package com.project.cryptoapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.cryptoapp.domain.model.CryptoHistory
import com.project.cryptoapp.domain.model.DigitalSignature
import com.project.cryptoapp.domain.model.OperationStatus
import com.project.cryptoapp.domain.model.OperationType
import com.project.cryptoapp.domain.usecase.ClearHistoryUseCase
import com.project.cryptoapp.domain.usecase.DecryptMessageUseCase
import com.project.cryptoapp.domain.usecase.EncryptMessageUseCase
import com.project.cryptoapp.domain.usecase.GenerateKeyPairUseCase
import com.project.cryptoapp.domain.usecase.GetHistoryUseCase
import com.project.cryptoapp.domain.usecase.SaveHistoryUseCase
import com.project.cryptoapp.domain.usecase.SignMessageUseCase
import com.project.cryptoapp.domain.usecase.VerifySignatureUseCase
import com.project.cryptoapp.util.toDisplayString
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class KeyGenerationViewModel(
    private val generateKeyPairUseCase: GenerateKeyPairUseCase,
    private val saveHistoryUseCase: SaveHistoryUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(KeyGenerationUiState())
    val uiState: StateFlow<KeyGenerationUiState> = _uiState.asStateFlow()

    fun generateKeyPair() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            runCatching { generateKeyPairUseCase() }
                .onSuccess { keyPair ->
                    val publicKey = keyPair.publicKey.toDisplayString()
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
) : ViewModel() {
    private val _uiState = MutableStateFlow(EncryptUiState())
    val uiState: StateFlow<EncryptUiState> = _uiState.asStateFlow()

    fun onPlaintextChange(value: String) = _uiState.update { it.copy(plaintext = value) }
    fun onPublicKeyChange(value: String) = _uiState.update { it.copy(publicKey = value) }

    fun encrypt() {
        val state = _uiState.value
        if (state.plaintext.isBlank() || state.publicKey.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Plaintext and public key are required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            runCatching { encryptMessageUseCase(state.plaintext, state.publicKey).toDisplayString() }
                .onSuccess { output ->
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
        saveHistoryUseCase(CryptoHistory(operationType = type, inputText = input, outputText = output, status = status, timestamp = System.currentTimeMillis()))
    }
}

class DecryptViewModel(
    private val decryptMessageUseCase: DecryptMessageUseCase,
    private val saveHistoryUseCase: SaveHistoryUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(DecryptUiState())
    val uiState: StateFlow<DecryptUiState> = _uiState.asStateFlow()

    fun onCipherTextChange(value: String) = _uiState.update { it.copy(cipherText = value) }
    fun onPrivateKeyChange(value: String) = _uiState.update { it.copy(privateKey = value) }

    fun decrypt() {
        val state = _uiState.value
        if (state.cipherText.isBlank() || state.privateKey.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Ciphertext and private key are required") }
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
        saveHistoryUseCase(CryptoHistory(operationType = type, inputText = input, outputText = output, status = status, timestamp = System.currentTimeMillis()))
    }
}

class SignViewModel(
    private val signMessageUseCase: SignMessageUseCase,
    private val saveHistoryUseCase: SaveHistoryUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(SignUiState())
    val uiState: StateFlow<SignUiState> = _uiState.asStateFlow()

    fun onMessageChange(value: String) = _uiState.update { it.copy(message = value) }
    fun onPrivateKeyChange(value: String) = _uiState.update { it.copy(privateKey = value) }

    fun sign() {
        val state = _uiState.value
        if (state.message.isBlank() || state.privateKey.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Message and private key are required") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            runCatching { signMessageUseCase(state.message, state.privateKey).toDisplayString() }
                .onSuccess { output ->
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
        saveHistoryUseCase(CryptoHistory(operationType = type, inputText = input, outputText = output, status = status, timestamp = System.currentTimeMillis()))
    }
}

class VerifyViewModel(
    private val verifySignatureUseCase: VerifySignatureUseCase,
    private val saveHistoryUseCase: SaveHistoryUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(VerifyUiState())
    val uiState: StateFlow<VerifyUiState> = _uiState.asStateFlow()

    fun onMessageChange(value: String) = _uiState.update { it.copy(message = value) }
    fun onPublicKeyChange(value: String) = _uiState.update { it.copy(publicKey = value) }
    fun onSignatureRChange(value: String) = _uiState.update { it.copy(signatureR = value) }
    fun onSignatureSChange(value: String) = _uiState.update { it.copy(signatureS = value) }

    fun verify() {
        val state = _uiState.value
        if (state.message.isBlank() || state.publicKey.isBlank() || state.signatureR.isBlank() || state.signatureS.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Message, public key, r and s are required") }
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
        saveHistoryUseCase(CryptoHistory(operationType = type, inputText = input, outputText = output, status = status, timestamp = System.currentTimeMillis()))
    }
}

class HistoryViewModel(
    getHistoryUseCase: GetHistoryUseCase,
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
}

class SettingsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setHistoryEnabled(enabled: Boolean) {
        _uiState.update { it.copy(enableHistory = enabled, successMessage = "Settings updated") }
    }
}
