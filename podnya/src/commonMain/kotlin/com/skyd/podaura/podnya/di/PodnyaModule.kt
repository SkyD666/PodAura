package com.skyd.podaura.podnya.di

import com.skyd.podaura.podnya.capabilities.articles.di.articleSearchModule
import com.skyd.podaura.podnya.capabilities.articles.model.ArticleHistoryCodec
import com.skyd.podaura.podnya.capabilities.feeds.model.FeedHistoryCodec
import com.skyd.podaura.podnya.capabilities.feeds.model.repository.LocalFeedSearchRepository
import com.skyd.podaura.podnya.model.db.ConversationRecordCodec
import com.skyd.podaura.podnya.model.repository.conversation.AssistantConversationRepository
import com.skyd.podaura.podnya.model.repository.conversation.ConversationStore
import com.skyd.podaura.podnya.model.repository.conversation.RoomConversationStore
import com.skyd.podaura.podnya.settings.AssistantSettingsRepository
import com.skyd.podaura.podnya.settings.AssistantSettingsViewModel
import com.skyd.podaura.podnya.ui.screen.chat.PodnyaViewModel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import org.koin.dsl.onClose
import kotlin.time.Duration.Companion.milliseconds

val podnyaModule = module {
    includes(assistantPlatformModule, articleSearchModule)
    single { LocalFeedSearchRepository(get()) }
    single { AssistantSettingsRepository(get(), get()) }
    single<ConversationStore> {
        RoomConversationStore(
            ConversationRecordCodec(
                listOf(
                    ArticleHistoryCodec,
                    FeedHistoryCodec
                )
            )
        )
    }
    single {
        AssistantConversationRepository(assistant = get(), settings = get(), store = get())
    }.onClose { repository ->
        // Koin only closes instances that were actually created. Desktop shutdown awaits this
        // best-effort flush; periodic saves remain the protection against forced termination.
        if (repository != null) {
            runBlocking {
                withTimeoutOrNull(2_000.milliseconds) { repository.close().join() }
            }
        }
    }
    viewModelOf(::PodnyaViewModel)
    viewModelOf(::AssistantSettingsViewModel)
}

expect val assistantPlatformModule: Module
