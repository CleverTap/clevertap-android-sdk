package com.clevertap.demo

import android.view.View
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar

inline fun View.snack(@StringRes messageRes: Int, length: Int = Snackbar.LENGTH_INDEFINITE, f: Snackbar.() -> Unit) {
    val snack = Snackbar.make(this, resources.getString(messageRes), length)
    snack.f()
    snack.show()
}

fun Snackbar.action(@StringRes actionRes: Int, color: Int? = null, listener: (View) -> Unit) {
    setAction(view.resources.getString(actionRes), listener)
    color?.let { setActionTextColor(ContextCompat.getColor(context, color)) }
}
