package com.pekka.guardmyrear;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
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
     * the following class, when it is ready to do so,
     * reads the latest sensor data, does some calculations,
     * and updates the layout for the sensor indicators
     */

    public class GraphicsThread extends Thread{

        StreamActivity m_activity;
        DataObject dataObject;
        String data_string;
        Handler UI_handler;


        public GraphicsThread(StreamActivity activity, DataObject object, Handler handler){
            m_activity = activity;
            dataObject = object;
            UI_handler = handler;
        }

        @Override
        public void run(){
            while (true){

                try {
                    Thread.sleep((long) 100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                data_string = dataObject.data_string;

                JSONObject js = parseJSON(data_string);
                final double[] dMan = Sensorize(js);

                UI_handler.post(new Runnable(){
                    public void run(){
                        //do stuff on the UI-thread
                        resizeLeftIndicator((int) dMan[0]);
                        resizeCenterIndicator((int) dMan[1]);
                        resizeRightIndicator((int) dMan[2]);
                    }
                });
            }
        }
    }

    /**
     * what the following class is doing is that it captures the datagram, and updates
     * the sensor data string, so that the graphics thread can work at its own pace.
     *
     */

    private class SensorThread extends Thread{
        DatagramSocket m_socket;
        DataObject dataObject;

        public SensorThread(DataObject object){
            dataObject = object;
            try {
                m_socket = new DatagramSocket(5005);  //the port number might be useful to keep outside the class
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }

        public void run(){

            while (true){
                final String data = SocketListen(m_socket);
                //System.out.println(data);

                dataObject.data_string = data; //Update the sensor data
            }

        }
        /**
         * Listening to UDP multicast and receiving sensor packets
         */
        private String SocketListen(DatagramSocket s) {
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

        //TimerTask check_task = new SensorTask(this, m_data_socket);
        //m_data_timer = new Timer("Data Timer");
        //m_data_timer.scheduleAtFixedRate(check_task, 100, 100);

        /**
         * There is a problem that the socket, and threads are tied to the lifetime of the
         * StreamActivity. When rotating the phone, onCreate is called again, and we probably get
         * some socket-exception (not tested) which might have some quick fix, but we should
         * investigate on how to get everything running in the background
         */

        Handler handler = new Handler(); //handler is now bound to this thread (the UI-thread)

        DataObject dataObject = new DataObject();

        SensorThread sensorThread = new SensorThread(dataObject);
        sensorThread.start();

        GraphicsThread graphicsThread = new GraphicsThread(this, dataObject, handler);
        graphicsThread.start();

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

