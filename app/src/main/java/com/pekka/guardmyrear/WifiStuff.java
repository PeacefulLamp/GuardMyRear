package com.pekka.guardmyrear;

import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;

import java.util.List;

/**
 * Created by havard on 16.03.16.
 */
public class WifiStuff {
    public static boolean ConnectNetwork(WifiManager man, String ssid, String pw)
    {
        WifiConfiguration con = new WifiConfiguration();
        con.SSID = String.format("\"%s\"",ssid);
        con.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        con.preSharedKey = String.format("\"%s\"",pw);

        man.addNetwork(con);

        List<WifiConfiguration> wifis = man.getConfiguredNetworks();

        if(wifis == null)
            return false;

        for(WifiConfiguration i : wifis)
        {
            if(i.SSID != null && i.SSID.equals(con.SSID))
            {
                man.disconnect();
                man.enableNetwork(i.networkId,true);
                man.reconnect();
                return true;
            }
        }
        return false;
    }
}
