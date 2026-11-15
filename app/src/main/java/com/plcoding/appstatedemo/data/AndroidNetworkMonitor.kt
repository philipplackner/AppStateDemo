package com.plcoding.appstatedemo.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

class AndroidNetworkMonitor(context: Context) : NetworkMonitor {

    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)

    private val _isOnline = mutableStateOf(checkCurrentConnectivity())
    override val isOnline: State<Boolean> = _isOnline

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities,
        ) {
            _isOnline.value = networkCapabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            )
        }

        override fun onLost(network: Network) {
            _isOnline.value = false
        }
    }

    init {
        connectivityManager.registerDefaultNetworkCallback(callback)
    }

    private fun checkCurrentConnectivity(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
