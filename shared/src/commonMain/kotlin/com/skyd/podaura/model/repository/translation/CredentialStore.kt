package com.skyd.podaura.model.repository.translation

interface CredentialStore {
    suspend fun put(id: String, secret: String)
    suspend fun get(id: String): String?
    suspend fun delete(id: String)
}

class CredentialStorageException : Exception("Secure credential storage is unavailable")
