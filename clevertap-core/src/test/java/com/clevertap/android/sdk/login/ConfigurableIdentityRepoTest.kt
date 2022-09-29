package com.clevertap.android.sdk.login

import android.content.Context
import com.clevertap.android.sdk.CleverTapInstanceConfig
import com.clevertap.android.sdk.Constants
import com.clevertap.android.sdk.CoreMetaData
import com.clevertap.android.sdk.DeviceInfo
import com.clevertap.android.sdk.validation.ValidationResultStack
import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito
import java.util.*

class ConfigurableIdentityRepoTest:BaseTestCase() {
    private lateinit var configurableIdentityRepo: ConfigurableIdentityRepo

    private lateinit var config: CleverTapInstanceConfig
    private lateinit var deviceInfo: DeviceInfo
    private lateinit var coreMetaData: CoreMetaData
    private lateinit var validationStack :ValidationResultStack
    private lateinit var loginInfoProvider: LoginInfoProvider



    override fun setUp() {
        super.setUp()
        // using spy everywhere to easily provide a custom value when <someDependency>.publicGetter() is called
        coreMetaData = Mockito.spy(CoreMetaData())
        config = Mockito.spy(CleverTapInstanceConfig.createInstance(appCtx, "id", "token", "region"))
        deviceInfo = Mockito.spy(DeviceInfo(appCtx,config,"clevertap_id",coreMetaData))
        loginInfoProvider = Mockito.spy(LoginInfoProvider(appCtx,config,deviceInfo))
        validationStack = Mockito.spy(ValidationResultStack())
//        configurableIdentityRepo = ConfigurableIdentityRepo(appCtx,config,loginInfoProvider,validationStack)

    }


    /**
     * Loads the identity set:
     *
     * 1. create IdentitySet1 : 'prefKeySet'
     * |-- 1.1. A string is provided by loginInfoProvider.getCachedIdentityKeysForAccount()
     * |-- 1.2 This string is of format "__,__,__etc" and is split by ',' .the new list is filtered for wrong keys and finally used to create 'prefKeySet'
     *
     * 2. create IdentitySet2 : 'configKeySet'
     * |-- 2.1 A string array is provided by config.getIdentityKeys()
     * |-- 2.2 this array is filtered for wrong keys and finally used to create 'configKeySet'
     *
     * <note>: the validation critieria is that keyset must not be empty</note>
     *
     * 3. validate sets  : handleError(prefKeySet, configKeySet);
     * |-- 3.1 : if prefKeySet.isValid() && configKeySet.isValid() && !prefKeySet.equals(configKeySet), it will generate a validation error on vr stack passed via external dependency
     *
     *
     * 4. setting identity set
     * |-- 4.1  identitySet = prefkeyset(if prefKeySet.isValid())
     * |-- 4.2  identitySet = configkeyset(if configKeySet.isValid()) or
     * |-- 4.3  (if above 2 doesn't apply) identitySet =IdentitySet.getDefault()  = ['Identity','Email']
     *
     * 5. if (!prefKeySet.isValid() ) loginInfoProvider.saveIdentityKeysForAccount(identitySet) is also called on the newly initialised identitySet
     */

    @Test
    fun getIdentitySet_when_ABC_should_XYZ(){
        val keys = arrayOf(Constants.TYPE_IDENTITY)
        Mockito.`when`(loginInfoProvider.cachedIdentityKeysForAccount).thenReturn(keys.joinToString(","))
        Mockito.`when`(config.identityKeys).thenReturn(keys)
        configurableIdentityRepo = ConfigurableIdentityRepo(appCtx,config,loginInfoProvider,validationStack)

        val result = configurableIdentityRepo.identitySet
        println("final result=== ${result.toString()}")
        Assert.assertEquals(true,result.isValid)
        Assert.assertEquals(true,result.contains(Constants.TYPE_IDENTITY))
//        Assert.assertEquals(true,result.contains(Constants.TYPE_PHONE))
    }
    @Test
    fun getIdentitySet2_when_ABC_should_XYZ(){
        configurableIdentityRepo.identitySet
    }


//    @Test
//    fun hasIdentity_when_ABC_should_XYZ(){
//        configurableIdentityRepo.hasIdentity("key")
//    }
//
//    @Test
//    fun loadIdentitySet_when_ABC_should_XYZ(){
//        configurableIdentityRepo.loadIdentitySet()
//    }
}