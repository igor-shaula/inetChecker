package com.igor_shaula.inetchecker.main.inet_polling;

import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.igor_shaula.inetchecker.main.utils.L;

final class DelayedSingleTaskEngineHandler extends DelayedSingleTaskEngine {

    private static final String CN = "DelayedSingleTaskEngineHandler";
    @Nullable
    private Handler handler;

    @Override
    void appointNextGeneration(@NonNull Runnable task, long delay) {
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
//        if (isNextCallGenerationAllowed) {
        handler.postDelayed(task, delay);
//        }
    }

    @Override
    boolean isCurrentGenerationAlive() {
        return handler != null;
    }

    @Override
    void stopCurrentGeneration() {
//        isNextCallGenerationAllowed = false;
        L.v(CN, "stopCurrentGeneration ` nothing to do here for handler");
        handler = null;
    }
}