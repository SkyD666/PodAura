package com.skyd.anivu.ui.screen.about.update

import com.skyd.anivu.appContext
import com.skyd.anivu.base.mvi.AbstractMviViewModel
import com.skyd.anivu.config.Const
import com.skyd.anivu.ext.catchMap
import com.skyd.anivu.ext.getAppVersionCode
import com.skyd.anivu.ext.openBrowser
import com.skyd.anivu.ext.startWith
import com.skyd.anivu.ext.toDateTimeString
import com.skyd.anivu.model.repository.UpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.take
import okhttp3.internal.toLongOrDefault
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject

@HiltViewModel
class UpdateViewModel @Inject constructor(private var updateRepo: UpdateRepository) :
    AbstractMviViewModel<UpdateIntent, UpdateState, UpdateEvent>() {

    override val viewState: StateFlow<UpdateState>

    init {
        val initialVS = UpdateState.initial()

        viewState = merge(
            intentFlow.filter { it is UpdateIntent.CheckUpdate && !it.isRetry }.take(1),
            intentFlow.filter { it is UpdateIntent.CheckUpdate && it.isRetry },
            intentFlow.filterNot { it is UpdateIntent.CheckUpdate }
        )
            .toUpdatePartialStateChangeFlow()
            .debugLog("UpdatePartialStateChange")
            .sendSingleEvent()
            .scan(initialVS) { vs, change -> change.reduce(vs) }
            .debugLog("ViewState")
            .toState(initialVS)
    }

    private fun Flow<UpdatePartialStateChange>.sendSingleEvent(): Flow<UpdatePartialStateChange> {
        return onEach { change ->
            val event = when (change) {
                is UpdatePartialStateChange.Error -> UpdateEvent.CheckError(change.msg)
                is UpdatePartialStateChange.CheckUpdate.NoUpdate,
                is UpdatePartialStateChange.CheckUpdate.HasUpdate -> UpdateEvent.CheckSuccess()

                else -> return@onEach
            }
            sendEvent(event)
        }
    }

    private fun Flow<UpdateIntent>.toUpdatePartialStateChangeFlow(): Flow<UpdatePartialStateChange> {
        return merge(
            filterIsInstance<UpdateIntent.CheckUpdate>().flatMapConcat {
                updateRepo.checkUpdate().map { data ->
                    if (appContext.getAppVersionCode() < data.tagName.toLongOrDefault(0L)) {
                        val date = SimpleDateFormat(
                            "yyyy-MM-dd'T'HH:mm:ss'Z'",
                            Locale.getDefault()
                        ).apply {
                            timeZone = TimeZone.getTimeZone("UTC")
                        }.parse(data.publishedAt)
                        val publishedAt: String =
                            date?.toDateTimeString(context = appContext) ?: data.publishedAt

                        UpdatePartialStateChange.CheckUpdate.HasUpdate(
                            data.copy(publishedAt = publishedAt)
                        )
                    } else {
                        UpdatePartialStateChange.CheckUpdate.NoUpdate
                    }
                }.startWith(UpdatePartialStateChange.LoadingDialog)
                    .catchMap { UpdatePartialStateChange.Error(it.message.orEmpty()) }
            },

            filterIsInstance<UpdateIntent.Update>().map { intent ->
                (intent.url ?: Const.GITHUB_REPO).openBrowser(appContext)
                UpdatePartialStateChange.RequestUpdate
            },
        )
    }
}