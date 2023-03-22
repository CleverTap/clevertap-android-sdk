package com.clevertap.android.sdk.variables
//
//import com.clevertap.android.sdk.variables.CTVariableUtils.*
//import com.clevertap.android.shared.test.BaseTestCase
//import org.junit.*
//import org.junit.Assert.*
//import org.junit.runner.*
//import org.robolectric.RobolectricTestRunner
//import java.util.*
//
//@RunWith(RobolectricTestRunner::class)
//class CTempTest : BaseTestCase() {
//
//
//    @Test
//    fun test_updateValues() {
//        val name = "mobile.smartphone.android.samsung.s22"
//        val components = CTVariableUtils.getNameComponents(name)
//        println("components=${Arrays.toString(components)}")
//        val value = 54999.99
//        val kind = "float"
//        val values = hashMapOf<String,Any?>()
//        val kinds= hashMapOf<String,String>()
//        CTVariableUtils.updateValuesAndKinds(name,components,value, kind, values, kinds)
//        println("a: new values = $values") // {mobile={smartphone={android={samsung={s22=54999.99}}}}}
//        println("a: new kinds = $kinds")  //  {"mobile.smartphone.android.samsung.s22"=float}
//
//        val name2 = "mobile.smartphone.apple.iphone.15pro"
//        val components2= CTVariableUtils.getNameComponents(name2)
//        val value2 = "unreleased"
//        CTVariableUtils.updateValuesAndKinds(name2,components2,value2, kind, values, kinds)
//        println("b: new values = $values") //{mobile={smartphone={apple={iphone={15pro=unreleased}}, android={samsung={s22=54999.99}}}}}
//        println("b: new kinds = $kinds")  //{mobile.smartphone.android.samsung.s22=float, mobile.smartphone.apple.iphone.15pro=float}
//
//        kotlin.test.assertTrue(true)
//    }

