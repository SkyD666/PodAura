package com.skyd.podaura.podnya.capabilities

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionReference(val title: String, val feedUrl: String)

internal interface SubscriptionTargetSource {
    fun currentSubscriptions(): Map<String, SubscriptionReference>
}

internal interface SubscriptionHistory {
    fun subscriptions(): List<SubscriptionReference>
}
