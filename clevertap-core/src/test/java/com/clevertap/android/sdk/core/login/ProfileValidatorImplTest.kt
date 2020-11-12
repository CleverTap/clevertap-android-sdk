package com.clevertap.android.sdk.core.login

import com.clevertap.android.sdk.Constants.IdentityType
import com.clevertap.android.sdk.Constants.IdentityType.TYPE_EMAIL
import com.clevertap.android.sdk.Constants.IdentityType.TYPE_INVALID
import com.clevertap.android.sdk.Constants.IdentityType.TYPE_PHONE
import com.clevertap.android.shared.test.BaseTestCase
import com.google.common.truth.Truth
import org.junit.*
import org.junit.runner.*
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ProfileValidatorImplTest : BaseTestCase() {

    lateinit var profileValidatorImpl: ProfileValidatorImpl

    @Before
    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        profileValidatorImpl = ProfileValidatorImpl()
    }

    @Test
    fun testToIdentityStringEmptySetReturnsEmpty() {
        Assert.assertTrue(profileValidatorImpl.toIdentityString(null).isEmpty())
        Assert.assertTrue(profileValidatorImpl.toIdentityString(HashSet()).isEmpty())
    }

    @Test
    fun testToIdentityStringInValidReturnsValid() {
        val hashSet = java.util.HashSet<IdentityType>()
        hashSet.add(TYPE_PHONE)
        hashSet.add(TYPE_EMAIL)
        hashSet.add(TYPE_INVALID)
        Truth.assertThat(profileValidatorImpl.toIdentityString(hashSet)).isIn(listOf("Phone,Email", "Email,Phone"))
    }

    @Test
    fun testToIdentityTypeEmptyArrayReturnsEmptySet() {
        Assert.assertTrue(profileValidatorImpl.toIdentityType(null).isEmpty())
        Assert.assertTrue(profileValidatorImpl.toIdentityType(arrayOf()).isEmpty())
    }

    @Test
    fun testToIdentityTypeValidArrayReturnsValidSet() {
        val array = arrayOf("Phone", "Email", "Random")
        val hashSet = java.util.HashSet<IdentityType>()
        hashSet.add(TYPE_PHONE)
        hashSet.add(TYPE_EMAIL)
        Assert.assertEquals(hashSet, profileValidatorImpl.toIdentityType(array))
    }
}