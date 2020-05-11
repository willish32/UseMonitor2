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

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.UUID;

public class BluetoothLeService extends Service {
    private BluetoothAdapter adapt;
    private BluetoothManager manager;
    private String deviceAddress;
    private static BluetoothGatt btGatt;
    private int connectionState = STATE_DISCONNECTED;

    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    public static int totalUse = 0;

    public final static String ACTION_GATT_CONNECTED = "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED = "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED = "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE = "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA = "com.example.bluetooth.le.EXTRA_DATA";

    public final static UUID VIBRATION = UUID.fromString("000000EE-0000-1000-8000-00805F9B34FB");

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                connectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                btGatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                connectionState = STATE_DISCONNECTED;
                broadcastUpdate(intentAction);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };

    static public List<BluetoothGattService> getSupportedGattServices() {
        if(btGatt == null) return null;
        return btGatt.getServices();
    }

    private void broadcastUpdate(final String action) {
        final Intent in = new Intent(action);
        sendBroadcast(in);
    }

    private void broadcastUpdate(final String action, final BluetoothGattCharacteristic characteristic) {
        final Intent in = new Intent(action);
        int format = BluetoothGattCharacteristic.FORMAT_UINT8;
        final int useTime = characteristic.getIntValue(format, 1);
        totalUse = totalUse + useTime;
        Log.d(BluetoothLeService.class.getSimpleName(), String.format("use time: %d", useTime));
        in.putExtra(EXTRA_DATA, String.valueOf(useTime));
        sendBroadcast(in);
    }

    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent){
        close();
        return super.onUnbind(intent);
    }

    private final IBinder binder = new LocalBinder();

    public void initialize() {
        manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        if(manager == null)
            return;
        adapt = manager.getAdapter();
    }

    public boolean connect(final String address) {
        if(adapt == null || address == null) {
            return false;
        }
        if(deviceAddress != null && address.equals(deviceAddress) && btGatt != null) {
            if(btGatt.connect()) {
                connectionState = STATE_CONNECTING;
                return true;
            }
            else {
                return false;
            }
        }
        final BluetoothDevice monitor = adapt.getRemoteDevice(address);
        if(monitor == null) {
            return false;
        }
        btGatt = monitor.connectGatt(this,false,gattCallback);
        deviceAddress = address;
        connectionState = STATE_CONNECTING;
        return true;
    }

    public void disconnect() {
        if(adapt == null || btGatt == null) {
            return;
        }
        btGatt.disconnect();
    }

    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if(adapt != null && btGatt != null) {
            btGatt.setCharacteristicNotification(characteristic, enabled);

            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            btGatt.writeDescriptor(descriptor);
        }
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if(adapt != null && btGatt != null) {
            btGatt.readCharacteristic(characteristic);
        }
    }

    public void close() {
        if(btGatt != null) {
            btGatt.close();
            btGatt = null;
        }
    }


}
