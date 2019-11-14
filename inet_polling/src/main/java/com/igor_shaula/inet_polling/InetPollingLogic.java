package com.igor_shaula.inet_polling;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.igor_shaula.inet_polling.polling_engine.DelayedSingleTaskEngineExecutor;
import com.igor_shaula.inet_polling.polling_logic.InetPollingLogicV1single;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import utils.L;

public abstract class InetPollingLogic {

    private static final String CN = "InetPollingLogic";

    // ALL FIELDS ----------------------------------------------------------------------------------

    protected boolean isWaitingForFirstResultFromPolling = true;
    protected boolean isPollingAllowed = false; // has to be enabled explicitly
    protected long oneGenerationAbsTime; // reset every time in askHost() implementation

    @Nullable
    private static InetPollingLogic thisInstance;

    // link to invoking class back - to change main flag & check connectivity which requires Context
    @Nullable
    protected PollingResultsConsumer consumerLink;
    // abstraction for mechanism of scheduling delayed tasks which start every new generation of polling

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

    @NonNull
    protected final Runnable pollingRunnable = new Runnable() {
        // main holder of payload to be done right after the new generation is started
        @Override
        public void run() {
            if (consumerLink != null && consumerLink.isConnectivityReadySyncCheck()) {
//                L.v(CN, "toggleInetCheck ` 1 second tick at " + System.currentTimeMillis());
                askHost();
            } else {
                // updating main flag for this case of connectivity absence
                updateFirstPollingReactionState(false); // pollingRunnable
                consumerLink.onInetStateChanged(false);
            }
        }
    };

    @NonNull
    protected DelayedSingleTaskEngine delayedSingleTaskEngine = new DelayedSingleTaskEngineExecutor();

    @NonNull
    protected final OkHttpClient okHttpClient = new OkHttpClient();
    @NonNull
    protected final Request googleRequest = new Request.Builder()
//            .addHeader(C.ACCEPT, C.APPLICATION_JSON)
            .url(PollingOptions.HOST_GOOGLE) // this should be changed later
            .get()
            .build();

    // ALL METHODS ---------------------------------------------------------------------------------

    // switch on or off - the only useful handling needed from outside
    public abstract void toggleInetCheck(boolean shouldLaunch);

    protected abstract void askHost();

    public boolean isPollingActive() { // main getter of polling agent state
        return delayedSingleTaskEngine.isCurrentGenerationAlive();
        // that's because consumer of this class must not know about its inner specifics
    }

    protected void updateFirstPollingReactionState(boolean isInetAvailable) {
        if (isWaitingForFirstResultFromPolling) {
            if (consumerLink != null) {
                consumerLink.onFirstResultReceived(isInetAvailable);
            } else {
                L.e("consumerLink is null - it cannot receive first polling reaction state");
            }
            isWaitingForFirstResultFromPolling = false;
        }
    }

    public void clearCurrentConsumerLink() {
        consumerLink = null;
    }
}