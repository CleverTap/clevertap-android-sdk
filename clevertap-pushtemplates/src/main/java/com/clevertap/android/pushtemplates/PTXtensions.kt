package com.clevertap.android.pushtemplates

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract
import androidx.annotation.RestrictTo

@OptIn(ExperimentalContracts::class)
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
internal fun String?.isNotNullAndEmpty() : Boolean {
    contract { returns(true) implies (this@isNotNullAndEmpty != null) }
    return isNullOrEmpty().not()
}
