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
        coreMetaData = Mockito.mock(CoreMetaData::class.java)
        config = Mockito.mock(CleverTapInstanceConfig::class.java)
        deviceInfo = Mockito.mock(DeviceInfo::class.java)
        loginInfoProvider = Mockito.mock(LoginInfoProvider::class.java)
        validationStack = Mockito.mock(ValidationResultStack::class.java)
        // note: no need to setup configurableIdentityRepo instance here as it makes a crucial function call during initialisation. therefore, we create an instance in tests, wherever required
    }

    @Test
    fun getIdentitySet(){
        //note: this function does not have any important cases on its own, so we are testing its creator function , i.e loadIdentitySet() which gets called when constructor is called
        Assert.assertEquals(true,true)
    }


    /**
     * Loads the identity set:
     *
     * 1. create IdentitySet1 : 'prefKeySet'
     * |-- 1.1. A string is provided by loginInfoProvider.getCachedIdentityKeysForAccount()
     * |    |-- 1.1.1-5 the above function gets either string or null from storage based on 5 scenarios: whether keyvalue are coming from sp of default/nondefault config, etc
     * |-- 1.2 This string is of format "__,__,__etc" and is split by ',' .the new list is filtered for wrong keys and finally used to create 'prefKeySet'
     *
     * 2. create IdentitySet2 : 'configKeySet'
     * |-- 2.1 A string array is provided by config.getIdentityKeys()
     *      |-- 2.1.0 config is a dependency passed to ConfigurableIdentityRepo . it can be default or non default
     *      |-- 2.1.1 for default config instance identitiyKeys =manifest.getProfileKeys()
     *      |-- 2.1.2 for nondefault config instance , identitiyKeys = either null keys array or array of strings that are set post creation via config.setIdentityKeys(Constants.TYPE_EMAIL,Constants.TYPE_PHONE,..etc)
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
    fun loadIdentitySet_when_KeysInLIPandConfigAreSame_should_SetThoseKeysAsIdentitySet(){
        //case 1 : when cachedIdentityKeysForAccount(from loginInfoProvider) and identityKeys
        //         (from config) are same, then no error is generated and idenity keys are set
        //         to keys from loginInfoProvider . since lip keys are valid, saveIdentityKeysForAccount(..) will  not be called

        val commonKeys = arrayOf(Constants.TYPE_IDENTITY)

        //assertions
        Mockito.`when`(loginInfoProvider.cachedIdentityKeysForAccount).thenReturn(commonKeys.joinToString(","))
        Mockito.`when`(config.identityKeys).thenReturn(commonKeys)

        //test call
        configurableIdentityRepo = ConfigurableIdentityRepo(config,loginInfoProvider,validationStack)
        val result = configurableIdentityRepo.identitySet

        //validations
        println("final result=== ${result.toString()}")
        Mockito.verify(validationStack,Mockito.never()).pushValidationResult(Mockito.any())
        Mockito.verify(loginInfoProvider,Mockito.times(0)).saveIdentityKeysForAccount(Mockito.any())
        Assert.assertEquals(true,result.isValid)
        commonKeys.forEach { Assert.assertEquals(true,result.contains(it)) }
    }

    @Test
    fun loadIdentitySet_when_KeysInLIPandConfigAreDifferent_should_SetLIPKeysAsIdenititySet(){
        //case 2: when cachedIdentityKeysForAccount(from loginInfoProvider) and identityKeys
        //         (from config) are different, then   error is generated and  keys are set to that of loginInfoProvider.
        //         since lip keys are valid, saveIdentityKeysForAccount(..) will  not be called


        val lipKeys = arrayOf(Constants.TYPE_IDENTITY)
        val configKeys = arrayOf(Constants.TYPE_EMAIL)
        //assertions
        Mockito.`when`(loginInfoProvider.cachedIdentityKeysForAccount).thenReturn(lipKeys.joinToString(","))
        Mockito.`when`(config.identityKeys).thenReturn(configKeys)

        //test call
        configurableIdentityRepo = ConfigurableIdentityRepo(config,loginInfoProvider,validationStack)
        val result = configurableIdentityRepo.identitySet

        //validations
        println("final result=== ${result.toString()}")
        Mockito.verify(validationStack,Mockito.times(1)).pushValidationResult(Mockito.any())
        Mockito.verify(loginInfoProvider,Mockito.times(0)).saveIdentityKeysForAccount(Mockito.any())


        Assert.assertEquals(true,result.isValid)
        lipKeys.forEach { Assert.assertEquals(true,result.contains(it)) }
    }

    @Test
    fun loadIdentitySet_when_KeysInLIPandConfigAreDifferentAndLIPKeysAreInvalid_should_SetConfigKeysAsIdenititySet(){
        //case 3 : when cachedIdentityKeysForAccount(from loginInfoProvider) are invalid (i.e empty set)
        //         and identityKeys  (from config) are valid, then  error is not generated(why??) and idenity keys are set
        //         to keys from config . since lip keys are invalid, saveIdentityKeysForAccount(..) will be called


        val lipKeys = emptyArray<String>()
        val configKeys = arrayOf(Constants.TYPE_EMAIL)
        //assertions
        Mockito.`when`(loginInfoProvider.cachedIdentityKeysForAccount).thenReturn(lipKeys.joinToString(","))
        Mockito.`when`(config.identityKeys).thenReturn(configKeys)

        //test call
        configurableIdentityRepo = ConfigurableIdentityRepo(config,loginInfoProvider,validationStack)
        val result = configurableIdentityRepo.identitySet

        //validations
        println("final result=== ${result.toString()}")
        Mockito.verify(validationStack,Mockito.times(0)).pushValidationResult(Mockito.any())
        Mockito.verify(loginInfoProvider,Mockito.times(1)).saveIdentityKeysForAccount(Mockito.any())

        Assert.assertEquals(true,result.isValid)
        configKeys.forEach { Assert.assertEquals(true,result.contains(it)) }
    }
    @Test
    fun loadIdentitySet_when_KeysInLIPandConfigAreBothInvalid_should_SetDefaultKeysAsIdentitySet(){
        //case 4 : when cachedIdentityKeysForAccount(from loginInfoProvider) are invalid (i.e empty set)
        //         and identityKeys  (from config) are also invalid, then  error is not generated(why??) and idenity keys are set
        //         to default keys.since lip keys are invalid, saveIdentityKeysForAccount(..) will be called

        val lipKeys = emptyArray<String>()
        val configKeys = emptyArray<String>()
        val defaultKeys = arrayOf(Constants.TYPE_IDENTITY, Constants.TYPE_EMAIL)
        //assertions
        Mockito.`when`(loginInfoProvider.cachedIdentityKeysForAccount).thenReturn(lipKeys.joinToString(","))
        Mockito.`when`(config.identityKeys).thenReturn(configKeys)

        //test call
        configurableIdentityRepo = ConfigurableIdentityRepo(config,loginInfoProvider,validationStack)
        val result = configurableIdentityRepo.identitySet

        //validations
        println("final result=== ${result.toString()}")
        Mockito.verify(validationStack,Mockito.times(0)).pushValidationResult(Mockito.any())
        Mockito.verify(loginInfoProvider,Mockito.times(1)).saveIdentityKeysForAccount(Mockito.any())

        Assert.assertEquals(true,result.isValid)
        defaultKeys.forEach { Assert.assertEquals(true,result.contains(it)) }
    }


    @Test
    fun hasIdentity_when_calledWithKey_should_ReturnWhetherTheKeyExistsInSetOrNot(){
        val commonKeys = arrayOf(Constants.TYPE_IDENTITY,Constants.TYPE_PHONE)
        //assertions
        Mockito.`when`(loginInfoProvider.cachedIdentityKeysForAccount).thenReturn(commonKeys.joinToString(","))
        Mockito.`when`(config.identityKeys).thenReturn(commonKeys)

        //test call
        configurableIdentityRepo = ConfigurableIdentityRepo(config,loginInfoProvider,validationStack)

        //validations
        Assert.assertEquals(true,configurableIdentityRepo.hasIdentity(Constants.TYPE_IDENTITY))
        Assert.assertEquals(true,configurableIdentityRepo.hasIdentity(Constants.TYPE_PHONE))
        Assert.assertEquals(false,configurableIdentityRepo.hasIdentity(Constants.TYPE_EMAIL))
    }
}