package com.igor_shaula.inetchecker.main;

import android.support.annotation.NonNull;

abstract class DelayedSingleTaskEngine {

//    protected boolean isNextCallGenerationAllowed;

    abstract void appointNextGeneration(@NonNull Runnable task, long delay);

    abstract boolean isCurrentGenerationAlive();

    abstract void stopCurrentGeneration();
}