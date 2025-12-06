# SLF4J
-keep class org.slf4j.** { *; }

# Apache5 engine
-dontwarn org.conscrypt.Conscrypt
-dontwarn org.brotli.dec.BrotliInputStream
-keep class io.ktor.client.engine.apache5.Apache5EngineContainer

# Database
-keep class com.skyd.podaura.model.db.AppDatabase_Impl # ClassNotFoundError
-keep class com.skyd.downloader.db.DownloadDatabase_Impl # ClassNotFoundError
-keep class androidx.sqlite.driver.bundled.** { *; } # Caused by: java.lang.NoSuchMethodError: Method androidx.sqlite.driver.bundled.BundledSQLiteDriverKt.nativeThreadSafeMode()I not found
-keep class org.sqlite.** { *; }

# Ktor
-keep class io.ktor.serialization.kotlinx.json.KotlinxSerializationJsonExtensionProvider

# Coil
-keep class coil3.util.DecoderServiceLoaderTarget { *; }
-keep class coil3.util.FetcherServiceLoaderTarget { *; }
-keep class coil3.util.ServiceLoaderComponentRegistry { *; }
-keep class * implements coil3.util.DecoderServiceLoaderTarget { *; }
-keep class * implements coil3.util.FetcherServiceLoaderTarget { *; }

# Okio
-keep class okio.Okio__OkioKt { *; } # VerifyError

# Jna
-keep class com.sun.jna.** { *; }
-keep class com.skyd.fundation.jna.** { *; }