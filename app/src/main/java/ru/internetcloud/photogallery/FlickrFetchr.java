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

    private static final String FETCH_RECENTS_METHOD = "flickr.photos.getRecent";
    private static final String SEARCH_METHOD = "flickr.photos.search";
    private static final Uri ENDPOINT = Uri
            .parse("https://api.flickr.com/services/rest/")
            .buildUpon()
            .appendQueryParameter("api_key", API_KEY)
            .appendQueryParameter("format", "json")
            .appendQueryParameter("nojsoncallback", "1")
            .appendQueryParameter("extras", "url_s,url_l")
            .build();

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

    public List<GalleryItem> downloadGalleryItems(String urlAddress) {

        List<GalleryItem> items = new ArrayList<>();

        try {
            // https://www.flickr.com/services/api/ - ???????????????? API

//            String urlAddress = Uri.parse("https://api.flickr.com/services/rest/")
//                    .buildUpon()
//                    .appendQueryParameter("method", "flickr.photos.getRecent")
//                    .appendQueryParameter("api_key", API_KEY)
//                    .appendQueryParameter("format", "json")
//                    .appendQueryParameter("nojsoncallback", "1")
//                    //.appendQueryParameter("per_page", "500") 500 ?????????????? 500 ????????
//                    //.appendQueryParameter("extras", "url_s")
//                    .appendQueryParameter("extras", "url_s,url_l")
//                    // url_l - ???????????? ???? ???????? ???????????????? ??????????????
//                    // url_s - ???????????? ???? ???????? ???????????????????? ??????????????
//                    .build().toString();

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

        // ?????????????????????? ?? ?????????????? ???????????????? id
        Collections.sort(items, new Comparator<GalleryItem>() {
            public int compare(GalleryItem o1, GalleryItem o2) {
                return o2.getId().compareTo(o1.getId());
            }
        });
    }

    private String buildUrl(String method, String query) {
        Uri.Builder uriBuilder = ENDPOINT.buildUpon().appendQueryParameter("method", method);
        if (method.equals(SEARCH_METHOD)) {
            uriBuilder.appendQueryParameter("text", query);
        }
        return uriBuilder.build().toString();
    }

    public List<GalleryItem> fetchRecentPhotos() {
        String url = buildUrl(FETCH_RECENTS_METHOD, null);
        return downloadGalleryItems(url);
    }

    public List<GalleryItem> searchPhotos(String query) {
        String url = buildUrl(SEARCH_METHOD, query);
        return downloadGalleryItems(url);
    }
}
