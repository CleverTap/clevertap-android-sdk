package com.clevertap.android.sdk.displayunits.model

import android.os.Parcel
import android.text.TextUtils
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CleverTapDisplayUnitContentTest:BaseTestCase() {

    @Test
    fun test_toContent_nullObject_ReturnInvalidObject() {
        val displayUnitContent = CleverTapDisplayUnitContent.toContent(null)
        Assert.assertTrue(TextUtils.isEmpty(displayUnitContent.message))
        Assert.assertTrue(TextUtils.isEmpty(displayUnitContent.messageColor))
        Assert.assertTrue(TextUtils.isEmpty(displayUnitContent.media))
        Assert.assertTrue(TextUtils.isEmpty(displayUnitContent.actionUrl))
        Assert.assertTrue(TextUtils.isEmpty(displayUnitContent.contentType))
        Assert.assertTrue(TextUtils.isEmpty(displayUnitContent.icon))
        Assert.assertTrue(TextUtils.isEmpty(displayUnitContent.posterUrl))
        Assert.assertTrue(TextUtils.isEmpty(displayUnitContent.title))
        Assert.assertTrue(TextUtils.isEmpty(displayUnitContent.titleColor))
        Assert.assertTrue(!TextUtils.isEmpty(displayUnitContent.error))
    }

    @Test
    fun test_toDisplayUnit_validArray_ReturnValidObject() {
        val displayUnitContent = CleverTapDisplayUnitContent.toContent(MockDisplayUnitContent().getContent())
        Assert.assertFalse(TextUtils.isEmpty(displayUnitContent.message))
        Assert.assertFalse(TextUtils.isEmpty(displayUnitContent.messageColor))
        Assert.assertFalse(TextUtils.isEmpty(displayUnitContent.media))
        Assert.assertFalse(TextUtils.isEmpty(displayUnitContent.actionUrl))
        Assert.assertFalse(TextUtils.isEmpty(displayUnitContent.contentType))
        Assert.assertFalse(TextUtils.isEmpty(displayUnitContent.icon))
        Assert.assertFalse(TextUtils.isEmpty(displayUnitContent.posterUrl))
        Assert.assertFalse(TextUtils.isEmpty(displayUnitContent.title))
        Assert.assertFalse(TextUtils.isEmpty(displayUnitContent.titleColor))
        Assert.assertFalse(!TextUtils.isEmpty(displayUnitContent.error))
    }

    @Test
    fun test_toString() {
        val displayUnitContentString = CleverTapDisplayUnitContent.toContent(MockDisplayUnitContent().getContent()).toString()
        Assert.assertTrue(displayUnitContentString.contains("title:"))
        Assert.assertTrue(displayUnitContentString.contains("titleColor:"))
        Assert.assertTrue(displayUnitContentString.contains("message:"))
        Assert.assertTrue(displayUnitContentString.contains("messageColor:"))
        Assert.assertTrue(displayUnitContentString.contains("media:"))
        Assert.assertTrue(displayUnitContentString.contains("posterUrl:"))
        Assert.assertTrue(displayUnitContentString.contains("actionUrl:"))
        Assert.assertTrue(displayUnitContentString.contains("error:"))
    }

    @Test
    fun test_createFromParcel_verify() {
        val displayUnitContent = CleverTapDisplayUnitContent.toContent(MockDisplayUnitContent().getContent())
        val parcel = Parcel.obtain()
        displayUnitContent.writeToParcel(parcel, displayUnitContent.describeContents())
        parcel.setDataPosition(0)

        val createdFromParcel = CleverTapDisplayUnitContent.CREATOR.createFromParcel(parcel)
        Assert.assertEquals(displayUnitContent.message,createdFromParcel.message)
        Assert.assertEquals(displayUnitContent.messageColor, createdFromParcel.messageColor)
        Assert.assertEquals(displayUnitContent.media,createdFromParcel.media )
        Assert.assertEquals(displayUnitContent.actionUrl,createdFromParcel.actionUrl)
        Assert.assertEquals(displayUnitContent.contentType,createdFromParcel.contentType)
        Assert.assertEquals(displayUnitContent.icon,createdFromParcel.icon)
        Assert.assertEquals(displayUnitContent.posterUrl,createdFromParcel.posterUrl)
        Assert.assertEquals(displayUnitContent.title,createdFromParcel.title)
        Assert.assertEquals(displayUnitContent.titleColor,createdFromParcel.titleColor)
        Assert.assertEquals(displayUnitContent.error,createdFromParcel.error)
    }
}