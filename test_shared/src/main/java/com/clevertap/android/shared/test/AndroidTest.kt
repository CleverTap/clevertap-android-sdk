package com.clevertap.android.shared.test

import android.os.Build.VERSION_CODES
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.intArrayOf

@Config(manifest = Config.NONE, sdk = [VERSION_CODES.P], application = TestApplication::class)
@RunWith(AndroidJUnit4::class)
abstract class AndroidTest
