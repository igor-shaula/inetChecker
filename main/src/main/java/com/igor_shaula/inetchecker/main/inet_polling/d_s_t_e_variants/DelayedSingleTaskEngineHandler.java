package com.igor_shaula.inetchecker.main.inet_polling.d_s_t_e_variants;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.igor_shaula.inetchecker.main.inet_polling.DelayedSingleTaskEngine;
import com.igor_shaula.inetchecker.main.utils.L;

final class DelayedSingleTaskEngineHandler extends DelayedSingleTaskEngine {

    private static final String CN = "DelayedSingleTaskEngineHandler";
    @Nullable
    private Handler handler;

    @Override
    public void appointNextGeneration(@NonNull Runnable task , long delay) {
        if (handler == null) {
            handler = new Handler(Looper.getMainLooper());
        }
//        if (isNextCallGenerationAllowed) {
        handler.postDelayed(task , delay);
//        }
    }

    @Override
    public boolean isCurrentGenerationAlive() {
        return handler != null;
    }

    @Override
    public void stopCurrentGeneration() {
//        isNextCallGenerationAllowed = false;
        L.v(CN , "stopCurrentGeneration ` nothing to do here for handler");
        handler = null;
    }
}