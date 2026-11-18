package com.skyd.fundation.credential

interface CredentialStore {
    suspend fun put(id: String, secret: String)
    suspend fun get(id: String): String?
    suspend fun delete(id: String)
}

open class CredentialStorageException(message: String = "Secure credential storage is unavailable") :
    Exception(message)

class CredentialTooLongException :
    CredentialStorageException("Credential exceeds system storage limit")
