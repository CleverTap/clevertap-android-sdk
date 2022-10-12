package com.clevertap.demo;

import com.clevertap.android.sdk.inapp.CTLocalInApp;
import com.clevertap.android.sdk.inapp.CTLocalInApp.InAppType;
import org.json.JSONObject;

/**
 * Class to test java=>Kotlin inter-op
 */
public class JavaToKtInterOp {

   public JSONObject test() {

      //AlertDialogPromptForSettings.show(context,AlertDialogPromptForSettings.);
      return CTLocalInApp.builder()
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
