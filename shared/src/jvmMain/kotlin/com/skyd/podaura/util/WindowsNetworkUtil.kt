package com.skyd.podaura.util

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.COM.COMUtils
import com.sun.jna.platform.win32.COM.Unknown
import com.sun.jna.platform.win32.Guid.CLSID
import com.sun.jna.platform.win32.Guid.IID
import com.sun.jna.platform.win32.IPHlpAPI
import com.sun.jna.platform.win32.Ole32
import com.sun.jna.platform.win32.WTypes
import com.sun.jna.platform.win32.WinDef.DWORDByReference
import com.sun.jna.platform.win32.WinError
import com.sun.jna.platform.win32.WinNT.HRESULT
import com.sun.jna.ptr.PointerByReference
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.NetworkInterface

internal fun isWindowsFreeNetworkAvailable(): Boolean {
    val interfaceType = getWindowsDefaultRouteInterfaceType() ?: return false

    val initializationResult = Ole32.INSTANCE.CoInitializeEx(
        null,
        Ole32.COINIT_MULTITHREADED,
    )
    val shouldUninitialize = COMUtils.SUCCEEDED(initializationResult)
    if (!shouldUninitialize && initializationResult.toInt() != WinError.RPC_E_CHANGED_MODE) {
        return false
    }

    var networkCostManager: NetworkCostManager? = null
    try {
        val managerPointer = PointerByReference()
        val creationResult = Ole32.INSTANCE.CoCreateInstance(
            CLSID_NETWORK_LIST_MANAGER,
            null,
            WTypes.CLSCTX_ALL,
            IID_NETWORK_COST_MANAGER,
            managerPointer,
        )
        if (COMUtils.FAILED(creationResult)) return false

        networkCostManager = NetworkCostManager(managerPointer.value)
        val cost = DWORDByReference()
        val costResult = networkCostManager.getCost(cost)
        if (COMUtils.FAILED(costResult)) return false

        return isFreeWindowsNetwork(cost.value.toLong(), interfaceType)
    } finally {
        networkCostManager?.Release()
        if (shouldUninitialize) Ole32.INSTANCE.CoUninitialize()
    }
}

private fun getWindowsDefaultRouteInterfaceType(): Int? {
    val localAddress = DatagramSocket().use { socket ->
        // Connecting a UDP socket selects a route without sending a packet.
        socket.connect(InetAddress.getByAddress(DEFAULT_ROUTE_PROBE_ADDRESS), DISCARD_PORT)
        socket.localAddress
    }
    if (localAddress.isAnyLocalAddress) return null

    val networkInterface = NetworkInterface.getByInetAddress(localAddress) ?: return null
    if (networkInterface.index < 0) return null

    val interfaceRow = IPHlpAPI.MIB_IF_ROW2().apply {
        InterfaceIndex = networkInterface.index
    }
    if (IPHlpAPI.INSTANCE.GetIfEntry2(interfaceRow) != WinError.NO_ERROR) return null
    return interfaceRow.Type
}

internal fun isFreeWindowsNetwork(cost: Long, interfaceType: Int): Boolean =
    interfaceType == IF_TYPE_IEEE80211 && isFreeWindowsNetworkCost(cost)

internal fun isFreeWindowsNetworkCost(cost: Long): Boolean {
    val costLevel = cost and COST_LEVEL_MASK
    val paidOrLimitedFlags = cost and PAID_OR_LIMITED_FLAGS
    return costLevel == NLM_CONNECTION_COST_UNRESTRICTED && paidOrLimitedFlags == 0L
}

private class NetworkCostManager(pointer: Pointer) : Unknown(pointer) {
    fun getCost(cost: DWORDByReference): HRESULT = _invokeNativeObject(
        GET_COST_VTABLE_INDEX,
        arrayOf(pointer, cost, Pointer.NULL),
        HRESULT::class.java,
    ) as HRESULT
}

private val CLSID_NETWORK_LIST_MANAGER = CLSID("{DCB00C01-570F-4A9B-8D69-199FDBA5723B}")
private val IID_NETWORK_COST_MANAGER = IID("{DCB00008-570F-4A9B-8D69-199FDBA5723B}")

private const val GET_COST_VTABLE_INDEX = 3
private const val IF_TYPE_IEEE80211 = 71
private const val DISCARD_PORT = 9
private const val COST_LEVEL_MASK = 0xFFFFL
private const val NLM_CONNECTION_COST_UNRESTRICTED = 0x1L
private const val NLM_CONNECTION_COST_FIXED = 0x2L
private const val NLM_CONNECTION_COST_VARIABLE = 0x4L
private const val NLM_CONNECTION_COST_OVER_DATALIMIT = 0x10000L
private const val NLM_CONNECTION_COST_ROAMING = 0x40000L
private const val NLM_CONNECTION_COST_APPROACHING_DATALIMIT = 0x80000L
private const val PAID_OR_LIMITED_FLAGS = NLM_CONNECTION_COST_FIXED or
        NLM_CONNECTION_COST_VARIABLE or
        NLM_CONNECTION_COST_OVER_DATALIMIT or
        NLM_CONNECTION_COST_ROAMING or
        NLM_CONNECTION_COST_APPROACHING_DATALIMIT

private val DEFAULT_ROUTE_PROBE_ADDRESS = byteArrayOf(1, 1, 1, 1)
