package com.clevertap.android.sdk.feat_variable

import android.annotation.SuppressLint
import androidx.annotation.Discouraged
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.android.sdk.feat_variable.FakeServer.Companion.ResposnseType.*
import org.json.JSONObject
import kotlin.concurrent.thread

@Discouraged(message = "remove once backend is available")
@SuppressLint("DiscouragedApi")
class FakeServer {

    companion object{
        enum class ResposnseType{
            VARS1_VARSCODE,
            VARS2_VARSCODE,
            VARSLOCAL_VARSLOCAL,
        }

        var expectedBackendData :ResposnseType = VARS1_VARSCODE

        var localVarsJson : JSONObject = JSONObject()


        var hasForcedPushedVariables = false



        @JvmStatic
        fun requestBE(onResponse:(JSONObject)->Unit){
            val codeVarsFromServer = JSONObject("{\"welcomeMsg\":\"HelloUser\",\"correctGuessPercentage\":50,\"initialCoins\":45,\"isOptedForOffers\":true,\"aiNames\":[\"don2\",\"jason2\",\"shiela2\",\"may2\"],\"userConfigurableProps\":{\"difficultyLevel\":1.8,\"ai_Gender\":\"F\",\"numberOfGuesses\":10},\"android\":{\"nokia\":{\"12\":\"UnReleased\",\"6a\":6400},\"samsung\":{\"s22\":54999.99,\"s23\":\"UnReleased\"}},\"apple\":{\"iphone15\":\"UnReleased\"}}")
            val vars1 = JSONObject("{\"aiNames\":{\"[0]\":\"s1a\",\"[1]\":\"s1b\",\"[2]\":\"s1c\"},\"userConfigurableProps\":{\"difficultyLevel\":3.3,\"ai_Gender\":\"F\",\"numberOfGuesses\":5,\"watchAddForAnotherGuess\":true},\"correctGuessPercentage\":\"80\",\"initialCoins\":\"100\",\"isOptedForOffers\":false,\"android\":{\"samsung\":{\"s22\":65000}},\"welcomeMsg\":\"Hey@{mateeee}\"}")
            val vars2 = JSONObject("{\"aiNames\":{\"[0]\":\"s2a\",\"[1]\":\"s2b\",\"[2]\":\"s2c\"},\"userConfigurableProps\":{\"difficultyLevel\":6.6,\"ai_Gender\":\"X\",\"numberOfGuesses\":25,\"watchAddForAnotherGuess\":true},\"correctGuessPercentage\":\"90\",\"initialCoins\":\"80\",\"isOptedForOffers\":true,\"android\":{\"samsung\":{\"s22\":85000}},\"welcomeMsg\":\"Hey from server\"}")

            val resp = JSONObject()
            val (finalVars,finalVarsFromCode) = when(expectedBackendData){
                VARS1_VARSCODE -> vars1 to (if (hasForcedPushedVariables) localVarsJson else codeVarsFromServer)
                VARS2_VARSCODE -> vars2 to (if (hasForcedPushedVariables) localVarsJson else codeVarsFromServer)
                VARSLOCAL_VARSLOCAL -> localVarsJson to localVarsJson
            }
            resp.put("vars",finalVars)
            resp.put("varsFromCode", finalVarsFromCode)

            thread {
                Thread.sleep(2000)
                Utils.runOnUiThread {
                    Logger.v("request complete. sending data from server to handleStartResponse")
                    Logger.v(resp.toString(2))
                    onResponse.invoke(resp)
                }
            }
        }

        @JvmStatic
        fun sendVariables(onResponse:(JSONObject)->Unit){
            hasForcedPushedVariables = true
            val resp = localVarsJson

            thread {
                Thread.sleep(2000)
                Utils.runOnUiThread {
                    Logger.v("request complete. sending data from server to handleStartResponse")
                    Logger.v(resp.toString(2))
                    onResponse.invoke(resp)
                }
            }
        }



    }

}