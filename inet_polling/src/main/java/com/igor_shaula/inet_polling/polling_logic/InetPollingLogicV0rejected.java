/*
package com.igor_shaula.inetchecker.main.inet_polling;

import android.support.annotation.NonNull;
import android.util.SparseBooleanArray;
import android.util.SparseLongArray;

import com.igor_shaula.inetchecker.main.utils.L;

import java.io.IOException;

import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class InetPollingLogicV0rejected extends InetPollingLogic { // 1st realization

    private static final String CN = "InetPollingLogicV0rejected";

    private static final String HOST_GOOGLE = "https://www.google.com/";
    private static final String HOST_APPLE = "http://captive.apple.com/"; // HANGS THE APP IF HTTPS
    //    private static final long POLLING_DELAY = 10_000; // millis FOR DEBUG
    private static final long POLLING_DELAY = 1000; // millis
    //    private static final long POLLING_TIMEOUT = 100; // millis FOR DEBUG
    private static final long POLLING_TIMEOUT = 3000; // millis

    @NonNull
    private final boolean[] inetAccessibleInChannel = new boolean[3];
    @NonNull
    private final SparseLongArray oneGenerationAbsTimes = new SparseLongArray(3);
    @NonNull
    private final SparseBooleanArray oneGenerationFlags = new SparseBooleanArray(3);
    @NonNull
    private final PollingResultsConsumer consumerLink;
    //    private final ConsumerLink consumerLink;
    @NonNull
    private final DelayedSingleTaskEngine delayedSingleTaskEngine;
    @NonNull
    private final OkHttpClient okHttpClient = new OkHttpClient();
    @NonNull
    private final Request requestGoogle = new Request.Builder()
            .addHeader("Accept", "application/json")
            .url(HOST_GOOGLE)
            .head()
            .build();
    @NonNull
    private final Request requestApple = new Request.Builder()
            .addHeader("Accept", "application/json")
            .url(HOST_APPLE)
            .get()
            .build();
    @NonNull
    private final Request requestBank = new Request.Builder()
            .addHeader("Accept", "application/json")
//            .addHeader(C.Api.HEADER_USER_AGENT, U.generateUserAgent()) // was inside interceptor
            .get()
            .build();
    @NonNull
    private final Runnable askAllHostsRunnable = new Runnable() {
        @Override
        public void run() {
            if (consumerLink.isConnectivityReadySyncCheck()) {
//                L.v(CN, "toggleInetCheck ` 1 second tick at " + System.currentTimeMillis());
                askHost(0); // Google
                askHost(1); // Apple
                askHost(2); // Amazon
            } else {
                inetAccessibleInChannel[0] = false;
                inetAccessibleInChannel[1] = false;
                inetAccessibleInChannel[2] = false;
                consumerLink.onInetStateChanged(false);
            }
        }
    };
    private long timeDeltaFromStartedFailures;

    public InetPollingLogicV0rejected(@NonNull PollingResultsConsumer consumerLink) {
        L.v(CN); // detection of used variant
        this.consumerLink = consumerLink;

//        delayedSingleTaskEngine = new DelayedSingleTaskEngineTimer();
//        delayedSingleTaskEngine = new DelayedSingleTaskEngineHandler();
        delayedSingleTaskEngine = new DelayedSingleTaskEngineExecutor();

//        okHttpClient.setConnectTimeout(POLLING_TIMEOUT, TimeUnit.MILLISECONDS);
//        okHttpClient.setReadTimeout(POLLING_TIMEOUT, TimeUnit.MILLISECONDS);
//        okHttpClient.setWriteTimeout(POLLING_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    // ---------------------------------------------------------------------------------------------

    @Override
    public boolean isPollingActive() {
        return delayedSingleTaskEngine.isCurrentGenerationAlive();
    }

    public void toggleInetCheck(boolean shouldLaunch) {
        if (shouldLaunch) {
            // potentially we can have here many commands to launch many executors - but only one is enough
            if (delayedSingleTaskEngine.isCurrentGenerationAlive()) {
                L.v(CN, "toggleInetCheck ` avoided duplication of oneGenerationExecutor");
            } else {
//                timeDeltaFromStartedFailures = 0; // resetting for future possible attempts with onFailure
                delayedSingleTaskEngine.appointNextGeneration(askAllHostsRunnable, 0);
            }
        } else {
            delayedSingleTaskEngine.stopCurrentGeneration(); // toggleInetCheck
        }
    }

    @Override
    protected void updateFirstPollingReactionState(boolean isInetAvailable) {

    }

    @Override
    protected void askHost() {

    }

    private void askHost(final int whichOne) {
        final Request request;
        switch (whichOne) {
            case 0:
                request = requestGoogle;
                break;
            case 1:
                request = requestApple;
                break;
            default:
                request = requestBank;
        }
        oneGenerationFlags.put(whichOne, true);

        // this link is created to be reused in closing response body later
        final ResponseBody[] responseBody = new ResponseBody[1];
        // A connection to https://www.google.com/ was leaked. Did you forget to close a response body?

        okHttpClient.newCall(request).enqueue(new Callback() {

            // previous realization for onFailure - this case resulted in sudden stop from Android OS
            @Override
            public void onFailure(Request request, IOException e) {
                if (e != null) {
                    L.v(CN, "askHost ` onFailure ` getLocalizedMessage = " + e.getLocalizedMessage());
                }
                delayedSingleTaskEngine.stopCurrentGeneration(); // onFailure

                if (timeDeltaFromStartedFailures > POLLING_TIMEOUT) {
                    consumerLink.onInetStateChanged(false);
                }

                // immediately detecting timeout - even before logging
                final long currentMillisNow = System.currentTimeMillis();
//                L.v(CN, "askHost ` onFailure in " + currentMillisNow);
                final long timeForThisRequest = currentMillisNow - oneGenerationAbsTimes.get(whichOne);
//                L.v(CN, "askHost ` onFailure ` timeForThisRequest = " + timeForThisRequest);
                final boolean isResponseReceivedInTime = timeForThisRequest <= POLLING_TIMEOUT;
//                L.v(CN, "askHost ` onFailure ` isResponseReceivedInTime = " + isResponseReceivedInTime);

                if (isResponseReceivedInTime) {
                    long delayBeforeNextGeneration = POLLING_DELAY - timeForThisRequest;
                    if (delayBeforeNextGeneration < 0) { // for the case of too long requests
                        delayBeforeNextGeneration = 0;
                    }
                    timeDeltaFromStartedFailures = timeDeltaFromStartedFailures + timeForThisRequest;
                    delayedSingleTaskEngine.appointNextGeneration(askAllHostsRunnable, delayBeforeNextGeneration);
//                    L.v(CN, "askHost ` onFailure ` new oneGenerationExecutor scheduled in: " + delayBeforeNextGeneration);
                } else {
                    onRequestStateChanged(whichOne, false);
                }
            }

            @Override
            public synchronized void onResponse(Response response) {
                // immediately detecting timeout - even before logging
                final long currentMillisNow = System.currentTimeMillis();
//                L.v(CN, "askHost ` onResponse in " + currentMillisNow);
                final long timeForThisRequest = currentMillisNow - oneGenerationAbsTimes.get(whichOne);
//                L.v(CN, "askHost ` onResponse ` timeForThisRequest = " + timeForThisRequest);
                final boolean isResponseReceivedInTime = timeForThisRequest <= POLLING_TIMEOUT;
//                L.v(CN, "askHost ` onResponse ` isResponseReceivedInTime = " + isResponseReceivedInTime);
                onRequestStateChanged(whichOne, isResponseReceivedInTime);

                if (isResponseReceivedInTime && oneGenerationFlags.get(whichOne)) { // one time latch for a generation

                    appointNextGenerationConsideringThisDelay(timeForThisRequest);
//                    L.v(CN, "askHost ` onResponse ` new oneGenerationExecutor scheduled in: " + delayBeforeNextGeneration);

                    // preventing from other two later responses
                    oneGenerationFlags.clear();
                }
                try {
                    responseBody[0] = response.body();
                    responseBody[0].close(); // not needed if response's body-method hasn't been called
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
//        L.v(CN, "askHost ` request was maid: " + request);
        oneGenerationAbsTimes.put(whichOne, System.currentTimeMillis());
    }

    private void appointNextGenerationConsideringThisDelay(long timeForThisRequest) {
        delayedSingleTaskEngine.stopCurrentGeneration();
        long delayBeforeNextGeneration = POLLING_DELAY - timeForThisRequest;
        if (delayBeforeNextGeneration < 0) { // for the case of too long requests
            delayBeforeNextGeneration = 0;
        }
        delayedSingleTaskEngine.appointNextGeneration(askAllHostsRunnable, delayBeforeNextGeneration);
    }

    private void onRequestStateChanged(int whichOne, boolean isInetAvailable) {
        inetAccessibleInChannel[whichOne] = isInetAvailable;

        if (inetAccessibleInChannel[0] || inetAccessibleInChannel[1] || inetAccessibleInChannel[2]) {
            consumerLink.onInetStateChanged(true);
        } else {
            consumerLink.onInetStateChanged(false);
        }
    }
}*/
