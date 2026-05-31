package com.project.cryptoapp.presentation.viewmodel

import android.content.Context
import android.net.Uri
import android.os.SystemClock
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.cryptoapp.data.repository.ActiveCurveRepository
import com.project.cryptoapp.domain.crypto.ActiveCurveRegistry
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
import com.project.cryptoapp.util.FileSignatureCodec
import com.project.cryptoapp.util.FileSignaturePayload
import com.project.cryptoapp.util.KeyPayloadCodec
import com.project.cryptoapp.util.ProtectedKeyStore
import com.project.cryptoapp.util.PublicKeyIdentityCodec
import com.project.cryptoapp.util.PublicKeyIdentityProof
import com.project.cryptoapp.util.toDisplayString
import com.project.cryptoapp.util.validateAad
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
import java.security.MessageDigest

class KeyGenerationViewModel(
    private val generateKeyPairUseCase: GenerateKeyPairUseCase,
    private val signMessageUseCase: SignMessageUseCase,
    private val verifySignatureUseCase: VerifySignatureUseCase,
    private val saveHistoryUseCase: SaveHistoryUseCase,
    private val sessionStore: CryptoSessionStore,
    private val settingsStore: AppSettingsStore,
    private val protectedKeyStore: ProtectedKeyStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        KeyGenerationUiState(hasProtectedKey = protectedKeyStore.hasKeyPair()),
    )
    val uiState: StateFlow<KeyGenerationUiState> = _uiState.asStateFlow()

    fun onIdentityLabelChange(value: String) = _uiState.update { it.copy(identityLabel = value) }

    fun generateKeyPair() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val measured = measureOperation { generateKeyPairUseCase() }
            measured.result
                .onSuccess { keyPair ->
                    val publicKey = keyPair.publicKey.toDisplayString()
                    sessionStore.setKeyPair(
                        privateKey = keyPair.privateKey,
                        publicKey = publicKey,
                    )
                    protectedKeyStore.saveKeyPair(
                        privateKey = keyPair.privateKey,
                        publicKey = publicKey,
                    )
                    _uiState.update {
                        it.copy(
                            privateKey = keyPair.privateKey,
                            publicKey = publicKey,
                            hasProtectedKey = true,
                            isLoading = false,
                            successMessage = "Key pair generated and protected by Android Keystore",
                        )
                    }
                    saveHistory(
                        OperationType.KEY_GENERATION,
                        "Generate ${ActiveCurveRegistry.current.id} key pair",
                        "Public key generated; private key protected by Android Keystore",
                        OperationStatus.SUCCESS,
                        measured.durationNanos,
                    )
                }
                .onFailure { error ->
                    val message = error.message ?: "Key generation failed"
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = message)
                    }
                    saveHistory(
                        OperationType.KEY_GENERATION,
                        "Generate ${ActiveCurveRegistry.current.id} key pair",
                        message,
                        OperationStatus.FAILED,
                        measured.durationNanos,
                    )
                }
        }
    }

    fun loadProtectedKeyPair() {
        viewModelScope.launch {
            runCatching { protectedKeyStore.loadKeyPair() }
                .onSuccess { keyPair ->
                    if (keyPair == null) {
                        _uiState.update {
                            it.copy(
                                hasProtectedKey = false,
                                errorMessage = "No protected key pair found",
                                successMessage = null,
                            )
                        }
                    } else {
                        sessionStore.setKeyPair(keyPair.privateKey, keyPair.publicKey)
                        _uiState.update {
                            it.copy(
                                privateKey = keyPair.privateKey,
                                publicKey = keyPair.publicKey,
                                hasProtectedKey = true,
                                errorMessage = null,
                                successMessage = "Protected key pair loaded",
                            )
                        }
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Unable to load protected key pair")
                    }
                }
        }
    }

    fun clearProtectedKeyPair() {
        protectedKeyStore.clear()
        _uiState.update {
            it.copy(hasProtectedKey = false, successMessage = "Protected key pair cleared", errorMessage = null)
        }
    }

    fun preparePublicKeyExport() {
        val publicKey = _uiState.value.publicKey
        val validationError = validatePublicKey(publicKey)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }
        val payload = KeyPayloadCodec.encode(KeyPayloadCodec.publicKeyPayload(publicKey))
        _uiState.update {
            it.copy(
                keyExportPayload = payload,
                errorMessage = null,
                successMessage = "Public key export prepared",
            )
        }
    }

    fun preparePrivateKeyBackup() {
        val state = _uiState.value
        val validationError = validatePrivateKey(state.privateKey) ?: validatePublicKey(state.publicKey)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }
        val payload = KeyPayloadCodec.encode(
            KeyPayloadCodec.privateKeyPayload(
                privateKey = state.privateKey,
                publicKey = state.publicKey,
            ),
        )
        _uiState.update {
            it.copy(
                keyExportPayload = payload,
                errorMessage = null,
                successMessage = "Private key backup prepared; keep this file secret",
            )
        }
    }

    fun saveKeyExport(context: Context, uri: Uri) {
        context.writeText(uri, _uiState.value.keyExportPayload)
        _uiState.update { it.copy(successMessage = "Key JSON saved", errorMessage = null) }
    }

    fun importKeyPayload(context: Context, uri: Uri) {
        viewModelScope.launch {
            runCatching {
                val payload = KeyPayloadCodec.decode(context.readText(uri))
                require(payload.version == 1) { "Unsupported key payload version: ${payload.version}" }
                require(payload.curve == ActiveCurveRegistry.current.id) {
                    "Unsupported key curve: ${payload.curve}; active curve is ${ActiveCurveRegistry.current.id}"
                }
                val publicKey = with(KeyPayloadCodec) { payload.publicKey.toDisplayKey() }
                validatePublicKey(publicKey)?.let { throw IllegalArgumentException(it) }
                val privateKey = payload.privateKey.orEmpty()
                if (privateKey.isNotBlank()) {
                    validatePrivateKey(privateKey)?.let { throw IllegalArgumentException(it) }
                    protectedKeyStore.saveKeyPair(privateKey, publicKey)
                }
                payload to publicKey
            }
                .onSuccess { (payload, publicKey) ->
                    val privateKey = payload.privateKey.orEmpty()
                    if (privateKey.isNotBlank()) {
                        sessionStore.setKeyPair(privateKey, publicKey)
                    } else {
                        sessionStore.setKeyPair(sessionStore.state.value.privateKey, publicKey)
                    }
                    _uiState.update {
                        it.copy(
                            privateKey = privateKey.ifBlank { it.privateKey },
                            publicKey = publicKey,
                            hasProtectedKey = protectedKeyStore.hasKeyPair(),
                            errorMessage = null,
                            successMessage = if (privateKey.isBlank()) {
                                "Public key imported"
                            } else {
                                "Private key imported and protected by Android Keystore"
                            },
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(errorMessage = error.message ?: "Unable to import key JSON") }
                }
        }
    }

    fun createPublicKeyIdentityProof() {
        val state = _uiState.value
        val label = state.identityLabel.ifBlank { "Unnamed key owner" }
        val validationError = validatePrivateKey(state.privateKey) ?: validatePublicKey(state.publicKey)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            val measured = measureOperation {
                val keyPayload = KeyPayloadCodec.publicKeyPayload(state.publicKey)
                val message = PublicKeyIdentityCodec.messageToSign(label, keyPayload.fingerprint)
                val signature = signMessageUseCase(message, state.privateKey)
                PublicKeyIdentityCodec.encode(
                    PublicKeyIdentityProof(
                        curve = ActiveCurveRegistry.current.id,
                        label = label,
                        publicKey = keyPayload.publicKey,
                        fingerprint = keyPayload.fingerprint,
                        signature = signature,
                    ),
                )
            }
            measured.result
                .onSuccess { proof ->
                    _uiState.update {
                        it.copy(
                            identityProofPayload = proof,
                            publicKeyAuthResult = "Identity proof created",
                            errorMessage = null,
                            successMessage = "Public key identity proof created",
                        )
                    }
                    saveHistory(
                        OperationType.SIGN,
                        "Public key identity proof",
                        "Identity proof signature generated",
                        OperationStatus.SUCCESS,
                        measured.durationNanos,
                    )
                }
                .onFailure { error ->
                    val message = error.message ?: "Unable to create identity proof"
                    _uiState.update { it.copy(errorMessage = message) }
                    saveHistory(
                        OperationType.SIGN,
                        "Public key identity proof",
                        message,
                        OperationStatus.FAILED,
                        measured.durationNanos,
                    )
                }
        }
    }

    fun verifyIdentityProof(context: Context, uri: Uri) {
        viewModelScope.launch {
            val measured = measureOperation {
                val proof = PublicKeyIdentityCodec.decode(context.readText(uri))
                require(proof.version == 1) { "Unsupported identity proof version: ${proof.version}" }
                require(proof.type == "public-key-identity-proof") { "Unsupported identity proof type: ${proof.type}" }
                require(proof.curve == ActiveCurveRegistry.current.id) {
                    "Unsupported curve: ${proof.curve}; active curve is ${ActiveCurveRegistry.current.id}"
                }
                val publicKey = with(KeyPayloadCodec) { proof.publicKey.toDisplayKey() }
                validatePublicKey(publicKey)?.let { throw IllegalArgumentException(it) }
                val expectedFingerprint = KeyPayloadCodec.fingerprint(publicKey)
                require(expectedFingerprint.equals(proof.fingerprint, ignoreCase = true)) {
                    "Fingerprint does not match public key"
                }
                val message = PublicKeyIdentityCodec.messageToSign(proof.label, proof.fingerprint)
                val valid = verifySignatureUseCase(message, publicKey, proof.signature)
                require(valid) { "Identity proof signature is invalid" }
                proof to publicKey
            }
            measured.result
                .onSuccess { (proof, publicKey) ->
                    sessionStore.setKeyPair(sessionStore.state.value.privateKey, publicKey)
                    _uiState.update {
                        it.copy(
                            publicKey = publicKey,
                            publicKeyAuthResult = "Authenticated public key for ${proof.label}\nFingerprint: ${proof.fingerprint}",
                            errorMessage = null,
                            successMessage = "Public key identity proof verified",
                        )
                    }
                    saveHistory(
                        OperationType.VERIFY,
                        "Public key identity proof",
                        "Identity proof signature verified",
                        OperationStatus.SUCCESS,
                        measured.durationNanos,
                    )
                }
                .onFailure { error ->
                    val message = error.message ?: "Unable to verify identity proof"
                    _uiState.update {
                        it.copy(
                            publicKeyAuthResult = "Public key authentication failed",
                            errorMessage = message,
                        )
                    }
                    saveHistory(
                        OperationType.VERIFY,
                        "Public key identity proof",
                        message,
                        OperationStatus.FAILED,
                        measured.durationNanos,
                    )
                }
        }
    }

    fun saveIdentityProof(context: Context, uri: Uri) {
        context.writeText(uri, _uiState.value.identityProofPayload)
        _uiState.update { it.copy(successMessage = "Identity proof JSON saved", errorMessage = null) }
    }

    private fun Context.readText(uri: Uri): String =
        contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: throw IllegalArgumentException("Unable to read key file")

    private fun Context.writeText(uri: Uri, value: String) {
        contentResolver.openOutputStream(uri)?.use { it.write(value.toByteArray(Charsets.UTF_8)) }
            ?: throw IllegalArgumentException("Unable to write key file")
    }

    private suspend fun saveHistory(
        type: OperationType,
        input: String,
        output: String,
        status: OperationStatus,
        durationNanos: Long,
    ) {
        if (!settingsStore.state.value.saveHistory) return
        saveHistoryUseCase(
            CryptoHistory(
                operationType = type,
                inputText = input,
                outputText = output,
                status = status,
                timestamp = System.currentTimeMillis(),
                durationNanos = durationNanos,
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
    fun onAadChange(value: String) = _uiState.update { it.copy(aad = value) }
    fun onOutputFormatChange(value: String) = _uiState.update { it.copy(outputFormat = value) }

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
                ?: validateAad(state.aad, settingsStore.state.value.defaultEncoding)
        }
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val measured = measureOperation {
                encryptMessageUseCase(state.plaintext, state.publicKey, state.aad)
                    .toDisplayString(pretty = state.outputFormat == "Pretty JSON")
            }
            measured.result
                .onSuccess { output ->
                    sessionStore.setCipherText(output)
                    _uiState.update {
                        it.copy(cipherText = output, isLoading = false, successMessage = "Message encrypted")
                    }
                    saveHistory(
                        OperationType.ENCRYPT,
                        "Hybrid encrypt (${state.plaintext.length} chars)",
                        "Hybrid ciphertext generated (${output.length} chars)",
                        OperationStatus.SUCCESS,
                        measured.durationNanos,
                    )
                }
                .onFailure { error ->
                    val message = error.message ?: "Encryption failed"
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                    saveHistory(
                        OperationType.ENCRYPT,
                        "Hybrid encrypt (${state.plaintext.length} chars)",
                        message,
                        OperationStatus.FAILED,
                        measured.durationNanos,
                    )
                }
        }
    }

    private suspend fun saveHistory(
        type: OperationType,
        input: String,
        output: String,
        status: OperationStatus,
        durationNanos: Long,
    ) {
        if (!settingsStore.state.value.saveHistory) return
        saveHistoryUseCase(CryptoHistory(operationType = type, inputText = input, outputText = output, status = status, timestamp = System.currentTimeMillis(), durationNanos = durationNanos))
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
    fun onAadChange(value: String) = _uiState.update { it.copy(aad = value) }

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
        val validationError = validateCipherText(state.cipherText)
            ?: validatePrivateKey(state.privateKey)
            ?: validateAad(state.aad, settingsStore.state.value.defaultEncoding)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val measured = measureOperation { decryptMessageUseCase(state.cipherText, state.privateKey, state.aad) }
            measured.result
                .onSuccess { output ->
                    _uiState.update {
                        it.copy(plaintext = output, isLoading = false, successMessage = "Ciphertext decrypted")
                    }
                    saveHistory(
                        OperationType.DECRYPT,
                        "Hybrid ciphertext JSON",
                        "Plaintext recovered (${output.length} chars)",
                        OperationStatus.SUCCESS,
                        measured.durationNanos,
                    )
                }
                .onFailure { error ->
                    val message = error.message ?: "Decryption failed"
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                    saveHistory(OperationType.DECRYPT, "Hybrid ciphertext JSON", message, OperationStatus.FAILED, measured.durationNanos)
                }
        }
    }

    private suspend fun saveHistory(type: OperationType, input: String, output: String, status: OperationStatus, durationNanos: Long) {
        if (!settingsStore.state.value.saveHistory) return
        saveHistoryUseCase(CryptoHistory(operationType = type, inputText = input, outputText = output, status = status, timestamp = System.currentTimeMillis(), durationNanos = durationNanos))
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
            val measured = measureOperation { signMessageUseCase(state.message, state.privateKey) }
            measured.result
                .onSuccess { signature ->
                    val output = signature.toDisplayString()
                    sessionStore.setSignature(signature.r, signature.s)
                    _uiState.update {
                        it.copy(signature = output, isLoading = false, successMessage = "Message signed")
                    }
                    saveHistory(
                        OperationType.SIGN,
                        "Message signing (${state.message.length} chars)",
                        "Deterministic ECDSA signature generated",
                        OperationStatus.SUCCESS,
                        measured.durationNanos,
                    )
                }
                .onFailure { error ->
                    val message = error.message ?: "Signing failed"
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                    saveHistory(
                        OperationType.SIGN,
                        "Message signing (${state.message.length} chars)",
                        message,
                        OperationStatus.FAILED,
                        measured.durationNanos,
                    )
                }
        }
    }

    private suspend fun saveHistory(type: OperationType, input: String, output: String, status: OperationStatus, durationNanos: Long) {
        if (!settingsStore.state.value.saveHistory) return
        saveHistoryUseCase(CryptoHistory(operationType = type, inputText = input, outputText = output, status = status, timestamp = System.currentTimeMillis(), durationNanos = durationNanos))
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
            val measured = measureOperation { verifySignatureUseCase(state.message, state.publicKey, signature) }
            measured.result
                .onSuccess { isValid ->
                    val output = if (isValid) "Signature is valid" else "Signature is invalid"
                    _uiState.update {
                        it.copy(
                            isValid = isValid,
                            isLoading = false,
                            successMessage = output,
                        )
                    }
                    saveHistory(
                        OperationType.VERIFY,
                        "Signature verification (${state.message.length} chars)",
                        output,
                        OperationStatus.SUCCESS,
                        measured.durationNanos,
                    )
                }
                .onFailure { error ->
                    val message = error.message ?: "Verification failed"
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                    saveHistory(
                        OperationType.VERIFY,
                        "Signature verification (${state.message.length} chars)",
                        message,
                        OperationStatus.FAILED,
                        measured.durationNanos,
                    )
                }
        }
    }

    private suspend fun saveHistory(type: OperationType, input: String, output: String, status: OperationStatus, durationNanos: Long) {
        if (!settingsStore.state.value.saveHistory) return
        saveHistoryUseCase(CryptoHistory(operationType = type, inputText = input, outputText = output, status = status, timestamp = System.currentTimeMillis(), durationNanos = durationNanos))
    }
}

