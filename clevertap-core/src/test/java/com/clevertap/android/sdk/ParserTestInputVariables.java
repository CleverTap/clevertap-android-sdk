package com.clevertap.android.sdk;

import com.clevertap.android.sdk.feat_variable.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class ParserTestInputVariables {
    @Variable public static String welcomeMsg = "Hi User";
    @Variable   public static Boolean isOptedForOffers = true;
    @Variable   public static Integer initialCoins = 35;
    @Variable   public static Float correctGuessPercentage = 100.0F;
    @Variable   public static HashMap<String,Object> userConfigurableProps = new HashMap<>();
    @Variable   public static ArrayList<String> aiName = new ArrayList<>(Arrays.asList("don", "jason", "shiela", "may"));

    static {
        userConfigurableProps.put("numberOfGuesses", 3);
        userConfigurableProps.put("difficultyLevel", 1.2F);
        userConfigurableProps.put("ai_Gender","M");
    }


}


/*

class MyApp:Application() {
    companion object {
        const val appID = "app_4E1oVqnj8hvB2KmrAXRS5M6STH1fQGy3RvuhSQM73ew"
        const val production1 = "prod_be2EraPzw8kcAjdwXogxsXaEu3aSXABQUt8WDiYrShU"
        const val production2 = "prod_4A36JePcRSLpJMNnLDrqCfAI6LOuB7GDkMzwTOuzFso"
        const val production3 = "prod_ZkAXNrgpXEpW8oBm5tRk1sAG6A43Q4HqrpUCiN84HRg"
        const val development1 = "dev_GwGwcOisoaviTLiWwhWlbXEaAwOsFE1JUaUXYFOdnAY"
        const val development2 = "dev_6H120hDBNeRtBCyZIVkOmdwW1WkW1eX1VgyA1fkOYfM"
        const val development3 = "dev_ggCy2JYsAMpoZBVEnuDQwiaLfgfQsCckSazYWlSIgns"
        const val dataExport = "exp_QjCwMsk6beGm75Z2ba9VkBDKx73ZYxqgTktMAORTfuQ"
        const val contentReadOnly = "cro_1DpId00UGT1eirFbagmskpaLcy6Zg2oZa5AhFVZJKYU"
        const val contentReadWrite = "crw_O8O3ZAjLXoojRzXAfi9ACmtzAVGZH3KWBxnjRllJJcs"
    }

    override fun onCreate() {
        Leanplum.setLogLevel(Level.DEBUG)
        Leanplum.setVariantDebugInfoEnabled(true)
        super.onCreate()
        Leanplum.setApplicationContext(this)
        MyVars.detect()
        LeanplumActivityHelper.enableLifecycleCallbacks(this)

        if (BuildConfig.DEBUG) {
            Leanplum.setAppIdForDevelopmentMode(appID, development1);
        } else {
            Leanplum.setAppIdForProductionMode(appID, production1);
        }
        // Leanplum.setVariantDebugInfoEnabled(true)
        Leanplum.start(this)

    }
}

class MainActivity : AppCompatActivity() {
    private val binding by lazy { LayoutMainBinding.inflate(layoutInflater) }
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)


        with(binding){
            btEventLikedVideo.setOnClickListener { Leanplum.track("LIKED_VIDEO")
                Log.e(TAG, "onCreate: my user= ${Leanplum.getUserId()}", )
            }

            etEmail.setOnEditorActionListener { v, k, event ->
                Log.e(TAG, "etEmail.setOnKeyListener: key=$k, eventaction=${event} ", )
                if(k== EditorInfo.IME_ACTION_DONE){
                    val email = etEmail.text.toString()
                    Leanplum.setUserAttributes(mapOf("email" to email))
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }

            etGender.setOnEditorActionListener { v, k, event ->
                Log.e(TAG, "etGender.setOnKeyListener: key=$k, eventaction=${event} ", )

                if(k== EditorInfo.IME_ACTION_DONE){
                    val gender = etGender.text.toString()
                    Leanplum.setUserAttributes(mapOf("gender" to gender))
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }

            btCheckVars.setOnClickListener { checkVariables() }
            btUpdateVars.setOnClickListener { VarCache.sendContentIfChanged(true, false)}
        }


        MyVars.enableHandler(object : VariablesChangedCallback(){
            override fun variablesChanged() {
                checkVariables()
            }
        }
        )
    }


}



 public class MyVars {
    private static final String TAG = "MyVariables";


    public static  void detect() {
        Log.d("MyVariables", "detect() called");
        Parser.parseVariables(MyVars.class);
        Parser.parseVariablesForClasses(MyVars.class);
    }

    public  static  void enableHandler(VariablesChangedCallback callback) {
        Log.d("MyVariables", "enableHandler() called with: callback = " + callback);
        Leanplum.addVariablesChangedHandler(callback);
    }


}


*/