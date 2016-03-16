package com.pekka.guardmyrear;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Handler;
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

    /** Sensor data */
    private DatagramSocket m_data_socket;
    private Timer m_data_timer;

    private NotificationManager m_notifyman;

    private class SensorTask extends TimerTask
    {
        private DatagramSocket m_socket;
        private Activity m_view;
        public SensorTask(Activity view, DatagramSocket socket)
        {
            m_socket = socket;
            m_view = view;
        }
        @Override
        public void run() {
            String data = SocketListen(m_socket);
            JSONObject js = parseJSON(data);
            Sensorize(m_view,js);
        }
    }

    /**

    _______________ON        CREATE ____________________

     **/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_stream);

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

        WebView view = (WebView)findViewById(R.id.webView);

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        view.loadUrl(getIntent().getStringExtra(getResources().getString(R.string.stream_resource)));

        try {
            m_data_socket = new DatagramSocket(5005);
            TimerTask check_task = new SensorTask(this, m_data_socket);
            m_data_timer = new Timer("Data Timer");
            m_data_timer.scheduleAtFixedRate(check_task,100,100);

            System.out.println("Created UDP multicast listener");

        }catch(SocketException e)
        {
            e.printStackTrace();
        }

        Context context = getApplicationContext();
        m_notifyman = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);

        Uri snd = NotificationCenter.GetRingtone();
        NotificationCenter.PingNotification(m_notifyman,context,snd,"Guard My Rear","Someone is at your rear!");
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

    /**
     * Listening to UDP multicast and receiving sensor packets
     */
    private static String SocketListen(DatagramSocket s) {
        byte[] data = new byte[4096];
        DatagramPacket p = new DatagramPacket(data,data.length);
        try {
            s.receive(p);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(p.getLength()>0)
        {
            return new String(Arrays.copyOf(data,p.getLength()));
        }
        return null;
    }

    //Parse JSON string
    private static JSONObject parseJSON(String s){
        JSONObject jsonObject = null;

        try {
            jsonObject = new JSONObject(s);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private static void Sensorize(Activity view, JSONObject jsonObject){

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

        resizeLeftIndicator(view, (int) sensor1);
        resizeRightIndicator(view, (int) sensor2);
        resizeCenterIndicator(view, (int) sensor3);
    }


    //Method to test resize
    int i = 200;
    public void resizeImage(Activity view){
        i += 5;
        this.resizeLeftIndicator(view, i);
        this.resizeRightIndicator(view, i);
        this.resizeCenterIndicator(view, i);
    }


    public static void resizeLeftIndicator(Activity view, int distance){
        ImageView imageView = (ImageView) view.findViewById(R.id.left_indicator_image);
        TextView textView = (TextView) view.findViewById(R.id.left_indicator_value);
        imageView.getLayoutParams().height = distance;
        imageView.getLayoutParams().width = distance;
        textView.setText(Integer.toString(distance));
    }
    public static void resizeRightIndicator(Activity view, int distance){
        ImageView imageView = (ImageView) view.findViewById(R.id.right_indicator_image);
        TextView textView = (TextView) view.findViewById(R.id.right_indicator_value);
        imageView.getLayoutParams().height = distance;
        imageView.getLayoutParams().width = distance;
        textView.setText(Integer.toString(distance));
    }

    public static void resizeCenterIndicator(Activity view, int distance){
        ImageView imageView = (ImageView) view.findViewById(R.id.center_indicator_image);
        TextView textView = (TextView) view.findViewById(R.id.center_indicator_value);
        imageView.getLayoutParams().height = distance;
        imageView.getLayoutParams().width = 2*distance;
        textView.setText(Integer.toString(distance));
    }
}
