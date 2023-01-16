package com.clevertap.android.sdk

import com.clevertap.android.shared.test.BaseTestCase
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.InputStream
import java.nio.charset.Charset


@RunWith(RobolectricTestRunner::class)
class VarCacheTest : BaseTestCase() {


    @Test
    fun test_getNameComponents() {
        MyInputStream.testMyInPutStream()

        Assert.assertTrue(true)
    }



}
// a basic input stream class .
// it will return a characters of a string(passed as input) one by one using the read() function
// it mimics a video stream or file stream. those get downloaded from the internet and returned in bits with a delay
class MyInputStream(private val str:String): InputStream() {
    private var pos = 0
    override fun read(): Int {
        Thread.sleep(10)
        if(pos < str.length ) {
            val charCode = str[pos].code
            pos++
            return charCode
        }
        else return -1
    }

    companion object{
        fun  testMyInPutStream(){
            val stream = MyInputStream("Hello world")

            println("====")
            var x = stream.read()
            while (x !=-1){
                print(x.toChar())
                x = stream.read()
            }
            println("\n====")

        }
    }
}


