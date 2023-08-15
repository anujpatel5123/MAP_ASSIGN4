package com.Assign4.stocks;

import androidx.annotation.NonNull;

public class News {
    final String title;
    final String source_name;
    final String publishedAt;
    final String urlToImage;
    final String url;

    News(@NonNull final String title, String source_name, String publishedAt, String urlToImage, String url) {
        this.title = title;
        this.source_name = source_name;
        this.publishedAt = publishedAt;
        this.urlToImage = urlToImage;
        this.url = url;
    }
}
