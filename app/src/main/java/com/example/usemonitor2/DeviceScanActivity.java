package com.example.usemonitor2;

import android.Manifest;
import android.app.Activity;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class DeviceScanActivity extends ListActivity {
    private static final int PERMIT = 1;
    private BluetoothAdapter btAdapt;
    private boolean scanning;
    private Handler handler;
    private static final long SCAN_PERIOD = 20000;
    private LeDeviceListAdapter bleListAdapt;
    private static final int REQUEST_ENABLE_BT = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        System.out.println("Scan Create Start");
        super.onCreate(savedInstanceState);
        //getActionBar().setTitle(R.string.title_devices);
        handler = new Handler();

        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)){
            Toast.makeText(this, R.string.no_ble, Toast.LENGTH_LONG).show();
            finish();
        }

        final BluetoothManager manager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapt = manager.getAdapter();

        if(btAdapt == null) {
            finish();
            return;
        }
        System.out.println("Scan Create Completed");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if(!scanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        System.out.println("onOptionsItemSelected");
        switch (item.getItemId()) {
            case R.id.menu_scan:
                bleListAdapt.clear();
                scanLeDevice(true);
                break;
            case R.id.menu_stop:
                scanLeDevice(false);
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        System.out.println("onResume");
        super.onResume();

        if(!btAdapt.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        bleListAdapt = new LeDeviceListAdapter();
        setListAdapter(bleListAdapt);
        scanLeDevice(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        System.out.println("Start onPause");
        super.onPause();
        scanLeDevice(false);
        bleListAdapt.clear();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = bleListAdapt.getDevice(position);
        if (device == null) return;
        final Intent intent = new Intent(this, DeviceControlActivity.class);
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
        intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
        if(scanning) {
            btAdapt.stopLeScan(leScanCallback);
            scanning = false;
        }
        startActivity(intent);
    }

    private void scanLeDevice(final boolean en) {
        System.out.println("ScanLeDevice Start");
        if(en) {
            System.out.println("Path1");
            handler.postDelayed(new Runnable(){
                @Override
                public void run() {
                    scanning = false;
                    btAdapt.stopLeScan(leScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);
            scanning = true;
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION,Manifest.permission.ACCESS_FINE_LOCATION}, PERMIT );
            }
            btAdapt.startLeScan(leScanCallback);
        } else {
            System.out.println("Path2");
            scanning = false;
            btAdapt.stopLeScan(leScanCallback);
        }
        invalidateOptionsMenu();
    }

    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> leDevices;
        private LayoutInflater inflater;

        public LeDeviceListAdapter() {
            super();
            leDevices = new ArrayList<BluetoothDevice>();
            inflater = DeviceScanActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if(!leDevices.contains(device)) {
                leDevices.add(device);
            }
        }

        public BluetoothDevice getDevice(int position) {
            return leDevices.get(position);
        }

        public void clear() {
            leDevices.clear();
        }

        @Override
        public int getCount() {
            return leDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return leDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            if(view == null) {
                view = inflater.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = leDevices.get(i);
            final String deviceName = device.getName();
            if(deviceName != null && deviceName.length() > 0) {
                viewHolder.deviceName.setText(deviceName);
            } else {
                viewHolder.deviceName.setText(R.string.unknown_device);
            }
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    bleListAdapt.addDevice(device);
                    bleListAdapt.notifyDataSetChanged();
                }
            });
        }
    };

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }
}
