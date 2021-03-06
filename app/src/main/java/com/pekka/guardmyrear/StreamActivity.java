package com.pekka.guardmyrear;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Handler;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.sql.Time;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class StreamActivity extends AppCompatActivity implements SensorIndicatorFragment.OnFragmentInteractionListener {
    BroadcastReceiver receiver;
    private NotificationManager m_notifyman;
    private boolean m_isCloseAlready = false;
    final private int proximity = 100;


    private boolean mVisible;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHidePart2Runnable = new Runnable() {
        @SuppressLint("InlinedApi")
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };
    private View mControlsView;
    private final Runnable mShowPart2Runnable = new Runnable() {
        @Override
        public void run() {
            // Delayed display of UI elements
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.show();
            }
            mControlsView.setVisibility(View.VISIBLE);
        }
    };
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    @Override
    public void onFragmentInteraction(Uri uri) {

    }

    /*
    /**
     * The following class stores the sensor values in an object to share between threads
     */

    public class DataObject{
        public String data_string;

        public DataObject(){
            data_string = "{\"key1\":0, \"key2\":0, \"key3\":0}"; //lazy quick fix to avoid null-pointer error in parseJSON method
        }
    }

    /**
     * #########################################
     * ______________ON  CREATE ________________
     * #########################################
     **/

    long lastBeepTime = 0;
    long currentTime;
    int beepDuration = 0;
    final static int MAGIC_COOKIE = 5;    // How much we amplify the distance by, to convert to ms
    final static int MAGIC_PAUSE_DURATION = 50; //  pause between beep (ms)
    ToneGenerator toneGenerator;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_stream);

        /**
         * This class receives JSON object data from the worker
         *  thread which receives data from the network.
         * Aforementioned worker is SensorizingService, which is launched
         *  on application start.
         */
        Context context = getApplicationContext();
        m_notifyman = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        toneGenerator = new ToneGenerator(AudioManager.STREAM_DTMF, ToneGenerator.MAX_VOLUME);
        final Uri snd = NotificationCenter.GetRingtone();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                /* Get JSON string from worker thread */
                String s = intent.getStringExtra("message");

                /* Parse the JSON string and get sensor values */

                JSONObject js = SensorHandling.parseJSON(s);

                if(js==null)
                    return;

                final int[] dMan = SensorHandling.Sensorize(js);

                /**
                if(dMan[0]<proximity || dMan[1]<proximity || dMan[2]<proximity)
                {
                    if(!m_isCloseAlready)
                    {
                        // NotificationCenter.PingNotification(m_notifyman, context, snd, "Guard My Rear", "Someone is at your rear!");


                    }
                }else
                    m_isCloseAlready = false;
                 **/

                /* Apply the sensor values to the UI widgets */

                // Find closest distance of all sensor values
                int closestDist = Integer.MAX_VALUE;
                for (int i = 0; i < dMan.length; i++) {
                    if(dMan[i]< closestDist){
                        closestDist = dMan[i];
                    }
                }
                currentTime = System.currentTimeMillis();
                // Check how long it has been since last beep
                if(currentTime-lastBeepTime > beepDuration+MAGIC_PAUSE_DURATION){
                    // Update when we last beeped
                    lastBeepTime = System.currentTimeMillis();
                    //BEEEEEEEEEEEEEEP Based on distance
                    beepDuration = Math.max(closestDist*MAGIC_COOKIE, 100);
                    toneGenerator.startTone(ToneGenerator.TONE_DTMF_A, beepDuration);
                }


                resizeRightIndicator( dMan[0]);
                resizeCenterIndicator( dMan[1]);
                resizeLeftIndicator( dMan[2]);
            }
        };

        WebView wview = (WebView)findViewById(R.id.webView);
        wview.setX(-20);
        wview.setY(-60);
        wview.setMinimumWidth(wview.getWidth() + 20);
        wview.setMinimumHeight(wview.getHeight() + 60);
        wview.setRight(wview.getWidth()+20);
        wview.setBottom(wview.getHeight()+60);

        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(receiver,new IntentFilter("result"));

        System.out.println("ON CREATE");

        mVisible = true;
        mControlsView = findViewById(R.id.fullscreen_content_controls);
        mContentView = findViewById(R.id.fullscreen_content);

        // Set up the user interaction to manually show or hide the system UI.
        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggle();
            }
        });

        // Upon interacting with UI controls, delay any scheduled hide()
        // operations to prevent the jarring behavior of controls going away
        // while interacting with the UI.
        WebView view = (WebView) findViewById(R.id.webView);

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        view.loadUrl(getIntent().getStringExtra(getResources().getString(R.string.stream_resource)));

        /**
         * There is a problem that the socket, and threads are tied to the lifetime of the
         * StreamActivity. When rotating the phone, onCreate is called again, and we probably get
         * some socket-exception (not tested) which might have some quick fix, but we should
         * investigate on how to get everything running in the background
         */

        resizeCenterIndicator(200);
    }

    /**
     * After creating our instance, hide the system bars
     * @param savedInstanceState
     */
    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    /**
     * Toggle visibility of notification bar and action bar
     */


    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

    /**
     * Hide the notification bar and action bar
     */
    private void hide() {
        // Hide UI first
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.hide();
        }
        mControlsView.setVisibility(View.GONE);
        mVisible = false;

        // Schedule a runnable to remove the status and navigation bar after a delay
        mHideHandler.removeCallbacks(mShowPart2Runnable);
        mHideHandler.postDelayed(mHidePart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * On swipe, show notification bar and action bar again
     */
    @SuppressLint("InlinedApi")
    private void show() {
        // Show the system bar
        mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        mVisible = true;

        // Schedule a runnable to display UI elements after a delay
        mHideHandler.removeCallbacks(mHidePart2Runnable);
        mHideHandler.postDelayed(mShowPart2Runnable, UI_ANIMATION_DELAY);
    }

    /**
     * Schedules a call to hide() in [delay] milliseconds, canceling any
     * previously scheduled calls.
     */
    private void delayedHide(int delayMillis) {
        mHideHandler.removeCallbacks(mHideRunnable);
        mHideHandler.postDelayed(mHideRunnable, delayMillis);
    }

    /**
     * Resize left circular indicator on screen, must run on GUI thread
     * @param distance The distance to be displayed
     */
    public void resizeLeftIndicator(int distance) {
        ImageView imageView = (ImageView) findViewById(R.id.left_indicator_image);
        TextView textView = (TextView) findViewById(R.id.left_indicator_value);
        resizeIndicator(imageView, textView, distance);
    }

    public void resizeRightIndicator(int distance) {
        ImageView imageView = (ImageView) findViewById(R.id.right_indicator_image);
        TextView textView = (TextView) findViewById(R.id.right_indicator_value);
        resizeIndicator(imageView, textView, distance);
    }

    public void resizeCenterIndicator(int distance) {
        ImageView imageView = (ImageView)
                findViewById(R.id.center_indicator_image);
        TextView textView = (TextView) findViewById(R.id.center_indicator_value);

        if(imageView.getLayoutParams().height==distance)
            return;
        //System.out.println("Resized: "+distance);
        imageView.getLayoutParams().height = distance;

        imageView.getLayoutParams().width = distance*2;

        if(textView!=null)
            textView.setText(Integer.toString(distance));
    }

    public void resizeIndicator(ImageView imageView, TextView textView, int distance){
        if(imageView.getLayoutParams().height==distance)
            return;

        imageView.getLayoutParams().height = distance;

        imageView.getLayoutParams().width = distance;


        /**
         * Quick fix
         * Mathematical operation to convert back from dp to cm
         * a better solution is to create a new JSON object with 6 parameters
         * to pass to the UI-Thread (this thread)
         */
        double cMeters = 100 - distance/5.0;

        if(textView!=null)
            textView.setText(Integer.toString((int) cMeters) + " cm");
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        this.finish();
    }
}

