package com.igor_shaula.inet_checker.main;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.igor_shaula.inet_polling.InetPollingLogic;
import com.igor_shaula.inet_polling.PollingResultsConsumer;

import utils.L;

public class MainActivity extends AppCompatActivity
        implements PollingResultsConsumer {

    private static final String CN = "MainActivity";

    @Nullable
    private TextView tvStatus;

    @Nullable
    InetPollingLogic inetPollingLogic;

    // LIFECYCLE ===================================================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvStatus = findViewById(R.id.tvStatus);

        final FloatingActionButton fab = findViewById(R.id.fab);
        fab.setActivated(true);
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

        inetPollingLogic = InetPollingLogic.getInstance(this);
    }

    @Override
    protected void onStop() {
        if (inetPollingLogic != null) {
            inetPollingLogic.clearCurrentPollingSetting();
            inetPollingLogic = null;
            L.i(CN , "onStop ` nulled inetPollingLogic");
        }
        super.onStop();
    }

    private void togglePolling(boolean launchFlag) {
        if (inetPollingLogic != null) {
            inetPollingLogic.toggleInetCheck(launchFlag);
        }
        if (tvStatus == null) return;
        if (inetPollingLogic != null && inetPollingLogic.isPollingActive()) {
            tvStatus.setText(R.string.pollingStatusEnabled);
        } else {
            tvStatus.setText(R.string.pollingStatusDisabled);
        }
    }

    // MENU ========================================================================================

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

    // IMPLEMENTATIONS =============================================================================

    @Override
    public int whichLogic() {
        return 1; // 1 for single & 3 for multiple - at the moment
    }

    @Override
    public boolean isConnectivityReadySyncCheck() {
        return true;
    }

    @Override
    public void onTogglePollingState(boolean isPollingActive) {
        L.d(CN , "onTogglePollingState: isPollingActive = " + isPollingActive);
    }

    @Override
    public void onFirstResultReceived(boolean isInetAvailable) {
        L.d(CN , "onFirstResultReceived: isInetAvailable = " + isInetAvailable);
    }

    @Override
    public void onInetStateChanged(boolean isAvailable) {
        L.d(CN , "onInetStateChanged: isAvailable = " + isAvailable);
    }

    // ALL PRIVATE =================================================================================

}