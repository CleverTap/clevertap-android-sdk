package com.clevertap.demo

import com.clevertap.android.sdk.variables.annotations.Variable

class TestMyVarsKTCompanion{
    companion object{
        @Variable val tmv2String:String = "code_string"
        @Variable val tmv2Float:Double = 1.42
        @Variable val tmv2Int : Int =1
        @Variable val tmv2Boolean: Boolean = false


//        @Variable(group = "group1.group11", name = "name1") val tmv2Group1111 = "code_string"
//        @Variable(group = "group1.group11", name = "name2") val tmv2Group1112 = 1.2
//        @Variable(group = "group1.group12", name = "name3") val tmv2Group1123 = false
//        @Variable(group = "group2", name = "name1") val tmv2Group21 = 21
    }
}