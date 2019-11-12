package com.igor_shaula.inet_checker.main.inet_polling;

import androidx.annotation.NonNull;

public abstract class DelayedSingleTaskEngine {

    public abstract boolean isCurrentGenerationAlive();

    public abstract void appointNextGeneration(@NonNull Runnable task , long delay);

    public abstract void stopCurrentGeneration();
}