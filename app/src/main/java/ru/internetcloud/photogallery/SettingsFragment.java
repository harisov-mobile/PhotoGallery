package ru.internetcloud.photogallery;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private Spinner show_photo_in_spinner;

    public static SettingsFragment newInstance() {
        return new SettingsFragment();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        show_photo_in_spinner = view.findViewById(R.id.show_photo_in_spinner);

        String showPhotoIn = QueryPreferences.getShowPhotoIn(getActivity().getApplicationContext());
        int selectionIndex = 0;
        if (showPhotoIn != null) {
            switch (showPhotoIn) {
                case "browser":
                    selectionIndex = 0;
                    break;

                case "web view":
                    selectionIndex = 1;
                    break;

                default:
                    break;
            }
        }

        show_photo_in_spinner.setSelection(selectionIndex);

        return view;
    }

    private void saveSettings() {
        QueryPreferences.setShowPhotoIn(getActivity().getApplicationContext(), show_photo_in_spinner.getSelectedItem().toString());
    }

    @Override
    public void onPause() {
        super.onPause();

        saveSettings();
    }
}
