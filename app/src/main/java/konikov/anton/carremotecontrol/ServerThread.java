package konikov.anton.carremotecontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.util.Log;

import java.io.IOException;
import java.util.UUID;

public class ServerThread extends Thread {
    private final BluetoothServerSocket bluetoothServerSocket;
    private final CommunicatorService communicatorService;

    public ServerThread(CommunicatorService communicatorService) {
        this.communicatorService = communicatorService;
        final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothServerSocket tmp = null;
        try {
            tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord("BluetoothApp", UUID.fromString(BluetoothActivity.UUID));
        } catch (IOException e) {
            Log.d("ServerThread", e.getLocalizedMessage());
        }
        bluetoothServerSocket = tmp;
    }

    public void run() {

        BluetoothSocket socket = null;

        Log.d("ServerThread", "Started");

        while (true) {
            try {
                socket = bluetoothServerSocket.accept();
            } catch (IOException e) {
                Log.d("ServerThread", "Stop: " + e.getLocalizedMessage());
                break;
            }
            if (socket != null) {
                communicatorService.createCommunicatorThread(socket).startCommunication();
            }
        }
    }

    public void cancel() {
        try {
            bluetoothServerSocket.close();
        } catch (IOException e) {
            Log.d("ServerThread", e.getLocalizedMessage());
        }
    }
}
