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
    /* SENSOR VARIABLES */
    private SensorManager mSensorManager;
    private android.hardware.Sensor mLight;
    private int lux;

    /* SHARED PREFERENCES */
    private SharedPreferences prefs;
    private final String SHARED_PREFS_KEY = "HBM_ANDROID";
    private final String LUX_BORDER_STRING = "lux_border";

    /* HBM VARIABLES */
    private int CHECK_LUX_RATE = 1000;
    private final float REDUCE_LUX_MULTIPLIER = 0.75f;
    private boolean isHbmEnabled = false;
    private boolean isHbmAutoMode = true;
    private int lux_border;
    private final int NUMBER_OF_LUX_VALUES = 50;
    private final int SLEEP_BETWEEN_LUX_VALUES = 100;
    private LuxThread luxThread;

    @Override
    public void onCreate() {
        Log.v("DEMO","##### Service - onCreate() #####");

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);

        prefs = getSharedPreferences(SHARED_PREFS_KEY,MODE_PRIVATE);
        lux_border = prefs.getInt(LUX_BORDER_STRING,2000);

        luxThread = new LuxThread();
        luxThread.start();
    }

    public void onDestroy() {
        Log.v("DEMO","##### Service - onDestroy() #####");
        if(luxThread != null) luxThread.exit();
    }

    public void stopService() {
        stopSelf();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.v("DEMO","##### Service - onUnbind() #####");
        return true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.v("DEMO","##### Service - onStartCommand() #####");
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
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

    public int getLux() {
        return lux;
    }

    public int getLuxBorder() {
        return lux_border;
    }

    public void setLuxBorder(int lux_border) {
        Log.v("HBM SERVICE","setLuxBorder: "+lux_border);
        prefs.edit().putInt(LUX_BORDER_STRING,lux_border).commit();
        this.lux_border = lux_border;
    }

    public void setHbmAutoMode(boolean toSet) {
        Log.v("HBM SERVICE","setHbmAutoMode(): "+toSet);
        isHbmAutoMode = toSet;
    }

    public void setHbm(boolean toEnable) {
        Log.v("HBM SERVICE","setHbm(): "+toEnable);
        if(toEnable) Shell.SU.run("echo 1 > /sys/devices/virtual/graphics/fb0/hbm");
        else Shell.SU.run("echo 0 > /sys/devices/virtual/graphics/fb0/hbm");
    }

    private class LuxThread extends Thread {
        private boolean exit = false;

        public void run() {
            if(!Shell.SU.available()) {
                Shell.SU.run("");

                while(!Shell.SU.available() && !exit) try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                postToastOnMainThread("HBM: No root access");
            }

            lux = 0;
            postToastOnMainThread("HBM-Service running");

            while(!exit) {
                try {
                    sleep(CHECK_LUX_RATE);
                    Log.v("HBM SERVICE","HBM Luxthread automode : "+isHbmAutoMode);

                    if(isHbmAutoMode) {
                        int luxAdd = 0;

                        if(!isHbmEnabled) {
                            if(lux >= lux_border) {
                                Log.v("HBM SERVICE","HBM enabled");
                                isHbmEnabled = true;
                                setHbm(true);
                            }
                        } else {
                            Log.v("HBM SERVICE","Checking lux values...");
                            for(int i=0 ; i<NUMBER_OF_LUX_VALUES ; i++) {
                                sleep(SLEEP_BETWEEN_LUX_VALUES);
                                luxAdd += lux;
                            }

                            int temp = luxAdd/NUMBER_OF_LUX_VALUES;
                            float multipliedBorder = lux_border*REDUCE_LUX_MULTIPLIER;
                            if(temp < multipliedBorder) {
                                Log.v("HBM SERVICE","Diabling HBM! LuxAdd/NUMBER_OF_LUX_VALUES: "+temp+" disable border: "+multipliedBorder);
                                setHbm(false);
                                isHbmEnabled = false;
                            } else {
                                Log.v("HBM SERVICE","Keeping HBM enabled! LuxAdd/NUMBER_OF_LUX_VALUES: "+temp+" disable border: "+multipliedBorder);
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void exit() {
            exit = true;
        }
    }

    @Override
    public final void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        lux = (int)event.values[0];
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
}

