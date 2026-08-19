package com.skyd.podaura.model.repository.translation

import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.interpretObjCPointer
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.ptr
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFRelease
import platform.Foundation.NSCopyingProtocol
import platform.Foundation.NSData
import platform.Foundation.NSMutableDictionary
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Security.SecItemAdd
import platform.Security.SecItemCopyMatching
import platform.Security.SecItemDelete
import platform.Security.errSecDuplicateItem
import platform.Security.errSecItemNotFound
import platform.Security.errSecSuccess
import platform.Security.kSecAttrAccessible
import platform.Security.kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
import platform.Security.kSecAttrAccount
import platform.Security.kSecAttrService
import platform.Security.kSecClass
import platform.Security.kSecClassGenericPassword
import platform.Security.kSecMatchLimit
import platform.Security.kSecMatchLimitOne
import platform.Security.kSecReturnData
import platform.Security.kSecValueData

class AppleCredentialStore : CredentialStore {

    override suspend fun put(id: String, secret: String) {
        if (secret.isBlank()) throw CredentialStorageException()
        val bytes = secret.encodeToByteArray()
        val data = bytes.usePinned {
            NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong())
        }
        val existingQuery = baseQuery(id)
        SecItemDelete(existingQuery.asCFDictionary())
        val attributes = dictionary(
            key(kSecClass) to value(kSecClassGenericPassword),
            key(kSecAttrService) to SERVICE,
            key(kSecAttrAccount) to id,
            key(kSecAttrAccessible) to value(kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly),
            key(kSecValueData) to data,
        )
        val status = SecItemAdd(
            attributes.asCFDictionary(),
            null,

        )
        if (status != errSecSuccess && status != errSecDuplicateItem) {
            throw CredentialStorageException()
        }
    }

    override suspend fun get(id: String): String? = memScoped {
        val result = alloc<COpaquePointerVar>()
        val query = dictionary(
            key(kSecClass) to value(kSecClassGenericPassword),
            key(kSecAttrService) to SERVICE,
            key(kSecAttrAccount) to id,
            key(kSecReturnData) to true,
            key(kSecMatchLimit) to value(kSecMatchLimitOne),
        )
        val status = SecItemCopyMatching(
            query.asCFDictionary(),
            result.ptr,
        )
        if (status == errSecItemNotFound) return@memScoped null
        if (status != errSecSuccess) throw CredentialStorageException()
        val dataPointer = result.value ?: throw CredentialStorageException()
        try {
            val data = interpretObjCPointer<NSData>(dataPointer.rawValue)
            NSString.create(data = data, encoding = NSUTF8StringEncoding)?.toString()
                ?: throw CredentialStorageException()
        } finally {
            CFRelease(dataPointer)
        }
    }

    override suspend fun delete(id: String) {
        val query = baseQuery(id)
        val status = SecItemDelete(query.asCFDictionary())
        if (status != errSecSuccess && status != errSecItemNotFound) {
            throw CredentialStorageException()
        }
    }

    private fun baseQuery(id: String): NSMutableDictionary = dictionary(
        key(kSecClass) to value(kSecClassGenericPassword),
        key(kSecAttrService) to SERVICE,
        key(kSecAttrAccount) to id,
    )

    private fun key(pointer: kotlinx.cinterop.COpaquePointer?): NSCopyingProtocol =
        interpretObjCPointer<NSString>(checkNotNull(pointer).rawValue)

    private fun value(pointer: kotlinx.cinterop.COpaquePointer?): NSString =
        interpretObjCPointer(checkNotNull(pointer).rawValue)

    private fun dictionary(vararg pairs: Pair<NSCopyingProtocol, Any>): NSMutableDictionary {
        val dictionary = NSMutableDictionary()
        pairs.forEach { (key, value) -> dictionary.setObject(value, forKey = key) }
        return dictionary
    }

    private fun NSMutableDictionary.asCFDictionary(): CFDictionaryRef =
        interpretCPointer(objcPtr()) ?: throw CredentialStorageException()

    private companion object {
        const val SERVICE = "com.skyd.podaura.translation"
    }
}
