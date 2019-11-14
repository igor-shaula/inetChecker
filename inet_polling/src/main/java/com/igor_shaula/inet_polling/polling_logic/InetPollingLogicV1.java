/*
package com.igor_shaula.inetchecker.main;

import android.support.annotation.NonNull;
import android.util.SparseBooleanArray;
import android.util.SparseLongArray;

import TO_BE_SET.backbase_wrapper.CoreLib;
import TO_BE_SET.utils.C;
import TO_BE_SET.utils.L;
import TO_BE_SET.utils.U;
import TO_BE_SET.utils.annotations.MeDoc;
import com.squareup.okhttp.Callback;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public final class InetPollingLogicV1 {

    private static final String CN = "InetPollingLogic";

    private static final String HOST_GOOGLE = "https://www.google.com/";
    private static final String HOST_APPLE = "http://captive.apple.com/"; // HANGS THE APP IF HTTPS
    private static final String HOST_AMAZON = "https://amazon.com/";
    //    private static final long POLLING_DELAY = 10_000; // millis FOR DEBUG
    private static final long POLLING_DELAY = 1000; // millis
    //    private static final long POLLING_TIMEOUT = 100; // millis FOR DEBUG
    private static final long POLLING_TIMEOUT = 3000; // millis

    private boolean isPollingAllowed;
    @NonNull
    // result information per channel
    private final boolean[] inetAccessibleInChannel = new boolean[3];
    @NonNull
    // timestamp of making currently active requests generation - individual for every channel
    private final SparseLongArray oneGenerationAbsTimes = new SparseLongArray(3);
    //    @NonNull
    // latch for consider & schedule new generation for the first successful response only
//    private final SparseBooleanArray oneGenerationResponseFlags = new SparseBooleanArray(3);
//    @NonNull
    // latch for consider & schedule new generation for the first happened failed request only
//    private final SparseBooleanArray oneGenerationFailureFlags = new SparseBooleanArray(3);
    @NonNull
    private final SparseBooleanArray oneGenerationReactionFlags = new SparseBooleanArray(3);
    @NonNull
    // link to invoking class back - to change main flag & check connectivity which requires Context
    private final ConsumerLink consumerLink;
    @NonNull
    // abstraction for mechanism of scheduling delayed tasks which start every new generation of polling
    private final DelayedSingleTaskEngine delayedSingleTaskEngine;
    @NonNull
    private final OkHttpClient okHttpClient = new OkHttpClient();
    @NonNull
    private final Request requestGoogle = new Request.Builder()
            .addHeader(C.ACCEPT, C.APPLICATION_JSON)
            .url(HOST_GOOGLE)
            .head() // for reducing of package size in response
            .build();
    @NonNull
    private final Request requestApple = new Request.Builder()
            .addHeader(C.ACCEPT, C.APPLICATION_JSON)
            .url(HOST_APPLE)
            .get()
            .build();
    @NonNull
    private final Request requestBank = new Request.Builder()
            .addHeader(C.ACCEPT, C.APPLICATION_JSON)
            .addHeader(C.Api.HEADER_USER_AGENT, U.generateUserAgent()) // may the request work without it ???
            .url(Objects.requireNonNull(CoreLib.getServerUrl() == null ? HOST_AMAZON : CoreLib.getServerUrl()))
            .get()
            .build();
    @NonNull
    // main holder of payload to be done right after the new generation is started
    private final Runnable askAllHostsRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPollingAllowed) {
                L.w(CN, "askAllHostsRunnable ` polling is NOT allowed -> exiting now");
            }
            if (consumerLink.isConnectivityReadySyncCheck()) {
//                L.v(CN, "toggleInetCheck ` 1 second tick at " + System.currentTimeMillis());
                askHost(0); // Google
                askHost(1); // Apple
                askHost(2); // Amazon
            } else {
                // updating initial info in every channel for future - because we don't start polling now
                inetAccessibleInChannel[0] = false;
                inetAccessibleInChannel[1] = false;
                inetAccessibleInChannel[2] = false;
                // updating main flag for this case of connectivity absence
                consumerLink.onInetStateChanged(false);
            }
        }
    };

    public InetPollingLogicV1(@NonNull ConsumerLink consumerLink) {
        this.consumerLink = consumerLink;

//        delayedSingleTaskEngine = new DelayedSingleTaskEngineHandler();
        delayedSingleTaskEngine = new DelayedSingleTaskEngineExecutor();
//        delayedSingleTaskEngine = new DelayedSingleTaskEngineTimer();

        okHttpClient.setConnectTimeout(POLLING_TIMEOUT, TimeUnit.MILLISECONDS);
        okHttpClient.setReadTimeout(POLLING_TIMEOUT, TimeUnit.MILLISECONDS);
        okHttpClient.setWriteTimeout(POLLING_TIMEOUT, TimeUnit.MILLISECONDS);
    }

    // ---------------------------------------------------------------------------------------------

    public void toggleInetCheck(boolean shouldLaunch) { // main switcher
        isPollingAllowed = shouldLaunch;
        if (shouldLaunch) {
            // potentially we can have here many commands to launch many executors - but only one is enough
            if (delayedSingleTaskEngine.isCurrentGenerationAlive()) {
                L.v(CN, "toggleInetCheck ` avoided duplication of oneGenerationExecutor");
            } else {
                delayedSingleTaskEngine.appointNextGeneration(askAllHostsRunnable, 0);
                L.v(CN, "toggleInetCheck ` launched new generation of polling");
            }
        } else {
            delayedSingleTaskEngine.stopCurrentGeneration(); // toggleInetCheck
            L.v(CN, "toggleInetCheck ` stopped current generation of polling");
        }
    }

    @MeDoc("payload for actions in current channel ")
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
//        oneGenerationResponseFlags.put(whichOne, true);
//        oneGenerationFailureFlags.put(whichOne, true);
        oneGenerationReactionFlags.put(whichOne, true);

        // this link is created to be reused in closing response body later
        final ResponseBody[] responseBody = new ResponseBody[1];
        // A connection to https://www.google.com/ was leaked. Did you forget to close a response body?

        okHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public synchronized void onFailure(Request request, IOException e) {
                // immediately detecting timeout - even before logging
                final long currentMillisNow = System.currentTimeMillis();
//                L.v(CN, "askHost ` onFailure in " + currentMillisNow);
//                if (e != null) {
//                    L.v(CN, "askHost ` onFailure ` getLocalizedMessage = " + e.getLocalizedMessage());
//                }
                final long timeForThisRequest = currentMillisNow - oneGenerationAbsTimes.get(whichOne);
//                L.v(CN, "askHost ` onFailure ` timeForThisRequest = " + timeForThisRequest);

                appointNextGenerationFromTheFirstReactionConsideringDelay(timeForThisRequest, whichOne);
//                if (oneGenerationFailureFlags.get(whichOne)) {
//                    appointNextGenerationConsideringThisDelay(timeForThisRequest);
//                    // preventing from launching the same job from other two later failures
//                    oneGenerationFailureFlags.clear();
//                }

                onRequestStateChanged(whichOne, false);

//                final boolean isResponseReceivedInTime = timeForThisRequest <= POLLING_TIMEOUT;
//                L.v(CN, "askHost ` onFailure ` isResponseReceivedInTime = " + isResponseReceivedInTime);
//
//                // if response failed in after timeout - we'll collect false for it - real offline
//                onRequestStateChanged(whichOne, isResponseReceivedInTime);
            }

            @Override
            public synchronized void onResponse(Response response) {
                // immediately detecting timeout - even before logging
                final long currentMillisNow = System.currentTimeMillis();
//                L.v(CN, "askHost ` onResponse in " + currentMillisNow);
                final long timeForThisRequest = currentMillisNow - oneGenerationAbsTimes.get(whichOne);
//                L.v(CN, "askHost ` onResponse ` timeForThisRequest = " + timeForThisRequest);
                final boolean isResponseReceivedInTime = timeForThisRequest < POLLING_TIMEOUT;
//                L.v(CN, "askHost ` onResponse ` isResponseReceivedInTime = " + isResponseReceivedInTime);
                onRequestStateChanged(whichOne, isResponseReceivedInTime);

                appointNextGenerationFromTheFirstReactionConsideringDelay(timeForThisRequest, whichOne);
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
        // updating time of making the request in the current channel
    }

    private void appointNextGenerationFromTheFirstReactionConsideringDelay(long timeForThisRequest,
                                                                           int whichOne) {
        if (timeForThisRequest < POLLING_TIMEOUT && oneGenerationReactionFlags.get(whichOne)) { // one time latch for a generation
            appointNextGenerationConsideringThisDelay(timeForThisRequest);
            // preventing from launching the same job from other two later responses
            oneGenerationReactionFlags.clear();
        }
        // next block of actions serves only for avoiding internal OkHTTP warnings
    }

    private void appointNextGenerationConsideringThisDelay(long timeForThisRequest) {
        delayedSingleTaskEngine.stopCurrentGeneration();
        long delayBeforeNextGeneration = POLLING_DELAY - timeForThisRequest;
        if (delayBeforeNextGeneration < 0) { // for the case of too long requests
            delayBeforeNextGeneration = 0;
        }
        delayedSingleTaskEngine.appointNextGeneration(askAllHostsRunnable, delayBeforeNextGeneration);
        L.v(CN, "new oneGenerationExecutor scheduled in: " + delayBeforeNextGeneration);
    }

    private void onRequestStateChanged(int whichOne, boolean isInetAvailable) {
        inetAccessibleInChannel[whichOne] = isInetAvailable;
        // the main check of request's result in every channel
        if (inetAccessibleInChannel[0] || inetAccessibleInChannel[1] || inetAccessibleInChannel[2]) {
            consumerLink.onInetStateChanged(true);
        } else {
            consumerLink.onInetStateChanged(false); // all channels fail here
        }
    }

    // actually implemented by Activity
    public interface ConsumerLink {

        boolean isConnectivityReadySyncCheck();

        void onInetStateChanged(boolean isAvailable);
    }
}*/
