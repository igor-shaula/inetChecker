package com.igor_shaula.inetchecker.main;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.igor_shaula.inetchecker.main.inet_polling.InetPollingLogic;
import com.igor_shaula.inetchecker.main.inet_polling.PollingResultsConsumer;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    @Nullable
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvStatus = findViewById(R.id.tvStatus);

        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (fab.isActivated()) {
                    togglePolling(true);
                    fab.setImageResource(android.R.drawable.ic_media_pause);
                    fab.setActivated(false);
                } else {
                    togglePolling(false);
                    fab.setImageResource(android.R.drawable.ic_media_play);
                    fab.setActivated(true);
                }
            }
        });
    }

    private void togglePolling(boolean launchFlag) {
        PollingResultsConsumer pollingResultsConsumer = new PollingResultsConsumer() {

            @Override
            public boolean isConnectivityReadySyncCheck() {
                return true;
            }

            @Override
            public void onTogglePollingState(boolean isPollingActive) {
                Log.d(TAG , "onTogglePollingState: isPollingActive = " + isPollingActive);
            }

            @Override
            public void onFirstResultReceived(boolean isInetAvailable) {
                Log.d(TAG , "onFirstResultReceived: isInetAvailable = " + isInetAvailable);
            }

            @Override
            public void onInetStateChanged(boolean isAvailable) {
                Log.d(TAG , "onInetStateChanged: isAvailable = " + isAvailable);
            }
        };
        final InetPollingLogic inetPollingLogic = InetPollingLogic.getInstance(pollingResultsConsumer);
        inetPollingLogic.toggleInetCheckNow(launchFlag);
        if (inetPollingLogic.isPollingActive()) {
            tvStatus.setText("polling is active");
        } else {
            tvStatus.setText("polling is disabled");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main , menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}