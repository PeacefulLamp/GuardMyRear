package com.pekka.guardmyrear;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;

public class NewLayoutActivity extends AppCompatActivity {

    ImageButton connectButton;

    static boolean SensorizeServiceStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_layout);

        connectButton = (ImageButton) findViewById(R.id.connectButton);

        /**
         * Start sensor service running in background, where broadcast activity
         *  is performed. Receives JSON values.
         */
        Intent intent = new Intent(this, SensorizingService.class);
        if(!SensorizeServiceStarted)
        {
            System.out.println("Service already active");
            startService(intent);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Invoked when user presses "Connect" button, connects to WiFi network
     *  if necessary.
     */
    public void startStreamActivity(View view){
        connectButton.setColorFilter(Color.argb(100, 0, 0, 0));

        new CountDownTimer(1000, 1000){
            public void onFinish(){
                connectButton.setColorFilter(Color.argb(0, 0, 0, 0));
            }

            public void onTick(long millisUntilFinished){
            }
        }.start();

        /* Test if the user is connected to WiFi */
        WifiManager wman = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        SupplicantState ws = wman.getConnectionInfo().getSupplicantState();
        NetworkInfo.DetailedState wstate = WifiInfo.getDetailedStateOf(ws);

        System.out.println(wstate.name());

        /* If the user is not connected, open the WiFi settings */
        if(
                !wman.isWifiEnabled() ||
                (wstate != NetworkInfo.DetailedState.OBTAINING_IPADDR
                        && wstate != NetworkInfo.DetailedState.CONNECTED
                        && wstate != NetworkInfo.DetailedState.SCANNING))
            startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), 10);
        else
        /* If the user is already connected, start the stream directly */
            startStream();
    }

    /**
     * Receives return value from activity start when pressing "Connect"
     * If successful, launches StreamActivity
     * @param requestCode
     * @param resultCode
     * @param data
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        /* Check if WiFi connected */
        if(requestCode==10) {
            WifiManager wman = (WifiManager) getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);

            SupplicantState ws = wman.getConnectionInfo().getSupplicantState();
            NetworkInfo.DetailedState wstate = WifiInfo.getDetailedStateOf(ws);

            /* If connection failed, pop a snackbar */
            if (wstate != NetworkInfo.DetailedState.CONNECTED) {
                Snackbar nice = Snackbar.make(findViewById(R.id.toolbar), R.string.wifi_error_message, Snackbar.LENGTH_LONG);
                nice.show();
            } else {
                startStream();
            }
        }
    }
    protected void startStream()
    {
        /* Finally, launch the stream view with video and sensor data */
        Intent a = new Intent(this, StreamActivity.class);
        a.putExtra(getResources().getString(R.string.stream_resource), "http://192.168.42.1:8554/stream");
        startActivity(a);
    }
}
