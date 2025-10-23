package com.clevertap.android.sdk.features

import com.clevertap.android.sdk.CoreContract

internal interface CleverTapFeature {

    fun coreContract(coreContract: CoreContract)
}