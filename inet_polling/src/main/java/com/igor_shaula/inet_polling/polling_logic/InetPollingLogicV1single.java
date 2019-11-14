package com.igor_shaula.inet_polling.polling_logic;

import androidx.annotation.NonNull;

import com.igor_shaula.inet_polling.InetPollingLogic;
import com.igor_shaula.inet_polling.PollingResultsConsumer;
import com.igor_shaula.inet_polling.polling_engine.DelayedSingleTaskEngineExecutor;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.ResponseBody;
import utils.L;

public final class InetPollingLogicV1single extends InetPollingLogic {

    private static final String CN = "InetPollingLogicV1single";

    public InetPollingLogicV1single(@NonNull PollingResultsConsumer consumerLink) {
        L.v(CN); // detection of used variant \\
        this.consumerLink = consumerLink;

//        delayedSingleTaskEngine = new DelayedSingleTaskEngineHandler();
        delayedSingleTaskEngine = new DelayedSingleTaskEngineExecutor();
//        delayedSingleTaskEngine = new DelayedSingleTaskEngineTimer();
        // TODO: 14.11.2019 carry selection of exact polling engine out of this class
    }

    // ---------------------------------------------------------------------------------------------

    @Override
    public boolean isPollingActive() { // main getter of polling agent state
        return delayedSingleTaskEngine.isCurrentGenerationAlive();
        // that's because consumer of this class must not know about its inner specifics
    }

    @Override
    public void toggleInetCheckNow(boolean shouldLaunch) { // main switcher \\
        if (shouldLaunch) {
            // potentially we can have here many commands to launch many executors - but only one is enough \\
            if (delayedSingleTaskEngine.isCurrentGenerationAlive()) {
                L.v(CN , "toggleInetCheckNow ` avoided duplication of oneGenerationExecutor");
                // just do nothing for now as we already have polling engine in work
            } else {
                // launch new sequence of polling actions
                isPollingAllowedInGeneral = true; // allowing future possible invocations \\
                isWaitingForFirstResultFromPolling = true;
                delayedSingleTaskEngine.appointNextGeneration(pollingRunnable , 0);
                // selection of timeout before polling start could be carried out to options
                L.v(CN , "toggleInetCheckNow ` launched new generation of polling");
                if (consumerLink != null) {
                    consumerLink.onTogglePollingState(true);
                    // TODO: 14.11.2019 this is not the right place for sending this callback
                }
            }
        } else { // launch is prohibited - so in any state of engine we must stop it here
            isPollingAllowedInGeneral = false; // preventing from future possible invocations \\
            delayedSingleTaskEngine.stopCurrentGeneration(); // toggleInetCheckNow \\
            L.v(CN , "toggleInetCheckNow ` stopped current generation of polling");
            if (consumerLink != null) {
                consumerLink.onTogglePollingState(false);
            }
        }
    }

    //    @MeDoc("payload for actions in current channel ")
    @Override
    protected void askHost() {
        if (!isPollingAllowedInGeneral) {
            L.w(CN , "askHost ` prevented from making requests & loosing battery");
            return;
        }

        // this link is created to be reused in closing response body later \\
        final ResponseBody[] responseBody = new ResponseBody[1];
        // A connection to https://www.google.com/ was leaked. Did you forget to close a response body?

        okHttpClient.newCall(googleRequest).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call , @NonNull IOException e) {
                // immediately detecting timeout - even before logging \\
                final long currentMillisNow = System.currentTimeMillis();
//                L.v(CN, "askHost ` onFailure in " + currentMillisNow);
//                L.v(CN, "askHost ` onFailure ` request = " + request);
//                if (e != null) {
//                    L.v(CN, "askHost ` onFailure ` getLocalizedMessage = " + e.getLocalizedMessage());
//                }
                final long timeForThisRequest = currentMillisNow - oneGenerationAbsTime;
//                L.v(CN, "askHost ` onFailure ` timeForThisRequest = " + timeForThisRequest);

                updateFirstPollingReactionState(false); // onFailure \\

//                consumerLink.onInetStateChanged(isResponseReceivedInTime);
                if (consumerLink != null) {
                    consumerLink.onInetStateChanged(false);
                }

                appointNextGenerationConsideringThisDelay(timeForThisRequest);

//                final boolean isResponseReceivedInTime = timeForThisRequest <= POLLING_TIMEOUT;
//                L.v(CN, "askHost ` onFailure ` isResponseReceivedInTime = " + isResponseReceivedInTime);

                // if response failed in after timeout - we'll collect false for it = real offline \\
            }

            @Override
            public void onResponse(@NonNull Call call , @NonNull Response response) {
                // immediately detecting timeout - even before logging \\
                final long currentMillisNow = System.currentTimeMillis();
//                L.v(CN, "askHost ` onResponse in " + currentMillisNow);
                final long timeForThisRequest = currentMillisNow - oneGenerationAbsTime;
//                L.v(CN, "askHost ` onResponse ` timeForThisRequest = " + timeForThisRequest);
                final boolean isResponseReceivedInTime = timeForThisRequest <= POLLING_TIMEOUT;
//                L.v(CN, "askHost ` onResponse ` isResponseReceivedInTime = " + isResponseReceivedInTime);

                updateFirstPollingReactionState(isResponseReceivedInTime); // onResponse \\

//                L.v(CN, "askHost ` onResponse ` response.code() = " + response.code());
                if (consumerLink != null) {
                    consumerLink.onInetStateChanged(isResponseReceivedInTime);
                }
//                consumerLink.onInetStateChanged(isResponseReceivedInTime && response.code() == 401);
                // we don't look on code here because it's onResponse - not onFailure invocation \\

                appointNextGenerationConsideringThisDelay(timeForThisRequest);

                // next block of actions serves only for avoiding internal OkHTTP warnings \\
                responseBody[0] = response.body();
                if (responseBody[0] != null) {
                    responseBody[0].close(); // not needed if response's body-method hasn't been called \\
                }
            }
        });
//        L.v(CN, "askHost ` request was maid: " + bankRequest);
        oneGenerationAbsTime = System.currentTimeMillis();
        // updating time of making the request in the current channel \\
    }

    @Override
    protected void updateFirstPollingReactionState(boolean isInetAvailable) {
        if (isWaitingForFirstResultFromPolling) {
            isWaitingForFirstResultFromPolling = false;
            if (consumerLink != null) {
                consumerLink.onFirstResultReceived(isInetAvailable);
            }
        }
    }

    private void appointNextGenerationConsideringThisDelay(long timeForThisRequest) {
        delayedSingleTaskEngine.stopCurrentGeneration();
        long delayBeforeNextGeneration = POLLING_DELAY - timeForThisRequest;
        if (delayBeforeNextGeneration < 0) { // for the case of too long requests \\
            delayBeforeNextGeneration = 0;
        }
        delayedSingleTaskEngine.appointNextGeneration(pollingRunnable , delayBeforeNextGeneration);
//        L.v(CN, "askHost ` onResponse ` new oneGenerationExecutor scheduled in: " + delayBeforeNextGeneration);
    }
}