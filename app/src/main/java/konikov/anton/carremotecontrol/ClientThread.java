package konikov.anton.carremotecontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public class ClientThread extends Thread {
    private final static String TAG = ClientThread.class.getName();
    private volatile Communicator communicator;

    private BluetoothSocket socket;
    private BluetoothAdapter bluetoothAdapter;
    private final CommunicatorService communicatorService;
    BluetoothDevice device;

    public ClientThread(BluetoothDevice device, CommunicatorService communicatorService) {
        this.device = device;
        this.communicatorService = communicatorService;

        BluetoothSocket tmp = null;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        try {
            //tmp = device.createRfcommSocketToServiceRecord(UUID.fromString("0000110E-0000-1000-8000-00805F9B34FB"));

            Method m = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
            tmp = (BluetoothSocket) m.invoke(device, 1);

            //tmp = device.createInsecureRfcommSocketToServiceRecord(UUID.fromString("0000110E-0000-1000-8000-00805F9B34FB"));
            Log.d(TAG, "Initialize connect to " + tmp.getRemoteDevice().getName());
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            Log.e(TAG, e.getLocalizedMessage());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
            Log.e(TAG, e.getLocalizedMessage());
        } catch (InvocationTargetException e) {
            e.printStackTrace();
            Log.e(TAG, e.getLocalizedMessage());
        }
        socket = tmp;
    }

    public synchronized Communicator getCommunicator() {
        return communicator;
    }

    public void run() {
        if (bluetoothAdapter.isDiscovering())
            bluetoothAdapter.cancelDiscovery();
        try {
            Log.d(TAG, "Before connected. Socket: " + socket.getRemoteDevice().getName());
            socket.connect();
            Log.d(TAG, "Is connected: " + socket.isConnected());
            synchronized (this) {
                communicator = communicatorService.createCommunicatorThread(socket);
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Start communication");
                    communicator.startCommunication();
                }
            }).start();
        } catch (IOException connectException) {
            try {
                socket.close();
            } catch (IOException closeException) {
                Log.d("ClientThread", closeException.getLocalizedMessage());
            }
        }
    }

    public void cancel() {
        if (communicator != null) {
            communicator.stopCommunication();
            Log.d(TAG, "Stop communication");
        }
    }
}
