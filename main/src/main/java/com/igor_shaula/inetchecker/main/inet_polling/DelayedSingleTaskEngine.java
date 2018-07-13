package com.igor_shaula.inetchecker.main.inet_polling;

import android.support.annotation.NonNull;

public abstract class DelayedSingleTaskEngine {

    public abstract void appointNextGeneration(@NonNull Runnable task, long delay);

    public abstract boolean isCurrentGenerationAlive();

    public abstract void stopCurrentGeneration();
}