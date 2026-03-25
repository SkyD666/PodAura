package com.skyd.podaura.ui.screen.feed.sheet

import androidx.paging.PagingData
import com.skyd.mvi.MviViewState
import com.skyd.podaura.model.bean.feed.FeedViewBean
import com.skyd.podaura.model.bean.group.GroupVo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

data class FeedSheetState(
    val editFeedDialogBean: FeedViewBean?,
    val groups: Flow<PagingData<GroupVo>>,
    val loadingDialog: Boolean,
) : MviViewState {
    companion object {
        fun initial() = FeedSheetState(
            editFeedDialogBean = null,
            groups = flowOf(PagingData.empty()),
            loadingDialog = false,
        )
    }
}