package com.pekka.guardmyrear;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

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

    private NotificationManager m_notifyman;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Stream Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.pekka.guardmyrear/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Stream Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.pekka.guardmyrear/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        client.disconnect();
    }

    private class SensorThread extends Thread{
        private DatagramSocket m_socket;
        private Activity m_view;

        public SensorThread(DatagramSocket socket, Activity activity){
            m_socket = socket;
            m_view = activity;
        }

        public void run(){

            while (true){
                final String data = SocketListen(m_socket); //stop and listen for datagram


                //run anything you want on the UI-thread
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        JSONObject js = parseJSON(data);
                        Sensorize(m_view, js); //pass activity down the rabbit hole
                    }
                });
            }

        }
    }

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
            System.out.println();
            System.out.println(data);
            System.out.println();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject js = parseJSON(data);
                    Sensorize(m_view, js);
                }
            });
        }
    }

    /**
     * #########################################
     * ______________ON  CREATE ________________
     * #########################################
     **/

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        System.out.println("ON CREATE");

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
        WebView view = (WebView) findViewById(R.id.webView);

        view.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        view.loadUrl(getIntent().getStringExtra(getResources().getString(R.string.stream_resource)));

        try {
            m_data_socket = new DatagramSocket(5005);

            /** SensorTask virker som den skal nå, men det er
             * kanskje bedre å time datagram'ene fra PI-siden??
             */

            //TimerTask check_task = new SensorTask(this, m_data_socket);
            //m_data_timer = new Timer("Data Timer");
            //m_data_timer.scheduleAtFixedRate(check_task, 100, 100);

            SensorThread sensorThread = new SensorThread(m_data_socket, this);
            sensorThread.start();

            System.out.println("Created UDP multicast listener");

        } catch (SocketException e) {
            System.out.println("this is just a line of text");
            e.printStackTrace();
        }

        Context context = getApplicationContext();
        m_notifyman = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Uri snd = NotificationCenter.GetRingtone();
        NotificationCenter.PingNotification(m_notifyman, context, snd, "Guard My Rear", "Someone is at your rear!");
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
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
        DatagramPacket p = new DatagramPacket(data, data.length);
        try {
            s.receive(p);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (p.getLength() > 0) {
            return new String(Arrays.copyOf(data, p.getLength()));
        }
        return null;
    }

    //Parse JSON string
    private static JSONObject parseJSON(String s) {
        JSONObject jsonObject = null;

        try {
            jsonObject = new JSONObject(s);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    private static void Sensorize(Activity view, JSONObject jsonObject) {

        System.out.println("Seinsorizing...");

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

    public void resizeImage(View view) {
        i += 5;
        resizeLeftIndicator(this, i);
        resizeRightIndicator(this, i);
        resizeCenterIndicator(this, i);
    }

    public static void resizeLeftIndicator(Activity view, int distance) {
        ImageView imageView = (ImageView) view.findViewById(R.id.left_indicator_image);
        TextView textView = (TextView) view.findViewById(R.id.left_indicator_value);
        imageView.getLayoutParams().height = distance;
        imageView.getLayoutParams().width = distance;
        textView.setText(Integer.toString(distance));
    }

    public static void resizeRightIndicator(Activity view, int distance) {
        ImageView imageView = (ImageView) view.findViewById(R.id.right_indicator_image);
        TextView textView = (TextView) view.findViewById(R.id.right_indicator_value);
        imageView.getLayoutParams().height = distance;
        imageView.getLayoutParams().width = distance;
        textView.setText(Integer.toString(distance));
    }

    public static void resizeCenterIndicator(Activity view, int distance) {
        ImageView imageView = (ImageView) view.findViewById(R.id.center_indicator_image);
        TextView textView = (TextView) view.findViewById(R.id.center_indicator_value);
        imageView.getLayoutParams().height = distance;
        imageView.getLayoutParams().width = 2 * distance;
        textView.setText(Integer.toString(distance));
    }
}

