package com.skyd.podaura.ui.screen.media

import com.skyd.podaura.model.bean.MediaGroupBean
import com.skyd.podaura.ui.mvi.MviSingleEvent

sealed interface MediaEvent : MviSingleEvent {
    sealed interface DeleteGroupResultEvent : MediaEvent {
        data class Success(val timestamp: Long) : DeleteGroupResultEvent
        data class Failed(val msg: String) : DeleteGroupResultEvent
    }

    sealed interface MoveFilesToGroupResultEvent : MediaEvent {
        data class Success(val timestamp: Long) : MoveFilesToGroupResultEvent
        data class Failed(val msg: String) : MoveFilesToGroupResultEvent
    }

    sealed interface ChangeFileGroupResultEvent : MediaEvent {
        data class Success(val timestamp: Long) : ChangeFileGroupResultEvent
        data class Failed(val msg: String) : ChangeFileGroupResultEvent
    }

    sealed interface CreateGroupResultEvent : MediaEvent {
        data class Success(val timestamp: Long) : CreateGroupResultEvent
        data class Failed(val msg: String) : CreateGroupResultEvent
    }

    sealed interface EditGroupResultEvent : MediaEvent {
        data class Success(val group: MediaGroupBean) : EditGroupResultEvent
        data class Failed(val msg: String) : EditGroupResultEvent
    }
}