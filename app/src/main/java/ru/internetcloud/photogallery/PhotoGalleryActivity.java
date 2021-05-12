package ru.internetcloud.photogallery;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import android.os.Bundle;

public class PhotoGalleryActivity extends TemplateFragmentActivity {

    @Override
    protected Fragment createFragment() {
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
}