//    @Test
//    fun test_registerVariable() {
//        val vars = listOf(
//            Var.define("strVar","123"),
//            Var.define("intVar",1),
//            Var.define("floatVar",1.1f),
//            Var.define("boolVar",false),
//            Var.define("arrayVar", arrayOf("books",23,5200.50,true)),
//            Var.define("booklist.size",23), //nestedVar
//        )
//
//
//        vars.forEach {
//            VarCache.registerVariable(it)
//        }
//    }
//
//    @Test
//    fun testgetNameComponents() {
//        val testCases = listOf(
//            Pair("", arrayOf("")),
//            Pair("abc", arrayOf("abc")),
//            Pair("abc.def", arrayOf("abc", "def")),
//            Pair("abc[def", arrayOf("abc[def")),
//            Pair("abc.def[ghi", arrayOf("abc", "def[ghi")),
//            Pair("abc.def[ghi.jkl", arrayOf("abc", "def[ghi.jkl")),
//            Pair("abc.def[ghi]jkl", arrayOf("abc", "def[ghi]jkl")),
//            Pair("abc\\.def", arrayOf("abc.def")),
//            Pair("abc[\\.def]", arrayOf("abc[.def]")),
//            Pair("abc\\[.ef]", arrayOf("abc", "ef")),
//            Pair("abc[.ef]", arrayOf("abc", "ef")),
//            Pair("abc\\\\ef]", arrayOf("abc", "ef")),
//            Pair("abc(ef]", arrayOf("abc", "ef")),
//
//            )
//
//        for (tc in testCases) {
//            val (input, expected) = tc
//            val actual = getNameComponents(input)
//            if (expected.contentEquals(actual)) {
//                println("Test case passed: input=$input, expected=${expected.contentToString()}, actual=${actual.contentToString()}")
//            } else {
//                println("Test case failed: input=$input, expected=${expected.contentToString()}, actual=${actual.contentToString()}")
//            }
//        }
//    }
//
//    /*
//        summary :
//        it will either return the value of key from the collection, or empty map if key
//        is not in  collection; and it will also add the empty map against
//        that key in collection
//
//        eg :
//        c1: traverse(mapOf("key" to 1234) , "key" , true/false) ->   1234
//        c2: traverse(mapOf("key" to 1234) , "unknownKey" , true) ->  hashMap() | also,changes collection to : mapOf("key" to 1234, "unknownKey" to hashMap())
//        c3: traverse(mapOf("key" to 1234) , "unknownKey" , false) ->  null
//        c4: traverse(listOf(1234,5678,1111,null),2, true/false) ->  1111
//        c5: traverse(listOf(1234,5678,1111,null),3, true) -> hashMap() | also changes collection to : listOf(1234,5678,1111,hashMap() )
//        c6: traverse(listOf(1234,5678,1111,null),3, false) -> null()
//    */
//    @Test
//    fun test_traverse1() {
//        val collection = mutableListOf("a",1.2,'c',24,false)
//        val key = 0
//        val autoInsert = true
//        val autoInsert2 = false
//
//
//        //list: if key is present and list[key] is a not null value,
//        // it will return its value irrespective of autoInsertValue
//
//        // note : for list, we don't consider a case where key is absent as
//        // that is an index out of bounds error
//        val res1 = CTVariableUtils.traverse(collection,key,autoInsert2)
//        assertTrue(res1 is String)
//        assertEquals(res1 ,  "a")
//        println("traverse(collection,key,autoInsert2) --> $res1")
//
//        val res2 = CTVariableUtils.traverse(collection,key,autoInsert)
//        assertTrue(res2 is String)
//        assertEquals(res2 ,  "a")
//        println("traverse(collection,key,autoInsert) --> $res2")
//
//
//    }
//    @Test
//    fun test_traverse2() {
//        val collection = mutableListOf("a",null,null,null)
//        val key = 2
//        val autoInsert = true
//
//        //list: if key is present and list[key] is a  null value,
//        // then autoInsert =true/false will be significant.
//        // if auto insert is true
//        // it will return an empty map as result and also
//        // update the list's index value to empty map
//
//        println("list before= $collection")
//        val res3 = CTVariableUtils.traverse(collection,key,autoInsert)
//        println("traverse(collection,key,autoInsert) --> $res3")
//        assertTrue(res3 is HashMap<*,*>)
//        assertEquals((res3 as HashMap<*,*>).size ,  0)
//        println("list after= $collection")
//
//    }
//    @Test
//    fun test_traverse3() {
//        val collection = mutableListOf("a",null,null,null)
//        val key = 2
//        val autoInsert2 = false
//
//        //list: if key is present and list[key] is a  null value,
//        // then autoInsert =true/false will be significant.
//        // if auto insert is false
//        // it will return result as it is, i.e null
//
//        println("list before= $collection")
//        val res3 = CTVariableUtils.traverse(collection,key,autoInsert2)
//        println("traverse(collection,key,autoInsert) --> $res3")
//        assertNull(res3)
//        println("list after= $collection")
//    }
//    @Test
//    fun test_traverse4() {
//        val collection = hashMapOf<String,Any>("k1" to "b", "k2" to 1.3, "k3" to 'd', "k4" to 25, "k5" to true )
//        val key = "k2"
//
//        //map: if map[key] is a not null value, it will be returned as it is
//        // irrespective of autoInsert without impacting the map
//
//        println("map before= $collection")
//
//        val res3 = CTVariableUtils.traverse(collection,key,true)
//        println("traverse(collection,key,autoInsert) --> $res3")
//        assertEquals(1.3,res3)
//        println("map after= $collection")
//
//        val res4 = CTVariableUtils.traverse(collection,key,false)
//        println("traverse(collection,key,autoInsert) --> $res4")
//        assertEquals(1.3,res4)
//        println("map after= $collection")
//
//    }
//    @Test
//    fun test_traverse5() {
//        val collection = hashMapOf<String,Any>("k1" to "b", "k2" to 1.3, "k3" to 'd', "k4" to 25, "k5" to true )
//        val key = "k7"
//
//        //map: if map[key] is a  NULL value, autoInsert will matter
//        // if auto insert is true, emptymap is returned
//        // also map is updated with a new entry : key=emptymap
//
//        println("map before= $collection")
//        val res3 = CTVariableUtils.traverse(collection,key,true)
//        println("traverse(collection,key,autoInsert) --> $res3")
//        assertTrue(res3 is HashMap<*,*>)
//        assertTrue((res3 as HashMap<*,*>).size==0)
//        println("map after= $collection")
//
//
//    }
//    @Test
//    fun test_traverse6() {
//        val collection = hashMapOf<String,Any>("k1" to "b", "k2" to 1.3, "k3" to 'd', "k4" to 25, "k5" to true )
//        val key = "k7"
//
//        //map: if map[key] is a  NULL value, autoInsert will matter
//        // if auto insert is false, map[key] i.e null is returned as it is
//        //
//
//        println("map before= $collection")
//        val res3 = CTVariableUtils.traverse(collection,key,false)
//        println("traverse(collection,key,autoInsert) --> $res3")
//        assertNull(res3)
//        println("map after= $collection")
//
//
//
//    }
//
//    //summary: CTVariableUtils.kindFromValue(1) --> "int"
//    @Test
//    fun test_kindFromValue() {
//        arrayOf<Pair<Any,String>>(
//            1 to CTVariableUtils.INT,
//            1.1f to CTVariableUtils.FLOAT,
//            1.1 to CTVariableUtils.FLOAT,
//            "" to CTVariableUtils.STRING,
//            false to CTVariableUtils.BOOLEAN,
//            arrayListOf(1,2,3) to CTVariableUtils.ARRAY,
//            hashMapOf("a" to "b") to CTVariableUtils.DICTIONARY
//
//        ).forEach {
//            val result = CTVariableUtils.kindFromValue(it.first)
//            println("kindFromValue(${it.first}) -> $result ")
//            Assert.assertEquals(it.second,result)
//        }
//
//    }
//    //summary :  "booklist.bookListSize" --> arrayOf("booklist","bookListSize")
//    @Test
//    fun test_getNameComponents() {
//        arrayOf(
//            "" to arrayOf(),
//            "book" to arrayOf("book"),
//            "booklist.bookListSize" to arrayOf("booklist","bookListSize"),
//        ).forEach {
//            val result: Array<String> = CTVariableUtils.getNameComponents(it.first)
//            println("getNameComponents(\"${it.first}\") -> ${Arrays.toString(result)} ")
//            Assert.assertArrayEquals(it.second,result)
//        }
//    }
//
//    @Test
//    fun test_fromJson() {
//    }
//
//    @Test
//    fun test_mapFromJson() {
//    }
//
//    @Test
//    fun test_toJson() {
//    }
//
//
//}