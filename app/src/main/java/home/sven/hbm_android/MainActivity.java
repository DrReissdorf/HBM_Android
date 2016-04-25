package home.sven.hbm_android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {
    private final Context context = this;
    private SensorService myService;  // Service, der aufgerufen werden soll
    private SensorService.SensorServiceBinder myBinder;  // Binder des Service
    private ConnectionToSensorService myConn;  // Überwacher der Verbindung zum Service

    private TextView luxTextView;
    private Switch automaticHbmSwitch;
    private Button button_hbm_on;
    private Button button_hbm_off;

    private SharedPreferences prefs;

    private UpdateThread updateThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(SharedPrefStrings.SHARED_PREFS_KEY,MODE_PRIVATE);

        initSettings(prefs);

        luxTextView = (TextView) findViewById(R.id.luxTextView);
        button_hbm_on = (Button)findViewById(R.id.button_hbm_on);
        button_hbm_off = (Button)findViewById(R.id.button_hbm_off);

        if(prefs.getBoolean(SharedPrefStrings.AUTOMATIC_HBM_STRING,false)) {
            button_hbm_on.setEnabled(false);
            button_hbm_off.setEnabled(false);
            button_hbm_on.setVisibility(View.INVISIBLE);
            button_hbm_off.setVisibility(View.INVISIBLE);
        }

        automaticHbmSwitch = (Switch) findViewById(R.id.automaticHbmSwitch);
        automaticHbmSwitch.setChecked(prefs.getBoolean(SharedPrefStrings.AUTOMATIC_HBM_STRING,false));
        automaticHbmSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                    button_hbm_on.setEnabled(false);
                    button_hbm_off.setEnabled(false);
                    button_hbm_on.setVisibility(View.INVISIBLE);
                    button_hbm_off.setVisibility(View.INVISIBLE);
                }
                else {
                    button_hbm_on.setEnabled(true);
                    button_hbm_off.setEnabled(true);
                    button_hbm_on.setVisibility(View.VISIBLE);
                    button_hbm_off.setVisibility(View.VISIBLE);
                }
                Log.v("HBM SERVICE","HBM-Auto-Mode: "+isChecked);
                prefs.edit().putBoolean(SharedPrefStrings.AUTOMATIC_HBM_STRING,isChecked).commit();
            }
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

    private void initSettings(SharedPreferences prefs) {
        if(!prefs.contains(SharedPrefStrings.LUX_ACTIVATION_LIMIT_STRING)) {
            prefs.edit().putInt(SharedPrefStrings.LUX_ACTIVATION_LIMIT_STRING,SharedPrefDefaults.DEFAULT_ACTIVATION_LIMIT).commit();
        }
        if(!prefs.contains(SharedPrefStrings.LUX_DEACTIVATION_LIMIT_STRING)) {
            prefs.edit().putInt(SharedPrefStrings.LUX_DEACTIVATION_LIMIT_STRING,SharedPrefDefaults.DEFAULT_DEACTIVATION_LIMIT).commit();
        }
        if(!prefs.contains(SharedPrefStrings.AUTOMATIC_HBM_STRING)) {
            prefs.edit().putBoolean(SharedPrefStrings.AUTOMATIC_HBM_STRING,false).commit();
        }
        if(!prefs.contains(SharedPrefStrings.AVERAGE_LUX_FULL_SLEEP_ACTIVATION_STRING)) {
            prefs.edit().putInt(SharedPrefStrings.AVERAGE_LUX_FULL_SLEEP_ACTIVATION_STRING,SharedPrefDefaults.DEFAULT_ACTIVATION_FULLSLEEP).commit();
        }
        if(!prefs.contains(SharedPrefStrings.AVERAGE_LUX_FULL_SLEEP_DEACTIVATION_STRING)) {
            prefs.edit().putInt(SharedPrefStrings.AVERAGE_LUX_FULL_SLEEP_DEACTIVATION_STRING,SharedPrefDefaults.DEFAULT_DEACTIVATION_FULLSLEEP).commit();
        }
        if(!prefs.contains(SharedPrefStrings.AVERAGE_LUX_VALUES_STRING)) {
            prefs.edit().putInt(SharedPrefStrings.AVERAGE_LUX_VALUES_STRING,SharedPrefDefaults.DEFAULT_AVERAGE_VALUES).commit();
        }
        if(!prefs.contains(SharedPrefStrings.SERVICE_AUTO_BOOT)) {
            prefs.edit().putBoolean(SharedPrefStrings.SERVICE_AUTO_BOOT,false).commit();
        }
        if(!prefs.contains(SharedPrefStrings.SCREEN_ACTIVATED)) {
            prefs.edit().putBoolean(SharedPrefStrings.SCREEN_ACTIVATED,true).commit();
        }

    }

    private void connectService() {
        myConn = new ConnectionToSensorService();
        Intent serviceIntent = new Intent(context, SensorService.class);
        context.startService(serviceIntent);
        bindService(serviceIntent, myConn, Context.BIND_AUTO_CREATE); //calls onServiceConnected in ConnectionToSensorService-Class
    }

    public void buttonClickListener(View v) {
        switch(v.getId()) {
            case R.id.button_hbm_on:
                Log.v("HBM","onButtonOnClickListener()");
                try {
                    myService.setHbm(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case R.id.button_hbm_off:
                Log.v("HBM","offButtonOnClickListener()");
                try {
                    myService.setHbm(false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case R.id.button_settings:
                Intent myIntent = new Intent(this,AutomaticSettingsActivity.class);
                myIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(myIntent);
                break;
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
        luxTextView.setText(text);
    }

    private class UpdateThread extends Thread {
        private boolean exit = false;

        public void run() {
            while(myService == null) {
                sleep(250);
            }

            while(!exit) {
                sleep(250);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLuxText(getString(R.string.lux_sensor_string)+" "+myService.getLux());
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
