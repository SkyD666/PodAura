package com.skyd.podaura.model.repository.importexport

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.skyd.podaura.model.preference.dataStore
import com.skyd.podaura.model.preference.preferences
import com.skyd.podaura.model.repository.BaseRepository
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.sink
import io.github.vinceglb.filekit.source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.addAll
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.io.decodeFromSource
import kotlinx.serialization.json.io.encodeToSink
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.koin.core.annotation.Factory
import kotlin.time.measureTime

@Factory(binds = [])
class ImportExportRepository : BaseRepository() {

    fun importPreferMeasureTime(jsonFile: PlatformFile): Flow<Long> = flow {
        val time = measureTime {
            jsonFile.source().buffered().use { importPreferences(it) }
        }.inWholeMilliseconds
        emit(time)
    }.flowOn(Dispatchers.IO)

    @Suppress("UNCHECKED_CAST")
    private suspend fun importPreferences(source: Source) {
        /**
         * https://github.com/Kotlin/kotlinx.serialization/issues/746
         * https://github.com/Kotlin/kotlinx.serialization/issues/296
         * https://youtrack.jetbrains.com/issue/KTOR-3063
         */
        val map: Map<String, JsonElement> = Json.decodeFromSource(source)
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
    fun exportPreferMeasureTime(outputFile: PlatformFile): Flow<Long> = flow {
        val time = measureTime {
            outputFile.sink().buffered().use { sink ->
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
                Json.encodeToSink(json, sink)
            }
        }.inWholeMilliseconds
        emit(time)
    }.flowOn(Dispatchers.IO)
}