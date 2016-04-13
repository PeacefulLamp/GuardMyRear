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
        } catch (JSONException ignored) {
        }
        return jsonObject;
    }

    /**
     * Transform the sensor value from distance to scaling value of proximity
     * @param v Value to be transformed, distance
     * @return Readily transformed value to scale UI elements
     */
    public static double FilterSensorValue(double v)
    {
        /*WARNING: MAGIC COOKIES INBOUND */
        if(v>0)
            return Math.max(Math.min(1 / v, 500)*40000,1);
        return 1;
    }

    /**
     * Extract sensor data from JSON object received from Raspberry Pi
     * @param jsonObject A valid JSON object, must not be null
     * @return Static array of 3 sensor values
     */
    public static double[] Sensorize(JSONObject jsonObject) {

        double sensor1 = 0;
        double sensor2 = 0;
        double sensor3 = 0;

        try {
            sensor1 = jsonObject.getDouble("key1");
            sensor2 = jsonObject.getDouble("key2");
            sensor3 = jsonObject.getDouble("key3");
        } catch (JSONException ignored) {
        }

        sensor1 = FilterSensorValue(sensor1);
        sensor2 = FilterSensorValue(sensor2);
        sensor3 = FilterSensorValue(sensor3);

        return new double[]{sensor1, sensor2, sensor3};
    }
}
