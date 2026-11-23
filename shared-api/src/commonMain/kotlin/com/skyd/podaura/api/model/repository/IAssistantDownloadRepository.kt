package com.skyd.podaura.api.model.repository

import com.skyd.podaura.api.model.bean.download.AssistantDownloadArticle
import com.skyd.podaura.api.model.bean.download.AssistantDownloadReceipt
import com.skyd.podaura.api.model.bean.download.AssistantDownloadScope
import com.skyd.podaura.api.model.bean.download.AssistantDownloadTarget

interface IAssistantDownloadRepository {
    suspend fun articlesByIds(articleIds: List<String>): List<AssistantDownloadArticle>

    /** Returns matching local articles in descending publication order, including articles without attachments. */
    suspend fun articlesInFeed(scope: AssistantDownloadScope): List<AssistantDownloadArticle>

    /** Check whether a user-provided HTTP(S) link can be queued for guarded transfer. A network failure may throw. */
    suspend fun checkDirectLink(url: String): Boolean

    /** Submit validated targets. Repeating a submission returns its original receipt. */
    suspend fun submit(
        batchId: String,
        submissionId: String,
        targets: List<AssistantDownloadTarget>,
    ): AssistantDownloadReceipt

    suspend fun receipt(batchId: String, submissionId: String): AssistantDownloadReceipt?
}
