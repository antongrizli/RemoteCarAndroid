package konikov.anton.carremotecontrol.service;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class BackgroundBluetoothConnection extends Service {
    private static final String TAG = BackgroundBluetoothConnection.class.getName();
    private BluetoothDevice device;
    private BluetoothSocket socket;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        device = intent.getParcelableExtra("BluetoothDevice");
        Method m = null;
        try {
            m = device.getClass().getMethod("createRfcommSocket", new Class[]{int.class});
            socket = (BluetoothSocket) m.invoke(device, Integer.valueOf(1));
            Connection connection = new Connection(socket);
            new Thread(connection).start();

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

        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new ReturnDataBind();
    }

    private class Connection implements Runnable {
        BluetoothSocket socket;

        Connection(BluetoothSocket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            Intent connectionIntent = new Intent("konikov.anton.carremotecontrol");
            try {
                socket.connect();
                connectionIntent.putExtra(BroadcastType.CONNECTION_STATUS.name(), "You connection to: " + socket.getRemoteDevice().getName());
            } catch (IOException e) {
                connectionIntent.putExtra(BroadcastType.CONNECTION_STATUS.name(), "You don't connection!");
                e.printStackTrace();
                Log.e(TAG, e.getLocalizedMessage());
            }
            sendBroadcast(connectionIntent);
        }
    }

    public class ReturnDataBind extends Binder {

        public boolean connect() {
            if (!socket.isConnected()) {
                new Thread(new Connection(socket)).start();
            }
            return socket.isConnected();
        }
    }
}
