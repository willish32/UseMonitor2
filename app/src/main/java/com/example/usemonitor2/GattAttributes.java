package com.example.usemonitor2;

import java.util.HashMap;

public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap<String, String>();
    public static String VIBRATE_MEASUREMENT = "000000EE-0000-1000-8000-00805F9B34FB";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "";

    static {
        attributes.put("000000FF-0000-1000-8000-00805F9B34FB", "Monitor Profile");
        attributes.put("0000FF01-0000-1000-8000-00805F9B34FB", "Monitoring Service");
    }

    public static String lookup(String uuid, String defName) {
        String name = attributes.get(uuid);
        return name == null ? defName : name;
    }

}
