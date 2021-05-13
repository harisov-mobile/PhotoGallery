package ru.internetcloud.photogallery;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class PhotoGalleryFragment extends Fragment {

    private static final String TAG = "rustam";

    private RecyclerView photo_recycler_view;
    private List<GalleryItem> galleryItemList = new ArrayList<>();
    private PhotoAdapter photoAdapter;
    private ThumbnailDownloader<PhotoViewHolder> thumbnailDownloader;

    public static PhotoGalleryFragment newInstance() {
        return new PhotoGalleryFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true); // удерживаем фрагмент
        // причина удержания: чтобы поворот не приводил к многократному порождению новых объектов AsyncTask для загрузки данных JSON

        new FetchItemsTask().execute(); // cкачиваем JSON c адресами url, и в список (List)

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

        @Override
        protected List<GalleryItem> doInBackground(Void... params) {
            return new FlickrFetchr().fetchItems();
        }

        @Override
        protected void onPostExecute(List<GalleryItem> items) {
            // из другого потока получаем данные и в главный поток их включаем.
            galleryItemList = items;
            updateUI();
        }
    }

    // внутренний класс PhotoViewHolder
    private class PhotoViewHolder extends RecyclerView.ViewHolder {

        private GalleryItem galleryItem; // ???
        //private TextView name_text_view;
        private ImageView item_image_view;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);

            item_image_view = itemView.findViewById(R.id.item_image_view);
        }

        private void bindDrawable(Drawable drawable) {
            item_image_view.setImageDrawable(drawable);
        }

        private void bind(GalleryItem item) {
            //this.galleryItem = item;
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

            holder.bindDrawable(tempDrawable); // временную картинку вывожу, белый квадратик с рамкой, пока не будет загружена иконка соответствующей фотографии.

            GalleryItem currentGalleryItem = galleryItemList.get(position);
            thumbnailDownloader.queueThumbnail(holder, currentGalleryItem.getIconUrl());

            //holder.bind(galleryItems.get(position));
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
