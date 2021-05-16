package ru.internetcloud.photogallery;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static androidx.core.content.ContextCompat.getSystemService;

public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "rustam";

    private RecyclerView photo_recycler_view;
    private List<GalleryItem> galleryItemList = new ArrayList<>();
    private PhotoAdapter photoAdapter;
    private ThumbnailDownloader<PhotoViewHolder> thumbnailDownloader;

    private String showPhotoIn;

    SearchView searchView = null;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true); // удерживаем фрагмент
        // причина удержания: чтобы поворот не приводил к многократному порождению новых объектов AsyncTask для загрузки данных JSON

        setHasOptionsMenu(true); // меню

        updateItems(); // cкачиваем JSON c адресами url, и в список (List) - одноразовый фоновый поток.

        Handler mainThreadHandler = new Handler();

        thumbnailDownloader = new ThumbnailDownloader<>(mainThreadHandler); // передаю в фоновый поток обработчик из главного потока.
        thumbnailDownloader.setThumbnailDownloadListener(new ThumbnailDownloader.ThumbnailDownloadListener<PhotoViewHolder>() {
            @Override
            public void onThumbnailDownloaded(PhotoViewHolder holder, Bitmap thumbnail) {
                Drawable drawable = new BitmapDrawable(getResources(), thumbnail);
                holder.bindDrawable(drawable); // правильную иконку вывожу.
            }
        });


        thumbnailDownloader.start();
        thumbnailDownloader.getLooper();

        Log.i(TAG, "Background thread started");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_photo_gallery, container, false);

        photo_recycler_view = view.findViewById(R.id.photo_recycler_view);
        photo_recycler_view.setLayoutManager(new GridLayoutManager(getActivity(), 3));

        updateUI();

        return view;
    }

    private void updateUI() {
 //       if (photoAdapter == null) {
            if (isAdded()) { // Return true if the fragment is currently added to its activity. (фрагмент был присоединен к активности, а следовательно, что
                // результат getActivity() будет отличен от null .
                photoAdapter = new PhotoAdapter(galleryItemList);
                photo_recycler_view.setAdapter(photoAdapter);
            }
//        } else { не работает, не обновляет список!
//            photoAdapter.setGalleryItemList(galleryItemList);
//            photoAdapter.notifyDataSetChanged();
//        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_photo_gallery_menu, menu);

        MenuItem toggleItem = menu.findItem(R.id.menu_item_toggle_polling);
        if (PollService.isServiceAlarmOn(getActivity())) {
            toggleItem.setTitle(R.string.stop_polling);
        } else {
            toggleItem.setTitle(R.string.start_polling);
        }

        MenuItem searchItem = menu.findItem(R.id.menu_item_search);

        searchView = (SearchView) searchItem.getActionView();

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                QueryPreferences.setStoredQuery(getActivity().getApplicationContext(), query);

                // скрыть клавиатуру: 2 способа
                // статья https://rmirabelle.medium.com/close-hide-the-soft-keyboard-in-android-db1da22b09d2
//                InputMethodManager inputMethodManager = (InputMethodManager) getActivity().getSystemService(Activity.INPUT_METHOD_SERVICE);
//                inputMethodManager.hideSoftInputFromWindow(searchView.getWindowToken(), 0);

                searchView.clearFocus(); // второй способ скрыть клавиатуру

                searchView.onActionViewCollapsed(); // скрывает SearchView

                updateItems();
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        searchView.setOnSearchClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String query = QueryPreferences.getStoredQuery(getActivity().getApplicationContext());
                searchView.setQuery(query, false);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_clear:
                QueryPreferences.setStoredQuery(getActivity().getApplicationContext(), null);
                if (searchView != null) {
                    searchView.onActionViewCollapsed(); // скрывает SearchView
                }
                updateItems();
                return true;

            case R.id.menu_item_toggle_polling:
                boolean shouldStartAlarm = !PollService.isServiceAlarmOn(getActivity());
                PollService.setServiceAlarm(getActivity(), shouldStartAlarm);
                getActivity().invalidateOptionsMenu();
                return true;

            case R.id.menu_item_settings:
                Intent intent = SettingsActivity.newIntent(getActivity());
                startActivity(intent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void updateItems() {
        String query = QueryPreferences.getStoredQuery(getActivity().getApplicationContext());

        String subtitle = "";
        if (query != null) {
            subtitle = "" + getResources().getString(R.string.search) + ": " + query;
        }

        AppCompatActivity currentActivity = (AppCompatActivity) getActivity();
        currentActivity.getSupportActionBar().setSubtitle(subtitle);


        new FetchItemsTask(query).execute(); // cкачиваем JSON c адресами url, и в список (List) - одноразовый фоновый поток.
    }

    @Override
    public void onResume() {
        super.onResume();

        showPhotoIn = QueryPreferences.getShowPhotoIn(getActivity().getApplicationContext());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        thumbnailDownloader.clearQueue();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        thumbnailDownloader.quit(); // обязательно надо завершать поток, иначе он никогда не умрет!
        Log.i(TAG, "Background thread destroyed");
    }

    // внутренний класс FetchItemsTask
    private class FetchItemsTask extends AsyncTask<Void, Void, List<GalleryItem>> { // важно! надо прописать List<GalleryItem>

        // третий параметр определяет тип результата, производимого AsyncTask ; он задает тип значения, возвращаемого doInBackground(…) , а также тип входного параметра onPostExecute(…)

        private String query;

        // конструктор:
        public FetchItemsTask(String query) {
            this.query = query;
        }

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {

            if (query == null) {
                return new FlickrFetchr().fetchRecentPhotos();
            } else {
                return new FlickrFetchr().searchPhotos(query);
            }
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            // из другого потока получаем данные и в главный поток их включаем.
            galleryItemList = items;
            updateUI();
        }
    }

    // внутренний класс PhotoViewHolder
    private class PhotoViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private GalleryItem galleryItem; // ???
        private ImageView item_image_view;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);

            item_image_view = itemView.findViewById(R.id.item_image_view);

            itemView.setOnClickListener(this);
        }

        private void bindDrawable(Drawable drawable) {
            item_image_view.setImageDrawable(drawable);
        }

        private void bind(GalleryItem item) {
            this.galleryItem = item;
        }

        @Override
        public void onClick(View v) {
            if (showPhotoIn == null ) {
                Toast.makeText(getActivity(), "default", Toast.LENGTH_SHORT).show();
            } else {
                switch (showPhotoIn) {
                    case "browser":
                        Intent intentBrowser = new Intent(Intent.ACTION_VIEW, galleryItem.getPhotoPageUri());
                        startActivity(intentBrowser);
                        return;

                    case "web view":
                        Intent intentWebView = PhotoPageActivity.newIntent(getActivity(), galleryItem.getPhotoPageUri());
                        startActivity(intentWebView);
                        return;

                    default:

                        return;
                }
            }
        }
    }

    // внутренний класс PhotoAdapter
    private class PhotoAdapter extends RecyclerView.Adapter<PhotoViewHolder> {

        private List<GalleryItem> galleryItemList;
        Drawable tempDrawable = getResources().getDrawable(R.drawable.temp_picture); // временное пустое изображение (квадратик), потом на его место реальная иконка загрузится.

        public PhotoAdapter(List<GalleryItem> galleryItemList) {
            this.galleryItemList = galleryItemList;
        }

        @NonNull
        @Override
        public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View itemView = layoutInflater.inflate(R.layout.photo_item, parent, false);
            return new PhotoViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {

            holder.bind(galleryItemList.get(position));
            holder.bindDrawable(tempDrawable); // временную картинку вывожу, белый квадратик с рамкой, пока не будет загружена иконка соответствующей фотографии.

            GalleryItem currentGalleryItem = galleryItemList.get(position);
            thumbnailDownloader.queueThumbnail(holder, currentGalleryItem.getIconUrl());


        }

        @Override
        public int getItemCount() {
            return galleryItemList.size();
        }

        public void setGalleryItemList(List<GalleryItem> galleryItemList) {
            this.galleryItemList = galleryItemList;
        }
    }
}
