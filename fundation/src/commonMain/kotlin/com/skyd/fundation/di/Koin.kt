package com.skyd.fundation.di

import org.koin.core.parameter.ParametersDefinition
import org.koin.core.qualifier.Qualifier
import org.koin.mp.KoinPlatform.getKoin

inline fun <reified T : Any> get(
    qualifier: Qualifier? = null,
    noinline parameters: ParametersDefinition? = null,
): T = getKoin().get(qualifier, parameters)

inline fun <reified T : Any> inject(): Lazy<T> = lazy { get<T>() }
