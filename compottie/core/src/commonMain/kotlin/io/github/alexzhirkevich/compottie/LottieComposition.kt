package io.github.alexzhirkevich.compottie

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.util.lerp
import io.github.alexzhirkevich.compottie.assets.LottieAssetsManager
import io.github.alexzhirkevich.compottie.assets.LottieFontManager
import io.github.alexzhirkevich.compottie.internal.Animation
import io.github.alexzhirkevich.compottie.internal.AnimationTheme
import io.github.alexzhirkevich.compottie.internal.CombinedSlotResolver
import io.github.alexzhirkevich.compottie.internal.LottieJson
import io.github.alexzhirkevich.compottie.internal.SlotResolver
import io.github.alexzhirkevich.compottie.internal.animation.expressions.ExpressionComposition
import io.github.alexzhirkevich.compottie.internal.assets.CharacterData
import io.github.alexzhirkevich.compottie.internal.assets.ImageAsset
import io.github.alexzhirkevich.compottie.internal.assets.LottieAsset
import io.github.alexzhirkevich.compottie.internal.helpers.Marker
import io.github.alexzhirkevich.compottie.internal.layers.Layer
import io.github.alexzhirkevich.compottie.statemachine.SMConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Transient
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration
import kotlin.time.Duration.Companion.microseconds


/**
 * Load and prepare [LottieComposition] for displaying.
 *
 * Remembers the composition produced by the [spec] if all [keys] are equal to the
 * values they had in the previous composition, otherwise produce and remember a new [LottieCompositionResult] by
 * calling [spec] again.
 * */
@OptIn(InternalCompottieApi::class)
@Composable
public fun rememberLottieComposition(
    vararg keys: Any?,
    cache: LottieCompositionCache? = LocalLottieCache.current,
    coroutineContext: CoroutineContext = Dispatchers.IO,
    spec: suspend () -> LottieCompositionSpec,
): LottieCompositionResult {

    val result = remember(*keys, cache) {
        LottieCompositionResultImpl()
    }

    LaunchedEffect(result) {
        try {
            val composition = withContext(coroutineContext) {
                val specInstance = spec()
                cache?.getOrPut(specInstance.key, specInstance::load) ?: specInstance.load()
            }
            result.complete(composition)
        } catch (c: CancellationException) {
            result.completeExceptionally(c)
            throw c
        } catch (t: Throwable) {
            result.completeExceptionally(
                CompottieException("Composition failed to load", t)
            )
        }
    }

    return result
}

/**
 * Load and prepare [LottieComposition] for displaying.
 *
 * Immediately returns the composition instance if it is already stored in the [cache].
 * Otherwise asynchronously loads, parses, caches and returns a new composition instance.
 * */
@OptIn(InternalCompottieApi::class)
@Composable
public fun rememberLottieComposition(
    spec: LottieCompositionSpec,
    cache: LottieCompositionCache? = LocalLottieCache.current,
    coroutineContext: CoroutineContext = Dispatchers.IO,
): LottieCompositionResult {

    val result = remember(spec, cache) {
        LottieCompositionResultImpl(cache?.get(spec.key))
    }

    if (!result.isComplete) {
        LaunchedEffect(result) {
            try {
                val composition = withContext(coroutineContext) {
                    cache?.getOrPut(spec.key, spec::load) ?: spec.load()
                }
                result.complete(composition)
            } catch (c: CancellationException) {
                result.completeExceptionally(c)
                throw c
            } catch (t: Throwable) {
                result.completeExceptionally(
                    CompottieException("Composition failed to load", t)
                )
            }
        }
    }

    return result
}

