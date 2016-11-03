package konikov.anton.carremotecontrol;

import android.Manifest;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class BluetoothActivity extends ListActivity {
    public final static String UUID = "e91521df-92b9-47bf-96d5-c52ee838f6f6";
    public final static String TAG = BluetoothActivity.class.getName();

    private BluetoothAdapter bluetoothAdapter;
    private BroadcastReceiver discoverDevicesReceiver;

    private final List<BluetoothDevice> discoveredDevices = new ArrayList<>();

    private ArrayAdapter<BluetoothDevice> listAdapter;

    private ProgressDialog progressDialog;

    private BluetoothDevice device;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN}, 1001);

        setContentView(R.layout.bluetooth_layout);
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Your device don't support Bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                bluetoothAdapter.enable();
            }
        }

        for (BluetoothDevice device : discoveredDevices) {
            Log.d(TAG, "Device " + device.getName());
        }
        listAdapter = new ArrayAdapter<BluetoothDevice>(getBaseContext(), android.R.layout.simple_list_item_1, discoveredDevices) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                final BluetoothDevice device = getItem(position);
                ((TextView) view.findViewById(android.R.id.text1)).setText(device.getName());
                return view;
            }
        };
        setListAdapter(listAdapter);
    }

    public void makeDiscoverable(View view) {
        Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(i);
    }

    public void discoverDevices(View view) {
        discoveredDevices.clear();
        listAdapter.notifyDataSetChanged();
        Log.d(TAG + ":DiscoveredDevice", "Run");
        if (discoverDevicesReceiver == null) {
            discoverDevicesReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    String action = intent.getAction();
                    switch (action) {
                        case BluetoothDevice.ACTION_FOUND:
                            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                            if (!discoveredDevices.contains(device)) {
                                discoveredDevices.add(device);
                                listAdapter.notifyDataSetChanged();
                                Log.d(TAG + ":Add bluetooth", "Device name: " + device.getName());
                            }
                            break;
                        case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                            if (progressDialog != null)
                                progressDialog.dismiss();
                            Toast.makeText(getBaseContext(), "Поиск закончен. Выберите устройство для отправки ообщения.", Toast.LENGTH_LONG).show();
                            getListView().setEnabled(true);
                            unregisterReceiver(discoverDevicesReceiver);
                            break;
                        default:
                            Log.d(TAG + ":BluetoothActivity", "action " + action);
                            break;
                    }
                }
            };
        }

        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothDevice.ACTION_NAME_CHANGED);

        registerReceiver(discoverDevicesReceiver, filter);

        getListView().setEnabled(false);

        progressDialog = ProgressDialog.show(this, "Поиск устройств", "Подождите...");
        bluetoothAdapter.startDiscovery();
    }

    @Override
    public void onPause() {
        super.onPause();
        bluetoothAdapter.cancelDiscovery();

        if (discoverDevicesReceiver != null) {
            try {
                unregisterReceiver(discoverDevicesReceiver);
            } catch (Exception e) {
                Log.e(TAG, "Не удалось отключить ресивер " + discoverDevicesReceiver);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        discoveredDevices.clear();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                discoveredDevices.add(device);
                Log.d(TAG, "Added paired device: " + device.getName() + " device address: " + device.getAddress());
            }
        }
        listAdapter.notifyDataSetChanged();
    }

    public void onListItemClick(ListView parent, View v, int position, long id) {
        device = discoveredDevices.get(position);
        Toast.makeText(this, "Вы подключились к устройству \"" + discoveredDevices.get(position).getName() + "\"", Toast.LENGTH_SHORT).show();
    }

    public void saveAndExit(View view) {
        if (device != null) {
            Intent intent = new Intent();
            intent.putExtra("BluetoothDevice", device);
            setResult(RESULT_OK, intent);
            finish();
        } else {
            Toast.makeText(this, R.string.please_select, Toast.LENGTH_SHORT).show();
        }
    }
}
