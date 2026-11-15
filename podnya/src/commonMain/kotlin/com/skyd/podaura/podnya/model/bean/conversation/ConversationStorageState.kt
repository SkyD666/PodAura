package com.skyd.podaura.podnya.model.bean.conversation

enum class ConversationStorageState { LOADING, READY, SAVE_FAILED, OPEN_FAILED, OCCUPIED, NEWER_VERSION }

class ConversationStorageException(val state: ConversationStorageState) : Exception(state.name)