@Stable
public class LottieComposition internal constructor(
    internal val animation: Animation,
) {

    public companion object {
        public fun parse(json: String): LottieComposition {
            return LottieComposition(LottieJson.decodeFromString(json))
        }
    }


    /**
     * Frame when animation becomes visible
     * */
    public val startFrame: Float get() = animation.inPoint

    /**
     * Frame when animation becomes no longer visible
     * */
    public val endFrame: Float get() = animation.outPoint

    /**
     * Animation duration
     * */
    public val duration: Duration = (durationFrames / frameRate * 1_000_000).toInt().microseconds

    public val durationFrames: Float
        get() = animation.outPoint - animation.inPoint

    /**
     * Animation start time in seconds
     * */
    public val startTime: Float
        get() = animation.inPoint / animation.frameRate

    /**
     * Animation frame rate
     * */
    public val frameRate: Float get() = animation.frameRate

    /**
     * Animation intrinsic width
     * */
    public val width: Float get() = animation.width

    /**
     * Animation intrinsic height
     * */
    public val height: Float get() = animation.height

    /**
     * Some animations may contain predefined number of interactions.
     * It will be used as a default value for the LottiePainter
     * */
    public var iterations: Int by mutableStateOf(1)
        @InternalCompottieApi
        set

    /**
     * Some animations may contain predefined speed multiplier.
     * It will be used as a default value for the LottiePainter
     * */
    public var speed: Float by mutableFloatStateOf(1f)
        @InternalCompottieApi
        set

    public var stateMachines: Map<String, SMConfig>? by mutableStateOf(null)
        @InternalCompottieApi
        set


    @Transient
    public var themes: Map<String, AnimationTheme>? = null
        @InternalCompottieApi
        set

    internal val slotResolver: SlotResolver = CombinedSlotResolver(
        first = { themes?.get(it.theme) },
        second = { animation.slots },
    )

    internal val expressionComposition = object : ExpressionComposition {

        override val name: String?
            get() = animation.name
        override val width: Float
            get() = this@LottieComposition.width
        override val height: Float
            get() = this@LottieComposition.height
        override val startTime: Float
            get() = this@LottieComposition.startTime

        override val durationFrames: Float
            get() = this@LottieComposition.endFrame - this@LottieComposition.startTime

        override val layersByName: Map<String, Layer> by lazy {
            animation.layers.associateBy { it.name.orEmpty() }
        }

        override val layers: List<Layer>
            get() = animation.layers

    }

    private val charGlyphs: Map<String, Map<String, CharacterData>> =
        animation.chars
            .groupBy(CharacterData::fontFamily)
            .mapValues { it.value.associateBy(CharacterData::character) }


    private val assetsMutex = Mutex()
    private val fontsMutex = Mutex()

    private var storedFonts: MutableMap<String, FontFamily> = mutableMapOf()

    private val markersMap = animation.markers.associateBy(Marker::name)


    internal val hasAssets: Boolean
        get() = animation.assets.isNotEmpty()

    internal val hasFonts: Boolean
        get() = animation.fonts?.list?.isNotEmpty() == true


    internal fun frameToProgress(frame: Float): Float {
        val p = (frame - animation.inPoint) /
                (animation.outPoint - animation.inPoint)
        return p.coerceIn(0f, 1f)
    }

    internal fun progressToFrame(progress: Float): Float {
        return lerp(startFrame, endFrame, progress.coerceIn(0f, 1f))
    }

    internal fun findGlyphs(family: String?): Map<String, CharacterData>? {
        return charGlyphs[family] ?: run {
            val font = animation.fonts?.list
                ?.find { it.name == family || it.family == family }
                ?: return@run null

            charGlyphs[font.family] ?: charGlyphs[font.name]
        }
    }


    @InternalCompottieApi
    public suspend fun prepareAssets(
        assetsManager: LottieAssetsManager,
        extraAssets: List<LottieAsset> = emptyList()
    ) {
        assetsMutex.withLock {
            loadAssets(assetsManager, false, extraAssets)
        }
    }

    @InternalCompottieApi
    public suspend fun prepareFonts(fontsManager: LottieFontManager) {
        fontsMutex.withLock {
            storedFonts.putAll(loadFontsInternal(fontsManager))
        }
    }

    internal suspend fun loadAssets(
        assetsManager: LottieAssetsManager,
        copy: Boolean,
        extraAssets: List<LottieAsset> = emptyList(),
    ): List<LottieAsset> {

        val assets = if (copy)
            animation.assets.map(LottieAsset::copy) + extraAssets
        else animation.assets + extraAssets

        coroutineScope {
            assets.mapNotNull { asset ->
                when (asset) {
                    is ImageAsset -> {
                        try {
                            asset.prepare()
                        } catch (t: Throwable) {
                            Compottie.logger?.error("Failed to prepare asset ${asset.name}", t)
                        }

                        if (asset.bitmap == null) {
                            launch {
                                try {
                                    assetsManager.image(asset.spec)?.let {
                                        asset.setBitmap(it.toBitmap(asset.width, asset.height))
                                    }
                                } catch (t: CancellationException) {
                                    throw t
                                } catch (t: Throwable) {
                                    Compottie.logger?.error("Image asset failed to load: ${asset.name}", t)
                                }
                            }
                        } else null
                    }

                    else -> null
                }
            }.joinAll()
        }
        return assets
    }

    internal suspend fun loadFonts(fontManager: LottieFontManager): Map<String, FontFamily> {
        return coroutineScope {
            storedFonts + loadFontsInternal(fontManager)
        }
    }

    private suspend fun loadFontsInternal(fontManager: LottieFontManager): Map<String, FontFamily> {
        return coroutineScope {
            storedFonts + animation.fonts?.list
                ?.fastMap {
                    async {
                        try {
                            val f = it.font ?: fontManager.font(it.spec)

                            it.font = f

                            if (f == null)
                                null
                            else listOf(it.family to f, it.name to f)
                        } catch (t: CancellationException) {
                            throw t
                        } catch (t: Throwable) {
                            Compottie.logger?.error("Font asset failed to load: ${it.family} - ${it.name}", t)
                            null
                        }
                    }
                }
                ?.awaitAll()
                ?.filterNotNull()
                ?.flatten()
                ?.groupBy { it.first }
                ?.filterValues { it.isNotEmpty() }
                ?.mapValues { FontFamily(it.value.map { it.second }) }
                .orEmpty()
        }
    }

    @OptIn(InternalCompottieApi::class)
    internal fun deepCopy(): LottieComposition {
        return LottieComposition(animation.deepCopy()).also {
            it.themes = themes
            it.iterations = iterations
            it.speed = speed
            it.stateMachines = stateMachines
        }
    }

    internal fun marker(name: String?) = markersMap[name]
}
