package home.sven.hbm_android.broadcast;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import home.sven.hbm_android.SharedPrefStrings;

public class ScreenBroadcastReceiver extends BootBroadcastReceiver {
    private SharedPreferences prefs;

    @Override
    public void onReceive(Context context, Intent intent) {
        prefs = context.getSharedPreferences(SharedPrefStrings.SHARED_PREFS_KEY,Context.MODE_PRIVATE);

        if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
            prefs.edit().putBoolean(SharedPrefStrings.SCREEN_ACTIVATED,false).commit();
            Log.v("Hbm","screen deactivated");
        } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
            prefs.edit().putBoolean(SharedPrefStrings.SCREEN_ACTIVATED,true).commit();
            Log.v("Hbm","screen activated");
        }
    }
}
