package com.igor_shaula.inet_polling;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.igor_shaula.inet_polling.polling_logic.InetPollingLogicV1single;
import utils.L;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public abstract class InetPollingLogic {

    private static final String CN = "InetPollingLogic";

    private static final String HOST_GOOGLE = "http://google.com";
    //    private static final long POLLING_DELAY = 10_000; // millis FOR DEBUG\\
    protected static final long POLLING_DELAY = 1000; // millis \\
    //    private static final long POLLING_TIMEOUT = 100; // millis FOR DEBUG \\
    protected static final long POLLING_TIMEOUT = 3000; // millis \\

    @NonNull
    protected final OkHttpClient okHttpClient = new OkHttpClient();
    @NonNull
    protected final Request googleRequest = new Request.Builder()
//            .addHeader(C.ACCEPT, C.APPLICATION_JSON)
            .url(HOST_GOOGLE) // this should be changed later \\
            .get()
            .build();
    @NonNull
    protected final Runnable pollingRunnable = new Runnable() {
        // main holder of payload to be done right after the new generation is started \\
        @Override
        public void run() {
            if (consumerLink != null && consumerLink.isConnectivityReadySyncCheck()) {
//                L.v(CN, "toggleInetCheckNow ` 1 second tick at " + System.currentTimeMillis());
                askHost();
            } else {
                // updating main flag for this case of connectivity absence \\
                updateFirstPollingReactionState(false); // pollingRunnable \\
                consumerLink.onInetStateChanged(false);
            }
        }
    };
    protected boolean isWaitingForFirstResultFromPolling = true;
    protected boolean isPollingAllowedInGeneral;
    protected long oneGenerationAbsTime;

    // link to invoking class back - to change main flag & check connectivity which requires Context \\
    @Nullable
    protected PollingResultsConsumer consumerLink;
    // abstraction for mechanism of scheduling delayed tasks which start every new generation of polling \\
    @SuppressWarnings("NullableProblems")
    // initialized in InetPollingLogicV1single constructor & used only there \\
    @NonNull
    protected DelayedSingleTaskEngine delayedSingleTaskEngine;

    @Nullable
    private static InetPollingLogic thisInstance;

    @NonNull
    public static InetPollingLogic getInstance(@NonNull PollingResultsConsumer pollingResultsConsumer) {
        if (thisInstance == null) {
            thisInstance = new InetPollingLogicV1single(pollingResultsConsumer);
        } else {
            thisInstance.consumerLink = pollingResultsConsumer;
            L.w(CN , "getInstance ` consumerLink updated with hash: " + pollingResultsConsumer.hashCode());
        }
        return thisInstance;
    }

    public void clearCurrentConsumerLink() {
        consumerLink = null;
    }

    public abstract boolean isPollingActive();

    // switch on or off - the only useful handling needed from outside \\
    public abstract void toggleInetCheckNow(boolean shouldLaunch);

    protected abstract void updateFirstPollingReactionState(boolean isInetAvailable);

    protected abstract void askHost();
}