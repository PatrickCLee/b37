package cool.very.brad37;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    private ListView listView;
    private SimpleAdapter adapter;
    private LinkedList<HashMap<String,String>> data = new LinkedList<>(); //資料結構可直接new,與介面無關
    private String[] from = {"name","mac"};
    private int[] to = {R.id.item_name, R.id.item_mac};
    private LinkedList<BluetoothDevice> devices = new LinkedList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    123);

        } else {
            init();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        init();
    }

    private BluetoothAdapter bluetoothAdapter;

    private void init(){
        listView = findViewById(R.id.listDevices);
        adapter = new SimpleAdapter(this, data, R.layout.item, from, to);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                connectDevice(position);
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(!bluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, 321);
        }else{
            regReceiver();
        }
    }

    private void connectDevice(int i){
        new ConnectThread(devices.get(i)).start();
    }

    private void regReceiver(){
        myReceiver = new MyReceiver();
        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(myReceiver, filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 321 && resultCode == RESULT_OK){
            regReceiver();
        }
    }

    public void at1(View view) {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();

        if (pairedDevices.size() > 0) {
            // There are paired devices. Get the name and address of each paired device.
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
                Log.v("brad",deviceName + ":" + deviceHardwareAddress); //已配對的裝置
            }
        }
    }

    // start Scan device
    public void at2(View view) {
        if(!bluetoothAdapter.isDiscovering()){  //如果沒有在搜尋
            data.clear();
            devices.clear();
            bluetoothAdapter.startDiscovery();  //開始搜尋
        }

    }

    //stop scan
    public void at3(View view) {
        if(bluetoothAdapter.isDiscovering()){   //如果在搜尋
            bluetoothAdapter.cancelDiscovery(); //停止搜尋
        }
    }

    private MyReceiver myReceiver;

    private class MyReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            String deviceName = device.getName();
            String deviceHardwareAddress = device.getAddress(); // MAC address
            Log.v("brad","scan: " + deviceName +":"+deviceHardwareAddress);

            if(!devices.contains(device)){                      //掃到重複的才不會在listview呈現
                HashMap<String,String> row = new HashMap<>();
                row.put(from[0], deviceName);
                row.put(from[1], deviceHardwareAddress);

                data.add(row);
                devices.add(device);

                adapter.notifyDataSetChanged();
            }

        }
    }

    private UUID uuid = UUID.fromString("");

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket
            // because mmSocket is final.
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                // Get a BluetoothSocket to connect with the given BluetoothDevice.
                // MY_UUID is the app's UUID string, also used in the server code.
                tmp = device.createRfcommSocketToServiceRecord(uuid);       //tmp就是socket (從要連線的裝置上拿到socket)
            } catch (IOException e) {
//                Log.v("brad", "Socket's create() method failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it otherwise slows down the connection.
            bluetoothAdapter.cancelDiscovery();

            try {
                // Connect to the remote device through the socket. This call blocks
                // until it succeeds or throws an exception.
                mmSocket.connect();

                InputStream in = mmSocket.getInputStream();
                in.close();

                OutputStream out = mmSocket.getOutputStream();
                out.flush();
                out.close();

            } catch (IOException connectException) {
                // Unable to connect; close the socket and return.
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
//                    Log.v("brad", "Could not close the client socket", closeException);
                }
                return;
            }

        }

        // Closes the client socket and causes the thread to finish.
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
               // Log.e(TAG, "Could not close the client socket", e);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(myReceiver != null){
            unregisterReceiver(myReceiver);
        }
    }

}
