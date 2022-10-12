package com.clevertap.demo;

import android.content.Context;
import com.clevertap.android.sdk.inapp.CTLocalInApp;
import com.clevertap.android.sdk.inapp.CTLocalInApp.InAppType;
import org.json.JSONObject;

/**
 * Class to test java=>Kotlin inter-op
 */
public class JavaToKtInterOp {

   public JSONObject test(Context context) {

      //AlertDialogPromptForSettings.show(context,AlertDialogPromptForSettings.);
      return CTLocalInApp.builder(context)
              .setInAppType(InAppType.HALF_INTERSTITIAL)
              .setTitleText("TitleText").setMessageText("mtext").followDeviceOrientation(true)
              .setPositiveBtnText("PosbtnText").setNegativeBtnText("NEgBtnTxt").setBtnTextColor("btntxtclr")
              .setBtnBorderColor("btnbordercolor").setBtnBorderRadius("BtnBorderRadius")
              .setBtnBackgroundColor("BtnBgColor")
              .setBackgroundColor("bgColor").setImageUrl("imgUrl").setMessageTextColor("MsgTxtColor")
              .setTitleTextColor("TitleTxtColor")
              .build();
   }
}
