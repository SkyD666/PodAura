package com.skyd.podaura.nano.core

import kotlinx.serialization.Serializable

/** Return results, report no match, ask for details, or explain an unsupported request. */
@Serializable
enum class AssistantAnswerKind { RESULTS, NO_MATCH, CLARIFICATION, UNSUPPORTED }
