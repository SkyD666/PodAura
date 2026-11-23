package com.skyd.podaura.nano.capabilities.downloads

import com.skyd.podaura.nano.capabilities.downloads.model.DownloadHistoryCodec
import com.skyd.podaura.nano.core.AssistantHistoryCodec
import com.skyd.podaura.nano.model.repository.conversation.ConversationCapability
import com.skyd.podaura.nano.ui.AssistantCapabilityUi
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module

val downloadModule = module {
    includes(downloadRuntimeModule)
    val key = named(DownloadHistoryCodec.capabilityId)
    single<AssistantHistoryCodec>(key) { DownloadHistoryCodec }
    single<AssistantCapabilityUi>(key) { DownloadUi }
    single<ConversationCapability>(key) { DownloadConversation(downloads = get()) }
}

internal expect val downloadRuntimeModule: Module
