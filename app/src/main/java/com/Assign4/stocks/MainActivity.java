package com.Assign4.stocks;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Assign4.R;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    ArrayList<Stock> stocks = new ArrayList<>();
    ArrayList<Stock> favStocks = new ArrayList<>();
    RecyclerViewAdapter portSectionAdapter;
    RecyclerViewAdapter favSectionAdapter;
    RequestQueue queue;
    Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_Assign4);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.tiingo).setOnClickListener(v -> {
            String url = "https://www.tiingo.com/";
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        });

        findViewById(R.id.progress).setVisibility(View.VISIBLE);
        findViewById(R.id.scrollview).setVisibility(View.GONE);

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        if (hasFocus)
            init();
        super.onWindowFocusChanged(hasFocus);
    }

    void init() {

        queue = Volley.newRequestQueue(this);

        portSectionAdapter = new RecyclerViewAdapter(stocks, this, false);
        favSectionAdapter = new RecyclerViewAdapter(favStocks, this, true);

        RecyclerView portRecyclerView = findViewById(R.id.recyclerview);
        portRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        portRecyclerView.setAdapter(portSectionAdapter);

        RecyclerView favRecyclerView = findViewById(R.id.recyclerview1);
        favRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        favRecyclerView.setAdapter(favSectionAdapter);

        CustomDividerItemDecorator dividerItemDecoration1 = new CustomDividerItemDecorator(ContextCompat.getDrawable(this, R.drawable.cursor));
        portRecyclerView.addItemDecoration(dividerItemDecoration1);
        favRecyclerView.addItemDecoration(dividerItemDecoration1);

        MovementCallback portMovementCallback = new MovementCallback(this, portSectionAdapter, false) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }
        };

        ItemTouchHelper itemTouchhelper = new ItemTouchHelper(portMovementCallback);
        itemTouchhelper.attachToRecyclerView(portRecyclerView);

        MovementCallback favMovementCallback = new MovementCallback(this, favSectionAdapter, true) {
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                final int position = viewHolder.getAdapterPosition();
                favStocks.remove(position);
                favSectionAdapter.notifyItemRemoved(position);
                JSONArray favs;
                SharedPreferences sharedPreferences = getSharedPreferences("", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                try {
                    favs = new JSONArray(sharedPreferences.getString("favs", "[]"));
                    favs.remove(position);
                    editor.putString("favs", favs.toString());
                    editor.apply();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };

        ItemTouchHelper itemTouchhelper1 = new ItemTouchHelper(favMovementCallback);
        itemTouchhelper1.attachToRecyclerView(favRecyclerView);

        fetchDetails(stocks, favStocks, portSectionAdapter, favSectionAdapter);

    }

    private void fetchDetails(ArrayList<Stock> stocks, ArrayList<Stock> favStocks, RecyclerViewAdapter portSectionAdapter, RecyclerViewAdapter favSectionAdapter) {
        Log.d("Update", "Calling fetch API");

        JSONObject portfolio = new JSONObject();
        JSONArray favs = new JSONArray();
        JSONArray ports = new JSONArray();
        SharedPreferences sharedPreferences = getSharedPreferences("", MODE_PRIVATE);

        if (sharedPreferences.contains("amount")) {
            try {
                portfolio = new JSONObject(sharedPreferences.getString("portfolio", "{}"));
                favs = new JSONArray(sharedPreferences.getString("favs", "[]"));
                ports = new JSONArray(sharedPreferences.getString("ports", "[]"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putFloat("amount", 20000);
            editor.apply();
        }

        Date current = Calendar.getInstance().getTime();
        DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.LONG);
        String format = dateFormat.format(current);
        ((TextView) findViewById(R.id.date)).setText(format);

        ((TextView) findViewById(R.id.amount)).setText(String.format(Locale.getDefault(), "%,.2f", sharedPreferences.getFloat("amount", 20000)));

        if (favs.length() == 0 && ports.length() == 0) {
            findViewById(R.id.progress).setVisibility(View.GONE);
            findViewById(R.id.scrollview).setVisibility(View.VISIBLE);
            return;
        }

        ArrayList<String> favorites = new ArrayList<>();
        ArrayList<String> portfolios = new ArrayList<>();

        StringBuilder tickConcat = new StringBuilder();

        for (int i = 0; i < favs.length(); i++) {
            String tick = null;
            try {
                tick = favs.getString(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            favorites.add(tick);
            tickConcat.append(tick).append(",");
        }

        for (int i = 0; i < ports.length(); i++) {
            String tick = null;
            try {
                tick = ports.getString(i);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            portfolios.add(tick);
            if (!favorites.contains(tick))
                tickConcat.append(tick).append(",");
        }

        tickConcat.deleteCharAt(tickConcat.length() - 1);

        ArrayList<Stock> newPorts = new ArrayList<>(ports.length());
        ArrayList<Stock> newFavs = new ArrayList<>(favs.length());

        JSONObject jsonObject = new JSONObject();

        String url = "https://api.tiingo.com/iex?tickers=" + tickConcat.toString() + "&token=b4e95182bcfae3fa31c6d095f26de94e3514a17b";
        JSONObject finalPortfolio = portfolio;
        JsonArrayRequest jsonArrayRequest1 = new JsonArrayRequest
                (Request.Method.GET, url, null, response -> {
                    Log.d("Response: ", response.toString());
                    try {

                        double totalInShares = 0;

                        for (int i = 0; i < response.length(); i++) {

                            JSONObject response1 = response.getJSONObject(i);

                            JSONObject jsonObject1 = new JSONObject();

                            double last = response1.getDouble("last");
                            double change = response1.getDouble("last") - response1.getDouble("prevClose");
                            String tick = response1.getString("ticker");

                            String name = finalPortfolio.getJSONObject(tick).getString("name");
                            float shares = 0;

                            if (finalPortfolio.has(tick))
                                if (finalPortfolio.getJSONObject(tick).has("shares"))
                                    shares = (float) finalPortfolio.getJSONObject(tick).getDouble("shares");

                            totalInShares += (shares * last);

                            jsonObject1.put("last", last);
                            jsonObject1.put("name", name);
                            jsonObject1.put("shares", shares);
                            jsonObject1.put("change", change);

                            jsonObject.put(tick, jsonObject1);

                        }

                        ((TextView) findViewById(R.id.amount)).setText(String.format(Locale.getDefault(), "%,.2f", sharedPreferences.getFloat("amount", 20000) + totalInShares));

                        for (String fav : favorites) {
                            JSONObject jsonObject2 = jsonObject.getJSONObject(fav);
                            if (portfolios.contains(fav)) {
                                newFavs.add(new Stock(jsonObject2.getString("name"), jsonObject2.getDouble("shares"), fav, jsonObject2.getDouble("last"), jsonObject2.getDouble("change")));
                            } else {
                                newFavs.add(new Stock(jsonObject2.getString("name"), 0, fav, jsonObject2.getDouble("last"), jsonObject2.getDouble("change")));
                            }
                        }

                        for (String port : portfolios) {
                            JSONObject jsonObject2 = jsonObject.getJSONObject(port);
                            newPorts.add(new Stock(jsonObject2.getString("name"), jsonObject2.getDouble("shares"), port, jsonObject2.getDouble("last"), jsonObject2.getDouble("change")));
                        }

                        stocks.clear();
                        stocks.addAll(newPorts);
                        portSectionAdapter.notifyDataSetChanged();
                        favStocks.clear();
                        favStocks.addAll(newFavs);
                        favSectionAdapter.notifyDataSetChanged();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    } finally {
                        findViewById(R.id.progress).setVisibility(View.GONE);
                        findViewById(R.id.scrollview).setVisibility(View.VISIBLE);
                    }

                }, error -> {
                    Log.d("Response: ", error.toString());
                    findViewById(R.id.progress).setVisibility(View.GONE);
                    findViewById(R.id.scrollview).setVisibility(View.VISIBLE);
                });

        queue.add(jsonArrayRequest1);


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.main, menu);

        MenuItem menuItem = menu.findItem(R.id.searchview);

        MenuItem menuItem1 = menu.findItem(R.id.refresh);
        menuItem1.setOnMenuItemClickListener(item -> {
            init();
            return true;
        });

        menuItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                return true;
            }
        });

        SearchView searchView = (SearchView) menuItem.getActionView();

        searchView.setOnSearchClickListener(v -> {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        });

        final SearchView.SearchAutoComplete searchAutoComplete = searchView.findViewById(androidx.appcompat.R.id.search_src_text);
        ArrayAdapter<String> newsAdapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_dropdown_item_1line, new ArrayList<>());
        searchAutoComplete.setAdapter(newsAdapter);

        searchAutoComplete.setOnItemClickListener((parent, view, position, id) -> {
            searchView.setQuery(newsAdapter.getItem(position), true);
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (!query.contains("-"))
                    return false;

                startActivityForResult(new Intent(getApplicationContext(), Details.class).putExtra("tick", query.split("-")[0].trim()), 100);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.length() < 3) {
                    newsAdapter.clear();
                    newsAdapter.notifyDataSetChanged();
                    searchAutoComplete.callOnClick();
                } else
                    runSearch(newText, newsAdapter, searchAutoComplete);
                return true;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    void runSearch(String query, ArrayAdapter<String> newsAdapter, SearchView.SearchAutoComplete searchAutoComplete) {
        String url = "https://api.tiingo.com/tiingo/utilities/search?token=b4e95182bcfae3fa31c6d095f26de94e3514a17b&query=" + query;

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, url, null, response -> {
                    ArrayList<String> newArr = new ArrayList<>();
                    for (int i = 0; i < response.length(); i++) {
                        try {
                            JSONObject object = (JSONObject) response.get(i);
                            String entry = object.getString("ticker") + " - " + object.getString("name");
                            if (!object.isNull("ticker") && !object.isNull("name") && newsAdapter.getPosition(entry) == -1)
                                newArr.add(entry);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    newsAdapter.addAll(newArr);
                    newsAdapter.notifyDataSetChanged();
                    searchAutoComplete.callOnClick();
                }, error -> {
                    Log.d("Response: ", error.toString());
                });

        queue.add(jsonArrayRequest);
    }

    @Override
    protected void onPause() {
        if (queue != null)
            queue.stop();
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (queue != null)
            queue.stop();
        if (handler != null)
            handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    static class CustomDividerItemDecorator extends RecyclerView.ItemDecoration {
        private Drawable mDivider;

        public CustomDividerItemDecorator(Drawable divider) {
            mDivider = divider;
        }

        @Override
        public void onDrawOver(Canvas canvas, RecyclerView parent, RecyclerView.State state) {
            int dividerLeft = parent.getPaddingLeft();
            int dividerRight = parent.getWidth() - parent.getPaddingRight();

            int childCount = parent.getChildCount();
            for (int i = 0; i <= childCount - 2; i++) {
                View child = parent.getChildAt(i);

                RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();

                int dividerTop = child.getBottom() + params.bottomMargin;
                int dividerBottom = dividerTop + mDivider.getIntrinsicHeight();

                mDivider.setBounds(dividerLeft, dividerTop, dividerRight, dividerBottom);
                mDivider.draw(canvas);
            }
        }
    }
}