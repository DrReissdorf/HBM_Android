package home.sven.hbm_android;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.Image;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public class AutomaticSettingsActivity extends AppCompatActivity {
    private final Context context = this;
    private EditText averageValuesEditText;
    private EditText averageLoopActivationSleepEditText;
    private EditText averageLoopDeactivationSleepEditText;
    private EditText luxActivationLimitEditText;
    private EditText luxDeactivationLimitEditText;
    private Switch autobootServiceSwitch;

    private ImageButton autoboot_imagebutton;
    private ImageButton hbm_on_imagebutton;
    private ImageButton hbm_off_imagebutton;
    private ImageButton values_imagebutton;
    private ImageButton activation_time_imagebutton;
    private ImageButton deactivation_time_imagebutton;

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_automatic_settings);

        prefs = getSharedPreferences(SharedPrefStrings.SHARED_PREFS_KEY, MODE_PRIVATE);

        initEditables();
        initInformation();
    }

    private void initInformation() {
        autoboot_imagebutton = (ImageButton)findViewById(R.id.autoboot_imagebutton);
        autoboot_imagebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Alerter.alertWithOkButton(context,"",getString(R.string.autoboot_setting_infotext));
            }
        });

        hbm_on_imagebutton = (ImageButton)findViewById(R.id.hbm_on_imagebutton);
        hbm_on_imagebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Alerter.alertWithOkButton(context,"",getString(R.string.hbm_on_setting_infotext));
            }
        });

        hbm_off_imagebutton = (ImageButton)findViewById(R.id.hbm_off_imagebutton);
        hbm_off_imagebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Alerter.alertWithOkButton(context,"",getString(R.string.hbm_off_setting_infotext));
            }
        });

        values_imagebutton = (ImageButton)findViewById(R.id.values_imagebutton);
        values_imagebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Alerter.alertWithOkButton(context,"",getString(R.string.values_setting_infotext));
            }
        });

        activation_time_imagebutton = (ImageButton)findViewById(R.id.activation_time_imagebutton);
        activation_time_imagebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Alerter.alertWithOkButton(context,"",getString(R.string.activation_time_setting_infotext));
            }
        });

        deactivation_time_imagebutton = (ImageButton)findViewById(R.id.deactivation_time_imagebutton);
        deactivation_time_imagebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Alerter.alertWithOkButton(context,"",getString(R.string.deactivation_time_setting_infotext));
            }
        });

    }

    private void initEditables() {
        autobootServiceSwitch = (Switch) findViewById(R.id.autobootServiceSwitch);
        autobootServiceSwitch.setChecked(prefs.getBoolean(SharedPrefStrings.SERVICE_AUTO_BOOT,false));
        autobootServiceSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Log.v("HBM SERVICE","HBM-Start-service on reboot: "+isChecked);
                if(isChecked) {
                    prefs.edit().putBoolean(SharedPrefStrings.SERVICE_AUTO_BOOT,true).commit();
                }
                else {
                    prefs.edit().putBoolean(SharedPrefStrings.SERVICE_AUTO_BOOT,false).commit();
                }
            }
        });

        luxActivationLimitEditText = (EditText) findViewById(R.id.luxActivationLimitEditText);
        luxActivationLimitEditText.setText(prefs.getInt(SharedPrefStrings.LUX_ACTIVATION_LIMIT_STRING,SharedPrefDefaults.DEFAULT_ACTIVATION_LIMIT)+"");
        luxActivationLimitEditText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                int actInt;

                if(luxActivationLimitEditText.getText().toString().equals("")) actInt = 999999;
                else  actInt = Integer.valueOf(luxActivationLimitEditText.getText().toString());

                Log.v("HBM","Settings: values for ACTIVATION-LIMIT changed:"+actInt);
                prefs.edit().putInt(SharedPrefStrings.LUX_ACTIVATION_LIMIT_STRING,actInt).commit();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        luxDeactivationLimitEditText = (EditText) findViewById(R.id.luxDeactivationLimitEditText);
        luxDeactivationLimitEditText.setText(prefs.getInt(SharedPrefStrings.LUX_DEACTIVATION_LIMIT_STRING,SharedPrefDefaults.DEFAULT_DEACTIVATION_LIMIT)+"");
        luxDeactivationLimitEditText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                int deactInt;

                if(luxDeactivationLimitEditText.getText().toString().equals("")) deactInt = 0;
                else  deactInt = Integer.valueOf(luxDeactivationLimitEditText.getText().toString());

                Log.v("HBM","Settings: values for DEACTIVATION-LIMIT changed:"+deactInt);
                prefs.edit().putInt(SharedPrefStrings.LUX_DEACTIVATION_LIMIT_STRING,deactInt).commit();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        averageValuesEditText = (EditText) findViewById(R.id.averageValuesEditText);
        averageValuesEditText.setText(prefs.getInt(SharedPrefStrings.AVERAGE_LUX_VALUES_STRING,SharedPrefDefaults.DEFAULT_AVERAGE_VALUES)+"");
        averageValuesEditText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                int values;

                if(averageValuesEditText.getText().toString().equals("")) values = 10;
                else values = Integer.valueOf(averageValuesEditText.getText().toString());

                Log.v("HBM","Settings: values for average changed:"+values);
                prefs.edit().putInt(SharedPrefStrings.AVERAGE_LUX_VALUES_STRING,values).commit();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        averageLoopActivationSleepEditText = (EditText) findViewById(R.id.averageLoopSleepActivationEditText);
        averageLoopActivationSleepEditText.setText(prefs.getInt(SharedPrefStrings.AVERAGE_LUX_FULL_SLEEP_ACTIVATION_STRING,SharedPrefDefaults.DEFAULT_ACTIVATION_FULLSLEEP)+"");
        averageLoopActivationSleepEditText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                int sleepTime;

                if(averageLoopActivationSleepEditText.getText().toString().equals("")) sleepTime = 2000;
                else sleepTime = Integer.valueOf(averageLoopActivationSleepEditText.getText().toString());

                Log.v("HBM","Settings: values for FULL-SLEEP-ACTIVATION changed:"+sleepTime);
                prefs.edit().putInt(SharedPrefStrings.AVERAGE_LUX_FULL_SLEEP_ACTIVATION_STRING,sleepTime).commit();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        averageLoopDeactivationSleepEditText = (EditText) findViewById(R.id.averageLoopSleepDeactivationEditText);
        averageLoopDeactivationSleepEditText.setText(prefs.getInt(SharedPrefStrings.AVERAGE_LUX_FULL_SLEEP_DEACTIVATION_STRING,SharedPrefDefaults.DEFAULT_DEACTIVATION_FULLSLEEP)+"");
        averageLoopDeactivationSleepEditText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                int sleepTime;

                if(averageLoopDeactivationSleepEditText.getText().toString().equals("")) sleepTime = 2000;
                else sleepTime = Integer.valueOf(averageLoopDeactivationSleepEditText.getText().toString());

                Log.v("HBM","Settings: values for FULL-SLEEP-DEACTIVATION changed:"+sleepTime);
                prefs.edit().putInt(SharedPrefStrings.AVERAGE_LUX_FULL_SLEEP_DEACTIVATION_STRING,sleepTime).commit();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }
}
