package com.clevertap.android.pushtemplates

import android.content.Context
import android.widget.Toast
import androidx.annotation.Nullable
import androidx.core.content.ContextCompat
import com.clevertap.android.sdk.CleverTapAPI

class CTPushTemplateAPI (private var context: Context, private var cleverTapAPI: CleverTapAPI){


    fun showToastWithCTId(@Nullable text: String?,
                  duration: Int,
                  @Nullable backgroundTint: Int?){
        val toastEgg = Toast.makeText(context, text + " CT_ID_" + cleverTapAPI.cleverTapID, duration)
        if (backgroundTint != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                toastEgg.view?.background?.setTintList(ContextCompat.getColorStateList(context, backgroundTint))
            }
        }

        toastEgg.show()

    }

    fun showToast(@Nullable text: CharSequence?,
                          duration: Int,
                          @Nullable backgroundTint: Int?){
        val toastEgg = Toast.makeText(context, text, duration)
        if (backgroundTint != null) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                toastEgg.view?.background?.setTintList(ContextCompat.getColorStateList(context, backgroundTint))
            }
        }

        toastEgg.show()

    }
}