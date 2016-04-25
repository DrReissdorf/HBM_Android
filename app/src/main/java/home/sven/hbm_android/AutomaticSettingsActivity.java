package home.sven.hbm_android;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public class AutomaticSettingsActivity extends AppCompatActivity {
    private EditText averageValuesEditText;
    private EditText averageLoopActivationSleepEditText;
    private EditText averageLoopDeactivationSleepEditText;
    private EditText luxActivationLimitEditText;
    private EditText luxDeactivationLimitEditText;
    private Switch autobootServiceSwitch;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_automatic_settings);

        prefs = getSharedPreferences(SharedPrefStrings.SHARED_PREFS_KEY,MODE_PRIVATE);

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
