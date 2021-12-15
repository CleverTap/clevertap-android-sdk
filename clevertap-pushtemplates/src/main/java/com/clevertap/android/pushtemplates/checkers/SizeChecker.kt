package com.clevertap.android.pushtemplates.checkers

abstract class SizeChecker<T>(private var entity: T?, private var size: Int, private var errorMsg: String) :
    Checker<T>