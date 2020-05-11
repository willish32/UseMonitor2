package com.example.usemonitor2;

/*
 * Modified and based off of code available under this copyright:
 * Link to sample code provided by Android: https://github.com/android/connectivity-samples/tree/master/BluetoothLeGatt
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


import java.util.HashMap;

public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap<String, String>();
    public static String VIBRATE_MEASUREMENT = "000000EE-0000-1000-8000-00805F9B34FB";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "0000FF04-0000-1000-8000-00805F9B34FB";

    static {
        attributes.put("000000FF-0000-1000-8000-00805F9B34FB", "Monitor Profile");
        attributes.put("0000FF01-0000-1000-8000-00805F9B34FB", "Monitoring Service");
    }

    public static String lookup(String uuid, String defName) {
        String name = attributes.get(uuid);
        return name == null ? defName : name;
    }

}
