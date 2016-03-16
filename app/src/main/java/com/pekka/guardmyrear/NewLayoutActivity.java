package com.pekka.guardmyrear;

import android.content.Context;
import android.content.Intent;
import android.net.Network;
import android.net.NetworkInfo;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class NewLayoutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_layout);
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

    public void startStreamActivity(View view){
        WifiManager wman = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

        SupplicantState ws = wman.getConnectionInfo().getSupplicantState();
        NetworkInfo.DetailedState wstate = WifiInfo.getDetailedStateOf(ws);

        System.out.println(wstate.name());

        if(
                !wman.isWifiEnabled() ||
                (wstate != NetworkInfo.DetailedState.OBTAINING_IPADDR
                        && wstate != NetworkInfo.DetailedState.CONNECTED
                        && wstate != NetworkInfo.DetailedState.SCANNING))
            startActivityForResult(new Intent(Settings.ACTION_WIFI_SETTINGS), 10);
        else
            startStream();
    }
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if(requestCode==10) {
            WifiManager wman = (WifiManager) getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);

            SupplicantState ws = wman.getConnectionInfo().getSupplicantState();
            NetworkInfo.DetailedState wstate = WifiInfo.getDetailedStateOf(ws);

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
        Intent a = new Intent(this, StreamActivity.class);
        a.putExtra(getResources().getString(R.string.stream_resource), "http://192.168.42.1:8554/stream");
        startActivity(a);
    }
}