class FileToolsViewModel(
    private val encryptMessageUseCase: EncryptMessageUseCase,
    private val decryptMessageUseCase: DecryptMessageUseCase,
    private val signMessageUseCase: SignMessageUseCase,
    private val verifySignatureUseCase: VerifySignatureUseCase,
    private val saveHistoryUseCase: SaveHistoryUseCase,
    private val sessionStore: CryptoSessionStore,
    private val settingsStore: AppSettingsStore,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        FileToolsUiState(
            publicKey = sessionStore.state.value.publicKey,
            privateKey = sessionStore.state.value.privateKey,
        ),
    )
    val uiState: StateFlow<FileToolsUiState> = _uiState.asStateFlow()

    fun onPublicKeyChange(value: String) = _uiState.update { it.copy(publicKey = value) }
    fun onPrivateKeyChange(value: String) = _uiState.update { it.copy(privateKey = value) }

    fun useLatestPublicKey() {
        val publicKey = sessionStore.state.value.publicKey
        if (publicKey.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Generate or load a key pair first") }
        } else {
            _uiState.update { it.copy(publicKey = publicKey, errorMessage = null, successMessage = "Latest public key loaded") }
        }
    }

    fun useLatestPrivateKey() {
        val privateKey = sessionStore.state.value.privateKey
        if (privateKey.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Generate or load a key pair first") }
        } else {
            _uiState.update { it.copy(privateKey = privateKey, errorMessage = null, successMessage = "Latest private key loaded") }
        }
    }

    fun encryptFile(context: Context, uri: Uri) {
        val publicKey = _uiState.value.publicKey
        val validationError = validatePublicKey(publicKey)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val measured = measureOperation {
                val bytes = context.readBytes(uri)
                val name = context.displayName(uri)
                encryptMessageUseCase(bytes.toHexPayload(), publicKey, "fileName=$name")
                    .toDisplayString(pretty = true)
            }
            measured.result
                .onSuccess { payload ->
                    _uiState.update {
                        it.copy(
                            encryptedFilePayload = payload,
                            isLoading = false,
                            successMessage = "File encrypted to hybrid JSON",
                        )
                    }
                    saveHistory(OperationType.ENCRYPT, "File encryption", "Encrypted file payload generated", OperationStatus.SUCCESS, measured.durationNanos)
                }
                .onFailure { error ->
                    val message = error.message ?: "File encryption failed"
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                    saveHistory(OperationType.ENCRYPT, "File encryption", message, OperationStatus.FAILED, measured.durationNanos)
                }
        }
    }

    fun decryptFile(context: Context, uri: Uri) {
        val privateKey = _uiState.value.privateKey
        val validationError = validatePrivateKey(privateKey)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val measured = measureOperation {
                val payload = context.readText(uri)
                decryptMessageUseCase(payload, privateKey).toPayloadBytes()
            }
            measured.result
                .onSuccess { bytes ->
                    _uiState.update {
                        it.copy(
                            decryptedFileBytes = bytes,
                            isLoading = false,
                            successMessage = "File decrypted and ready to save",
                        )
                    }
                    saveHistory(OperationType.DECRYPT, "File decryption", "Decrypted file bytes recovered", OperationStatus.SUCCESS, measured.durationNanos)
                }
                .onFailure { error ->
                    val message = error.message ?: "File decryption failed"
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                    saveHistory(OperationType.DECRYPT, "File decryption", message, OperationStatus.FAILED, measured.durationNanos)
                }
        }
    }

    fun signFile(context: Context, uri: Uri) {
        val privateKey = _uiState.value.privateKey
        val validationError = validatePrivateKey(privateKey)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val measured = measureOperation {
                val bytes = context.readBytes(uri)
                val fileHash = bytes.sha512HexPayload()
                val signature = signMessageUseCase(fileHash, privateKey)
                FileSignatureCodec.encode(
                    FileSignaturePayload(
                        curve = ActiveCurveRegistry.current.id,
                        fileName = context.displayName(uri),
                        sha512 = fileHash,
                        signature = signature,
                    ),
                )
            }
            measured.result
                .onSuccess { signaturePayload ->
                    _uiState.update {
                        it.copy(
                            fileSignaturePayload = signaturePayload,
                            isLoading = false,
                            successMessage = "File signature generated",
                        )
                    }
                    saveHistory(OperationType.SIGN, "File signing", "File SHA-512 signature generated", OperationStatus.SUCCESS, measured.durationNanos)
                }
                .onFailure { error ->
                    val message = error.message ?: "File signing failed"
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                    saveHistory(OperationType.SIGN, "File signing", message, OperationStatus.FAILED, measured.durationNanos)
                }
        }
    }

    fun verifyFileSignature(context: Context, fileUri: Uri, signatureUri: Uri) {
        val publicKey = _uiState.value.publicKey
        val validationError = validatePublicKey(publicKey)
        if (validationError != null) {
            _uiState.update { it.copy(errorMessage = validationError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val measured = measureOperation {
                val fileHash = context.readBytes(fileUri).sha512HexPayload()
                val signaturePayload = FileSignatureCodec.decode(context.readText(signatureUri))
                require(signaturePayload.curve == ActiveCurveRegistry.current.id) {
                    "Unsupported signature curve: ${signaturePayload.curve}; active curve is ${ActiveCurveRegistry.current.id}"
                }
                val hashMatches = signaturePayload.sha512.equals(fileHash, ignoreCase = true)
                val signatureValid = verifySignatureUseCase(fileHash, publicKey, signaturePayload.signature)
                hashMatches && signatureValid
            }
            measured.result
                .onSuccess { isValid ->
                    val output = if (isValid) "File signature is valid" else "File signature is invalid"
                    _uiState.update {
                        it.copy(
                            verificationResult = output,
                            isLoading = false,
                            successMessage = output,
                        )
                    }
                    saveHistory(OperationType.VERIFY, "File signature verification", output, OperationStatus.SUCCESS, measured.durationNanos)
                }
                .onFailure { error ->
                    val message = error.message ?: "File signature verification failed"
                    _uiState.update { it.copy(isLoading = false, errorMessage = message) }
                    saveHistory(OperationType.VERIFY, "File signature verification", message, OperationStatus.FAILED, measured.durationNanos)
                }
        }
    }

    fun saveEncryptedPayload(context: Context, uri: Uri) {
        context.writeText(uri, _uiState.value.encryptedFilePayload)
        _uiState.update { it.copy(successMessage = "Encrypted file payload saved", errorMessage = null) }
    }

    fun saveDecryptedFile(context: Context, uri: Uri) {
        val bytes = _uiState.value.decryptedFileBytes ?: return
        context.writeBytes(uri, bytes)
        _uiState.update { it.copy(successMessage = "Decrypted file saved", errorMessage = null) }
    }

    fun saveSignaturePayload(context: Context, uri: Uri) {
        context.writeText(uri, _uiState.value.fileSignaturePayload)
        _uiState.update { it.copy(successMessage = "Signature file saved", errorMessage = null) }
    }

    private suspend fun saveHistory(
        type: OperationType,
        input: String,
        output: String,
        status: OperationStatus,
        durationNanos: Long,
    ) {
        if (!settingsStore.state.value.saveHistory) return
        saveHistoryUseCase(
            CryptoHistory(
                operationType = type,
                inputText = input,
                outputText = output,
                status = status,
                timestamp = System.currentTimeMillis(),
                durationNanos = durationNanos,
            ),
        )
    }

    private fun Context.readBytes(uri: Uri): ByteArray =
        contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("Unable to read selected file")

    private fun Context.readText(uri: Uri): String =
        readBytes(uri).toString(Charsets.UTF_8)

    private fun Context.writeBytes(uri: Uri, bytes: ByteArray) {
        contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            ?: throw IllegalArgumentException("Unable to write selected file")
    }

    private fun Context.writeText(uri: Uri, value: String) =
        writeBytes(uri, value.toByteArray(Charsets.UTF_8))

    private fun Context.displayName(uri: Uri): String {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) return cursor.getString(index)
            }
        return uri.lastPathSegment ?: "selected-file"
    }

    private fun ByteArray.toHexPayload(): String =
        "0x" + joinToString("") { "%02X".format(it) }

    private fun ByteArray.sha512HexPayload(): String =
        MessageDigest.getInstance("SHA-512").digest(this).toHexPayload()

    private fun String.toPayloadBytes(): ByteArray {
        val trimmed = trim()
        val hex = trimmed.removePrefix("0x").removePrefix("0X")
        return if (trimmed.startsWith("0x", ignoreCase = true) && hex.length % 2 == 0) {
            hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
        } else {
            toByteArray(Charsets.UTF_8)
        }
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

class CurveParametersViewModel(
    private val activeCurveRepository: ActiveCurveRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        CurveParametersUiState(
            params = activeCurveRepository.state.value.curve.toParams(),
            status = activeCurveRepository.state.value.status,
        ),
    )
    val uiState: StateFlow<CurveParametersUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            activeCurveRepository.state.collect { runtimeState ->
                _uiState.update {
                    it.copy(
                        params = runtimeState.curve.toParams(),
                        status = runtimeState.status,
                        isLoading = false,
                    )
                }
            }
        }
    }

    fun refreshCurve() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            runCatching { activeCurveRepository.refreshFromServer() }
                .onSuccess { curve ->
                    _uiState.update {
                        it.copy(
                            params = curve.toParams(),
                            isLoading = false,
                            successMessage = "Active curve: ${curve.id}",
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Unable to refresh curve",
                        )
                    }
                }
        }
    }
}

private data class MeasuredResult<T>(
    val result: Result<T>,
    val durationNanos: Long,
)

private inline fun <T> measureOperation(block: () -> T): MeasuredResult<T> {
    val startedAt = SystemClock.elapsedRealtimeNanos()
    val result = runCatching(block)
    return MeasuredResult(
        result = result,
        durationNanos = SystemClock.elapsedRealtimeNanos() - startedAt,
    )
}
