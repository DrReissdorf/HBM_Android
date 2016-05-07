package home.sven.hbm_android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import eu.chainfire.libsuperuser.Shell;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private final Context context = this;
    private final int UPDATE_LIGHT_VALUE_SLEEP = 500;

    private TextView luxTextView;
    private Switch automaticHbmSwitch;
    private Button button_hbm_on;
    private Button button_hbm_off;

    private SharedPreferences prefs;

    private int lux;

    private UpdateThread updateThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences(SharedPrefs.SHARED_PREFS_KEY,MODE_PRIVATE);

        /************************** SENSOR LISTENER ************************************/
        SensorManager mSensorManager;
        android.hardware.Sensor mLight;
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
        /********************************************************************************/

        luxTextView = (TextView) findViewById(R.id.luxTextView);
        button_hbm_on = (Button)findViewById(R.id.button_hbm_on);
        button_hbm_off = (Button)findViewById(R.id.button_hbm_off);

        if(prefs.getBoolean(SharedPrefs.AUTOMATIC_HBM_STRING,false)) {
            button_hbm_on.setEnabled(false);
            button_hbm_off.setEnabled(false);
            button_hbm_on.setVisibility(View.INVISIBLE);
            button_hbm_off.setVisibility(View.INVISIBLE);
        }

        automaticHbmSwitch = (Switch) findViewById(R.id.automaticHbmSwitch);
        automaticHbmSwitch.setChecked(prefs.getBoolean(SharedPrefs.AUTOMATIC_HBM_STRING,false));
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
                prefs.edit().putBoolean(SharedPrefs.AUTOMATIC_HBM_STRING,isChecked).commit();
            }
        });

        startSensorService();
    }

    public void onResume() {
        super.onResume();
        Log.v("HBM","onResume()");
        updateThread = new UpdateThread();
        updateThread.start();
    }

    public void onPause() {
        Log.v("HBM","onPause()");
        super.onPause();
        updateThread.stopThread();
    }

    public void onDestroy() {
        Log.v("HBM","onDestroy()");
        super.onDestroy();
    }

    private void setHbm(boolean toEnable) {
        Log.v("HBM SERVICE","setHbm(): "+toEnable);
        if(toEnable) {
            Shell.SU.run("echo 1 > /sys/devices/virtual/graphics/fb0/hbm");
        }
        else {
            Shell.SU.run("echo 0 > /sys/devices/virtual/graphics/fb0/hbm");
        }
    }

    private void startSensorService() {
        Intent serviceIntent = new Intent(context, SensorService.class);
        context.startService(serviceIntent);
    }

    public void buttonClickListener(View v) {
        switch(v.getId()) {
            case R.id.button_hbm_on:
                Log.v("HBM","onButtonOnClickListener()");
                try {
                    setHbm(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;

            case R.id.button_hbm_off:
                Log.v("HBM","offButtonOnClickListener()");
                try {
                    setHbm(false);
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

    @Override
    public void onSensorChanged(SensorEvent event) {
        lux = (int)event.values[0];
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void setLuxText(String text) {
        luxTextView.setText(text);
    }

    private class UpdateThread extends Thread {
        private boolean exit = false;

        public void run() {
            while(!exit) {
                sleep(UPDATE_LIGHT_VALUE_SLEEP);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLuxText(getString(R.string.lux_sensor_string)+" "+lux);
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
