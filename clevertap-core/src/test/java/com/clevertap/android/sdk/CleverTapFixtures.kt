package com.clevertap.android.sdk

import com.clevertap.android.shared.test.Constant

class CleverTapFixtures {

    companion object {

        const val PUBLIC_ENCRYPTION_KEY = "public-encryption-key"
        const val PUBLIC_ENCRYPTION_KEY_VERSION = "public-encryption-key-version-v1"

        val manifestInfo = ManifestInfo(
            Constant.ACC_ID,
            Constant.ACC_TOKEN,
            null,
            "",
            "",
            "",
            PUBLIC_ENCRYPTION_KEY,
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
            ).apply {
                publicEncryptionKey = PUBLIC_ENCRYPTION_KEY
                publicEncryptionKeyVersion = PUBLIC_ENCRYPTION_KEY_VERSION
            }

        fun configWithoutEncryptionKey(): CleverTapInstanceConfig =
            CleverTapInstanceConfig.createInstanceWithManifest(
                manifestInfo,
                Constant.ACC_ID,
                Constant.ACC_TOKEN,
                null,
                true
            ).apply {
                publicEncryptionKey = null
                publicEncryptionKeyVersion = null
            }
    }
}