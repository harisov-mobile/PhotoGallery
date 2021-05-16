package ru.internetcloud.photogallery;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.fragment.app.Fragment;

public class SettingsActivity extends TemplateFragmentActivity {

    public static Intent newIntent(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);
        return intent;
    }

    @Override
    protected Fragment createFragment() {
        return SettingsFragment.newInstance();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
