package com.skyd.podaura.ui.screen.feed.autodl

import com.skyd.podaura.ui.mvi.MviIntent

sealed interface AutoDownloadRuleIntent : MviIntent {
    data class Init(val feedUrl: String) : AutoDownloadRuleIntent
    data class Enabled(val feedUrl: String, val enabled: Boolean) : AutoDownloadRuleIntent
    data class RequireWifi(val feedUrl: String, val requireWifi: Boolean) : AutoDownloadRuleIntent
    data class RequireBatteryNotLow(val feedUrl: String, val requireBatteryNotLow: Boolean) :
        AutoDownloadRuleIntent

    data class RequireCharging(val feedUrl: String, val requireCharging: Boolean) :
        AutoDownloadRuleIntent

    data class UpdateMaxDownloadCount(val feedUrl: String, val maxDownloadCount: Int) :
        AutoDownloadRuleIntent

    data class UpdateFilterPattern(val feedUrl: String, val filterPattern: String?) :
        AutoDownloadRuleIntent
}