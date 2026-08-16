package com.skyd.podaura.model.repository.translation

import com.sun.jna.Memory
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import com.sun.jna.platform.mac.CoreFoundation
import com.sun.jna.ptr.PointerByReference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class DesktopCredentialStore : CredentialStore {
    override suspend fun put(id: String, secret: String) = withContext(Dispatchers.IO) {
        if (secret.isBlank()) throw CredentialStorageException()
        when (operatingSystem) {
            OperatingSystem.MacOS -> MacOsKeychain.put(id, secret)

            OperatingSystem.Linux -> runCommand(
                command = listOf(
                    "secret-tool", "store", "--label=PodAura translation service",
                    "service", SERVICE, "account", id,
                ),
                input = secret,
            )

            OperatingSystem.Unsupported -> throw CredentialStorageException()
        }
        Unit
    }

    override suspend fun get(id: String): String? = withContext(Dispatchers.IO) {
        val result = when (operatingSystem) {
            OperatingSystem.MacOS -> return@withContext MacOsKeychain.get(id)

            OperatingSystem.Linux -> runCommand(
                listOf("secret-tool", "lookup", "service", SERVICE, "account", id),
                allowNotFound = true,
            )

            OperatingSystem.Unsupported -> throw CredentialStorageException()
        } ?: return@withContext null
        result.trimEnd('\r', '\n').takeIf { it.isNotEmpty() }
    }

    override suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        when (operatingSystem) {
            OperatingSystem.MacOS -> MacOsKeychain.delete(id)

            OperatingSystem.Linux -> runCommand(
                listOf("secret-tool", "clear", "service", SERVICE, "account", id),
                allowNotFound = true,
            )

            OperatingSystem.Unsupported -> throw CredentialStorageException()
        }
        Unit
    }

    private fun runCommand(
        command: List<String>,
        input: String? = null,
        allowNotFound: Boolean = false,
    ): String? {
        val process = runCatching {
            ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
        }.getOrElse { throw CredentialStorageException() }
        input?.let { value ->
            process.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                writer.write(value)
                writer.newLine()
            }
        } ?: process.outputStream.close()
        if (!process.waitFor(COMMAND_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
            process.destroyForcibly()
            throw CredentialStorageException()
        }
        val output = process.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (process.exitValue() != 0) {
            if (allowNotFound) return null
            throw CredentialStorageException()
        }
        return output
    }

    private val operatingSystem: OperatingSystem
        get() = System.getProperty("os.name").lowercase().let { name ->
            when {
                name.contains("mac") -> OperatingSystem.MacOS
                name.contains("nix") || name.contains("nux") || name.contains("aix") ->
                    OperatingSystem.Linux

                else -> OperatingSystem.Unsupported
            }
        }

    private enum class OperatingSystem { MacOS, Linux, Unsupported }

    private companion object {
        const val SERVICE = "com.skyd.podaura.translation"
        const val COMMAND_TIMEOUT_SECONDS = 15L
    }

    private object MacOsKeychain {
        private val coreFoundation = CoreFoundation.INSTANCE
        private val coreFoundationLibrary = NativeLibrary.getInstance("CoreFoundation")
        private val security = NativeLibrary.getInstance("Security")
        private val dictionaryKeyCallbacks =
            coreFoundationLibrary.getGlobalVariableAddress("kCFTypeDictionaryKeyCallBacks")
        private val dictionaryValueCallbacks =
            coreFoundationLibrary.getGlobalVariableAddress("kCFTypeDictionaryValueCallBacks")

        private val secClass = securityConstant("kSecClass")
        private val secClassGenericPassword = securityConstant("kSecClassGenericPassword")
        private val secAttrService = securityConstant("kSecAttrService")
        private val secAttrAccount = securityConstant("kSecAttrAccount")
        private val secValueData = securityConstant("kSecValueData")
        private val secReturnData = securityConstant("kSecReturnData")
        private val secMatchLimit = securityConstant("kSecMatchLimit")
        private val secMatchLimitOne = securityConstant("kSecMatchLimitOne")
        private val trueValue = coreFoundationConstant("kCFBooleanTrue")

        fun put(id: String, secret: String) {
            val updateStatus = withBaseQuery(id) { query ->
                withDictionary {
                    setData(secValueData, secret)
                    invoke("SecItemUpdate", query.pointer, pointer)
                }
            }
            when (updateStatus) {
                ERR_SEC_SUCCESS -> return
                ERR_SEC_ITEM_NOT_FOUND -> Unit
                else -> throw CredentialStorageException()
            }

            val addStatus = withDictionary {
                set(secClass, secClassGenericPassword)
                setString(secAttrService, SERVICE)
                setString(secAttrAccount, id)
                setData(secValueData, secret)
                invoke("SecItemAdd", pointer, null)
            }
            if (addStatus != ERR_SEC_SUCCESS) throw CredentialStorageException()
        }

        fun get(id: String): String? = withBaseQuery(id) { query ->
            query.set(secReturnData, trueValue)
            query.set(secMatchLimit, secMatchLimitOne)
            val result = PointerByReference()
            when (invoke("SecItemCopyMatching", query.pointer, result)) {
                ERR_SEC_ITEM_NOT_FOUND -> null
                ERR_SEC_SUCCESS -> decodeResult(result.value)
                else -> throw CredentialStorageException()
            }
        }

        fun delete(id: String) {
            val status = withBaseQuery(id) { query ->
                invoke("SecItemDelete", query.pointer)
            }
            if (status != ERR_SEC_SUCCESS && status != ERR_SEC_ITEM_NOT_FOUND) {
                throw CredentialStorageException()
            }
        }

        private fun decodeResult(pointer: Pointer?): String {
            val data = pointer?.let(CoreFoundation::CFDataRef)
                ?: throw CredentialStorageException()
            return try {
                val bytes = data.bytePtr.getByteArray(0, data.length)
                try {
                    bytes.decodeToString()
                } finally {
                    bytes.fill(0)
                }
            } catch (_: Throwable) {
                throw CredentialStorageException()
            } finally {
                data.release()
            }
        }

        private inline fun <T> withBaseQuery(
            id: String,
            block: (Dictionary) -> T,
        ): T = withDictionary {
            set(secClass, secClassGenericPassword)
            setString(secAttrService, SERVICE)
            setString(secAttrAccount, id)
            block(this)
        }

        private inline fun <T> withDictionary(block: Dictionary.() -> T): T {
            val dictionary = Dictionary(
                coreFoundation.CFDictionaryCreateMutable(
                    null,
                    CoreFoundation.CFIndex(0),
                    dictionaryKeyCallbacks,
                    dictionaryValueCallbacks,
                ) ?: throw CredentialStorageException()
            )
            return dictionary.use(block)
        }

        private fun invoke(functionName: String, vararg arguments: Any?): Int =
            security.getFunction(functionName).invokeInt(arguments)

        private fun securityConstant(name: String) = CoreFoundation.CFTypeRef(
            security.getGlobalVariableAddress(name).getPointer(0)
                ?: throw CredentialStorageException()
        )

        private fun coreFoundationConstant(name: String) = CoreFoundation.CFTypeRef(
            coreFoundationLibrary.getGlobalVariableAddress(name).getPointer(0)
                ?: throw CredentialStorageException()
        )

        private class Dictionary(
            private val reference: CoreFoundation.CFMutableDictionaryRef,
        ) : AutoCloseable {
            val pointer: Pointer
                get() = reference.pointer

            fun set(key: CoreFoundation.CFTypeRef, value: CoreFoundation.CFTypeRef) {
                reference.setValue(key, value)
            }

            fun setString(key: CoreFoundation.CFTypeRef, value: String) {
                val string = CoreFoundation.CFStringRef.createCFString(value)
                try {
                    set(key, string)
                } finally {
                    string.release()
                }
            }

            fun setData(key: CoreFoundation.CFTypeRef, value: String) {
                val bytes = value.encodeToByteArray()
                try {
                    Memory(bytes.size.toLong()).use { memory ->
                        memory.write(0, bytes, 0, bytes.size)
                        val data = coreFoundation.CFDataCreate(
                            null,
                            memory,
                            CoreFoundation.CFIndex(bytes.size.toLong()),
                        ) ?: throw CredentialStorageException()
                        try {
                            set(key, data)
                        } finally {
                            data.release()
                        }
                    }
                } finally {
                    bytes.fill(0)
                }
            }

            override fun close() {
                reference.release()
            }
        }

        private const val ERR_SEC_SUCCESS = 0
        private const val ERR_SEC_ITEM_NOT_FOUND = -25300
    }
}
