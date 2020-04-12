package com.example.linus.bluetooth;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresPermission;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    static final int REQUEST_LOCATION_PERMISSION = 0;
    static final int REQUEST_ENABLE_BT = 1;
    static final int REQUEST_DISCOVERABLE_BT = 2;
    static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    Spinner spinner;
    Button enbluetooth, scanbluetooth, connectbluetooth, visible, exit;


    ArrayList<String> DevList = new ArrayList<String>();
    ArrayAdapter<String> DevAdapter;


    BluetoothAdapter BTAdapter;
    BluetoothSocket BTSocket;


    String device_address;
    String device_name;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this,             // 確任位置權限
                Manifest.permission.ACCESS_COARSE_LOCATION);
        if(permissionCheck != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION);
        }


        spinner = (Spinner)findViewById(R.id.DEVList);
        enbluetooth = (Button) findViewById(R.id.ENbluetooth);
        scanbluetooth = (Button)findViewById(R.id.ScanBluetooth);
        connectbluetooth = (Button)findViewById(R.id.ConnectBluetooth);
        visible = (Button)findViewById(R.id.Visible);
        exit = (Button)findViewById(R.id.Exit);


        DevAdapter = new ArrayAdapter<String>(MainActivity.this, android.R.layout.simple_spinner_item, DevList);
        DevAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(DevAdapter);
        spinner.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                device_address = (String)DevList.get(position).split("\\|")[1];
                device_name = (String)DevList.get(position).split("\\|")[0];
                Toast.makeText(MainActivity.this,
                        "選擇裝置: "+ device_name, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                Toast.makeText(MainActivity.this, "Didn't select any device", Toast.LENGTH_LONG).show();
            }
        });


        BTAdapter = BluetoothAdapter.getDefaultAdapter();                                             // 設置藍牙
        if(BTAdapter == null){                                                                      // .getDefaultAdapter() return null, 裝置不支援藍牙
            Toast.makeText(this,
                    "Device doesn't support Bluetooth", Toast.LENGTH_LONG).show();
        }
        IntentFilter intentFilter = new IntentFilter();                                            // 設定選擇監聽的項目
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);                                  // 藍牙找到設備
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(searchDevices, intentFilter);                                              // 監聽項目發生時，執行searchDevices function


        enbluetooth.setOnClickListener(new View.OnClickListener() {                                // 開啟/關閉藍牙
            @Override
            public void onClick(View view) {
                enable_bt();
            }
        });


        scanbluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(BTAdapter.isEnabled()){
                    BTAdapter.cancelDiscovery();
                    BTAdapter.startDiscovery();                                                    // 開始偵測周圍藍牙裝置約12秒，發現裝置後發出broadcast，使用BroadcastReceiver 處理廣播內容
                }
                else{
                    Toast.makeText(MainActivity.this,
                            "Please Enable Bluetooth", Toast.LENGTH_LONG).show();
                }
            }
        });


        visible.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
                startActivityForResult(discoverableIntent, REQUEST_DISCOVERABLE_BT);
            }
        });


        connectbluetooth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(!BTAdapter.isEnabled()){
                    Toast.makeText(MainActivity.this,
                            "Please enable bluetooth", Toast.LENGTH_LONG).show();
                }
                else{
                    if(device_address != null){
                        try {
                            BTSocket = BTAdapter.getRemoteDevice(device_address).createInsecureRfcommSocketToServiceRecord(uuid);
                            BTAdapter.cancelDiscovery();
                            BTSocket.connect();
                            Log.e("BtSPP", "Socket connect");
                            connect();
                            Toast.makeText(MainActivity.this, "Connection successed!!   Device:"+ device_name, Toast.LENGTH_SHORT).show();
                        } catch (IOException e) {
                            Log.e("BtSPP", "Socket connect failed", e);
                            try {
                                BTSocket.close();
                                Log.e("BtSPP", "Socket close socket", e);
                            } catch (IOException e1) {
                                Log.e("BtSPP", "Socket cannot close", e);
                            }
                            Toast.makeText(MainActivity.this, "Connection failed", Toast.LENGTH_SHORT).show();
                        }
                    }
                    else{
                        Toast.makeText(MainActivity.this,
                                "Please Select the Device", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });


        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                pauseThread();
                disconnect();
                onDestory();

            }
        });
    }


    private void disconnect(){
        spinner.setClickable(true);
    }


    private void connect(){
        try {
            InputStream BTIn = BTSocket.getInputStream();
            OutputStream BTOut = BTSocket.getOutputStream();
            Log.e("BtSPP", "get stream");

            Thread readThread = new ReadThread(BTIn, BTOut);
            readThread.start();
            spinner.setClickable(false);

        } catch (IOException e) {
            Log.e("BtSPP", "failed to get stream", e);
        }
    }


    private class ReadThread extends Thread{

        private byte[] messageBuffer;
        private InputStream mmInStream;
        private OutputStream mmOutStream;
        mHandler handler = new mHandler();

        public ReadThread(InputStream BTIn, OutputStream BTOut){
            if(BTIn == null){
                Log.d("BtSPP", "InputStream null");
                return;
            }
            mmInStream = BTIn;
            mmOutStream = BTOut;
        }
        @Override
        public void run(){
            super.run();
            messageBuffer = new byte[1024];
            int numBytes;                                                                           // bytes returned from read()
            while(true){                                                                            // Keep listening to the inputstream
                try {
                    numBytes = mmInStream.read(messageBuffer);
                    handler.obtainMessage(
                            0, numBytes, -1, messageBuffer).sendToTarget();
                } catch (IOException e) {
                    Log.d("BtSPP", "Input stream was disconnectd", e);
                    break;
                }
            }
        }

        public void write(byte[] bytes){                                                            // used to call in the main activity
            try {
                String newinput = null;
                mmOutStream.write(newinput.getBytes());
            } catch (IOException e) {
                Log.e("BtSpp", "Error occurred when sending data", e);
            }
        }

        public void cancel(){
            try {
                BTSocket.close();
            } catch (IOException e) {
                Log.e("BtSPP", "Could not close the connect socket", e);
            }
        }
    }

    public class mHandler extends Handler{
        @Override
        public void handleMessage(Message msg){
            super.handleMessage(msg);
//            String stirng = String.valueOf(msg.obj);                                                                                                                             // read message
//            Integer integer = Integer.valueOf(msg.what);
        }
    }

    public void pauseThread(){
        Log.d("BtSpp", "pauseThread");
        ReadThread.currentThread().interrupt();
        ReadThread.currentThread().equals(null);

    }


    public void startThread(){
        Log.d("BtSpp", "start Thread");
        ReadThread.currentThread().currentThread();
    }


    private BroadcastReceiver searchDevices = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            BluetoothDevice device = null;

            if(BluetoothDevice.ACTION_FOUND.equals(action)){
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String name_address = device.getName() + "|" + device.getAddress();
                if(DevList.indexOf(name_address) == -1){                                            // 確認裝置是否已被記錄過
                    DevList.add(name_address);
                }
                DevAdapter.notifyDataSetChanged();                                                 // 裝置的List發生改變
            }
            else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
                device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                switch (device.getBondState()){
                    case BluetoothDevice.BOND_BONDING:
                        Toast.makeText(MainActivity.this,
                                "pairing.....", Toast.LENGTH_LONG).show();
                        break;
                    case BluetoothDevice.BOND_BONDED:
                        Toast.makeText(MainActivity.this,
                                "paired.....", Toast.LENGTH_LONG).show();
                        break;
                    case BluetoothDevice.BOND_NONE:
                        Toast.makeText(MainActivity.this,
                                "unpair.....", Toast.LENGTH_LONG).show();
                        break;
                }
            }
        }
    };


    private void enable_bt(){
        if(!BTAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        else{
            BTAdapter.disable();
            Toast.makeText(MainActivity.this,
                    "Bluetooth Off", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        switch (requestCode){
            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK){
                    Toast.makeText(MainActivity.this,
                            "Bluetooth ON", Toast.LENGTH_LONG).show();
                }
                else if (resultCode == RESULT_CANCELED){
                    Toast.makeText(MainActivity.this,
                            "Deny to turn Bluetooth ON", Toast.LENGTH_LONG).show();
                }
                break;
            case REQUEST_DISCOVERABLE_BT:
                if (resultCode == RESULT_CANCELED){
                    Toast.makeText(MainActivity.this,
                            "Deny to make visible", Toast.LENGTH_LONG).show();
                }
                break;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == REQUEST_LOCATION_PERMISSION){
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED){                          // 拒絕給予權限將會無法使用藍牙scan功能，直接退出程式
                Toast.makeText(this, "Please agree to the request ",Toast.LENGTH_SHORT).show();
                android.os.Process.killProcess(android.os.Process.myPid());
            }
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        ReadThread.currentThread().interrupt();
    }


    protected void onDestory(){
        super.onDestroy();
        this.unregisterReceiver(searchDevices);
        try {
            BTSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
