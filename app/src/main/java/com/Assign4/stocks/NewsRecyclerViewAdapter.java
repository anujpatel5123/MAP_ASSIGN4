package com.Assign4.stocks;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.LruCache;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.Assign4.R;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.android.volley.toolbox.Volley;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

public class NewsRecyclerViewAdapter extends RecyclerView.Adapter<NewsRecyclerViewAdapter.NewsViewHolder> {

    private final Activity activity;
    private final ArrayList<News> data;
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private RequestQueue mRequestQueue;
    private ImageLoader mImageLoader;
    private long today;


    public NewsRecyclerViewAdapter(ArrayList<News> data, Activity activity) {
        this.data = data;
        this.activity = activity;
        mRequestQueue = Volley.newRequestQueue(activity);
        today = System.currentTimeMillis();
        mImageLoader = new ImageLoader(mRequestQueue, new ImageLoader.ImageCache() {
            private final LruCache<String, Bitmap> mCache = new LruCache<String, Bitmap>(10);

            public void putBitmap(String url, Bitmap bitmap) {
                mCache.put(url, bitmap);
            }

            public Bitmap getBitmap(String url) {
                return mCache.get(url);
            }
        });
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0)
            return 0;
        return 1;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;
        if (viewType == 0)
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.news_first, parent, false);
        else
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.news_second, parent, false);
        return new NewsViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(NewsViewHolder holder, int position) {
        News news = data.get(position);
        holder.title.setText(news.title);
        holder.source.setText(String.valueOf(news.source_name));
        try {
            Date date = format.parse(news.publishedAt);
            Instant instant = date.toInstant().minusMillis(today);
            long diff = Math.abs(instant.toEpochMilli());
            String d = "";
            if (diff >= 86400000) {
                //days
                long num = diff / 86400000;
                d = num + " days ago";
                if (num == 1) {
                    d = num + " day ago";
                }
            } else if (diff >= 3600000) {
                //hours
                long num = diff / 3600000;
                d = num + " hours ago";
                if (num == 1) {
                    d = num + " hour ago";
                }
            } else {
                //mins
                long num = diff / 60000;
                d = num + " minutes ago";
                if (num == 1) {
                    d = num + " minute ago";
                }
            }
            holder.date.setText(d);
        } catch (ParseException e) {
            holder.date.setText(news.publishedAt);
        }

        holder.img.setImageUrl(news.urlToImage, mImageLoader);
        holder.rowView.setOnLongClickListener(v -> {
            Dialog dialog = new Dialog(activity);
            dialog.setContentView(R.layout.news_popup);
            ((TextView) dialog.findViewById(R.id.news_heading)).setText(news.title);
            ((NetworkImageView) dialog.findViewById(R.id.news_image)).setImageUrl(news.urlToImage, mImageLoader);
            dialog.findViewById(R.id.twitter_share).setOnClickListener(v1 -> {
                String url = "https://twitter.com/intent/tweet?text=" + "Check out this Link:" + "&url=" + news.url + "&hashtags=CSCI571StockApp";
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                activity.startActivity(i);
            });
            dialog.findViewById(R.id.chrome_share).setOnClickListener(v1 -> {
                String url = news.url;
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(url));
                activity.startActivity(i);
            });
            dialog.show();
            return true;
        });
        holder.rowView.setOnClickListener(v -> {
            String url = news.url;
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            activity.startActivity(i);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class NewsViewHolder extends RecyclerView.ViewHolder {

        private final TextView title, date, source;
        NetworkImageView img;
        View rowView;

        public NewsViewHolder(View itemView) {
            super(itemView);
            rowView = itemView;
            source = itemView.findViewById(R.id.textView1);
            date = itemView.findViewById(R.id.textView2);
            title = itemView.findViewById(R.id.textView3);
            img = itemView.findViewById(R.id.imageView);
        }
    }

}

