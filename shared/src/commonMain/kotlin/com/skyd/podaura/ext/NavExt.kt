package com.skyd.podaura.ext

import com.skyd.podaura.ui.component.navigation.ListDetailSceneStrategy

val ListDetailSceneStrategy<*>.isSinglePane: Boolean
    get() = directive.maxHorizontalPartitions == 1
