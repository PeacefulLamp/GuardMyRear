package com.pekka.guardmyrear;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

/**
 * Created by Ozzy on 16.03.2016.
 */
public class SplashScreenActivity extends Activity{

    private final int TIME_OUT = 3000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splashscreen);

        new Handler().postDelayed(new Runnable() {

            /*
             * Showing splash screen with a timer. This will be useful when you
             * want to show case your app logo / company
             */

            @Override
            public void run() {
                // Run will execute when time has run out
                // Then switch to main menu
                Intent i = new Intent(SplashScreenActivity.this, StreamActivity.class);
                startActivity(i);

                // close the splashscreenactivity
                finish();
            }
        }, TIME_OUT);
    }

}
