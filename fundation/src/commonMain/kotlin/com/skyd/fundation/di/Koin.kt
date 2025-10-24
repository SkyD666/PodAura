package com.skyd.fundation.di

import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier

inline fun <reified T : Any> get(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null,
): T {
    return object : KoinComponent {
        val value: T = get(qualifier, parameters)
    }.value
}

inline fun <reified T : Any> inject(): Lazy<T> = lazy { get<T>() }