package com.skyd.podaura.model.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences

fun createDataStore(context: Context): DataStore<Preferences> = createDataStore(
    dirPath = { context.filesDir.absolutePath }
)