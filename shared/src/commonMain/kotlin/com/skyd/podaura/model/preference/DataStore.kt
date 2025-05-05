package com.skyd.podaura.model.preference

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.skyd.ksp.preference.PreferencesList
import com.skyd.podaura.di.inject
import okio.Path.Companion.toPath
import kotlin.reflect.KClass


fun createDataStore(dirPath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        produceFile = { dirPath().toPath().resolve(dataStoreFileName) }
    )

internal const val dataStoreFileName = "App.preferences_pb"

val dataStore: DataStore<Preferences> by inject()

@PreferencesList
expect val preferences: List<Pair<BasePreference<*>, KClass<*>>>