package io.github.alexzhirkevich.compottie

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import okio.ByteString.Companion.encodeUtf8

@Stable
public interface LottieCompositionSpec {

    /**
     * Key that uniquely identifies composition instance.
     * Equal specs should produce the same key
     * */
    public val key: String?

    public suspend fun load(): LottieComposition

    public companion object {

        /**
         *  [LottieComposition] from a [jsonString]
         */
        @Stable
        public fun JsonString(
            jsonString: String
        ): LottieCompositionSpec = JsonStringImpl(jsonString)
    }
}

@Immutable
private class JsonStringImpl(
    private val jsonString: String
) : LottieCompositionSpec {

    override val key: String = "string_${jsonString.encodeUtf8().md5()}"

    override suspend fun load(): LottieComposition {
        return LottieComposition.parse(jsonString)
    }

    override fun toString(): String {
        return "JsonString(jsonString='$jsonString')"
    }
}
