package com.skyd.podaura.nano.di

import com.skyd.podaura.nano.model.db.ConversationRecordCodec
import com.skyd.podaura.nano.model.repository.conversation.AssistantConversationRepository
import com.skyd.podaura.nano.model.repository.conversation.ConversationStore
import com.skyd.podaura.nano.model.repository.conversation.RoomConversationStore
import com.skyd.podaura.nano.settings.AssistantSettingsRepository
import com.skyd.podaura.nano.settings.AssistantSettingsViewModel
import com.skyd.podaura.nano.ui.screen.chat.NanoViewModel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.onClose
import kotlin.time.Duration.Companion.milliseconds

val nanoModule = module {
    includes(assistantPlatformModule)
    single { AssistantSettingsRepository(get(), get()) }
    single { ConversationRecordCodec(getAll()) }
    single<ConversationStore> { RoomConversationStore(get()) }
    single {
        AssistantConversationRepository(
            assistant = get(),
            settings = get(),
            capabilities = getAll(),
            store = get(),
        )
    }.onClose { repository ->
        // Koin only closes instances that were actually created. Desktop shutdown awaits this
        // best-effort flush; periodic saves remain the protection against forced termination.
        if (repository != null) {
            runBlocking {
                withTimeoutOrNull(2_000.milliseconds) { repository.close().join() }
            }
        }
    }
    viewModelOf(::NanoViewModel)
    viewModelOf(::AssistantSettingsViewModel)
}

expect val assistantPlatformModule: Module
