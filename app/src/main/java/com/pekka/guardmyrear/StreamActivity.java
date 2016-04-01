package com.pekka.guardmyrear;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class StreamActivity extends AppCompatActivity implements SensorIndicatorFragment.OnFragmentInteractionListener {

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
    private boolean mVisible;
    private final Runnable mHideRunnable = new Runnable() {
        @Override
        public void run() {
            hide();
        }
    };

    /**
     * Sensor data
     */
    private DatagramSocket m_data_socket;
    private Timer m_data_timer;
    BroadcastReceiver receiver;

    private NotificationManager m_notifyman;

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

    /*
    private class SensorTask extends TimerTask {
        private DatagramSocket m_socket;
        private Activity m_view;

        public SensorTask(Activity view, DatagramSocket socket) {
            m_socket = socket;
            m_view = view;
        }

        @Override
        public void run() {
            final String data = SocketListen(m_socket);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //JSONObject js = parseJSON(data);
                    //Sensorize(m_view, js);
                }
            });
        }
    }
    */

    /**
     * #########################################
     * ______________ON  CREATE ________________
     * #########################################
     **/

    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        super.setContentView(R.layout.activity_stream);



        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String s = intent.getStringExtra("message");
                // do something here.
                System.out.println(s);


                String data_string = s;
                JSONObject js = parseJSON(data_string);
                final double[] dMan = Sensorize(js);

                resizeLeftIndicator((int) dMan[0]);
                resizeCenterIndicator((int) dMan[1]);
                resizeRightIndicator((int) dMan[2]);

            }
        };

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

        //TimerTask check_task = new SensorTask(this, m_data_socket);
        //m_data_timer = new Timer("Data Timer");
        //m_data_timer.scheduleAtFixedRate(check_task, 100, 100);

        /**
         * There is a problem that the socket, and threads are tied to the lifetime of the
         * StreamActivity. When rotating the phone, onCreate is called again, and we probably get
         * some socket-exception (not tested) which might have some quick fix, but we should
         * investigate on how to get everything running in the background
         */

        Context context = getApplicationContext();
        m_notifyman = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Uri snd = NotificationCenter.GetRingtone();
        NotificationCenter.PingNotification(m_notifyman, context, snd, "Guard My Rear", "Someone is at your rear!");
    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        System.out.println(uri);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        // Trigger the initial hide() shortly after the activity has been
        // created, to briefly hint to the user that UI controls
        // are available.
        delayedHide(100);
    }

    private void toggle() {
        if (mVisible) {
            hide();
        } else {
            show();
        }
    }

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

    //Parse JSON string
    public static JSONObject parseJSON(String s) {
        JSONObject jsonObject = null;

        try {
            jsonObject = new JSONObject(s);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    public static double[] Sensorize(JSONObject jsonObject) {

        double sensor1 = 0;
        double sensor2 = 0;
        double sensor3 = 0;

        try {
            sensor1 = jsonObject.getDouble("key1");
            sensor2 = jsonObject.getDouble("key2");
            sensor3 = jsonObject.getDouble("key3");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        //do math here

        return new double[]{sensor1, sensor2, sensor3};
    }


    public void resizeLeftIndicator(int distance) {
        ImageView imageView = (ImageView) findViewById(R.id.left_indicator_image);
        TextView textView = (TextView) findViewById(R.id.left_indicator_value);
        imageView.getLayoutParams().height = distance;
        imageView.getLayoutParams().width = distance;
        textView.setText(Integer.toString(distance));
    }

    public void resizeRightIndicator(int distance) {
        ImageView imageView = (ImageView) findViewById(R.id.right_indicator_image);
        TextView textView = (TextView) findViewById(R.id.right_indicator_value);
        imageView.getLayoutParams().height = distance;
        imageView.getLayoutParams().width = distance;
        textView.setText(Integer.toString(distance));
    }

    public void resizeCenterIndicator(int distance) {
        ImageView imageView = (ImageView)
                findViewById(R.id.center_indicator_image);
        TextView textView = (TextView) findViewById(R.id.center_indicator_value);
        imageView.getLayoutParams().height = distance;
        imageView.getLayoutParams().width = 2 * distance;
        textView.setText(Integer.toString(distance));
    }
}

