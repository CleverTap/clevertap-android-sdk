package com.clevertap.android.pushtemplates.validators

import android.os.Bundle
import com.clevertap.android.pushtemplates.TemplateRenderer
import com.clevertap.android.pushtemplates.TemplateType
import com.clevertap.android.shared.test.BaseTestCase
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ValidatorFactoryTest: BaseTestCase(){

    @Test
    fun test_getValidator_basicTemplateValidatorWithValidKeys_ReturnsTrue(){
        val bundle = Bundle()
        bundle.putString("pt_dl1","https://www.myntra.com/")
        bundle.putString("pt_msg","Get 50-80% off")
        bundle.putString("pt_bg","#fafafa")
        bundle.putString("pt_id","pt_basic")
        bundle.putString("pt_title","End of Reason Sale is coming")

        val templateType = TemplateType.BASIC
        val templateRenderer = TemplateRenderer(application,bundle)
        val result = ValidatorFactory.getValidator(templateType,templateRenderer)

        assertTrue(result!!.validate())
    }

    @Test
    fun test_getValidator_basicTemplateValidatorWithInValidValidKeys_ReturnsFalse(){
        val bundle = Bundle()
        bundle.putString("pt_dl1","https://www.myntra.com/")
        bundle.putString("pt_msg","Get 50-80% off")
        bundle.putString("pt_id","pt_basic")
        bundle.putString("pt_title","End of Reason Sale is coming")

        val templateType = TemplateType.BASIC
        val templateRenderer = TemplateRenderer(application,bundle)
        val result = ValidatorFactory.getValidator(templateType,templateRenderer)

        assertFalse(result!!.validate())
    }

    @Test
    fun test_getValidator_anyCarouselTemplateValidatorWithValidKeys_ReturnsTrue(){
        val bundle = Bundle()
        bundle.putString("pt_dl1","https://www.myntra.com/")
        bundle.putString("pt_msg","Get 50-80% off")
        bundle.putString("pt_id","pt_carousel")
        bundle.putString("pt_bg","#fafafa")
        bundle.putString("pt_title","End of Reason Sale is coming")
        bundle.putString("pt_dl1","https://www.myntra.com/")
        bundle.putString("pt_img1","https://www.myntra.com/")
        bundle.putString("pt_img2","https://www.myntra.com/")
        bundle.putString("pt_img3","https://www.myntra.com/")

        val templateType = TemplateType.AUTO_CAROUSEL
        val templateRenderer = TemplateRenderer(application,bundle)
        val result = ValidatorFactory.getValidator(templateType,templateRenderer)

        assertTrue(result!!.validate())
    }

    @Test
    fun test_getValidator_anyCarouselTemplateValidatorWithInValidKeys_ReturnsFalse(){
        val bundle = Bundle()
        bundle.putString("pt_dl1","https://www.myntra.com/")
        bundle.putString("pt_msg","Get 50-80% off")
        bundle.putString("pt_id","pt_carousel")
        bundle.putString("pt_bg","#fafafa")
        bundle.putString("pt_title","End of Reason Sale is coming")
        bundle.putString("pt_dl1","https://www.myntra.com/")
        bundle.putString("pt_img1","https://www.myntra.com/")

        val templateType = TemplateType.MANUAL_CAROUSEL
        val templateRenderer = TemplateRenderer(application,bundle)
        val result = ValidatorFactory.getValidator(templateType,templateRenderer)

        assertFalse(result!!.validate())
    }

    @Test
    fun test_getValidator_ratingTemplateValidatorWithValidKeys_ReturnsTrue(){
        val bundle = Bundle()
        bundle.putString("pt_dl1","https://www.myntra.com/")
        bundle.putString("pt_msg","Get 50-80% off")
        bundle.putString("pt_id","pt_rating")
        bundle.putString("pt_bg","#fafafa")
        bundle.putString("pt_title","End of Reason Sale is coming")
        bundle.putString("pt_dl1","https://www.myntra.com/")
        bundle.putString("wzrk_dl","https://www.google.com/")

        val templateType = TemplateType.RATING
        val templateRenderer = TemplateRenderer(application,bundle)
        val result = ValidatorFactory.getValidator(templateType,templateRenderer)

        assertTrue(result!!.validate())
    }

    @Test
    fun test_getValidator_ratingTemplateValidatorWithInValidKeys_ReturnsFalse(){
        val bundle = Bundle()
        bundle.putString("pt_dl1","https://www.myntra.com/")
        bundle.putString("pt_msg","Get 50-80% off")
        bundle.putString("pt_id","pt_rating")
        bundle.putString("pt_bg","#fafafa")
        bundle.putString("pt_title","End of Reason Sale is coming")
        bundle.putString("pt_dl1","https://www.myntra.com/")

        val templateType = TemplateType.RATING
        val templateRenderer = TemplateRenderer(application,bundle)
        val result = ValidatorFactory.getValidator(templateType,templateRenderer)

        assertFalse(result!!.validate())
    }

    @Test
    fun test_getValidator_fiveIconTemplateValidatorWithValidKeys_ReturnsTrue(){
        val bundle = Bundle()
        bundle.putString("pt_id","pt_five_icons")
        bundle.putString("pt_bg","#fafafa")
        bundle.putString("pt_dl1","https://www.myntra.com/")
        bundle.putString("pt_dl2","https://www.myntra.com/")
        bundle.putString("pt_dl3","https://www.myntra.com/")
        bundle.putString("pt_img1","https://www.myntra.com/")
        bundle.putString("pt_img2","https://www.myntra.com/")
        bundle.putString("pt_img3","https://www.myntra.com/")

        val templateType = TemplateType.FIVE_ICONS
        val templateRenderer = TemplateRenderer(application,bundle)
        val result = ValidatorFactory.getValidator(templateType,templateRenderer)

        assertTrue(result!!.validate())
    }

    @Test
    fun test_getValidator_fiveIconTemplateValidatorWithInValidKeys_ReturnsFalse(){
        val bundle = Bundle()
        bundle.putString("pt_id","pt_five_icons")
        bundle.putString("pt_bg","#fafafa")
        bundle.putString("pt_dl1","https://www.myntra.com/")
        bundle.putString("pt_dl2","https://www.myntra.com/")
        bundle.putString("pt_img1","https://www.myntra.com/")
        bundle.putString("pt_img2","https://www.myntra.com/")

        val templateType = TemplateType.FIVE_ICONS
        val templateRenderer = TemplateRenderer(application,bundle)
        val result = ValidatorFactory.getValidator(templateType,templateRenderer)

        assertFalse(result!!.validate())
    }

    @Test
    fun test_getValidator_productDisplayTemplateValidatorWithValidKeys_ReturnsTrue(){
        val bundle = Bundle()
        bundle.putString("pt_msg","Get 50-80% off")
        bundle.putString("pt_id","pt_product_display")
        bundle.putString("pt_bg","#fafafa")
        bundle.putString("pt_title","End of Reason Sale is coming")
        bundle.putString("pt_dl1","https://www.myntra.com/")
        bundle.putString("pt_dl2","https://www.myntra.com/")
        bundle.putString("pt_dl3","https://www.myntra.com/")
        bundle.putString("pt_img1","https://www.myntra.com/")
        bundle.putString("pt_img2","https://www.myntra.com/")
        bundle.putString("pt_img3","https://www.myntra.com/")
        bundle.putString("pt_bt1","BigText1")
        bundle.putString("pt_bt2","BigText2")
        bundle.putString("pt_bt3","BigText3")
        bundle.putString("pt_st1","SmallText1")
        bundle.putString("pt_st2","SmallText2")
        bundle.putString("pt_st3","SmallText3")
        bundle.putString("pt_product_display_action","Buy Now")
        bundle.putString("pt_product_display_action_clr","#fafafa")

        val templateType = TemplateType.PRODUCT_DISPLAY
        val templateRenderer = TemplateRenderer(application,bundle)
        val result = ValidatorFactory.getValidator(templateType,templateRenderer)

        assertTrue(result!!.validate())
    }

    @Test
    fun test_getValidator_productDisplayTemplateValidatorWithInValidKeys_ReturnsFalse(){
        val bundle = Bundle()
        bundle.putString("pt_msg","Get 50-80% off")
        bundle.putString("pt_id","pt_product_display")
        bundle.putString("pt_bg","#fafafa")
        bundle.putString("pt_title","End of Reason Sale is coming")
        bundle.putString("pt_dl1","https://www.myntra.com/")
        bundle.putString("pt_dl2","https://www.myntra.com/")
        bundle.putString("pt_dl3","https://www.myntra.com/")
        bundle.putString("pt_bt1","BigText1")
        bundle.putString("pt_bt2","BigText2")
        bundle.putString("pt_bt3","BigText3")
        bundle.putString("pt_st1","SmallText1")
        bundle.putString("pt_st2","SmallText2")
        bundle.putString("pt_st3","SmallText3")
        bundle.putString("pt_product_display_action","Buy Now")
        bundle.putString("pt_product_display_action_clr","#fafafa")

        val templateType = TemplateType.PRODUCT_DISPLAY
        val templateRenderer = TemplateRenderer(application,bundle)
        val result = ValidatorFactory.getValidator(templateType,templateRenderer)

        assertFalse(result!!.validate())
    }

    @Test
    fun test_getValidator_zeroBezelValidatorWithValidKeys_ReturnsTrue(){
        val bundle = Bundle()
        bundle.putString("pt_msg","Get 50-80% off")
        bundle.putString("pt_id","pt_zero_bezel")
        bundle.putString("pt_title","End of Reason Sale is coming")
        bundle.putString("pt_big_img","https://s1.1zoom.me/prev/437/436033.jpg")

        val templateType = TemplateType.ZERO_BEZEL
        val templateRenderer = TemplateRenderer(application,bundle)
        val result = ValidatorFactory.getValidator(templateType,templateRenderer)

        assertTrue(result!!.validate())
    }

    @Test
    fun test_getValidator_zeroBezelValidatorWithInValidKeys_ReturnsFalse(){
        val bundle = Bundle()
        bundle.putString("pt_msg","Get 50-80% off")
        bundle.putString("pt_id","pt_zero_bezel")
        bundle.putString("pt_title","End of Reason Sale is coming")

        val templateType = TemplateType.ZERO_BEZEL
        val templateRenderer = TemplateRenderer(application,bundle)
        val result = ValidatorFactory.getValidator(templateType,templateRenderer)

        assertFalse(result!!.validate())
    }

    @Test
    fun test_getValidator_timerValidatorWithValidKeys_ReturnsTrue(){
        val bundle = Bundle()
        bundle.putString("pt_msg","Get 50-80% off")
        bundle.putString("pt_id","pt_timer")
        bundle.putString("pt_bg","#fafafa")
        bundle.putString("pt_title","End of Reason Sale is coming")
        bundle.putString("pt_timer_threshold","12")

        val templateType = TemplateType.TIMER
        val templateRenderer = TemplateRenderer(application,bundle)
        val result = ValidatorFactory.getValidator(templateType,templateRenderer)

        assertTrue(result!!.validate())
    }

    @Test
    fun test_getValidator_timerValidatorWithInValidKeys_ReturnsFalse(){
        val bundle = Bundle()
        bundle.putString("pt_msg","Get 50-80% off")
        bundle.putString("pt_id","pt_timer")
        bundle.putString("pt_bg","#fafafa")
        bundle.putString("pt_title","End of Reason Sale is coming")

        val templateType = TemplateType.TIMER
        val templateRenderer = TemplateRenderer(application,bundle)
        val result = ValidatorFactory.getValidator(templateType,templateRenderer)

        assertFalse(result!!.validate())
    }

    @Test
    fun test_getValidator_inputBoxTemplateValidatorWithValidKeys_ReturnsTrue(){
        val bundle = Bundle()
        val array = JSONArray()
        array.put("'l':'True','id':'2'")
        array.put("'l':'True','id':'3'")
        array.put("'l':'True','id':'4'")

        bundle.putString("pt_msg","Get 50-80% off")
        bundle.putString("pt_id","pt_input")
        bundle.putString("pt_title","End of Reason Sale is coming")
        bundle.putString("pt_input_feedback","Thanks")
        bundle.putString("wzrk_acts",array.toString())

        val templateType = TemplateType.INPUT_BOX
        val templateRenderer = TemplateRenderer(application,bundle)
        val result = ValidatorFactory.getValidator(templateType,templateRenderer)

        assertTrue(result!!.validate())
    }

    @Test
    fun test_getValidator_inputBoxTemplateValidatorWithInValidKeys_ReturnsFalse(){
        val bundle = Bundle()
        val array = JSONArray()
        array.put("'l':'True','id':'2'")
        array.put("'l':'True','id':'3'")
        array.put("'l':'True','id':'4'")

        bundle.putString("pt_msg","Get 50-80% off")
        bundle.putString("pt_id","pt_input")
        bundle.putString("pt_title","End of Reason Sale is coming")
        bundle.putString("pt_input_feedback","Thanks")

        val templateType = TemplateType.INPUT_BOX
        val templateRenderer = TemplateRenderer(application,bundle)
        val result = ValidatorFactory.getValidator(templateType,templateRenderer)

        assertFalse(result!!.validate())
    }
}