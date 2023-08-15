package com.Assign4.stocks;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

import static android.content.Context.MODE_PRIVATE;

import com.Assign4.R;
import com.Assign4.stocks.Stock;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {

    private final Activity activity;
    private final boolean isFav;
    private final ArrayList<Stock> data;

    public RecyclerViewAdapter(ArrayList<Stock> data, Activity activity, boolean isFav) {
        this.data = data;
        this.activity = activity;
        this.isFav = isFav;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.cardview_row, parent, false);
        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Stock stock = data.get(position);
        holder.ticker.setText(stock.ticker);
        holder.last.setText(String.format(Locale.getDefault(), "%,.2f", stock.last));

        if (stock.shares == 0) {
            holder.shares.setText(stock.name);
        } else {
            holder.shares.setText(String.format(Locale.getDefault(), "%,.2f shares", stock.shares));
        }

        holder.change.setText(String.format(Locale.getDefault(), "%,.2f", stock.change));
        holder.diff.setVisibility(View.VISIBLE);
        if (stock.change > 0) {
            holder.diff.setImageDrawable(ContextCompat.getDrawable(activity.getApplicationContext(), R.drawable.ic_twotone_trending_up_24));
            holder.change.setTextColor(activity.getColor(R.color.green));
        } else if (stock.change == 0) {
            holder.diff.setVisibility(View.GONE);
            holder.change.setTextColor(activity.getColor(R.color.textgray));
        } else {
            holder.diff.setImageDrawable(ContextCompat.getDrawable(activity.getApplicationContext(), R.drawable.ic_baseline_trending_down_24));
            holder.change.setTextColor(activity.getColor(R.color.red));
            holder.change.setText(String.format(Locale.getDefault(), "%,.2f", Math.abs(stock.change)));
        }

        holder.rowView.setOnClickListener(v -> {
            activity.startActivityForResult(new Intent(activity.getApplicationContext(), Details.class).putExtra("tick", stock.ticker), 200);
        });
        holder.button.setOnClickListener(v -> {
            activity.startActivityForResult(new Intent(activity.getApplicationContext(), Details.class).putExtra("tick", stock.ticker), 200);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public void onRowMoved(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(data, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(data, i, i - 1);
            }
        }

        notifyItemMoved(fromPosition, toPosition);
    }

    public void onRowSelected(MyViewHolder myViewHolder) {
        myViewHolder.rowView.setBackgroundColor(activity.getColor(R.color.selected));
    }

    public void onRowClear(MyViewHolder myViewHolder) {
        myViewHolder.rowView.setBackgroundColor(activity.getColor(R.color.halfwhite));

        ArrayList<String> newOrder = new ArrayList<>();
        for (Stock stock : data) {
            newOrder.add(stock.ticker);
        }

        JSONArray jsonArray = new JSONArray(newOrder);

        SharedPreferences sharedPreferences = activity.getSharedPreferences("", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        if (isFav)
            editor.putString("favs", jsonArray.toString());
        else
            editor.putString("ports", jsonArray.toString());
        editor.apply();

    }


    public static class MyViewHolder extends RecyclerView.ViewHolder {

        private final TextView ticker, last, shares, change;
        ImageView diff;
        ImageButton button;
        View rowView;

        public MyViewHolder(View itemView) {
            super(itemView);
            rowView = itemView;
            ticker = itemView.findViewById(R.id.ticker);
            last = itemView.findViewById(R.id.last);
            shares = itemView.findViewById(R.id.shares);
            change = itemView.findViewById(R.id.change);
            diff = itemView.findViewById(R.id.diff);
            button = itemView.findViewById(R.id.button);
        }
    }
}

