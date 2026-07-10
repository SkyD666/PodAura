package io.github.alexzhirkevich.compottie

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf

/**
 * In-memory cache instance for parsed [LottieComposition]s.
 *
 * By default, saves the 10 most recently used animations across the entire application.
 *
 * @see LottieCompositionCache
 * */
public val LocalLottieCache: ProvidableCompositionLocal<LottieCompositionCache> = compositionLocalOf {
    LottieCompositionCache(10)
}

/**
 * In-memory cache for parsed [LottieComposition]s.
 *
 * Reduces number of costly operations (such as resource reading and JSON parsing)
 * for frequently used animations
 * */
public interface LottieCompositionCache {

    /**
     * Get cached composition for [key] or [create] new one and cache it by [key]
     * */
    public suspend fun getOrPut(
        key: Any?,
        create: suspend () -> LottieComposition
    ): LottieComposition

    public fun get(key: Any?): LottieComposition? = null

    /**
     * Clear all in-memory cached compositions.
     * This will not clear the file system cache
     * */
    public suspend fun clear()

    @Deprecated("Use null cache instead")
    public object Empty : LottieCompositionCache {

        override suspend fun getOrPut(
            key: Any?,
            create: suspend () -> LottieComposition
        ): LottieComposition = create()

        override suspend fun clear() {}
    }
}

/**
 * Parse and prepare composition [spec] for instant displaying.
 *
 * NOTE: displaying can still take some time if the composition
 * requires external resources or custom fonts.
 * */
public suspend fun LottieCompositionCache.prepare(spec: LottieCompositionSpec): LottieComposition {
    return getOrPut(spec.key, spec::load)
}

/**
 * In-memory cache factory for parsed [LottieComposition]s.
 *
 * @param size number of least recently used compositions to store
 * */
public fun LottieCompositionCache(size: Int): LottieCompositionCache = object : LottieCompositionCache {

    private val cache = LruMap<LottieComposition>(limit = size)


    override fun get(key: Any?): LottieComposition? {
        return if (key == null) null else cache[key]
    }

    override suspend fun getOrPut(
        key: Any?,
        create: suspend () -> LottieComposition
    ): LottieComposition {
        if (key == null)
            return create()

        return cache.getOrPutSuspend(key, create)
    }

    override suspend fun clear() {
        cache.clear()
    }
}
