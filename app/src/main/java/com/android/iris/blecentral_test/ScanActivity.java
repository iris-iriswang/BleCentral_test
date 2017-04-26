package com.android.iris.blecentral_test;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ScanActivity extends AppCompatActivity {
    private Button btn_Scan;
    private ListView lst_BleDevices;
    private TextView txt_Advertise;
    private BleDeviceAdapter mBleDeviceAdapter;

    private BluetoothLeScanner mBleScanner;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothAdapter mBluetoothAdapter;
    private Handler mHandler = new Handler();
    private List<BluetoothGattService> mServices = null;
    private final  String TAG = "TAG";
    String SERVICE_HEART_RATE = "0000180D-0000-1000-8000-00805F9B34FB";
    String CHAR_Body_Sensor_Location_READ = "00002A38-0000-1000-8000-00805F9B34FB";
    //String strRandomService = "";


    private Handler MessageHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            txt_Advertise.setText(txt_Advertise.getText() + "\n"+ msg.getData().getString("title") + ":"+ msg.getData().getString("msg"));

            super.handleMessage(msg);
        }
    };

    private Handler mUIHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        //get UI thread
        mUIHandler = new Handler(getApplicationContext().getMainLooper());

        //get components from layout
        btn_Scan = (Button) findViewById(R.id.btn_Scan);
        lst_BleDevices = (ListView) findViewById(R.id.lst_BleDevices);
        txt_Advertise = (TextView) findViewById(R.id.txt_Advertise);

        //mBleDeviceAdapter = new BleDeviceAdapter();
        //lst_BleDevices.setAdapter(mBleDeviceAdapter);

        //get Bluetooth Adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBleScanner = mBluetoothAdapter.getBluetoothLeScanner();
        mBluetoothAdapter.setName("sony");

        btn_Scan.setOnClickListener(btn_Scan_Listener);
    }

    Button.OnClickListener btn_Scan_Listener = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            discoverDevice();
        }
    };


    TextView.OnClickListener txt_Advertise_Listener = new TextView.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(getApplicationContext(),"text onClick", Toast.LENGTH_LONG).show();
            if(mBluetoothDevice == null){
                Toast.makeText(getApplicationContext(),"mBluetoothDevice is null", Toast.LENGTH_LONG).show();
            }else{
                if(mBluetoothGatt != null){
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;
                }
                Toast.makeText(getApplicationContext(),"Device Name:" + mBluetoothDevice.getName(), Toast.LENGTH_LONG).show();
                boolean boo = mUIHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "connect runnable");
                        mBluetoothGatt = mBluetoothDevice.connectGatt(getApplicationContext(), false, mGattCallback);
                    }
                });
                Log.d(TAG, "connectGatt: " +boo);
            }
        }
    };

    /**
     * discover advertise of the device*/
    private void discoverDevice(){
        Log.d(TAG, "discoverDevice");
        Toast.makeText(getApplicationContext(), "discover", Toast.LENGTH_SHORT).show();
        if(mBleScanner == null){
            Toast.makeText(getApplicationContext(), "scanner null", Toast.LENGTH_SHORT).show();
            Log.d(TAG, "mBleScanner is null");
        }

        List<ScanFilter> filters = new ArrayList<>();

        ScanFilter filter = new ScanFilter.Builder()
                //.setServiceUuid(new ParcelUuid(UUID.fromString(SERVICE_HEART_RATE)))
                //.setServiceUuid(new ParcelUuid(UUID.fromString(getString(R.string.ble_uuid16))))
                .build();

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        filters.add(filter);

        mBleScanner.startScan(filters, settings, mScanCallback);

        Log.d(TAG, "after startScan");

//        /*after 10 seconds, the device stop to scan*/
//        mHandler.postDelayed(new Runnable() {
//            @Override
//            public void run() {
//               stopDiscover();
//            }
//        }, 60000);
    }

    /**
     * stop to discover advertise of the device*/
    private void stopDiscover(){
        mBleScanner.stopScan(mScanCallback);
        Log.d(TAG, "stopDiscover");
    }

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            Log.d(TAG, "ScanCallback");
            if(result == null || result.getDevice() == null || TextUtils.isEmpty(result.getDevice().getName()))
                return;

            stopDiscover();
            StringBuilder builder = new StringBuilder(result.getDevice().getName());
            builder.append("\n").append(new String(result.getScanRecord().getServiceData(result.getScanRecord().getServiceUuids().get(0)), Charset.forName("UTF-8")));
            builder.append("\n").append(result.toString());

            Message msg = new Message();
            Bundle data = new Bundle();
            data.putString("title", "advertise:");
            data.putString("msg", builder.toString());
            msg.setData(data);
            MessageHandler.sendMessage(msg);

            //get remote device address
            mBluetoothDevice = result.getDevice();
            SERVICE_HEART_RATE = result.getScanRecord().getServiceUuids().get(0).toString();

            txt_Advertise.setOnClickListener(txt_Advertise_Listener);

        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            Log.d(TAG, "ScanCallback Batch");
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            Log.d(TAG, "ScanCallback onScanFailed");
            String strError = "";
            if(errorCode == ScanCallback.SCAN_FAILED_INTERNAL_ERROR){
                strError = "SCAN_FAILED_INTERNAL_ERROR";
            }
            if(errorCode == ScanCallback.SCAN_FAILED_ALREADY_STARTED){
                strError = "SCAN_FAILED_ALREADY_STARTED";
            }
            if(errorCode == ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED){
                strError = "SCAN_FAILED_APPLICATION_REGISTRATION_FAILED";
            }
            if(errorCode == ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED){
                strError = "SCAN_FAILED_FEATURE_UNSUPPORTED";
            }
            Log.e(TAG, "Discovery onScanFailed: " + errorCode + "-" + strError);
            Toast.makeText(getApplicationContext(), "Discovery onStartFailure: " + errorCode + "-" + strError, Toast.LENGTH_SHORT).show();
        }
    };



    /**
     * Callback handles GATT client events, such as results from
     * reading or writing a characteristic value on the server.
     */
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);
            //Log.d(TAG, "onConnectionStateChange() gatt: " + gatt.getConnectedDevices());
            mBluetoothGatt = gatt;
            //GATT_SUCCESS=0
            if(status == BluetoothGatt.GATT_SUCCESS){
                //STATE_CONNECTED=2 //STATE_DISCONNECTED=0
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    Log.d(TAG, "gattcallback Connected");

                    Message msg = new Message();
                    Bundle data = new Bundle();
                    data.putString("title", "state");
                    data.putString("msg", "Connected");
                    msg.setData(data);
                    MessageHandler.sendMessage(msg);

                    boolean boo = mUIHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "discoverService runnable");
                            mBluetoothGatt.discoverServices();
                        }
                    });
                    Log.d(TAG,"DiscoverService: "+ boo);

                }else{
                    Log.d(TAG, "gattcallback DisConnected newState: " + newState);

                    Message msg = new Message();
                    Bundle data = new Bundle();
                    data.putString("title", "state");
                    data.putString("msg", "DisConnected");
                    msg.setData(data);
                    MessageHandler.sendMessage(msg);


                }
            }else{
                Log.d(TAG, "gattcallback onConnectionStateChange status: " +status + " newState: "+ newState);
                mBluetoothGatt.close();
                mBluetoothGatt = null;
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);
            mServices = gatt.getServices();
            Log.i(TAG, "gattcallback onServicesDiscovered: " + mServices.toString());
            Log.i(TAG, "gattcallback onServicesDiscovered status: " + status);
            BluetoothGattService gs = gatt.getService(UUID.fromString(SERVICE_HEART_RATE));
            gatt.readCharacteristic(gs.getCharacteristics().get(0));

            for(BluetoothGattService bleGattService : mServices){
                Message msg = new Message();
                Bundle data = new Bundle();
                data.putString("title", "Service UUID: ");
                data.putString("msg", bleGattService.getUuid().toString());
                msg.setData(data);
                MessageHandler.sendMessage(msg);
            }

        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            super.onCharacteristicRead(gatt, characteristic, status);
            final String charValue = characteristic.getStringValue(0);
            Log.d(TAG,"Char Read:" + charValue );

            Message msg = new Message();
            Bundle data = new Bundle();
            data.putString("title", "value: ");
            data.putString("msg", charValue);
            msg.setData(data);
            MessageHandler.sendMessage(msg);

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.i(TAG, "Notification of time characteristic changed on server.");
            final int charValue = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    //mLatestValue.setText(String.valueOf(charValue));
                }
            });

        }
    };


    /**
    * the Adapter for the list of Devices*/
    private class BleDeviceAdapter extends BaseAdapter {
        private LayoutInflater mLayoutInflater;


        public BleDeviceAdapter() {
            mLayoutInflater = LayoutInflater.from(getApplicationContext());
        }

        @Override
        public int getCount() {
            return 0;
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder vholder;
            if (convertView == null) {
                convertView = mLayoutInflater.inflate(R.layout.list_item, null);

                vholder = new ViewHolder();
                //in order not to call the findViewById too early, so use "View.findViewById" to call the obj.
                vholder.txt_DeviceName = (TextView) convertView.findViewById(R.id.txt_DeviceName);
                vholder.txt_DeviceAddress = (TextView) convertView.findViewById(R.id.txt_DeviceAddress);

                //set txt color
                vholder.txt_DeviceName.setTextColor(Color.parseColor("#000000"));
                vholder.txt_DeviceAddress.setTextColor(Color.parseColor("#000000"));

                //in order to reuse the view by tag, store the vholder within convertView by tag
                convertView.setTag(vholder);
            } else {
                //reuse the view by getting tag
                vholder = (ViewHolder) convertView.getTag();
            }

            vholder.txt_DeviceName.setText("這是第幾個" );
            vholder.txt_DeviceAddress.setText("這是holder" + convertView.getTag().toString());

            return convertView;
        }
    }

    class ViewHolder {
        TextView txt_DeviceName, txt_DeviceAddress;
    }

}
