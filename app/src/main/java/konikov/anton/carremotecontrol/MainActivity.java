package konikov.anton.carremotecontrol;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import konikov.anton.carremotecontrol.service.BackgroundBluetoothConnection;
import konikov.anton.carremotecontrol.service.BroadcastType;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private class SendMessageTask extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... args) {
            try {
                clientThread.getCommunicator().write(args[0]);
            } catch (Exception e) {
                Log.e(TAG, e.getClass().getSimpleName() + " " + e.getLocalizedMessage());
            }
            return null;
        }
    }

    BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getStringExtra(BroadcastType.CONNECTION_STATUS.name()) != null) {
                String status = intent.getStringExtra(BroadcastType.CONNECTION_STATUS.name());
                Toast.makeText(getApplicationContext(), status, Toast.LENGTH_SHORT).show();
            }
        }
    };

    public static final String TAG = MainActivity.class.getName();
    private static final int REQUEST_BT = 1;
    private ImageButton leftBtn;
    private ImageButton rightBtn;
    private ImageButton upBtn;
    private ImageButton downBtn;
    private Button stopBtn;
    private TextView logTv;

    private View.OnTouchListener touchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    Log.d(TAG, "Your action: " + event);
                    pressedToButton(v);
                    break;
                case MotionEvent.ACTION_UP:
                    Log.d(TAG, "Your action: " + event);
                    unPressedButton(v);

                    break;
                default:
                    Log.d(TAG, "Your action: " + event);
                    break;

            }
            return false;
        }
    };

    private void pressedToButton(View view) {
        switch (view.getId()) {
            case R.id.leftBtn:
                if (clientThread != null)
                    new SendMessageTask().execute(String.valueOf(Action.MOVE_LEFT.getId()));
                Log.d(TAG, "onClick id:" + view.getId() + " Action: " + Action.MOVE_LEFT + " Id:" + Action.MOVE_LEFT.getId());
                break;
            case R.id.rightBtn:
                if (clientThread != null)
                    new SendMessageTask().execute(String.valueOf(Action.MOVE_RIGHT.getId()));
                Log.d(TAG, "onClick id:" + view.getId() + " Action: " + Action.MOVE_RIGHT + " Id:" + Action.MOVE_RIGHT.getId());
                break;
            case R.id.upBtn:
                if (clientThread != null)
                    new SendMessageTask().execute(String.valueOf(Action.MOVE_UP.getId()));
                Log.d(TAG, "onClick id:" + view.getId() + " Action: " + Action.MOVE_UP + " Id:" + Action.MOVE_UP.getId());
                break;
            case R.id.downBtn:
                if (clientThread != null)
                    new SendMessageTask().execute(String.valueOf(Action.MOVE_DOWN.getId()));
                Log.d(TAG, "onClick id:" + view.getId() + " Action: " + Action.MOVE_DOWN + " Id:" + Action.MOVE_DOWN.getId());
                break;
            case R.id.stopBtn:
                if (clientThread != null)
                    new SendMessageTask().execute(String.valueOf(Action.MOVE_STOP.getId()));
                Log.d(TAG, "onClick id:" + view.getId() + " Action: " + Action.MOVE_STOP + " Id:" + Action.MOVE_STOP.getId());
                break;
            default:
                Log.d(TAG, "onClick id:" + view.getId());
                break;
        }
    }

    private void unPressedButton(View view) {
        switch (view.getId()) {
            case R.id.leftBtn:
            case R.id.rightBtn:
                if (clientThread != null)
                    new SendMessageTask().execute(String.valueOf(Action.RESET_RUDDER.getId()));
                Log.d(TAG, "onClick id:" + view.getId() + " Action: " + Action.RESET_RUDDER);
                break;
            case R.id.upBtn:
            case R.id.downBtn:
                if (clientThread != null)
                    new SendMessageTask().execute(String.valueOf(Action.RESET_MOTION.getId()));
                Log.d(TAG, "onClick id:" + view.getId() + " Action: " + Action.RESET_MOTION);
                break;
        }
    }

    private BluetoothDevice selectedDevice;
    private BluetoothAdapter bluetoothAdapter;
    private ClientThread clientThread;

    private Intent serviceIntent;
    private ServiceConnection serviceConnection;

    private final CommunicatorService communicatorService = new CommunicatorService() {
        @Override
        public Communicator createCommunicatorThread(BluetoothSocket socket) {
            return new CommunicatorImpl(socket, new CommunicatorImpl.CommunicationListener() {
                @Override
                public void onMessage(final String message) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            logTv.setText("Log: " + message);
                            Log.i(TAG, "Input message: " + message);
                        }
                    });
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        leftBtn = (ImageButton) findViewById(R.id.leftBtn);
        leftBtn.setOnClickListener(this);
        leftBtn.setOnTouchListener(touchListener);
        rightBtn = (ImageButton) findViewById(R.id.rightBtn);
        rightBtn.setOnClickListener(this);
        rightBtn.setOnTouchListener(touchListener);
        upBtn = (ImageButton) findViewById(R.id.upBtn);
        upBtn.setOnClickListener(this);
        downBtn = (ImageButton) findViewById(R.id.downBtn);
        downBtn.setOnClickListener(this);
        stopBtn = (Button) findViewById(R.id.stopBtn);
        stopBtn.setOnClickListener(this);
        logTv = (TextView) findViewById(R.id.Log);

        IntentFilter intentFilter = new IntentFilter("konikov.anton.carremotecontrol");
        registerReceiver(receiver, intentFilter);

        serviceIntent = new Intent(this, BackgroundBluetoothConnection.class);
        startService(serviceIntent);

        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                ((BackgroundBluetoothConnection.ReturnDataBind) service).connect();
                Log.d(TAG, "onServiceConnected");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d(TAG, "onServiceDisconnected");
            }
        };

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bindService(serviceIntent, serviceConnection, 0);
        if (bluetoothAdapter.isEnabled()) {
            leftBtn.setEnabled(clientThread != null);
            rightBtn.setEnabled(clientThread != null);
            upBtn.setEnabled(clientThread != null);
            downBtn.setEnabled(clientThread != null);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.bt_settings:
                Log.d(TAG, "Run Bluetooth Settings");
                switchToActivity(BluetoothActivity.class, REQUEST_BT);
                break;
            case R.id.exit:
                Log.d(TAG, "Exit from application");
                exitFromApp();
                break;
            default:
                Log.d(TAG, "Item " + item.getItemId());
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void exitFromApp() {
        stopService(serviceIntent);
        this.finish();
        System.exit(0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "requestCode = " + requestCode + ", resultCode = " + resultCode);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_BT:
                    selectedDevice = data.getParcelableExtra("BluetoothDevice");
                    clientThread = new ClientThread(selectedDevice, communicatorService);
                    clientThread.start();
                    Log.d(TAG, "Device name: " + selectedDevice.getName());
                    break;
                default:
                    Log.d(TAG, "Request code: " + requestCode);
                    break;
            }
        }
    }

    private void switchToActivity(Class aClass, int requestCode) {
        Intent intent = new Intent(this, aClass);
        startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (clientThread != null) clientThread.cancel();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.leftBtn:
                if (clientThread != null)
                    new SendMessageTask().execute(String.valueOf(Action.MOVE_LEFT.getId()));
                Log.d(TAG, "onClick id:" + v.getId() + " Action: " + Action.MOVE_LEFT + " Id:" + Action.MOVE_LEFT.getId());
                break;
            case R.id.rightBtn:
                if (clientThread != null)
                    new SendMessageTask().execute(String.valueOf(Action.MOVE_RIGHT.getId()));
                Log.d(TAG, "onClick id:" + v.getId() + " Action: " + Action.MOVE_RIGHT + " Id:" + Action.MOVE_RIGHT.getId());
                break;
            case R.id.upBtn:
                if (clientThread != null)
                    new SendMessageTask().execute(String.valueOf(Action.MOVE_UP.getId()));
                Log.d(TAG, "onClick id:" + v.getId() + " Action: " + Action.MOVE_UP + " Id:" + Action.MOVE_UP.getId());
                break;
            case R.id.downBtn:
                if (clientThread != null)
                    new SendMessageTask().execute(String.valueOf(Action.MOVE_DOWN.getId()));
                Log.d(TAG, "onClick id:" + v.getId() + " Action: " + Action.MOVE_DOWN + " Id:" + Action.MOVE_DOWN.getId());
                break;
            case R.id.stopBtn:
                if (clientThread != null)
                    new SendMessageTask().execute(String.valueOf(Action.MOVE_STOP.getId()));
                Log.d(TAG, "onClick id:" + v.getId() + " Action: " + Action.MOVE_STOP + " Id:" + Action.MOVE_STOP.getId());
                break;
            default:
                Log.d(TAG, "onClick id:" + v.getId());
                break;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(receiver);
        unbindService(serviceConnection);
    }
}