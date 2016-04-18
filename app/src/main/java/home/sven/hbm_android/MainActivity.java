package home.sven.hbm_android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private final Context context = this;
    private SensorService myService;  // Service, der aufgerufen werden soll
    private SensorService.SensorServiceBinder myBinder;  // Binder des Service
    private ConnectionToSensorService myConn;  // Überwacher der Verbindung zum Service

    private TextView textView;
    private Switch automaticHbmSwitch;
    private EditText luxBorderEditText;
    private Button onButton;
    private Button offButton;

    private UpdateThread updateThread;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(SharedPrefStrings.SHARED_PREFS_KEY,MODE_PRIVATE);

        textView = (TextView) findViewById(R.id.textView);

        onButton = (Button)findViewById(R.id.onButton);
        offButton = (Button)findViewById(R.id.offButton);

        automaticHbmSwitch = (Switch) findViewById(R.id.automaticHbmSwitch);
        automaticHbmSwitch.setChecked(prefs.getBoolean(SharedPrefStrings.AUTOMATIC_HBM_STRING,false));
        automaticHbmSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                setAutomaticHbm(isChecked);
            }
        });

        luxBorderEditText = (EditText) findViewById(R.id.luxBorderEditText);
        luxBorderEditText.setText(prefs.getInt(SharedPrefStrings.LUX_BORDER_STRING,2000)+"");
        luxBorderEditText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                String temp = luxBorderEditText.getText().toString();
                int toSet;

                if(temp.equals("")) {
                    toSet = 999999;
                } else {
                    toSet = Integer.valueOf(temp);
                }
                myService.setLuxBorder(toSet);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    public void onResume() {
        super.onResume();
        Log.v("HBM","onResume()");
        connectService();
        updateThread = new UpdateThread();
        updateThread.start();
    }

    public void onPause() {
        Log.v("HBM","onPause()");
        super.onPause();
        updateThread.stopThread();

        try {
            context.unbindService(myConn);
        } catch (IllegalArgumentException e){
            Log.v("HBM","onPause() couldnt unbind");
        }
    }

    public void onDestroy() {
        Log.v("HBM","onDestroy()");
        super.onDestroy();
    }

    private void setAutomaticHbm(boolean value) {
        onButton.setEnabled(!value);
        offButton.setEnabled(!value);

        if(value) {
            connectService();
            myService.setHbm(false);
            onButton.setEnabled(false);
            offButton.setEnabled(false);
        } else {
            myService.setHbm(false);
            try {
                context.unbindService(myConn);
            } catch (IllegalArgumentException e){
                Log.v("HBM","SwitchListener() couldnt unbind");
            }
        }

        myService.setHbmAutoMode(value);
    }

    private void connectService() {
        myConn = new ConnectionToSensorService();
        Intent serviceIntent = new Intent(context, SensorService.class);
        context.startService(serviceIntent);
        bindService(serviceIntent, myConn, Context.BIND_AUTO_CREATE); //calls onServiceConnected in ConnectionToSensorService-Class
    }

    public void onButtonOnClickListener(View v) {
        Log.v("HBM","onButtonOnClickListener()");
        try {
            myService.setHbm(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void offButtonOnClickListener(View v) {
        Log.v("HBM","offButtonOnClickListener()");
        try {
            myService.setHbm(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ConnectionToSensorService implements ServiceConnection {
        public ConnectionToSensorService() {
            Log.v("HBM","ConnectionToSensorService: starting #####");
        }

        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.v("HBM","##### Activity - onServiceConnected(): starting #####");

            myBinder = (SensorService.SensorServiceBinder) binder;
            myService = myBinder.getService();
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.v("HBM","##### Activity - onServiceDisconnected() #####");
        }
    }

    private void setLuxText(String text) {
        textView.setText(text);
    }

    private class UpdateThread extends Thread {
        private boolean exit = false;

        public void run() {
            while(myService == null) {
                sleep(250);
            }

            myService.setHbm(false);

            while(!exit) {
                sleep(250);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLuxText("Current Lux: "+myService.getLux());
                    }
                });
            }
        }

        private void stopThread() {
            exit = true;
        }

        private void sleep(int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
