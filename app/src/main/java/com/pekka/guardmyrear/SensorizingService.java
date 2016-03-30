package com.pekka.guardmyrear;

import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

/**
 * This class if for running network operations in the background
 */

public class SensorizingService extends IntentService {


    //the old sensor class
    private class SensorThread extends Thread{
        DatagramSocket m_socket;
        StreamActivity.DataObject dataObject;

        public SensorThread(StreamActivity.DataObject object){
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
    public SensorizingService() {
        super("SensorizingService");
    }
    @Override
    protected void onHandleIntent(Intent intent) {

        //do stuff here

    }
}