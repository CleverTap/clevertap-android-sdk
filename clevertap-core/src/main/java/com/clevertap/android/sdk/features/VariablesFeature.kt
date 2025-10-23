package com.clevertap.android.sdk.features

import com.clevertap.android.sdk.variables.CTVariables
import com.clevertap.android.sdk.variables.Parser
import com.clevertap.android.sdk.variables.VarCache
import com.clevertap.android.sdk.variables.repo.VariablesRepo

/**
 * CleverTap Variables feature
 * Manages dynamic variables, product config, and feature flags
 */
internal data class VariablesFeature(
    val cTVariables: CTVariables,
    val varCache: VarCache,
    val parser: Parser,
    val variablesRepository: VariablesRepo
)
