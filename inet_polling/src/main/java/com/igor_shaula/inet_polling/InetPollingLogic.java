package com.igor_shaula.inet_polling;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.igor_shaula.inet_polling.polling_engine.DelayedSingleTaskEngineExecutor;
import com.igor_shaula.inet_polling.polling_logic.InetPollingLogicSingle;

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
    protected final DelayedSingleTaskEngine delayedSingleTaskEngine = new DelayedSingleTaskEngineExecutor();

    // ALL METHODS ---------------------------------------------------------------------------------

    @NonNull
    public static InetPollingLogic getInstance(@NonNull PollingResultsConsumer pollingResultsConsumer) {
        if (thisInstance == null) {
            /* selection of concrete logic agent and polling engine have to be on the same level */
            thisInstance = new InetPollingLogicSingle();
//            thisInstance = new InetPollingLogicMultiple();
        }
        thisInstance.consumerLink = pollingResultsConsumer;
        L.w(CN , "getInstance ` consumerLink updated with hash: " + pollingResultsConsumer.hashCode());
        return thisInstance;
    }

    public boolean isPollingActive() { // main getter of polling agent state
        return delayedSingleTaskEngine.isCurrentGenerationAlive();
        // that's because consumer of this class must not know about its inner specifics
    }

    public void clearCurrentPollingSetting() {
        consumerLink = null;
        thisInstance = null;
    }

    // switch on or off - the only useful handling needed from outside
    public abstract void toggleInetCheck(boolean shouldLaunch);
}