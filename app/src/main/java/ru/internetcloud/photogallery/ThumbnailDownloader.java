package ru.internetcloud.photogallery;

import android.os.HandlerThread;
import android.util.Log;

public class ThumbnailDownloader<T> extends HandlerThread {

    private static final String TAG = "ThumbnailDownloader";

    private boolean hasQuit = false;

    public ThumbnailDownloader() {
        super(TAG);
    }


    @Override
    public boolean quit() {
        hasQuit = true;
        return super.quit();
    }
    public void queueThumbnail(T target, String urlAddress) {
        Log.i(TAG, "Got a URL: " + urlAddress);
    }
}
