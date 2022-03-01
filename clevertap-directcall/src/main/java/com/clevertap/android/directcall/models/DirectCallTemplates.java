package com.clevertap.android.directcall.models;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import com.clevertap.android.directcall.R;

import org.json.JSONException;
import org.json.JSONObject;

@RestrictTo(RestrictTo.Scope.LIBRARY)
public class DirectCallTemplates {

   public static class PinView {
        private int mPinItemCount;
        /*
        private int mViewType;
        private int mPinInputType;
        private int mPinItemWidth;
        private int mPinItemHeight;
        private float mPinTextSize;
        private int mPinItemRadius;
        private int mPinItemSpacing;
        private ColorStateList mLineColor;
        private int mLineWidth;
        private boolean isCursorVisible;
        private int mCursorColor;
        private int mCursorWidth;
        private Drawable mItemBackground;
        private boolean mHideLineWhenFilled;
        */

        public PinView() {

        }

       public PinView(int mPinItemCount) {
           this.mPinItemCount = mPinItemCount;
       }

       public void setItemCount(int count) {
            mPinItemCount = count;
        }

       public int getItemCount() {
           return mPinItemCount;
       }

       public JSONObject toJson() {
           JSONObject jo = new JSONObject();
           try {
               jo.put("pinItemCount", mPinItemCount);
           } catch (JSONException e) {
               e.printStackTrace();
           }
           return jo;
       }

       public static PinView fromJson(JSONObject jsonObject) {
           PinView pinView = null;
           try {
               pinView =  new PinView(jsonObject.getInt("pinItemCount"));
           } catch (JSONException e) {
               e.printStackTrace();
           }
           return pinView;
       }

        /*
        public void setItemWidth(@Px int itemWidth) {
            mPinItemWidth = itemWidth;
        }

        public void setItemHeight(@Px int itemHeight) {
            mPinItemHeight = itemHeight;
        }

        public void setTextSize(float size) {
            mPinTextSize = size;
        }

        public void setItemRadius(@Px int itemRadius) {
            mPinItemRadius = itemRadius;
        }

        public void setLineColor(@ColorInt int color) {
            mLineColor = ColorStateList.valueOf(color);
        }


        public void setCursorWidth(@Px int width) {
            mCursorWidth = width;
        }

         public void setHideLineWhenFilled(boolean hideLineWhenFilled) {
            this.mHideLineWhenFilled = hideLineWhenFilled;
        }

        public void setItemBackgroundColor(@ColorInt int color) {
            setItemBackground(new ColorDrawable(color));
        }

        public void setItemBackground(Drawable background) {
            mItemBackground = background;
        }

        public void setInputType(int mPinInputType) {
            this.mPinInputType = mPinInputType;
        }

        public void setLineWidth(@Px int borderWidth) {
            mLineWidth = borderWidth;
        }

        public void setCursorVisible(boolean visible) {
            isCursorVisible = visible;
        }


        public int getInputType() {
            return mPinInputType;
        }

        public int getItemWidth() {
            return mPinItemWidth;
        }

        public int getItemHeight() {
            return mPinItemHeight;
        }

        public float getTextSize() {
            return mPinTextSize;
        }

        public int getItemRadius() {
            return mPinItemRadius;
        }

        public int getItemSpacing() {
            return mPinItemSpacing;
        }

        public ColorStateList getLineColor() {
            return mLineColor;
        }

        public int getLineWidth() {
            return mLineWidth;
        }

        public boolean isCursorVisible() {
            return isCursorVisible;
        }

        public int getCursorColor() {
            return mCursorColor;
        }

        public int getCursorWidth() {
            return mCursorWidth;
        }

        public Drawable getItemBackground() {
            return mItemBackground;
        }

        public boolean ismHideLineWhenFilled() {
            return mHideLineWhenFilled;
        }
 */
    }

    public static class ScratchCard {
        //Outer Layer
        private String mOuterTextHeader = "";
        private String mOuterTextFooter = "";
        private int mOuterDrawableRes;
        //private String mOuterImageUrl;

        //Inner Layer
        private int mInnerBackgroundColorRes = R.color.darkGray;
        private int mInnerDrawableRes;
        private String mInnerText = "";
        //private String mInnerImageUrl;

        public ScratchCard() {
        }

        public ScratchCard(String mOuterTextHeader, String mOuterTextFooter, int mOuterDrawableRes, int mInnerBackgroundColorRes, int mInnerDrawableRes, String mInnerText) {
            this.mOuterTextHeader = mOuterTextHeader;
            this.mOuterTextFooter = mOuterTextFooter;
            this.mOuterDrawableRes = mOuterDrawableRes;
            this.mInnerBackgroundColorRes = mInnerBackgroundColorRes;
            this.mInnerDrawableRes = mInnerDrawableRes;
            this.mInnerText = mInnerText;
        }

        public String getOuterTextHeader() {
            return mOuterTextHeader;
        }

        public void setOuterTextHeader(@NonNull String mOuterTextHeader) {
            this.mOuterTextHeader = mOuterTextHeader;
        }

        public String getOuterTextFooter() {
            return mOuterTextFooter;
        }

        public void setOuterTextFooter(@NonNull String mOuterTextFooter) {
            this.mOuterTextFooter = mOuterTextFooter;
        }

        public int getOuterDrawableRes() {
            return mOuterDrawableRes;
        }

        public void setOuterDrawableRes(@DrawableRes int mOuterDrawableRes) {
            this.mOuterDrawableRes = mOuterDrawableRes;
        }

        public int getInnerBackgroundColorRes() {
            return mInnerBackgroundColorRes;
        }

        public void setInnerBackgroundColorRes(@ColorRes int mInnerBackgroundColorRes) {
            this.mInnerBackgroundColorRes = mInnerBackgroundColorRes;
        }

        public int getInnerDrawableRes() {
            return mInnerDrawableRes;
        }

        public void setInnerDrawableRes(int mInnerDrawableRes) {
            this.mInnerDrawableRes = mInnerDrawableRes;
        }

        public String getInnerText() {
            return mInnerText;
        }

        public void setInnerText(@NonNull String mInnerText) {
            this.mInnerText = mInnerText;
        }

        public JSONObject toJson() {
            JSONObject jo = new JSONObject();
            try {
                jo.put("outerTextHeader", mOuterTextHeader);
                jo.put("outerTextFooter", mOuterTextFooter);
                jo.put("outerDrawableRes", mOuterDrawableRes);

                jo.put("innerBackgroundColorRes", mInnerBackgroundColorRes);
                jo.put("innerDrawableRes", mInnerDrawableRes);
                jo.put("innerText", mInnerText);

            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jo;
        }

        public static ScratchCard fromJson(JSONObject jsonObject) {
            ScratchCard scratchCard = null;
            try {
                scratchCard =  new ScratchCard(
                        jsonObject.getString("outerTextHeader"),
                        jsonObject.getString("outerTextFooter"),
                        jsonObject.getInt("outerDrawableRes"),
                        jsonObject.getInt("innerBackgroundColorRes"),
                        jsonObject.getInt("innerDrawableRes"),
                        jsonObject.getString("innerText")
                        );
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return scratchCard;
        }
    }
}
