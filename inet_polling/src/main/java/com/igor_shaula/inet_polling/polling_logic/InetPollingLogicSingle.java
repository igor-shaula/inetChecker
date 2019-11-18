package com.igor_shaula.inet_polling.polling_logic;

import androidx.annotation.NonNull;

import com.igor_shaula.inet_polling.InetPollingLogic;
import com.igor_shaula.inet_polling.InetRequestResult;
import com.igor_shaula.inet_polling.PollingOptions;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import utils.L;

public final class InetPollingLogicSingle extends InetPollingLogic {

    private static final String CN = "InetPollingLogicSingle";

    @NonNull
    private final OkHttpClient okHttpClient = new OkHttpClient();
    @NonNull
    private final Request googleRequest = new Request.Builder()
            .url(PollingOptions.HOST_GOOGLE) // this should be changed later
            .get()
            .build();

    @NonNull
    private final Runnable pollingRunnable = new Runnable() {
        // main holder of payload to be done right after the new generation is started
        @Override
        public void run() {
            if (consumerLink != null && consumerLink.isConnectivityReadySyncCheck()) {
                L.v(CN , "toggleInetCheck ` 1 second tick at " + System.currentTimeMillis());
                if (isPollingAllowed) {
                    askHost();
                } else {
                    L.w(CN , "askHost ` prevented from making requests & loosing battery");
                }
            } else {
                L.v(CN , "pollingRunnable - else");
                // updating main flag for this case of connectivity absence
                updateFirstPollingReactionState(false); // pollingRunnable
                consumerLink.onInetResult(new InetRequestResult()); // failed by default
            }
        }
    };

    private final InetRequestResult inetRequestResult = new InetRequestResult();
    // creating here to have one instance instead of many

    // ---------------------------------------------------------------------------------------------

    @Override
    public void toggleInetCheck(boolean shouldLaunch) { // main switcher
        isPollingAllowed = shouldLaunch;
        if (shouldLaunch) {
            // potentially we can have here many commands to launch many executors - but only one is enough
            if (delayedSingleTaskEngine.isCurrentGenerationAlive()) {
                L.v(CN , "toggleInetCheck ` avoided duplication of oneGenerationExecutor");
                // just do nothing for now as we already have polling engine in work
            } else {
                // launch new sequence of polling actions
                isPollingAllowed = true; // allowing future possible invocations
                isWaitingForFirstResultFromPolling = true;
                delayedSingleTaskEngine.appointNextGeneration(pollingRunnable , 0);
                // selection of timeout before polling start could be carried out to options
                L.v(CN , "toggleInetCheck ` launched new generation of polling");
                if (consumerLink != null) {
                    consumerLink.onTogglePollingState(true);
                }
            }
        } else { // launch is prohibited - so in any state of engine we must stop it here
//            isPollingAllowed = false; // preventing from future possible invocations
            delayedSingleTaskEngine.stopCurrentGeneration(); // toggleInetCheck
            L.v(CN , "toggleInetCheck ` stopped current generation of polling");
            if (consumerLink != null) {
                consumerLink.onTogglePollingState(false);
            }
        }
    }

    private void askHost() {

        // this link is created to be reused in closing response body later
        final ResponseBody[] responseBody = new ResponseBody[1];
        // A connection to https://www.google.com/ was leaked. Did you forget to close a response body?

        inetRequestResult.prepareForNewData();

        okHttpClient.newCall(googleRequest).enqueue(new Callback() {

            @Override
            public void onFailure(@NonNull Call call , @NonNull IOException e) {
                // immediately detecting timeout - even before logging
                final long currentMillisNow = System.currentTimeMillis();
                L.v(CN , "askHost ` onFailure in " + currentMillisNow);
                L.v(CN , "askHost ` onFailure ` call = " + call);
                L.v(CN , "askHost ` onFailure ` getLocalizedMessage = " + e.getLocalizedMessage());

                final long timeForThisRequest = currentMillisNow - oneGenerationAbsTime;
                L.v(CN , "askHost ` onFailure ` timeForThisRequest = " + timeForThisRequest);

                updateFirstPollingReactionState(false); // onFailure

                if (consumerLink != null) {
                    inetRequestResult.setTimeForRequest(timeForThisRequest);
                    inetRequestResult.setInetAvailable(false);
                    consumerLink.onInetResult(inetRequestResult);
                }

                appointNextGenerationConsideringThisDelay(timeForThisRequest);

                final boolean isResponseReceivedInTime = timeForThisRequest <= PollingOptions.POLLING_TIMEOUT;
                L.v(CN , "askHost ` onFailure ` isResponseReceivedInTime = " + isResponseReceivedInTime);

                // if response failed in after timeout - we'll collect false for it = real offline
            }

            @Override
            public void onResponse(@NonNull Call call , @NonNull Response response) {
                // immediately detecting timeout - even before logging
                final long currentMillisNow = System.currentTimeMillis();
                L.v(CN , "askHost ` onResponse in " + currentMillisNow);
                final long timeForThisRequest = currentMillisNow - oneGenerationAbsTime;
                L.v(CN , "askHost ` onResponse ` timeForThisRequest = " + timeForThisRequest);
                final boolean isResponseReceivedInTime = timeForThisRequest <= PollingOptions.POLLING_TIMEOUT;
                L.v(CN , "askHost ` onResponse ` isResponseReceivedInTime = " + isResponseReceivedInTime);

                updateFirstPollingReactionState(isResponseReceivedInTime); // onResponse

                L.v(CN , "askHost ` onResponse ` response.code() = " + response.code());
                if (consumerLink != null) {
                    inetRequestResult.setTimeForRequest(timeForThisRequest);
                    inetRequestResult.setInetAvailable(isResponseReceivedInTime);
                    consumerLink.onInetResult(inetRequestResult);
                }
//                consumerLink.onInetResult(isResponseReceivedInTime && response.code() == 401);
                // we don't look on code here because it's onResponse - not onFailure invocation

                appointNextGenerationConsideringThisDelay(timeForThisRequest);

                // next block of actions serves only for avoiding internal OkHTTP warnings
                responseBody[0] = response.body();
                if (responseBody[0] != null) {
                    responseBody[0].close(); // not needed if response's body-method hasn't been called
                }
            }
        });
//        L.v(CN, "askHost ` request was maid: " + bankRequest);
        oneGenerationAbsTime = System.currentTimeMillis();
        // updating time of making the request in the current channel
    } // askHost

    private void updateFirstPollingReactionState(boolean isInetAvailable) {
        if (isWaitingForFirstResultFromPolling) {
            if (consumerLink != null) {
                consumerLink.onFirstResultReceived(isInetAvailable);
            } else {
                L.e("consumerLink is null - it cannot receive first polling reaction state");
            }
            isWaitingForFirstResultFromPolling = false;
        }
    }

    private void appointNextGenerationConsideringThisDelay(long timeForThisRequest) {
        delayedSingleTaskEngine.stopCurrentGeneration();
        long delayBeforeNextGeneration = PollingOptions.POLLING_DELAY - timeForThisRequest;
        if (delayBeforeNextGeneration < 0) { // for the case of too long requests
            delayBeforeNextGeneration = 0;
        }
        delayedSingleTaskEngine.appointNextGeneration(pollingRunnable , delayBeforeNextGeneration);
//        L.v(CN, "askHost ` onResponse ` new oneGenerationExecutor scheduled in: " + delayBeforeNextGeneration);
    }
}