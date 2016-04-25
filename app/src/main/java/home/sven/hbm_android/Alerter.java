package home.sven.hbm_android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

public class Alerter {
    public static void alertWithOkButton(Context context, String title, String message) {
        new AlertDialog.Builder(context)
                .setTitle(title)
                .setMessage(message)
                .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }
}
