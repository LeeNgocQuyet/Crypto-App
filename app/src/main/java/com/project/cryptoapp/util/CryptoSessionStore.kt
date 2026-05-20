package com.project.cryptoapp.util

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class CryptoSessionState(
    val privateKey: String = "",
    val publicKey: String = "",
    val cipherText: String = "",
    val signatureR: String = "",
    val signatureS: String = "",
)

class CryptoSessionStore {
    private val _state = MutableStateFlow(CryptoSessionState())
    val state: StateFlow<CryptoSessionState> = _state.asStateFlow()

    fun setKeyPair(privateKey: String, publicKey: String) {
        _state.update { it.copy(privateKey = privateKey, publicKey = publicKey) }
    }

    fun setCipherText(cipherText: String) {
        _state.update { it.copy(cipherText = cipherText) }
    }

    fun setSignature(signatureR: String, signatureS: String) {
        _state.update { it.copy(signatureR = signatureR, signatureS = signatureS) }
    }
}
