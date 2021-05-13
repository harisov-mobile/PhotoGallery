package ru.internetcloud.photogallery;

import android.net.Uri;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class FlickrFetchr {

    private static final String TAG = "rustam";
    private static final String API_KEY = "58d2f9ef49b1a0cae04abc39243b7e18";

    public byte[] getUrlBytes(String urlAddress) throws IOException {

        URL url = new URL(urlAddress);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            InputStream inputStream = connection.getInputStream();

            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() + " : with " + urlAddress);
            }

            int bytesRead = 0;

            byte[] buffer = new byte[1024];
            while ((bytesRead = inputStream.read(buffer)) > 0) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            byteArrayOutputStream.close();
            return byteArrayOutputStream.toByteArray();

        } finally {
            connection.disconnect();
        }

    }

    public String getUrlString(String urlAddress) throws IOException {
        return new String(getUrlBytes(urlAddress));
    }

    public List<GalleryItem> fetchItems() {

        List<GalleryItem> items = new ArrayList<>();

        try {
            // https://www.flickr.com/services/api/ - описание API

            String urlAddress = Uri.parse("https://api.flickr.com/services/rest/")
                    .buildUpon()
                    .appendQueryParameter("method", "flickr.photos.getRecent")
                    .appendQueryParameter("api_key", API_KEY)
                    .appendQueryParameter("format", "json")
                    .appendQueryParameter("nojsoncallback", "1")
                    //.appendQueryParameter("per_page", "500") 500 скачать 500 фото
                    //.appendQueryParameter("extras", "url_s")
                    .appendQueryParameter("extras", "url_s,url_l")
                    // url_l - ссылка на фото большого размера
                    // url_s - ссылка на фото маленького размера
                    .build().toString();

            String jsonString = getUrlString(urlAddress);
            //Log.i(TAG, "JSON = " + jsonString);
            JSONObject jsonBody = new JSONObject(jsonString);
            parseItems(items, jsonBody);
        } catch (IOException ioe) {
            Log.e(TAG, "fail ", ioe);
        } catch (JSONException je) {
            Log.e(TAG, "Failed to parse JSON", je);
        }

        return items;
    }

    private void parseItems(List<GalleryItem> items, JSONObject jsonBody) throws IOException, JSONException {

        JSONObject photosJsonObject = jsonBody.getJSONObject("photos");
        JSONArray photoJsonArray = photosJsonObject.getJSONArray("photo");

        for (int i = 0; i < photoJsonArray.length(); i++) {
            JSONObject photoJsonObject = photoJsonArray.getJSONObject(i);
            GalleryItem item = new GalleryItem();
            item.setId(photoJsonObject.getString("id"));
            item.setCaption(photoJsonObject.getString("title"));
            item.setOwner(photoJsonObject.getString("owner"));

            if (!photoJsonObject.has("url_s")) {
                continue;
            }
            item.setIconUrl(photoJsonObject.getString("url_s"));

            if (!photoJsonObject.has("url_l")) {
                continue;
            }
            item.setPhotoUrl(photoJsonObject.getString("url_l"));

            items.add(item);
        }

        // сортировать в порядке убывания id
        Collections.sort(items, new Comparator<GalleryItem>() {
            public int compare(GalleryItem o1, GalleryItem o2) {
                return o2.getId().compareTo(o1.getId());
            }
        });
    }
}
