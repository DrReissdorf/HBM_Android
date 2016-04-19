package home.sven.hbm_android;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
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

public class SensorService extends Service implements SensorEventListener {
    /* SENSOR VARIABLE */
    private float lux;

    /* SHARED PREFERENCES */
    private SharedPreferences prefs;

    /* HBM VARIABLES */
    private final int NO_AUTO_HBM_SLEEP = 2000;
    private final int NUMBER_OF_LUX_VALUES = 10;
    private final int SLEEP_BETWEEN_LUX_VALUES = 200;
    private boolean isHbmEnabled = false;
    private boolean isHbmAutoMode;
    private int luxActivationLimit;
    private int luxDeactivationLimit;
    private int luxAverageActivateCalculatedLimit;
    private int luxAverageDeactivateCalculatedLimit;


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

        loadPrefs();

        new LuxThread().start();
    }

    private void loadPrefs() {
        prefs = getSharedPreferences(SharedPrefStrings.SHARED_PREFS_KEY,MODE_PRIVATE);

        luxActivationLimit = prefs.getInt(SharedPrefStrings.LUX_ACTIVATION_LIMIT_STRING,2000);
        luxAverageActivateCalculatedLimit = luxActivationLimit * NUMBER_OF_LUX_VALUES;

        luxDeactivationLimit = prefs.getInt(SharedPrefStrings.LUX_DEACTIVATION_LIMIT_STRING,1500);
        luxAverageDeactivateCalculatedLimit = luxDeactivationLimit * NUMBER_OF_LUX_VALUES;

        isHbmAutoMode = prefs.getBoolean(SharedPrefStrings.AUTOMATIC_HBM_STRING,false);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v("HBM","##### Service - onUnbind() #####");
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("HBM","##### Service - onStartCommand() #####");
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    public int getLux() {
        return (int)lux;
    }

    public void setLuxActivationLimit(int luxActivationLimit) {
        Log.v("HBM SERVICE","setLuxActivationLimit: "+luxActivationLimit);
        prefs.edit().putInt(SharedPrefStrings.LUX_ACTIVATION_LIMIT_STRING,luxActivationLimit).commit();
        this.luxActivationLimit = luxActivationLimit;
        luxAverageActivateCalculatedLimit = luxActivationLimit * NUMBER_OF_LUX_VALUES;
    }

    public void setLuxDeactivationLimit(int luxDeactivationLimit) {
        Log.v("HBM SERVICE","setLuxDeactivationLimit: "+luxDeactivationLimit);
        prefs.edit().putInt(SharedPrefStrings.LUX_DEACTIVATION_LIMIT_STRING,luxDeactivationLimit).commit();
        this.luxDeactivationLimit = luxDeactivationLimit;
        luxAverageDeactivateCalculatedLimit = luxDeactivationLimit * NUMBER_OF_LUX_VALUES;
    }

    public void setHbmAutoMode(boolean toSet) {
        Log.v("HBM SERVICE","setHbmAutoMode(): "+toSet);
        prefs.edit().putBoolean(SharedPrefStrings.AUTOMATIC_HBM_STRING,toSet).commit();
        isHbmAutoMode = toSet;
    }

    public void setHbm(boolean toEnable) {
        Log.v("HBM SERVICE","setHbm(): "+toEnable);
        if(toEnable) Shell.SU.run("echo 1 > /sys/devices/virtual/graphics/fb0/hbm");
        else Shell.SU.run("echo 0 > /sys/devices/virtual/graphics/fb0/hbm");
    }

    private class LuxThread extends Thread {
        public void run() {
            if(!Shell.SU.available()) {
                Shell.SU.run("");

                while(!Shell.SU.available()) try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                postToastOnMainThread("HBM: No root access");
            }

            lux = 0;
            postToastOnMainThread("HBM-Service running");

            while(true) {
                try {
                    if(isHbmAutoMode) {
                        int luxAdd = 0;

                        if(!isHbmEnabled) {
                            /****** AVERGE LUX OVER TIME CALCULATION ******/
                            for(int i=0 ; i<NUMBER_OF_LUX_VALUES ; i++) {
                                sleep(SLEEP_BETWEEN_LUX_VALUES);
                                luxAdd += lux;
                            }

                            /**********************************************/
                            if(luxAdd >= luxAverageActivateCalculatedLimit) {
                                isHbmEnabled = true;
                                setHbm(true);
                            }
                        } else {
                            /****** AVERGE LUX OVER TIME CALCULATION ******/
                            for(int i=0 ; i<NUMBER_OF_LUX_VALUES ; i++) {
                                sleep(SLEEP_BETWEEN_LUX_VALUES);
                                luxAdd += lux;
                            }
                            /**********************************************/

                            if(luxAdd < luxAverageDeactivateCalculatedLimit) {
                                Log.v("HBM SERVICE","Diabling HBM! Average Lux-Value: "+luxAdd+". Disable-border: "+luxDeactivationLimit);
                                setHbm(false);
                                isHbmEnabled = false;
                            } else {
                                Log.v("HBM SERVICE","Keeping HBM enabled! Average Lux-Value: "+luxAdd+". Disable-border: "+luxDeactivationLimit);
                                setHbm(true);
                            }
                        }
                    } else {
                        sleep(NO_AUTO_HBM_SLEEP);
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

