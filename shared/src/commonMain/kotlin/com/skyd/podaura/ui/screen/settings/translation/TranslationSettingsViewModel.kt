package com.skyd.podaura.ui.screen.settings.translation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.skyd.podaura.model.bean.translation.TranslationError
import com.skyd.podaura.model.bean.translation.TranslationProfile
import com.skyd.podaura.model.bean.translation.TranslationVerificationResult
import com.skyd.podaura.model.repository.translation.CredentialStorageException
import com.skyd.podaura.model.repository.translation.TranslationProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

data class TranslationSettingsState(
    val profiles: List<TranslationProfile> = emptyList(),
    val working: Boolean = false,
    val notice: TranslationSettingsNotice? = null,
)

sealed interface TranslationSettingsNotice {
    data object Saved : TranslationSettingsNotice
    data object Deleted : TranslationSettingsNotice
    data object Copied : TranslationSettingsNotice
    data object CacheCleared : TranslationSettingsNotice
    data object ConnectionSucceeded : TranslationSettingsNotice
    data class Failed(val error: TranslationError? = null) : TranslationSettingsNotice
}

class TranslationSettingsViewModel(
    private val repository: TranslationProfileRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(TranslationSettingsState())
    val state: StateFlow<TranslationSettingsState> = mutableState.asStateFlow()

    init {
        repository.observeAll()
            .onEach { profiles -> mutableState.update { it.copy(profiles = profiles) } }
            .launchIn(viewModelScope)
    }

    fun save(profile: TranslationProfile, credential: String?) = runOperation {
        repository.save(profile, credential)
        TranslationSettingsNotice.Saved
    }

    fun setDefault(id: String) = runOperation {
        repository.setDefault(id)
        TranslationSettingsNotice.Saved
    }

    @OptIn(ExperimentalUuidApi::class)
    fun copy(profile: TranslationProfile, newName: String) = runOperation {
        repository.copy(
            sourceId = profile.id,
            newId = Uuid.random().toString(),
            newName = newName,
        )
        TranslationSettingsNotice.Copied
    }

    fun delete(id: String, clearCachedTranslations: Boolean) = runOperation {
        repository.delete(id, clearCachedTranslations)
        TranslationSettingsNotice.Deleted
    }

    fun verify(profile: TranslationProfile, credential: String?) = runOperation {
        when (val result = repository.verify(profile, credential)) {
            is TranslationVerificationResult.Success ->
                TranslationSettingsNotice.ConnectionSucceeded

            is TranslationVerificationResult.Failure ->
                TranslationSettingsNotice.Failed(result.error)
        }
    }

    fun clearCache() = runOperation {
        repository.clearTranslationCache()
        TranslationSettingsNotice.CacheCleared
    }

    fun consumeNotice() {
        mutableState.update { it.copy(notice = null) }
    }

    private fun runOperation(
        block: suspend () -> TranslationSettingsNotice,
    ) {
        if (mutableState.value.working) return
        viewModelScope.launch {
            mutableState.update { it.copy(working = true, notice = null) }
            val notice = try {
                block()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: CredentialStorageException) {
                TranslationSettingsNotice.Failed(TranslationError.SecureStorageUnavailable)
            } catch (_: Throwable) {
                TranslationSettingsNotice.Failed()
            }
            mutableState.update { it.copy(working = false, notice = notice) }
        }
    }
}
