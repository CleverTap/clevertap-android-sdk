package com.clevertap.android.sdk

import com.clevertap.android.shared.test.Constant

class CleverTapFixtures {

    companion object {

        val manifestInfo = ManifestInfo(
            Constant.ACC_ID,
            Constant.ACC_TOKEN,
            null,
            "",
            "",
            "",
            false,
            false,
            "notification icon",
            null,
            false,
            false,
            false,
            "fcm:sender:id",
            "some.app.package",
            false,
            "serviceName",
            "push-channel-id",
            emptyArray<String>(),
            0
        )

        fun provideCleverTapInstanceConfig(): CleverTapInstanceConfig =
            CleverTapInstanceConfig.createInstanceWithManifest(
                manifestInfo,
                Constant.ACC_ID,
                Constant.ACC_TOKEN,
                null,
                true
            )
    }
}