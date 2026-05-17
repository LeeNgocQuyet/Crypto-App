package com.project.cryptoapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.project.cryptoapp.util.AppContainer

class AppViewModelFactory(
    private val appContainer: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(KeyGenerationViewModel::class.java) ->
                KeyGenerationViewModel(
                    appContainer.generateKeyPairUseCase,
                    appContainer.saveHistoryUseCase,
                )

            modelClass.isAssignableFrom(EncryptViewModel::class.java) ->
                EncryptViewModel(
                    appContainer.encryptMessageUseCase,
                    appContainer.saveHistoryUseCase,
                )

            modelClass.isAssignableFrom(DecryptViewModel::class.java) ->
                DecryptViewModel(
                    appContainer.decryptMessageUseCase,
                    appContainer.saveHistoryUseCase,
                )

            modelClass.isAssignableFrom(SignViewModel::class.java) ->
                SignViewModel(
                    appContainer.signMessageUseCase,
                    appContainer.saveHistoryUseCase,
                )

            modelClass.isAssignableFrom(VerifyViewModel::class.java) ->
                VerifyViewModel(
                    appContainer.verifySignatureUseCase,
                    appContainer.saveHistoryUseCase,
                )

            modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                HistoryViewModel(
                    appContainer.getHistoryUseCase,
                    appContainer.clearHistoryUseCase,
                )

            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel()

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        } as T
    }
}
