package com.skyd.podaura.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.navigation3.runtime.NavKey
import com.skyd.podaura.podnya.core.AssistantAction
import org.koin.core.module.Module
import org.koin.core.scope.Scope
import kotlin.uuid.Uuid

/** All dependencies and host actions installed by one application registration. */
internal class PodnyaPlugin(
    val id: String,
    vararg val modules: Module,
    val bindNavigation: @Composable (
        scope: Scope,
        navigate: (NavKey) -> Unit,
    ) -> (conversationId: Uuid, action: AssistantAction) -> Unit,
)

@Composable
internal fun List<PodnyaPlugin>.bindNavigation(
    scope: Scope,
    navigate: (NavKey) -> Unit,
): (conversationId: Uuid, action: AssistantAction) -> Unit {
    val handlers = map { plugin ->
        key(plugin.id) { plugin.bindNavigation(scope, navigate) }
    }
    return { id, action -> handlers.forEach { it(id, action) } }
}
