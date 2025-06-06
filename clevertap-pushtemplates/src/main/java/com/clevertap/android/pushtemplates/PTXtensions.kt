package com.clevertap.android.pushtemplates

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
fun String?.isNotNullAndEmpty() : Boolean {
    contract { returns(true) implies (this@isNotNullAndEmpty != null) }
    return isNullOrEmpty().not()
}