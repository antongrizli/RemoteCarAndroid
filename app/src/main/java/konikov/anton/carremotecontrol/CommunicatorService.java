package konikov.anton.carremotecontrol;

import android.bluetooth.BluetoothSocket;

import konikov.anton.carremotecontrol.Communicator;

public interface CommunicatorService {
    Communicator createCommunicatorThread(BluetoothSocket socket);
}
