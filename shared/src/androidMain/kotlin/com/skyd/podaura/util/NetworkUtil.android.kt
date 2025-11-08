package com.skyd.podaura.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.skyd.fundation.di.get

actual fun isFreeNetworkAvailable(): Boolean {
    val connectivityManager =
        get<Context>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = connectivityManager.activeNetwork ?: return false
    val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

    return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}