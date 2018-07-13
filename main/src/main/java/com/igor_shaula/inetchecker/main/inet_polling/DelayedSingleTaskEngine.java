package com.igor_shaula.inetchecker.main.inet_polling;

import android.support.annotation.NonNull;

abstract class DelayedSingleTaskEngine {

    abstract void appointNextGeneration(@NonNull Runnable task, long delay);

    abstract boolean isCurrentGenerationAlive();

    abstract void stopCurrentGeneration();
}