package home.sven.hbm_android;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;

public class AutomaticSettingsActivity extends AppCompatActivity {
    private final Context context = this;
    private EditText luxActivationLimitEditText;
    private EditText luxDeactivationLimitEditText;
    private Switch autobootServiceSwitch;

    private ImageButton autoboot_imagebutton;
    private ImageButton hbm_on_imagebutton;
    private ImageButton hbm_off_imagebutton;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_automatic_settings);

        prefs = getSharedPreferences(SharedPrefs.SHARED_PREFS_KEY, MODE_PRIVATE);

        initEditables();
        initInformation();
    }

    private void initInformation() {
        autoboot_imagebutton = (ImageButton)findViewById(R.id.autoboot_imagebutton);
        autoboot_imagebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertWithOkButton(context,"",getString(R.string.autoboot_setting_infotext));
            }
        });

        hbm_on_imagebutton = (ImageButton)findViewById(R.id.hbm_on_imagebutton);
        hbm_on_imagebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertWithOkButton(context,"",getString(R.string.hbm_on_setting_infotext));
            }
        });

        hbm_off_imagebutton = (ImageButton)findViewById(R.id.hbm_off_imagebutton);
        hbm_off_imagebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alertWithOkButton(context,"",getString(R.string.hbm_off_setting_infotext));
            }
        });
    }

    private void initEditables() {
        autobootServiceSwitch = (Switch) findViewById(R.id.autobootServiceSwitch);
        autobootServiceSwitch.setChecked(prefs.getBoolean(SharedPrefs.SERVICE_AUTO_BOOT,false));
        autobootServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.v("HBM SERVICE","HBM-Start-service on reboot: "+isChecked);
                if(isChecked) {
                    prefs.edit().putBoolean(SharedPrefs.SERVICE_AUTO_BOOT,true).commit();
                }
                else {
                    prefs.edit().putBoolean(SharedPrefs.SERVICE_AUTO_BOOT,false).commit();
                }
            }
        });

        luxActivationLimitEditText = (EditText) findViewById(R.id.luxActivationLimitEditText);
        luxActivationLimitEditText.setText(prefs.getInt(SharedPrefs.LUX_ACTIVATION_LIMIT_STRING,SharedPrefs.DEFAULT_ACTIVATION_LIMIT)+"");
        luxActivationLimitEditText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                int actInt;

                if(luxActivationLimitEditText.getText().toString().equals("")) actInt = 999999;
                else  actInt = Integer.valueOf(luxActivationLimitEditText.getText().toString());

                Log.v("HBM","Settings: values for ACTIVATION-LIMIT changed:"+actInt);
                prefs.edit().putInt(SharedPrefs.LUX_ACTIVATION_LIMIT_STRING,actInt).commit();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        luxDeactivationLimitEditText = (EditText) findViewById(R.id.luxDeactivationLimitEditText);
        luxDeactivationLimitEditText.setText(prefs.getInt(SharedPrefs.LUX_DEACTIVATION_LIMIT_STRING,SharedPrefs.DEFAULT_DEACTIVATION_LIMIT)+"");
        luxDeactivationLimitEditText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                int deactInt;

                if(luxDeactivationLimitEditText.getText().toString().equals("")) deactInt = 0;
                else  deactInt = Integer.valueOf(luxDeactivationLimitEditText.getText().toString());

                Log.v("HBM","Settings: values for DEACTIVATION-LIMIT changed:"+deactInt);
                prefs.edit().putInt(SharedPrefs.LUX_DEACTIVATION_LIMIT_STRING,deactInt).commit();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    private void alertWithOkButton(Context context, String title, String message) {
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
