package com.igor_shaula.inet_polling;

public final class InetRequestResult {

    private long timeForRequest = -1;

    private boolean isInetAvailable = false;

    public long getTimeForRequest() {
        return timeForRequest;
    }

    public void setTimeForRequest(long timeForRequest) {
        this.timeForRequest = timeForRequest;
    }

    public boolean isInetAvailable() {
        return isInetAvailable;
    }

    public void setInetAvailable(boolean inetAvailable) {
        isInetAvailable = inetAvailable;
    }

    public void prepareForNewData() {
        timeForRequest = -1;
        isInetAvailable = false;
    }
}