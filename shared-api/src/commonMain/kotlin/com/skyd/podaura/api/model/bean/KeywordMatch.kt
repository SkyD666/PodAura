package com.skyd.podaura.api.model.bean

import kotlinx.serialization.Serializable

/** Match every keyword (ALL), or at least one keyword (ANY). */
@Serializable
enum class KeywordMatch { ALL, ANY }
