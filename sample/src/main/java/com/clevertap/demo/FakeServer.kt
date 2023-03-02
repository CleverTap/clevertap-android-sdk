package com.clevertap.demo

import android.content.Context
import android.content.res.AssetManager
import org.json.JSONObject

class FakeServer {

    companion object{
        fun getJson(number: Int,ctx:Context): JSONObject {
            fun AssetManager.readAssetsFile(fileName : String): String = open(fileName).bufferedReader().use{it.readText()}

            val name = when(number){
                0 -> "localVarsOld.json"
                1 -> "serverResponse1.json"
                2 -> "serverResponse2.json"
                3 -> "localVarsRequest.json"

                else -> "localVarsOld.json"
            }
            val jsonStr= ctx.assets.readAssetsFile(name)
            return JSONObject(jsonStr)

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