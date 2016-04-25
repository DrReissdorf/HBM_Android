package home.sven.hbm_android;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import eu.chainfire.libsuperuser.Shell;
import home.sven.hbm_android.broadcast.ScreenBroadcastReceiver;

public class SensorService extends Service implements SensorEventListener {
    /* BROADCAST RECEIVER */
    private final BroadcastReceiver screenBroadcastReceiver = new ScreenBroadcastReceiver();

    /* SENSOR VARIABLE */
    private float lux; // light sensor will store its value in this variable

    /* SHARED PREFERENCES */
    private SharedPreferences prefs;

    /* HBM VARIABLES */
    private final int SCREEN_OFF_SLEEP = 500; //sleeptime when screen is off
    private final int NO_AUTO_HBM_SLEEP = 2000; // when isHbmAutoMode is false, LuxThread will wait for this long before checking if isHbmAutoMode is true
    private boolean isHbmEnabled = false; // when hbm mode is on, this is true

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

        prefs = getSharedPreferences(SharedPrefStrings.SHARED_PREFS_KEY,MODE_PRIVATE);

        /* init screenflag with true */
        prefs.edit().putBoolean(SharedPrefStrings.SCREEN_ACTIVATED,true);

        new LuxThread().start();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v("HBM","##### Service - onUnbind() #####");
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("HBM","##### Service - onStartCommand() #####");

        return START_STICKY; // We want this service to continue running until it is explicitly stopped, so return sticky.
    }

    public int getLux() {
        return (int)lux;
    }


    public void setHbm(boolean toEnable) {
        Log.v("HBM SERVICE","setHbm(): "+toEnable);
        if(toEnable) {
            Shell.SU.run("echo 1 > /sys/devices/virtual/graphics/fb0/hbm");
            isHbmEnabled = true;
        }
        else {
            Shell.SU.run("echo 0 > /sys/devices/virtual/graphics/fb0/hbm");
            isHbmEnabled = false;
        }
    }

    private class LuxThread extends Thread {
        public void run() {
            lux = 0;

            postToastOnMainThread(getString(R.string.service_running_toast_text));

            while(true) {
                try {
                    if(prefs.getBoolean(SharedPrefStrings.SCREEN_ACTIVATED,true)) {
                        if (prefs.getBoolean(SharedPrefStrings.AUTOMATIC_HBM_STRING, false)) { //is automode enabled?
                            int luxAdd = 0;
                            int numberOfLuxValues = prefs.getInt(SharedPrefStrings.AVERAGE_LUX_VALUES_STRING, SharedPrefDefaults.DEFAULT_AVERAGE_VALUES); // for-loop which calucaltes average lux will loop NUMBER_OF_LUX_VALUE times

                            if (!isHbmEnabled) {
                                int luxActivationLimit = prefs.getInt(SharedPrefStrings.LUX_ACTIVATION_LIMIT_STRING, SharedPrefDefaults.DEFAULT_ACTIVATION_LIMIT); //when lux reaches this value, hbm will be activated
                                int luxAverageActivateLimit = luxActivationLimit * numberOfLuxValues;
                                int fullSleepAverageActivateLuxLoop_ms = prefs.getInt(SharedPrefStrings.AVERAGE_LUX_FULL_SLEEP_ACTIVATION_STRING, SharedPrefDefaults.DEFAULT_ACTIVATION_FULLSLEEP);
                                int sleepBetweenLuxValuesActivation = fullSleepAverageActivateLuxLoop_ms / numberOfLuxValues; // sleep time after one cycle in for-loop which calculates average-lux value for activation

                        /* AVERGE LUX OVER TIME CALCULATION ACTIVATION */
                                for (int i = 0; i < numberOfLuxValues; i++) {
                                    sleep(sleepBetweenLuxValuesActivation);
                                    luxAdd += lux;
                                }

                                /***********************************************/
                                if (luxAdd >= luxAverageActivateLimit) {
                                    setHbm(true);
                                }
                            } else {
                                int luxDeactivationLimit = prefs.getInt(SharedPrefStrings.LUX_DEACTIVATION_LIMIT_STRING, SharedPrefDefaults.DEFAULT_DEACTIVATION_LIMIT); // user-definable variable. when lux drops under this value, hbm will be deactivated
                                int luxAverageDeactivateLimit = luxDeactivationLimit * numberOfLuxValues;
                                int fullSleepAverageDeactivateLuxLoop_ms = prefs.getInt(SharedPrefStrings.AVERAGE_LUX_FULL_SLEEP_DEACTIVATION_STRING, SharedPrefDefaults.DEFAULT_DEACTIVATION_FULLSLEEP);
                                int sleepBetweenLuxValuesDeactivation = fullSleepAverageDeactivateLuxLoop_ms / numberOfLuxValues; // sleep time after one cycle in for-loop which calculates average-lux value for deactivation

                        /* AVERGE LUX OVER TIME CALCULATION DEACTIVATION */
                                for (int i = 0; i < numberOfLuxValues; i++) {
                                    sleep(sleepBetweenLuxValuesDeactivation);
                                    luxAdd += lux;
                                }
                                /*************************************************/

                                if (luxAdd < luxAverageDeactivateLimit) {
                                    Log.v("HBM SERVICE", "Diabling HBM! Average Lux-Value: " + luxAdd + ". Disable-border: " + luxAverageDeactivateLimit);
                                    setHbm(false);
                                } else {
                                    Log.v("HBM SERVICE", "Keeping HBM enabled! Average Lux-Value: " + luxAdd + ". Disable-border: " + luxAverageDeactivateLimit);
                                    setHbm(true);
                                }
                            }
                        } else {
                            sleep(NO_AUTO_HBM_SLEEP);
                        }
                    } else {
                        sleep(SCREEN_OFF_SLEEP);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public final void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        lux = event.values[0];
        // Do something with this sensor data.
    }

    private void postToastOnMainThread(final String message) {
        Handler h = new Handler(this.getMainLooper());
        h.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),message,Toast.LENGTH_LONG).show();
            }
        });
    }

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
}

