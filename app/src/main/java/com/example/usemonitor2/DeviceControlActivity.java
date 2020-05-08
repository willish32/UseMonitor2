package com.example.usemonitor2;

import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DeviceControlActivity extends Activity {
    private BluetoothLeService bleService;
    private String deviceAddress = "DEVICE_ADDRESS";
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> gCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private BluetoothGattCharacteristic notifyChar;
    private boolean connected = false;
    private ExpandableListView gattServicesList;
    private TextView connectionState;
    private TextView dataField;
    public static int useSinceFix = BluetoothLeService.totalUse;

    public static final int MAINTENANCE_APPROX = 6000;

    public static final String EXTRAS_DEVICE_NAME = "device name";
    public static final String EXTRAS_DEVICE_ADDRESS = "device address";


    private String deviceName = "VIBRATION_MONITOR_PROTOTYPE";

    private final ServiceConnection servConnect = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bleService = ((BluetoothLeService.LocalBinder) service).getService();
            bleService.initialize();
            bleService.connect(deviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            bleService = null;
        }
    };

    private final BroadcastReceiver updateRec = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                connected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)){
                connected = false;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)){
                displayGattServices(BluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)){
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    private final ExpandableListView.OnChildClickListener servListClickListener = new ExpandableListView.OnChildClickListener() {
        @Override
        public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
            if(gCharacteristics != null) {
                final BluetoothGattCharacteristic charac = gCharacteristics.get(groupPosition).get(childPosition);
                final int charP = charac.getProperties();
                if((charP | BluetoothGattCharacteristic.PROPERTY_READ)>0) {
                    if(notifyChar != null) {
                        bleService.setCharacteristicNotification(notifyChar, false);
                        notifyChar = null;
                    }
                    bleService.readCharacteristic(charac);
                }
                if ((charP | BluetoothGattCharacteristic.PROPERTY_NOTIFY)>0){
                    notifyChar = charac;
                    bleService.setCharacteristicNotification(charac, true);
                }
                return true;
            }
            return false;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        System.out.println("Control Create Start");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        deviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        deviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        ((TextView) findViewById(R.id.device_address)).setText(deviceAddress);
        gattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        gattServicesList.setOnChildClickListener(servListClickListener);
        connectionState = (TextView) findViewById(R.id.connection_state);
        dataField = (TextView) findViewById(R.id.data_value);

        //getActionBar().setTitle(deviceName);
        //getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServIntent, servConnect, BIND_AUTO_CREATE);

        if(useSinceFix >= MAINTENANCE_APPROX){
            Toast.makeText(this, R.string.maintenance, Toast.LENGTH_LONG).show();
        }

        System.out.println("Control Create Completed");
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(updateRec, makeGattUpdateIntentFilter());
        if(bleService!=null) {
            bleService.connect(deviceAddress);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(updateRec);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(servConnect);
        bleService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if(connected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case R.id.menu_connect:
                bleService.connect(deviceAddress);
                return true;
            case R.id.menu_disconnect:
                bleService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int rID) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                connectionState.setText(rID);
            }
        });
    }

    private void displayData(String data) {
        if(data != null) {
            dataField.setText(data);
        }
    }

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServString = getResources().getString(R.string.unknown_service);
        String unknownCharString = getResources().getString(R.string.unknown_characteristic);
        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharData = new ArrayList<ArrayList<HashMap<String, String>>>();
        gCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        for (BluetoothGattService gService : gattServices) {
            HashMap<String, String> currServData = new HashMap<String, String>();
            uuid = gService.getUuid().toString();
            currServData.put("NAME", GattAttributes.lookup(uuid, unknownServString));
            currServData.put("UUID", uuid);
            gattServiceData.add(currServData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData = new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics = gService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> characteristics = new ArrayList<BluetoothGattCharacteristic>();
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                characteristics.add(gattCharacteristic);
                HashMap<String, String> currentCharData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharData.put("NAME", GattAttributes.lookup(uuid, unknownCharString));
                currentCharData.put("UUID", uuid);
                gattCharacteristicGroupData.add(currentCharData);
            }
            gCharacteristics.add(characteristics);
            gattCharData.add(gattCharacteristicGroupData);

            SimpleExpandableListAdapter gattServAdapt = new SimpleExpandableListAdapter(
                    this, gattServiceData, android.R.layout.simple_expandable_list_item_2, new String[] {"List", "UUID"},
                    new int[] {android.R.id.text1, android.R.id.text2}, gattCharData, android.R.layout.simple_expandable_list_item_2, new String[] {"List","UUID"},
                    new int[] {android.R.id.text1, android.R.id.text2}
            );
            gattServicesList.setAdapter(gattServAdapt);
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
