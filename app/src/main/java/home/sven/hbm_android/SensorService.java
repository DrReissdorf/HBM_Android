package home.sven.hbm_android;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;

import eu.chainfire.libsuperuser.Shell;
import home.sven.hbm_android.broadcast.BootBroadcastReceiver;

public class SensorService extends Service implements SensorEventListener {
    /* BROADCAST RECEIVER */
    private final BroadcastReceiver screenBroadcastReceiver = new ScreenBroadcastReceiver();

    /* SENSOR VARIABLE */
    private float lux; // light sensor will store its value in this variable

    /* SHARED PREFERENCES */
    private SharedPreferences prefs;
    private SharedPreferences.OnSharedPreferenceChangeListener myPrefListener;

    /* HBM VARIABLES */
    private Process runtimeProcess;
    private OutputStream runtimeProcessOutputStream;

    private int luxActivationLimit;
    private int luxDeactivationLimit; // user-definable variable. when lux drops under this value, hbm will be deactivated
    private boolean automaticHbmModeEnabled;
    private boolean isScreenOn = true;
    private boolean isHbmOn = false;

    @Override
    public void onCreate() {
        Log.v("DEMO","##### Service - onCreate() #####");

        /************************** SENSOR LISTENER ************************************/
        SensorManager mSensorManager;
        android.hardware.Sensor mLight;
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);
        /********************************************************************************/

        /******************************* BROADCAST INIT *********************************/
        final IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(screenBroadcastReceiver, filter);
        /********************************************************************************/

        try {
            runtimeProcess = Runtime.getRuntime().exec("su");
            runtimeProcessOutputStream = runtimeProcess.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        prefs = getSharedPreferences(SharedPrefs.SHARED_PREFS_KEY,MODE_PRIVATE);
        initSettings(prefs);

        luxActivationLimit = prefs.getInt(SharedPrefs.LUX_ACTIVATION_LIMIT_STRING, SharedPrefs.DEFAULT_ACTIVATION_LIMIT);
        luxDeactivationLimit = prefs.getInt(SharedPrefs.LUX_DEACTIVATION_LIMIT_STRING, SharedPrefs.DEFAULT_DEACTIVATION_LIMIT); // user-definable variable. when lux drops under this value, hbm will be deactivated
        automaticHbmModeEnabled = prefs.getBoolean(SharedPrefs.AUTOMATIC_HBM_STRING, false);

        /* LISTENER FOR SETTINGS */
        myPrefListener = new SharedPreferences.OnSharedPreferenceChangeListener(){
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                switch(key) {
                    case SharedPrefs.LUX_ACTIVATION_LIMIT_STRING:
                        luxActivationLimit = prefs.getInt(SharedPrefs.LUX_ACTIVATION_LIMIT_STRING, SharedPrefs.DEFAULT_ACTIVATION_LIMIT);
                        break;

                    case SharedPrefs.LUX_DEACTIVATION_LIMIT_STRING:
                        luxDeactivationLimit = prefs.getInt(SharedPrefs.LUX_DEACTIVATION_LIMIT_STRING, SharedPrefs.DEFAULT_DEACTIVATION_LIMIT);
                        break;

                    case SharedPrefs.AUTOMATIC_HBM_STRING:
                        automaticHbmModeEnabled = prefs.getBoolean(SharedPrefs.AUTOMATIC_HBM_STRING, false);
                        setHbm(false);
                        break;
                }
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(myPrefListener);

        setHbm(false); // start app with hbm false
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v("HBM","##### Service - onUnbind() #####");
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("HBM","##### Service - onStartCommand() #####");
        postToastOnMainThread(getString(R.string.service_running_toast_text));
        return START_STICKY; // We want this service to continue running until it is explicitly stopped, so return sticky.
    }

    private void postToastOnMainThread(final String message) {
        Handler h = new Handler(this.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setHbm(boolean toEnable) {
        Log.v("HBM SERVICE","setHbm(): "+toEnable);

        if (toEnable) {
            isHbmOn = true;
            executeShellCmd("echo 1 > /sys/devices/virtual/graphics/fb0/hbm");
        } else {
            isHbmOn = false;
            executeShellCmd("echo 0 > /sys/devices/virtual/graphics/fb0/hbm");
        }
    }

    private void executeShellCmd(String cmd) {
        try {
            runtimeProcessOutputStream.write(((cmd + "\n").getBytes()));
            runtimeProcessOutputStream.flush();
        } catch (Exception e) {

        }
    }

    @Override
    public final void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {}

    @Override
    public final void onSensorChanged(SensorEvent event) {
        lux = event.values[0];

        if(isScreenOn && automaticHbmModeEnabled) {
            if(lux > luxActivationLimit && !isHbmOn) {
                setHbm(true);
            } else {
                if(lux <= luxDeactivationLimit && isHbmOn) {
                    setHbm(false);
                }
            }
        }
    }

    private void initSettings(SharedPreferences prefs) {
        if(!prefs.contains(SharedPrefs.LUX_ACTIVATION_LIMIT_STRING)) {
            prefs.edit().putInt(SharedPrefs.LUX_ACTIVATION_LIMIT_STRING,SharedPrefs.DEFAULT_ACTIVATION_LIMIT).commit();
        }
        if(!prefs.contains(SharedPrefs.LUX_DEACTIVATION_LIMIT_STRING)) {
            prefs.edit().putInt(SharedPrefs.LUX_DEACTIVATION_LIMIT_STRING,SharedPrefs.DEFAULT_DEACTIVATION_LIMIT).commit();
        }
        if(!prefs.contains(SharedPrefs.AUTOMATIC_HBM_STRING)) {
            prefs.edit().putBoolean(SharedPrefs.AUTOMATIC_HBM_STRING,false).commit();
        }
        if(!prefs.contains(SharedPrefs.SERVICE_AUTO_BOOT)) {
            prefs.edit().putBoolean(SharedPrefs.SERVICE_AUTO_BOOT,false).commit();
        }
        if(!prefs.contains(SharedPrefs.SCREEN_ACTIVATED)) {
            prefs.edit().putBoolean(SharedPrefs.SCREEN_ACTIVATED,true).commit();
        }
    }

    /**************************** SERVICE BINDING *********************************/
    // Binder: Hierüber kann eine andere Komponente, die im selben Prozess läuft,
    // auf den Service zugreifen
    private final IBinder addBinder = new SensorServiceBinder();

    public class SensorServiceBinder extends Binder {
        // getService() liefert eine Referenz auf den Service,
        // über die dann dessen Dienstmethode(n) aufgerufen werden kann/können.
        SensorService getService() {
            Log.v("DEMO","##### Service Binder - getService() #####");
            return SensorService.this;
        }
    }

    // onBind() liefert den Binder des Service zurück,
    // wird als Reaktion auf den bindService()-Aufruf des Service-Nutzers ausgeführt
    public IBinder onBind(Intent intent) {
        Log.v("HBM-Service","##### Service - onBind() #####");
        return addBinder;
    }
    /*******************************************************************************/

    private class ScreenBroadcastReceiver extends BootBroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                setHbm(false);
                isScreenOn = false;
                Log.v("Hbm","screen deactivated");
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                isScreenOn = true;
                Log.v("Hbm","screen activated");
            }
        }
    }
}

