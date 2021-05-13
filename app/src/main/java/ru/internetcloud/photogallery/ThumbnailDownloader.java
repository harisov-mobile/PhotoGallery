package ru.internetcloud.photogallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ThumbnailDownloader<T> extends HandlerThread {

    private static final String TAG = "ThumbnailDownloader";
    private static final int MESSAGE_DOWNLOAD = 0;

    private boolean hasQuit = false;
    private Handler requestHandler; // и постановщик в очередь, он же и обработчик
    private ConcurrentMap<T,String> requestMap = new ConcurrentHashMap<>();

    private Handler mainThreadHandler;
    private ThumbnailDownloadListener<T>  thumbnailDownloadListener;

    public interface ThumbnailDownloadListener<T> {
        public void onThumbnailDownloaded(T holder, Bitmap thumbnail);
    }

    public ThumbnailDownloader(Handler mainThreadHandler) {
        super(TAG);
        this.mainThreadHandler = mainThreadHandler;
    }

    @Override
    public boolean quit() {
        hasQuit = true;
        return super.quit();
    }
    public void queueThumbnail(T holder, String urlAddress) {
        Log.i(TAG, "Got a URL: " + urlAddress);

        if (urlAddress == null) {
            requestMap.remove(holder);
        } else {
            requestMap.put(holder, urlAddress); // с каждым holder связываем url-адрес на загрузку иконки.
            requestHandler.obtainMessage(MESSAGE_DOWNLOAD, holder).sendToTarget(); // создается сообщение и передается обработчику (мвязывается с обработчиком)
        }
    }

    @Override
    protected void onLooperPrepared() {
        requestHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == MESSAGE_DOWNLOAD) {
                    T holder = (T) msg.obj;
                    Log.i(TAG, "Got a request for URL: " + requestMap.get(holder));
                    handleRequest(holder);
                }
            }
        };
    }

    private void handleRequest(final T holder) {
        try {
            final String urlAddress = requestMap.get(holder);
            if (urlAddress == null) {
                return;
            }
            byte[] bitmapBytes = new FlickrFetchr().getUrlBytes(urlAddress);
            final Bitmap bitmap = BitmapFactory.decodeByteArray(bitmapBytes, 0, bitmapBytes.length);
            Log.i(TAG, "Bitmap created");

            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (requestMap.get(holder) != urlAddress || hasQuit) {
                        return;
                    }

                    requestMap.remove(holder);
                    thumbnailDownloadListener.onThumbnailDownloaded(holder, bitmap);
                }
            });
        } catch (IOException ioe) {
            Log.e(TAG, "Error downloading image", ioe);
        }
    }

    public void setThumbnailDownloadListener(ThumbnailDownloadListener<T> listener) {
        thumbnailDownloadListener = listener;
    }

    public void clearQueue() {
        requestHandler.removeMessages(MESSAGE_DOWNLOAD);
        requestMap.clear();
    }
}
