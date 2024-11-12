package com.clevertap.android.sdk.validation

import android.content.pm.ActivityInfo
import android.content.pm.PackageInfo
import android.content.pm.ServiceInfo
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.robolectric.shadows.ShadowPackageManager
import kotlin.test.assertTrue
import kotlin.test.assertFalse


class ManifestValidatorTest :BaseTestCase() {
    @Test
    fun `test isComponentPresentInManifest() should return true if service is present in manifest`() {

        val service = ServiceInfo().also {
            it.name = "com.example.MyService"
            it.packageName = application.applicationInfo.packageName
        }
        val packageInfo = PackageInfo().also {
            it.packageName = application.applicationInfo.packageName
            it.applicationInfo = application.applicationInfo
            it.services = arrayOf(service)
        }
        ShadowPackageManager().installPackage(packageInfo)

        assertTrue {
            ManifestValidator.isComponentPresentInManifest(
                appCtx,
                "com.example.MyService",
                ManifestValidator.ComponentType.SERVICE
            )
        }
    }

    @Test
    fun `test isComponentPresentInManifest() should return true if receiver is present in manifest`() {

        val receiver = ActivityInfo().also {
            it.name = "com.example.MyReceiver"
            it.packageName = application.applicationInfo.packageName
        }
        val packageInfo = PackageInfo().also {
            it.packageName = application.applicationInfo.packageName
            it.applicationInfo = application.applicationInfo
            it.receivers = arrayOf(receiver)
        }
        ShadowPackageManager().installPackage(packageInfo)

        assertTrue {
            ManifestValidator.isComponentPresentInManifest(
                appCtx,
                "com.example.MyReceiver",
                ManifestValidator.ComponentType.RECEIVER
            )
        }
    }

    @Test
    fun `test isComponentPresentInManifest() should return true if activity is present in manifest`() {

        val activity = ActivityInfo().also {
            it.name = "com.example.MyActivity"
            it.packageName = application.applicationInfo.packageName
        }
        val packageInfo = PackageInfo().also {
            it.packageName = application.applicationInfo.packageName
            it.applicationInfo = application.applicationInfo
            it.activities = arrayOf(activity)
        }
        ShadowPackageManager().installPackage(packageInfo)

        assertTrue {
            ManifestValidator.isComponentPresentInManifest(
                appCtx,
                "com.example.MyActivity",
                ManifestValidator.ComponentType.ACTIVITY
            )
        }
    }

    @Test
    fun `test isComponentPresentInManifest() should return false if service is not present in manifest`() {

        val packageInfo = PackageInfo().also {
            it.packageName = application.applicationInfo.packageName
            it.applicationInfo = application.applicationInfo
            it.services = arrayOf() // No services
        }
        ShadowPackageManager().installPackage(packageInfo)

        assertFalse {
            ManifestValidator.isComponentPresentInManifest(
                appCtx,
                "com.example.MyNonExistentService",
                ManifestValidator.ComponentType.SERVICE
            )
        }
    }

    @Test
    fun `test isComponentPresentInManifest() should return false if receiver is not present in manifest`() {

        val packageInfo = PackageInfo().also {
            it.packageName = application.applicationInfo.packageName
            it.applicationInfo = application.applicationInfo
            it.receivers = arrayOf() // No receivers
        }
        ShadowPackageManager().installPackage(packageInfo)

        assertFalse {
            ManifestValidator.isComponentPresentInManifest(
                appCtx,
                "com.example.MyNonExistentReceiver",
                ManifestValidator.ComponentType.RECEIVER
            )
        }
    }


    @Test
    fun `test isComponentPresentInManifest() should return false if activity is not present in manifest`() {

        val packageInfo = PackageInfo().also {
            it.packageName = application.applicationInfo.packageName
            it.applicationInfo = application.applicationInfo
            it.activities = arrayOf() // No activities
        }
        ShadowPackageManager().installPackage(packageInfo)

        assertFalse {
            ManifestValidator.isComponentPresentInManifest(
                appCtx,
                "com.example.MyNonExistentActivity",
                ManifestValidator.ComponentType.ACTIVITY
            )
        }
    }
}