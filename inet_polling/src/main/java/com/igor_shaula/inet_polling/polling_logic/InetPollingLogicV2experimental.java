package com.igor_shaula.inet_polling.polling_logic;

import android.util.SparseBooleanArray;
import android.util.SparseLongArray;

import androidx.annotation.NonNull;

import com.igor_shaula.inet_polling.InetPollingLogic;
import com.igor_shaula.inet_polling.PollingOptions;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import utils.L;

public final class InetPollingLogicV2experimental extends InetPollingLogic {

    private static final String CN = "InetPollingLogicV2experimental";

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
    private final OkHttpClient okHttpClient = new OkHttpClient();
    @NonNull
    private final Request requestGoogle = new Request.Builder()
//            .addHeader(C.ACCEPT, C.APPLICATION_JSON)
            .url(PollingOptions.HOST_GOOGLE)
            .head() // for reducing of package size in response
            .build();
    @NonNull
    private final Request requestApple = new Request.Builder()
//            .addHeader(C.ACCEPT, C.APPLICATION_JSON)
            .url(PollingOptions.HOST_APPLE)
            .get()
            .build();
    @NonNull
    private final Request requestOther = new Request.Builder()
//            .addHeader(C.ACCEPT, C.APPLICATION_JSON)
            .url(PollingOptions.HOST_OTHER)
            .get()
            .build();
    @NonNull
    // main holder of payload to be done right after the new generation is started
    private final Runnable askAllHostsRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPollingAllowed) {
                L.w(CN , "askAllHostsRunnable ` polling is NOT allowed -> exiting now");
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

    public InetPollingLogicV2experimental() {
        L.v(CN); // detection of used variant
    }

    // ---------------------------------------------------------------------------------------------

//    @Override
//    public boolean isPollingActive() {
//        return delayedSingleTaskEngine.isCurrentGenerationAlive();
//    }

    public void toggleInetCheck(boolean shouldLaunch) { // main switcher
        isPollingAllowed = shouldLaunch;
        if (shouldLaunch) {
            // potentially we can have here many commands to launch many executors - but only one is enough
            if (delayedSingleTaskEngine.isCurrentGenerationAlive()) {
                L.v(CN , "toggleInetCheck ` avoided duplication of oneGenerationExecutor");
            } else {
                delayedSingleTaskEngine.appointNextGeneration(askAllHostsRunnable , 0);
                L.v(CN , "toggleInetCheck ` launched new generation of polling");
            }
        } else {
            delayedSingleTaskEngine.stopCurrentGeneration(); // toggleInetCheck
            L.v(CN , "toggleInetCheck ` stopped current generation of polling");
        }
    }

    @Override
    protected void updateFirstPollingReactionState(boolean isInetAvailable) {

    }

    @Override
    protected void askHost() {

    }

    //    @MeDoc("payload for actions in current channel ")
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
                request = requestOther;
        }
//        oneGenerationResponseFlags.put(whichOne, true);
//        oneGenerationFailureFlags.put(whichOne, true);
        oneGenerationReactionFlags.put(whichOne , true);

        // this link is created to be reused in closing response body later
        final ResponseBody[] responseBody = new ResponseBody[1];
        // A connection to https://www.google.com/ was leaked. Did you forget to close a response body?

        okHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onResponse(@NotNull Call call , @NotNull Response response) throws IOException {
                // immediately detecting timeout - even before logging
                final long currentMillisNow = System.currentTimeMillis();
//                L.v(CN, "askHost ` onResponse in " + currentMillisNow);
                final long timeForThisRequest = currentMillisNow - oneGenerationAbsTimes.get(whichOne);
//                L.v(CN, "askHost ` onResponse ` timeForThisRequest = " + timeForThisRequest);
                final boolean isResponseReceivedInTime = timeForThisRequest < PollingOptions.POLLING_TIMEOUT;
//                L.v(CN, "askHost ` onResponse ` isResponseReceivedInTime = " + isResponseReceivedInTime);
                onRequestStateChanged(whichOne , isResponseReceivedInTime);

                appointNextGenerationFromTheFirstReactionConsideringDelay(timeForThisRequest , whichOne);
//                try {
                responseBody[0] = response.body();
                if (responseBody[0] != null) {
                    responseBody[0].close(); // not needed if response's body-method hasn't been called
                }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
            }

            @Override
            public void onFailure(@NotNull Call call , @NotNull IOException e) {
                // immediately detecting timeout - even before logging
                final long currentMillisNow = System.currentTimeMillis();
//                L.v(CN, "askHost ` onFailure in " + currentMillisNow);
//                if (e != null) {
//                    L.v(CN, "askHost ` onFailure ` getLocalizedMessage = " + e.getLocalizedMessage());
//                }
                final long timeForThisRequest = currentMillisNow - oneGenerationAbsTimes.get(whichOne);
//                L.v(CN, "askHost ` onFailure ` timeForThisRequest = " + timeForThisRequest);

                appointNextGenerationFromTheFirstReactionConsideringDelay(timeForThisRequest , whichOne);
//                if (oneGenerationFailureFlags.get(whichOne)) {
//                    appointNextGenerationConsideringThisDelay(timeForThisRequest);
//                    // preventing from launching the same job from other two later failures
//                    oneGenerationFailureFlags.clear();
//                }

                onRequestStateChanged(whichOne , false);

//                final boolean isResponseReceivedInTime = timeForThisRequest <= POLLING_TIMEOUT;
//                L.v(CN, "askHost ` onFailure ` isResponseReceivedInTime = " + isResponseReceivedInTime);
//
//                // if response failed in after timeout - we'll collect false for it - real offline
//                onRequestStateChanged(whichOne, isResponseReceivedInTime);
            }
        });
//        L.v(CN, "askHost ` request was maid: " + request);
        oneGenerationAbsTimes.put(whichOne , System.currentTimeMillis());
        // updating time of making the request in the current channel
    }

    private void appointNextGenerationFromTheFirstReactionConsideringDelay(long timeForThisRequest ,
                                                                           int whichOne) {
        if (timeForThisRequest < PollingOptions.POLLING_TIMEOUT && oneGenerationReactionFlags.get(whichOne)) { // one time latch for a generation
            appointNextGenerationConsideringThisDelay(timeForThisRequest);
            // preventing from launching the same job from other two later responses
            oneGenerationReactionFlags.clear();
        }
        // next block of actions serves only for avoiding internal OkHTTP warnings
    }

    private void appointNextGenerationConsideringThisDelay(long timeForThisRequest) {
        delayedSingleTaskEngine.stopCurrentGeneration();
        long delayBeforeNextGeneration = PollingOptions.POLLING_DELAY - timeForThisRequest;
        if (delayBeforeNextGeneration < 0) { // for the case of too long requests
            delayBeforeNextGeneration = 0;
        }
        delayedSingleTaskEngine.appointNextGeneration(askAllHostsRunnable , delayBeforeNextGeneration);
        L.v(CN , "new oneGenerationExecutor scheduled in: " + delayBeforeNextGeneration);
    }

    private void onRequestStateChanged(int whichOne , boolean isInetAvailable) {
        inetAccessibleInChannel[whichOne] = isInetAvailable;
        if (consumerLink == null) return;
        // the main check of request's result in every channel
        if (inetAccessibleInChannel[0] || inetAccessibleInChannel[1] || inetAccessibleInChannel[2]) {
            consumerLink.onInetStateChanged(true);
        } else {
            consumerLink.onInetStateChanged(false); // all channels fail here
        }
    }
}
