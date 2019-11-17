package com.igor_shaula.inet_polling;

public interface PollingResultsConsumer {

    int whichLogic();

    boolean isConnectivityReadySyncCheck();

    void onTogglePollingState(boolean isPollingActive);

    void onFirstResultReceived(boolean isInetAvailable);

    void onInetStateChanged(boolean isAvailable);
}