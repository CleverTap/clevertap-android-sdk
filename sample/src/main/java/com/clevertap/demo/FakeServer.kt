package com.clevertap.demo

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.AssetManager
import androidx.annotation.Discouraged
import com.clevertap.android.sdk.Logger
import com.clevertap.android.sdk.Utils
import com.clevertap.demo.FakeServer.Companion.ResposnseType.*
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

        var expectedBackendData : ResposnseType = VARS1_VARSCODE

        var localVarsJson : JSONObject = JSONObject()


        var hasForcedPushedVariables = false

        var ctx : Context? = null


        fun getJson(number: Int): JSONObject {
            fun AssetManager.readAssetsFile(fileName : String): String = open(fileName).bufferedReader().use{it.readText()}
            val ctx = ctx ?: return JSONObject()

            val name = when(number){
                0 -> "localVarsOld.json"
                1-> "serverResponse1.json"
                2 -> "serverResponse2.json"
                3 -> "localVarsRequest.json"

                else -> "localVarsOld.json"
            }
            val jsonStr= ctx.assets.readAssetsFile(name)
            return JSONObject(jsonStr)

        }

        @JvmStatic
        fun simulateBERequest(onResponse:(JSONObject)->Unit){
            val vars1 = getJson(1)
            val vars2 = getJson(2)

            val resp = JSONObject()
            val finalVars = when(expectedBackendData){
                VARS1_VARSCODE -> vars1
                VARS2_VARSCODE -> vars2
                VARSLOCAL_VARSLOCAL -> localVarsJson
            }
            resp.put("vars",finalVars)

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
        fun sendVariables(requestData: JSONObject,onResponse: Runnable){
            Logger.v("sendVariables() called with: requestData = $requestData, onResponse = $onResponse")
            hasForcedPushedVariables = true
            thread {
                Thread.sleep(2000)
                Utils.runOnUiThread {
                    Logger.v("request complete. sent data from sdk to server.")
                    onResponse.run()
                }
            }
        }



    }

}
/*


  "android": {
    "samsung": {
      "s22": 54999.99,
      "s23": "UnReleased"
    },
    "nokia": {
      "6a": 6400,
      "12": "UnReleased"
    }

  },
  "apple": {
    "iphone15": "UnReleased"
  }

"android": {
    "samsung": {
      "s22": 64999.99,
      "s23": "Announced"
    },
    "nokia": {
      "6a": 6000,
      "12": "UnReleased"
    }

  },
  "apple": {
    "iphone15": "Announced"
  }

//-----

"android": {
    "samsung": {
      "s22": 34999.99,
      "s23": "Unlisted"
    },
    "nokia": {
      "6a": 8000,
      "12": "Unlisted"
    }

  },
  "apple": {
    "iphone15": "Unlisted"
  }
* */