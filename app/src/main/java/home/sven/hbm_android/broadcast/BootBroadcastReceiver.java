package home.sven.hbm_android.broadcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import home.sven.hbm_android.SensorService;
import home.sven.hbm_android.SharedPrefStrings;

public class BootBroadcastReceiver extends BroadcastReceiver {
    final String ACTION = "android.intent.action.BOOT_COMPLETED";
    SharedPreferences prefs;

    @Override
    public void onReceive(Context context, Intent intent) {
        // BOOT_COMPLETED” start Service
        if (intent.getAction().equals(ACTION)) {
            prefs = context.getSharedPreferences(SharedPrefStrings.SHARED_PREFS_KEY, Context.MODE_PRIVATE);

            if(prefs.getBoolean(SharedPrefStrings.SERVICE_AUTO_BOOT,false)) {
                //Service
                Intent serviceIntent = new Intent(context, SensorService.class);
                context.startService(serviceIntent);
            }
        }
    }
}
