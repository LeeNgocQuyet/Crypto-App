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
                    appContainer.signMessageUseCase,
                    appContainer.verifySignatureUseCase,
                    appContainer.saveHistoryUseCase,
                    appContainer.cryptoSessionStore,
                    appContainer.appSettingsStore,
                    appContainer.protectedKeyStore,
                )

            modelClass.isAssignableFrom(EncryptViewModel::class.java) ->
                EncryptViewModel(
                    appContainer.encryptMessageUseCase,
                    appContainer.saveHistoryUseCase,
                    appContainer.cryptoSessionStore,
                    appContainer.appSettingsStore,
                )

            modelClass.isAssignableFrom(DecryptViewModel::class.java) ->
                DecryptViewModel(
                    appContainer.decryptMessageUseCase,
                    appContainer.saveHistoryUseCase,
                    appContainer.cryptoSessionStore,
                    appContainer.appSettingsStore,
                )

            modelClass.isAssignableFrom(SignViewModel::class.java) ->
                SignViewModel(
                    appContainer.signMessageUseCase,
                    appContainer.saveHistoryUseCase,
                    appContainer.cryptoSessionStore,
                    appContainer.appSettingsStore,
                )

            modelClass.isAssignableFrom(VerifyViewModel::class.java) ->
                VerifyViewModel(
                    appContainer.verifySignatureUseCase,
                    appContainer.saveHistoryUseCase,
                    appContainer.cryptoSessionStore,
                    appContainer.appSettingsStore,
                )

            modelClass.isAssignableFrom(FileToolsViewModel::class.java) ->
                FileToolsViewModel(
                    appContainer.encryptMessageUseCase,
                    appContainer.decryptMessageUseCase,
                    appContainer.signMessageUseCase,
                    appContainer.verifySignatureUseCase,
                    appContainer.saveHistoryUseCase,
                    appContainer.cryptoSessionStore,
                    appContainer.appSettingsStore,
                )

            modelClass.isAssignableFrom(HistoryViewModel::class.java) ->
                HistoryViewModel(
                    appContainer.getHistoryUseCase,
                    appContainer.deleteHistoryByIdUseCase,
                    appContainer.clearHistoryUseCase,
                )

            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(
                    appContainer.clearHistoryUseCase,
                    appContainer.appSettingsStore,
                )

            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        } as T
    }
}
