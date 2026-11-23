package com.skyd.fundation.credential

import com.sun.jna.Memory
import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.WString
import com.sun.jna.platform.win32.WinError
import com.sun.jna.ptr.PointerByReference
import com.sun.jna.win32.StdCallLibrary

internal object WindowsCredentialStore {
    private const val TYPE_GENERIC = 1
    private const val PERSIST_LOCAL_MACHINE = 2
    private const val MAX_BLOB_BYTES = 2560
    private val api by lazy { Native.load("Advapi32", WinCred::class.java) }
    private fun target(id: String) = WString("PodAura/$id")

    fun put(id: String, secret: String) {
        val bytes = secret.encodeToByteArray()
        try {
            if (bytes.size > MAX_BLOB_BYTES) throw CredentialTooLongException()
            Memory(bytes.size.toLong()).use { blob ->
                blob.write(0, bytes, 0, bytes.size)
                try {
                    val credential = Credential().apply {
                        type = TYPE_GENERIC
                        targetName = target(id)
                        blobSize = bytes.size
                        credentialBlob = blob
                        persist = PERSIST_LOCAL_MACHINE
                    }
                    if (!api.CredWriteW(credential, 0)) throw CredentialStorageException()
                } finally {
                    blob.clear()
                }
            }
        } finally {
            bytes.fill(0)
        }
    }

    fun get(id: String): String? {
        val result = PointerByReference()
        if (!api.CredReadW(
                targetName = target(id),
                type = TYPE_GENERIC,
                flags = 0,
                result = result
            )
        ) {
            if (Native.getLastError() == WinError.ERROR_NOT_FOUND) return null
            throw CredentialStorageException()
        }
        try {
            val credential = Credential(result.value).apply { read() }
            val bytes = credential.credentialBlob?.getByteArray(0, credential.blobSize)
                ?: throw CredentialStorageException()
            return try {
                bytes.decodeToString()
            } finally {
                bytes.fill(0)
            }
        } finally {
            api.CredFree(result.value)
        }
    }

    fun delete(id: String) {
        if (!api.CredDeleteW(targetName = target(id), type = TYPE_GENERIC, flags = 0) &&
            Native.getLastError() != WinError.ERROR_NOT_FOUND
        ) throw CredentialStorageException()
    }

    internal interface WinCred : StdCallLibrary {
        fun CredWriteW(credential: Credential, flags: Int): Boolean
        fun CredReadW(
            targetName: WString,
            type: Int,
            flags: Int,
            result: PointerByReference
        ): Boolean

        fun CredDeleteW(targetName: WString, type: Int, flags: Int): Boolean
        fun CredFree(buffer: Pointer)
    }

    @Structure.FieldOrder(
        "flags",
        "type",
        "targetName",
        "comment",
        "lastWrittenLow",
        "lastWrittenHigh",
        "blobSize",
        "credentialBlob",
        "persist",
        "attributeCount",
        "attributes",
        "targetAlias",
        "userName",
    )
    internal class Credential(pointer: Pointer? = null) : Structure(pointer) {
        @JvmField
        var flags = 0
        @JvmField
        var type = 0
        @JvmField
        var targetName: WString? = null
        @JvmField
        var comment: Pointer? = null
        @JvmField
        var lastWrittenLow = 0
        @JvmField
        var lastWrittenHigh = 0
        @JvmField
        var blobSize = 0
        @JvmField
        var credentialBlob: Pointer? = null
        @JvmField
        var persist = 0
        @JvmField
        var attributeCount = 0
        @JvmField
        var attributes: Pointer? = null
        @JvmField
        var targetAlias: Pointer? = null
        @JvmField
        var userName: Pointer? = null
    }
}
