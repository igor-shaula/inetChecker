package com.igor_shaula.inet_polling;

import androidx.annotation.NonNull;

public interface PollingResultsConsumer {

    int whichLogic();

    boolean isConnectivityReadySyncCheck();

    void onTogglePollingState(boolean isPollingActive);

    void onFirstResultReceived(boolean isInetAvailable);

    void onInetResult(@NonNull InetRequestResult isAvailable);
}