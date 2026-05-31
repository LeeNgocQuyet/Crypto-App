package com.project.cryptoapp.data.repository

import android.content.Context
import com.project.cryptoapp.data.remote.CurveApiClient
import com.project.cryptoapp.data.remote.toCurveSpec
import com.project.cryptoapp.domain.crypto.ActiveCurveRegistry
import com.project.cryptoapp.domain.crypto.BrainpoolP512r1
import com.project.cryptoapp.domain.crypto.CurveSource
import com.project.cryptoapp.domain.crypto.ECCurveSpec
import com.project.cryptoapp.domain.crypto.ECCurveValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class ActiveCurveRepository(
    context: Context,
    private val apiClient: CurveApiClient = CurveApiClient(),
) {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var attemptedRemoteLoad = false

    private val _state = MutableStateFlow(
        CurveRuntimeState(
            curve = loadCachedCurve() ?: BrainpoolP512r1.spec,
            status = "Using fallback curve",
        ),
    )
    val state: StateFlow<CurveRuntimeState> = _state.asStateFlow()

    init {
        ActiveCurveRegistry.update(_state.value.curve)
    }

    suspend fun activeCurve(): ECCurveSpec {
        if (!attemptedRemoteLoad) {
            refreshFromServer()
        }
        return _state.value.curve
    }

    suspend fun refreshFromServer(): ECCurveSpec = withContext(Dispatchers.IO) {
        attemptedRemoteLoad = true
        runCatching {
            val response = apiClient.fetchCurrentCurve()
            val curve = ECCurveValidator.validate(response.spec)
            preferences.edit().putString(KEY_CURVE_JSON, response.rawJson).apply()
            setActiveCurve(curve, "Loaded curve from local server")
            curve
        }.getOrElse { error ->
            val cached = loadCachedCurve()
            if (cached != null) {
                setActiveCurve(cached, "Server unavailable; using cached curve: ${error.message}")
                cached
            } else {
                setActiveCurve(BrainpoolP512r1.spec, "Server unavailable; using Brainpool fallback: ${error.message}")
                BrainpoolP512r1.spec
            }
        }
    }

    private fun setActiveCurve(curve: ECCurveSpec, status: String) {
        ActiveCurveRegistry.update(curve)
        _state.value = CurveRuntimeState(curve = curve, status = status)
    }

    private fun loadCachedCurve(): ECCurveSpec? =
        preferences.getString(KEY_CURVE_JSON, null)
            ?.let { json ->
                runCatching {
                    ECCurveValidator.validate(json.toCurveSpec(CurveSource.CACHE))
                }.getOrNull()
            }

    companion object {
        private const val PREFS_NAME = "active_curve"
        private const val KEY_CURVE_JSON = "curve_json"
    }
}

data class CurveRuntimeState(
    val curve: ECCurveSpec,
    val status: String,
)

