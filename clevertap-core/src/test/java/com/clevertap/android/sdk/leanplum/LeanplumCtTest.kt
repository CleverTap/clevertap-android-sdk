package com.clevertap.android.sdk.leanplum

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LeanplumCtTest : BaseTestCase() {

  @Before
  fun setup() {
    LeanplumCT.initWithInstance(cleverTapAPI)
  }

  @Test
  fun testTrack() {
    LeanplumCT.track(
      event = "event",
      value = 1.0,
      info = "info",
      params = mapOf("param" to "value")
    )

    Mockito.verify(cleverTapAPI).pushEvent(
      "event",
      mapOf("value" to 1.0, "info" to "info", "param" to "value")
    )
  }

  @Test
  fun testTrack_not_supported_values() {
    LeanplumCT.track(
      event = "event",
      params = mapOf(
        "param" to "value",
        "byte_param" to 1.toByte(),
        "short_param" to 2.toShort(),
        "list" to listOf(1, 2, "three", null),
      )
    )

    Mockito.verify(cleverTapAPI).pushEvent(
      "event",
      mapOf(
        "value" to .0,
        "param" to "value",
        "byte_param" to 1,
        "short_param" to 2,
        "list" to "[1,2,three]",
      )
    )
  }

  @Test
  fun testAdvanceTo() {
    LeanplumCT.advanceTo(
      state = "state",
      info = "info",
      params = mapOf("param" to "value"))

    Mockito.verify(cleverTapAPI).pushEvent(
      "state_state",
      mapOf("info" to "info", "param" to "value")
    )
  }

  @Test
  fun testAdvanceTo_not_supported_values() {
    LeanplumCT.advanceTo(
      state = "state",
      params = mapOf(
        "param" to "value",
        "list" to listOf(1, 2, 3),
      )
    )

    Mockito.verify(cleverTapAPI).pushEvent(
      "state_state",
      mapOf(
        "param" to "value",
        "list" to "[1,2,3]",
      )
    )
  }

  @Test
  fun testSetTrafficSourceInfo() {
    LeanplumCT.setTrafficSourceInfo(mapOf(
      "publisherName" to "source",
      "publisherSubPublisher" to "medium",
      "publisherSubCampaign" to "campaign",
    ))

    Mockito.verify(cleverTapAPI).pushInstallReferrer("source", "medium", "campaign")
  }

  @Test
  fun testSetUserAttributes() {
    LeanplumCT.setUserAttributes(mapOf(
      "attr1" to "value1",
      "attr2" to "value2"
    ))

    Mockito.verify(cleverTapAPI).pushProfile(mapOf(
      "attr1" to "value1",
      "attr2" to "value2"
    ))
  }

  @Test
  fun testSetUserAttributes_not_supported_values() {
    LeanplumCT.setUserAttributes(mapOf(
      "list" to listOf(1, 2, 3)
    ))

    Mockito.verify(cleverTapAPI).pushProfile(mapOf(
      "list" to "[1,2,3]",
    ))
  }

  @Test
  fun testSetUserAttributes_remove_attribute() {
    LeanplumCT.setUserAttributes(mapOf(
      "attr" to "value",
      "attr_to_remove" to null,
    ))

    Mockito.verify(cleverTapAPI).pushProfile(mapOf(
      "attr" to "value",
    ))
    Mockito.verify(cleverTapAPI).removeValueForKey(
      "attr_to_remove"
    )
  }

  @Test
  fun testSetUserAttributes_with_userId() {
    LeanplumCT.setUserAttributes(
      userId = "userId",
      attributes = mapOf("attr" to "value"),
    )

    Mockito.verify(cleverTapAPI).onUserLogin(
      mapOf("Identity" to "userId")
    )
    Mockito.verify(cleverTapAPI).pushProfile(
      mapOf("attr" to "value")
    )
  }

  @Test
  fun testSetUserId() {
    LeanplumCT.setUserId("userId")

    Mockito.verify(cleverTapAPI).onUserLogin(
      mapOf("Identity" to "userId")
    )
  }

  @Test
  fun testTrackGooglePlayPurchase() {
    LeanplumCT.trackGooglePlayPurchase(
      eventName = "event",
      item = "item",
      priceMicros = 1000000,
      currencyCode = "currency",
      purchaseData = "data",
      dataSignature = "signature",
      params = mapOf("param" to "value"),
    )

    Mockito.verify(cleverTapAPI).pushChargedEvent(hashMapOf(
      "event" to "event",
      "value" to 1.0,
      "currencyCode" to "currency",
      "googlePlayPurchaseData" to "data",
      "googlePlayPurchaseDataSignature" to "signature",
      "item" to "item",
      "param" to "value",
    ), arrayListOf())
  }

  @Test
  fun testTrackGooglePlayPurchase_not_supported_values() {
    LeanplumCT.trackGooglePlayPurchase(
      item = "item",
      priceMicros = 1000000,
      currencyCode = "currency",
      purchaseData = "data",
      dataSignature = "signature",
      params = mapOf("list" to listOf(1, 2, 3)),
    )

    Mockito.verify(cleverTapAPI).pushChargedEvent(hashMapOf(
      "event" to "Purchase",
      "value" to 1.0,
      "currencyCode" to "currency",
      "googlePlayPurchaseData" to "data",
      "googlePlayPurchaseDataSignature" to "signature",
      "item" to "item",
      "list" to "[1,2,3]",
    ), arrayListOf())
  }

  @Test
  fun testTrackPurchase() {
    LeanplumCT.trackPurchase(
      event = "event",
      value = 1.0,
      currencyCode = "currency",
      params = mapOf("param" to "value", "list" to "[1,2,3]"),
    )

    Mockito.verify(cleverTapAPI).pushChargedEvent(hashMapOf(
      "event" to "event",
      "value" to 1.0,
      "currencyCode" to "currency",
      "param" to "value",
      "list" to "[1,2,3]",
    ), arrayListOf())
  }

}
