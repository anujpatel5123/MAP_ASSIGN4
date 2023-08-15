package com.Assign4.stocks;

import androidx.annotation.NonNull;

final class Stock {
    final String name;
    final double shares;
    final String ticker;
    final double last;
    final double change;

    Stock(@NonNull final String name, double shares, String ticker, double last, double change) {
        this.name = name;
        this.shares = shares;
        this.ticker = ticker;
        this.last = last;
        this.change = Math.round(change * Math.pow(10, 2)) / Math.pow(10, 2);
    }
}
