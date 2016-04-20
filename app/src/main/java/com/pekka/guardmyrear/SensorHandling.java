package com.pekka.guardmyrear;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by havard on 06.04.16.
 */
public class SensorHandling {
    /**
     *
     * @param s
     * @return
     */
    public static JSONObject parseJSON(String s) {
        JSONObject jsonObject = null;

        try {
            jsonObject = new JSONObject(s);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject;
    }

    /**
     * Transform the sensor value from distance to scaling value of proximity
     * @return Readily transformed value to scale UI elements
     */
    public static int FilterSensorValue(int value) {
        /*WARNING: MAGIC COOKIES INBOUND */

        double v = value;
        double number = Math.min(40000 / v - 270, 500);
        //int n2 = 10* (int) Math.round(number/10.0); //on second thoughts; this should be done on the pi-side
        return Math.max((int) number, 1);
    }

    /**
     * Extract sensor data from JSON object received from Raspberry Pi
     * @param jsonObject A valid JSON object, must not be null
     * @return Static array of 3 sensor values
     */
    public static int[] Sensorize(JSONObject jsonObject) {

        int sensor1 = 0;
        int sensor2 = 0;
        int sensor3 = 0;

        try {
            sensor1 = jsonObject.getInt("k1");
            sensor2 = jsonObject.getInt("k2");
            sensor3 = jsonObject.getInt("k3");
        } catch (JSONException ignored) {
        }

        //sensor1 = FilterSensorValue(sensor1);
        //sensor2 = FilterSensorValue(sensor2);
        //sensor3 = FilterSensorValue(sensor3);
        if (sensor1 == 0){
            System.out.println("sensor 1 er null")ø
        }

        return new int[]{sensor1, sensor2, sensor3};
    }
}
