package com.skyd.anivu.model.repository.importexport

import android.net.Uri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.skyd.anivu.appContext
import com.skyd.anivu.model.repository.BaseRepository
import com.skyd.anivu.model.preference.dataStore
import com.skyd.anivu.model.preference.preferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.addAll
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.koin.core.annotation.Factory
import java.io.InputStream
import kotlin.time.measureTime

@Factory(binds = [])
class ImportExportRepository : BaseRepository() {

    fun importPreferMeasureTime(preferUri: Uri): Flow<Long> = flow {
        val time = measureTime {
            appContext.contentResolver.openInputStream(preferUri)!!
                .use { importPreferences(it) }
        }.inWholeMilliseconds

        emit(time)
    }.flowOn(Dispatchers.IO)

    @Suppress("UNCHECKED_CAST")
    private suspend fun importPreferences(inputStream: InputStream) {
        /**
         * https://github.com/Kotlin/kotlinx.serialization/issues/746
         * https://github.com/Kotlin/kotlinx.serialization/issues/296
         * https://youtrack.jetbrains.com/issue/KTOR-3063
         */
        val map: Map<String, JsonElement> = Json.decodeFromStream(inputStream)
        dataStore.edit { dsPreferences ->
            preferences.forEach { (pref, kClazz) ->
                val value: JsonElement = map[pref.key.name] ?: return@forEach
                when (kClazz) {
                    String::class -> dsPreferences[pref.key as Preferences.Key<String>] =
                        value.jsonPrimitive.content

                    Int::class -> dsPreferences[pref.key as Preferences.Key<Int>] =
                        value.jsonPrimitive.content.toInt()

                    Boolean::class -> dsPreferences[pref.key as Preferences.Key<Boolean>] =
                        value.jsonPrimitive.content.toBoolean()

                    Float::class -> dsPreferences[pref.key as Preferences.Key<Float>] =
                        value.jsonPrimitive.content.toFloat()

                    Double::class -> dsPreferences[pref.key as Preferences.Key<Double>] =
                        value.jsonPrimitive.content.toDouble()

                    Long::class -> dsPreferences[pref.key as Preferences.Key<Long>] =
                        value.jsonPrimitive.content.toLong()

                    ByteArray::class -> dsPreferences[pref.key as Preferences.Key<ByteArray>] =
                        value.jsonPrimitive.content.encodeToByteArray()

                    Set::class -> dsPreferences[pref.key as Preferences.Key<Set<String>>] =
                        value.jsonArray.map { it.jsonPrimitive.content }.toSet()

                    else -> return@forEach
                }
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun exportPreferMeasureTime(outputFile: Uri): Flow<Long> = flow {
        val time = measureTime {
            appContext.contentResolver.openOutputStream(outputFile)!!.use { outputStream ->
                val dataStorePreferences = dataStore.data.first()
                val json = buildJsonObject {
                    preferences.forEach { (pref, _) ->
                        val k = pref.key.name
                        when (val v = pref.fromPreferences(dataStorePreferences)) {
                            is String -> put(k, v)
                            is Int -> put(k, v)
                            is Boolean -> put(k, v)
                            is Float -> put(k, v)
                            is Double -> put(k, v)
                            is Long -> put(k, v)
                            is ByteArray -> put(k, v.toHexString())
                            is Set<*> -> putJsonArray(k) { addAll(v as Set<String>) }
                            else -> return@forEach
                        }
                    }
                }
                Json.encodeToStream(json, outputStream)
            }
        }.inWholeMilliseconds
        emit(time)
    }.flowOn(Dispatchers.IO)
}