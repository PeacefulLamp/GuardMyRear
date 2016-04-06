package com.pekka.guardmyrear;


import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import org.json.JSONObject;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;

public class SensorizingService extends Service {
    LocalBroadcastManager broadcaster;

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


        public GraphicsThread(DataObject object, Handler handler){
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

                UI_handler.post(new Runnable(){
                    public void run(){
                        sendResult(data_string);
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

            System.out.println("Worker thread has started");

            while (true){
                //TODO: Use timer to preserve battery
                final String data = SocketListen(m_socket);
                //System.out.println(data);

                dataObject.data_string = data; //Update the sensor data
            }

        }
        /**
         * Listening to UDP multicast and receiving sensor packets
         */
        private String SocketListen(DatagramSocket s) {
            /* Read socket data from multicast UDP socket */
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



    /**
     * A constructor is required, and must call the super IntentService(String)
     * constructor with a name for the worker thread.
     */

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void sendResult(String message) {
        Intent intent = new Intent("result");
        if(message != null)
            intent.putExtra("message", message);
        broadcaster.sendBroadcast(intent);
    }

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns, IntentService
     * stops the service, as appropriate.
     */

    @Override
    public void onCreate() {
        broadcaster = LocalBroadcastManager.getInstance(this);

    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        System.out.println("________________SERVICE IS RUNNING______________");


        Handler handler = new Handler(); //handler is now bound to this thread (the UI-thread)

        DataObject dataObject = new DataObject();

        SensorThread sensorThread = new SensorThread(dataObject);
        sensorThread.start();

        GraphicsThread graphicsThread = new GraphicsThread(dataObject, handler);
        graphicsThread.start();

        return Service.START_NOT_STICKY;
    }

}