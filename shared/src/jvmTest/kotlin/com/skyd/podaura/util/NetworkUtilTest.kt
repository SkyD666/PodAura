package com.skyd.podaura.util

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NetworkUtilTest {
    @Test
    fun unrestrictedWifiIsFree() {
        assertTrue(isFreeWindowsNetwork(cost = 0x1, interfaceType = 71))
    }

    @Test
    fun meteredWifiIsNotFree() {
        assertFalse(isFreeWindowsNetwork(cost = 0x2, interfaceType = 71))
        assertFalse(isFreeWindowsNetwork(cost = 0x4, interfaceType = 71))
    }

    @Test
    fun unrestrictedEthernetIsNotFree() {
        assertFalse(isFreeWindowsNetwork(cost = 0x1, interfaceType = 6))
    }

    @Test
    fun unrestrictedNetworkIsFree() {
        assertTrue(isFreeWindowsNetworkCost(0x1))
    }

    @Test
    fun meteredNetworksAreNotFree() {
        assertFalse(isFreeWindowsNetworkCost(0x2))
        assertFalse(isFreeWindowsNetworkCost(0x4))
    }

    @Test
    fun limitedOrRoamingNetworksAreNotFree() {
        assertFalse(isFreeWindowsNetworkCost(0x10001))
        assertFalse(isFreeWindowsNetworkCost(0x40001))
        assertFalse(isFreeWindowsNetworkCost(0x80001))
    }

    @Test
    fun congestedUnrestrictedNetworkIsStillFree() {
        assertTrue(isFreeWindowsNetworkCost(0x20001))
    }

    @Test
    fun unknownNetworkIsNotFree() {
        assertFalse(isFreeWindowsNetworkCost(0))
    }
}
