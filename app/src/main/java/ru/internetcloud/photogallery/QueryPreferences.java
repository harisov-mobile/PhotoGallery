package ru.internetcloud.photogallery;

import android.content.Context;
import android.preference.PreferenceManager;

public class QueryPreferences {

    private static final String PREF_SEARCH_QUERY = "searchQuery";
    private static final String PREF_LAST_RESULT_ID = "lastResultId";
    private static final String PREF_IS_ALARM_ON = "isAlarmOn";
    private static final String PREF_SHOW_PHOTO_IN = "showPhotoIn";

    public static String getStoredQuery(Context contex) {
        return PreferenceManager.getDefaultSharedPreferences(contex).getString(PREF_SEARCH_QUERY, null);
    }

    public static void setStoredQuery(Context context, String query) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_SEARCH_QUERY, query)
                .apply();
    }

    public static String getLastResultId(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_LAST_RESULT_ID, null);
    }

    public static void setLastResultId(Context context, String lastResultId) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_LAST_RESULT_ID, lastResultId)
                .apply();
    }

    public static boolean isAlarmOn(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_IS_ALARM_ON, false);
    }

    public static void setAlarmOn(Context context, boolean isOn) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(PREF_IS_ALARM_ON, isOn)
                .apply();
    }

    public static void setShowPhotoIn(Context context, String showPhotoIn) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_SHOW_PHOTO_IN, showPhotoIn)
                .apply();
    }

    public static String getShowPhotoIn(Context contex) {
        return PreferenceManager.getDefaultSharedPreferences(contex).getString(PREF_SHOW_PHOTO_IN, null);
    }


}
