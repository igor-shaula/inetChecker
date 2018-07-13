package com.igor_shaula.inetchecker.main.inet_polling;

public interface PollingResultsConsumer {

    boolean isConnectivityReadySyncCheck();

    void onTogglePollingState(boolean isPollingActive);

    void onFirstResultReceived(boolean isInetAvailable);

    void onInetStateChanged(boolean isAvailable);
}