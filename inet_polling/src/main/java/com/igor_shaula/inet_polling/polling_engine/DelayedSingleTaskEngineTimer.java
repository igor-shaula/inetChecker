package com.igor_shaula.inet_polling.polling_engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.igor_shaula.inet_polling.InetPollingLogic;
import utils.L;

import java.util.Timer;
import java.util.TimerTask;

final class DelayedSingleTaskEngineTimer {

    private static final String CN = "DelayedSingleTaskEngineTimer";
    @Nullable
    private Timer jobTimer;

    //    @Override
    void launchPollingEngineAfter(long delay , @NonNull final InetPollingLogic inetPollingLogic) {
        destroyPollingEngineNow();
        jobTimer = new Timer("TimerToDeleteShortestCar" , true);
        final TimerTask ttRemove = new TimerTask() {
            @Override
            public void run() {
//                if (inetPollingLogic.isConnectivityReadySyncCheck()) {
//                    L.v(CN, "toggleInetCheckNow ` 1 second tick at " + System.currentTimeMillis());
//                    inetPollingLogic.askHost(0); // Google
//                    inetPollingLogic.askHost(1); // Apple
//                    inetPollingLogic.askHost(2); // Amazon
//                } else {
//                    inetPollingLogic.onInetStateChanged(false);
//                }
            }
        };
        jobTimer.schedule(ttRemove , delay);
        L.d(CN , "runTimerToDeleteShortestCar ` jobTimer scheduled for " + delay);
    }

    //    @Override
    boolean isOneGenerationExecutorAlive() {
        return jobTimer == null;
    }

    //    @Override
    void destroyPollingEngineNow() {
        L.v(CN , "stopCurrentGeneration ` nothing to do here for handler");
        if (jobTimer != null) {
            jobTimer.cancel();
            jobTimer = null;
            L.d(CN , "stopTimerToDeleteShortestCar ` jobTimer cancelled and nulled");
        }
    }
}