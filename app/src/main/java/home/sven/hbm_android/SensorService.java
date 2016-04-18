package home.sven.hbm_android;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
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
    private SensorManager mSensorManager;
    private android.hardware.Sensor mLight;
    private int lux;

    private int CHECK_LUX_RATE = 1000;
    private final int CHANGE_HBM_LOCK_MILLIS = 5000;
    private final float multiplier = 0.75f;

    private boolean hbm_lock = false;
    private boolean isHbmEnabled = false;
    private long currentTime;
    private long lastTime = 0;
    private boolean isHbmAutoMode = true;
    private int lux_border = 1000;

    @Override
    public final void onAccuracyChanged(android.hardware.Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
        lux = (int)event.values[0];
        // Do something with this sensor data.
    }

    public int getLux() {
        return lux;
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
        Log.v("DEMO","##### Service - onBind() #####");

        if(!Shell.SU.available()) {
            Shell.SU.run("");

            while(!Shell.SU.available()) try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            postToastOnMainThread("HBM: No root access");
        }

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);

        lux = 0;
        postToastOnMainThread("HBM-Service running");

        new LuxThread().start();

        return addBinder;
    }

    public void setLuxBorder(int lux_border) {
        Log.v("HBM SERVICE","setLuxBorder: "+lux_border);
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

    private void automaticHbm() {
        boolean changed = false;
        boolean toSet = false;

        if(lux >= lux_border) {
            changed = true;
            toSet = true;
        }
        else if(lux < lux_border*multiplier) {
            changed = true;
            toSet = false;
        }

        if(changed) {
            if(toSet != isHbmEnabled) {
                setHbm(toSet);
                lastTime = System.currentTimeMillis();
                if(toSet) hbm_lock = true;  //only lock if screen lights up
                isHbmEnabled = toSet;
            }
        }
    }

    private class LuxThread extends Thread {
        public void run() {
            while(true) {
                try {
                    sleep(CHECK_LUX_RATE);
                    Log.v("HBM SERVICE","HBM Luxthread automode : "+isHbmAutoMode);
                    currentTime = System.currentTimeMillis();

                    if(isHbmAutoMode) {
                        Log.v("HBM SERVICE","HBM Lock: "+hbm_lock);

                        if(hbm_lock) {
                            if(currentTime-lastTime > CHANGE_HBM_LOCK_MILLIS) hbm_lock = false;
                        }

                        if(!hbm_lock) automaticHbm();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        }
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

