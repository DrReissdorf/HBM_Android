package home.sven.hbm_android;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import eu.chainfire.libsuperuser.Shell;

public class MainActivity extends AppCompatActivity {
    private SensorService myService;  // Service, der aufgerufen werden soll
    private SensorService.SensorServiceBinder myBinder;  // Binder des Service
    private ConnectionToSensorService myConn;  // Überwacher der Verbindung zum Service

    private TextView textView;
    private Switch automaticHbmSwitch;
    private EditText luxBorderEditText;
    private Button onButton;
    private Button offButton;

    private UpdateThread updateThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Shell.SU.run("");

        textView = (TextView) findViewById(R.id.textView);

        onButton = (Button)findViewById(R.id.onButton);
        offButton = (Button)findViewById(R.id.offButton);

        automaticHbmSwitch = (Switch) findViewById(R.id.automaticHbmSwitch);
        automaticHbmSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (myService != null) myService.setHbmAutoMode(isChecked);
                if(isChecked) {
                    onButton.setEnabled(false);
                    offButton.setEnabled(false);
                } else {
                    onButton.setEnabled(true);
                    offButton.setEnabled(true);
                }

            }
        });

        luxBorderEditText = (EditText) findViewById(R.id.luxBorderEditText);
        luxBorderEditText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                // you can call or do what you want with your EditText here
                String temp = luxBorderEditText.getText().toString();
                int toSet;

                if(temp.equals("")) {
                    toSet = 999999;
                } else {
                    toSet = Integer.valueOf(temp);
                }
                myService.setLuxBorder(toSet);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
    }

    public void onResume() {
        super.onResume();

        Log.v("HBM","onStart()");

        updateThread = new UpdateThread();
        updateThread.start();
    }

    public void onPause() {
        super.onPause();
        updateThread.stopThread();
    }

    public void onDestroy() {
        super.onDestroy();
    }

    private void connectService() {
        myConn = new ConnectionToSensorService();
        Intent intent = new Intent(getApplicationContext(),SensorService.class);
        // Bindung zum Service herstellen (und dabei Service starten, falls nötig).
        // Der Parameter 'myConn' referenziert ein Objekt, das dann die hergestellte Verbindung überwacht
        // und Callback-Methoden bereitstellt, die bei Zustandsänderungen ausgeführt werden.
        // Als Folge des bindService()-Aufrufs wird die callback-Methode myConn.onServiceConnected() (siehe unten) ausgeführt.
        bindService(intent, myConn, Context.BIND_AUTO_CREATE);
    }

    public void onButtonOnClickListener(View v) {
        Log.v("HBM","onButtonOnClickListener()");
        try {
            myService.setHbm(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void offButtonOnClickListener(View v) {
        Log.v("HBM","offButtonOnClickListener()");
        try {
            myService.setHbm(false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private class ConnectionToSensorService implements ServiceConnection {
        public ConnectionToSensorService() {
            Log.v("HBM","ConnectionToSensorService: starting #####");
        }

        // onServiceConnected(): Callback-Methode - wird automatisch aufgerufen, wenn die Verbindung zum Service hergestellt ist.
        // Erhält ein IBinder-Objekt als Parameter, über das auf den Service mit seinem angebotenen Dienst zugegriffen werden kann.
        public void onServiceConnected(ComponentName className, IBinder binder) {
            Log.v("HBM","##### Activity - onServiceConnected(): starting #####");

            // über den Binder eine Referenz auf den Service beschaffen:
            myBinder = (SensorService.SensorServiceBinder) binder;
            myService = myBinder.getService();

            Log.v("HBM","##### Activity - onServiceConnected(): calling myService.add() #####");
        }

        public void onServiceDisconnected(ComponentName className) {

        }

    } // Ende von 'ConnectionToAddService'

    private void setLuxText(String text) {
        textView.setText(text);
    }

    private class UpdateThread extends Thread {
        private boolean exit = false;

        public void run() {
            while(myService == null) {
                Shell.SU.run("");
                if(!Shell.SU.available()) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"HBM: No root access",Toast.LENGTH_LONG).show();
                        }
                    });
                } else {
                    connectService();
                }
                sleep(100);
            }

            myService.setHbm(false);

            while(!exit) {
                sleep(1000);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        setLuxText("Current Lux: "+myService.getLux());
                    }
                });
            }
        }

        private void stopThread() {
            exit = true;
        }

        private void sleep(int millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
