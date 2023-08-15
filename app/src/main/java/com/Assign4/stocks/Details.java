package com.Assign4.stocks;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.Assign4.R;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class Details extends AppCompatActivity {


    boolean isFavourite = false;
    boolean changesMade = false;
    ArrayList<String> favorites, portfolios;
    String tick;
    double last = 0;
    double shares = 0;
    float amount = 20000;
    int total_requests = 3;
    int num_requests_done = 0;
    RequestQueue queue;
    private String company;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_details);
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        queue = Volley.newRequestQueue(this);
        findViewById(R.id.progress).setVisibility(View.VISIBLE);
        findViewById(R.id.scrollview).setVisibility(View.GONE);
        tick = getIntent().getStringExtra("tick");
        NewsRecyclerViewAdapter sectionAdapter = null;
        ArrayList<News> newsArrayList = null;

        newsArrayList = new ArrayList<>();
        sectionAdapter = new NewsRecyclerViewAdapter(newsArrayList, this);
        RecyclerView recyclerView = findViewById(R.id.news_layout);
        recyclerView.setNestedScrollingEnabled(false);
        recyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
        recyclerView.setAdapter(sectionAdapter);

        fetchDetails(tick, sectionAdapter, newsArrayList);
        findViewById(R.id.show_more).setOnClickListener(v -> {
            if (((TextView) findViewById(R.id.description)).getMaxLines() == 2) {
                ((TextView) findViewById(R.id.description)).setMaxLines(Integer.MAX_VALUE);
                ((TextView) findViewById(R.id.show_more)).setText("Show less");
            } else {
                ((TextView) findViewById(R.id.description)).setMaxLines(2);
                ((TextView) findViewById(R.id.show_more)).setText("Show more...");
            }
        });
        SharedPreferences sharedPreferences = getSharedPreferences("", MODE_PRIVATE);

        amount = sharedPreferences.getFloat("amount", 20000);

        findViewById(R.id.trade).setOnClickListener(v -> {
            Dialog dialog = new Dialog(this);
            dialog.setContentView(R.layout.trade_dialog);
            ((TextView) dialog.findViewById(R.id.textView1)).setText("Trade " + company + " shares");
            ((TextView) dialog.findViewById(R.id.available)).setText(String.format(Locale.getDefault(), "$%,.2f available to buy " + tick, amount));

            EditText editText = dialog.findViewById(R.id.editTextNumber);

            final float[] tempShares = {0};
            final float[] total = {(float) (tempShares[0] * last)};

            editText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String value = s.toString().trim();
                    float num;
                    try {
                        num = Float.parseFloat(value);
                    } catch (NumberFormatException e) {
                        num = 0;
                    }
                    if (num <= 0) {
                        num = 0;
                    }
                    tempShares[0] = num;
                    total[0] = (float) (tempShares[0] * last);
                    ((TextView) dialog.findViewById(R.id.calc)).setText(String.format(Locale.getDefault(), "%,.2f x $%,.2f/share = $%,.2f", tempShares[0], last, total[0]));
                }

                @Override
                public void afterTextChanged(Editable s) {
                }
            });

            ((TextView) dialog.findViewById(R.id.calc)).setText(String.format(Locale.getDefault(), "%,.2f x $%,.2f/share = $%,.2f", tempShares[0], last, total[0]));

            dialog.findViewById(R.id.buy).setOnClickListener(v1 -> {

                String value = editText.getText().toString().trim();
                try {
                    float num = Float.parseFloat(value);
                    if (num <= 0) {
                        Toast.makeText(this, "Cannot buy less than 0 shares", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (amount < total[0]) {
                        Toast.makeText(this, "Not enough money to buy", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter valid amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                amount = amount - total[0];
                sharedPreferences.edit().putFloat("amount", amount).apply();

                shares = shares + tempShares[0];
                if (!portfolios.contains(tick)) {
                    portfolios.add(tick);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("ports", portfolios.toString());
                    editor.apply();
                }
                SharedPreferences.Editor editor = sharedPreferences.edit();
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("name", company);
                    jsonObject.put("shares", shares);
                    JSONObject jsonObject1 = new JSONObject(sharedPreferences.getString("portfolio", "{}"));
                    jsonObject1.put(tick, jsonObject);
                    editor.putString("portfolio", jsonObject1.toString());
                    editor.apply();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Dialog dialog1 = new Dialog(this, R.style.DialogTheme);
                dialog1.setContentView(R.layout.trade_congrats_dialog);
                ((TextView) dialog1.findViewById(R.id.textView1)).setText("You have successfully bought " + editText.getText() + " shares of " + tick);
                dialog1.findViewById(R.id.done).setOnClickListener(v11 -> dialog1.dismiss());
                dialog.dismiss();
                updateViews();
                dialog1.show();
            });

            dialog.findViewById(R.id.sell).setOnClickListener(v1 -> {

                String value = editText.getText().toString().trim();
                try {
                    float num = Float.parseFloat(value);
                    if (num <= 0) {
                        Toast.makeText(this, "Cannot sell less than 0 shares", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (tempShares[0] > shares) {
                        Toast.makeText(this, "Not enough shares to sell", Toast.LENGTH_SHORT).show();
                        return;
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Please enter valid amount", Toast.LENGTH_SHORT).show();
                    return;
                }

                amount = amount + total[0];
                sharedPreferences.edit().putFloat("amount", amount).apply();

                shares = shares - tempShares[0];
                if (shares < 0.01) {
                    shares = 0;
                    if (portfolios.contains(tick)) {
                        portfolios.remove(tick);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("ports", portfolios.toString());
                        editor.apply();
                    }
                }
                SharedPreferences.Editor editor = sharedPreferences.edit();
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("name", company);
                    jsonObject.put("shares", shares);
                    JSONObject jsonObject1 = new JSONObject(sharedPreferences.getString("portfolio", "{}"));
                    jsonObject1.put(tick, jsonObject);
                    editor.putString("portfolio", jsonObject1.toString());
                    editor.apply();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                Dialog dialog1 = new Dialog(this);
                dialog1.setContentView(R.layout.trade_congrats_dialog);
                ((TextView) dialog1.findViewById(R.id.textView1)).setText("You have successfully sold " + editText.getText() + " shares of " + tick);
                dialog1.findViewById(R.id.done).setOnClickListener(v11 -> {
                    dialog1.dismiss();
                });
                dialog.dismiss();
                updateViews();
                dialog1.show();
            });
            dialog.show();
        });

        JSONArray favs, ports;
        favorites = new ArrayList<>();
        portfolios = new ArrayList<>();

        try {
            favs = new JSONArray(sharedPreferences.getString("favs", "[]"));
            for (int i = 0; i < favs.length(); i++) {
                String tick1 = favs.getString(i);
                favorites.add(tick1);
            }
            ports = new JSONArray(sharedPreferences.getString("ports", "[]"));
            for (int i = 0; i < ports.length(); i++) {
                String tick1 = ports.getString(i);
                portfolios.add(tick1);
            }

            if (favorites.contains(tick))
                isFavourite = true;
            invalidateOptionsMenu();
        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void updateViews() {
        JSONObject jsonObject;
        try {
            jsonObject = new JSONObject(getSharedPreferences("", MODE_PRIVATE).getString("portfolio", "{}"));
            shares = jsonObject.getJSONObject(tick).getDouble("shares");
        } catch (JSONException e) {

        }

        if (shares == 0) {
            ((TextView) findViewById(R.id.shares)).setText("You have " + 0 + " shares of " + tick + ".");
            ((TextView) findViewById(R.id.value)).setText("Start trading!");
        } else {
            ((TextView) findViewById(R.id.shares)).setText(String.format(Locale.getDefault(), "Shares owned:%,.4f", shares));
            ((TextView) findViewById(R.id.value)).setText(String.format(Locale.getDefault(), "Market value: $%,.4f", shares * last));
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.favourite_button);
        if (!isFavourite) {
            item.setIcon(R.drawable.ic_baseline_star_border_24);
        } else {
            item.setIcon(R.drawable.ic_baseline_star_24);
        }
        if (num_requests_done == total_requests) {
            item.setVisible(true);
        } else {
            item.setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getMenuInflater().inflate(R.menu.details, menu);

        MenuItem menuItem = menu.findItem(R.id.favourite_button);
        menuItem.setIcon(R.drawable.ic_baseline_star_border_24);
        if (num_requests_done == total_requests) {
            menuItem.setVisible(true);
        } else {
            menuItem.setVisible(false);
        }
        menuItem.setOnMenuItemClickListener(item -> {
            isFavourite = !isFavourite;

            if (isFavourite) {
                favorites.add(tick);
                Toast.makeText(this, "'" + tick + "' was added to favorites", Toast.LENGTH_SHORT).show();
            } else {
                favorites.remove(tick);
                Toast.makeText(this, "'" + tick + "' was removed from favorites", Toast.LENGTH_SHORT).show();
            }

            JSONArray favs = new JSONArray(favorites);

            SharedPreferences sharedPreferences = getSharedPreferences("", MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("favs", favs.toString());

            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("name", company);
                jsonObject.put("shares", shares);
                JSONObject jsonObject1 = new JSONObject(sharedPreferences.getString("portfolio", "{}"));
                jsonObject1.put(tick, jsonObject);
                editor.putString("portfolio", jsonObject1.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            editor.apply();

            invalidateOptionsMenu();

            return true;
        });

        return super.onCreateOptionsMenu(menu);
    }

    void fetchDetails(String query, NewsRecyclerViewAdapter sectionAdapter, ArrayList<News> newsArrayList) {
        String url = "https://api.tiingo.com/tiingo/daily/" + query + "?token=b4e95182bcfae3fa31c6d095f26de94e3514a17b";
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, response -> {
                    try {
                        ((TextView) findViewById(R.id.ticker)).setText(response.getString("ticker"));
                        company = response.getString("name");
                        ((TextView) findViewById(R.id.company)).setText(response.getString("name"));
                        ((TextView) findViewById(R.id.description)).getViewTreeObserver().addOnGlobalLayoutListener((ViewTreeObserver.OnGlobalLayoutListener) new ViewTreeObserver.OnGlobalLayoutListener() {
                            @Override
                            public void onGlobalLayout() {
                                if (((TextView) Details.this.findViewById(R.id.description)).getLineCount() > 2) {
                                    ((TextView) Details.this.findViewById(R.id.description)).setMaxLines(2);
                                    Details.this.findViewById(R.id.show_more).setVisibility(View.VISIBLE);
                                    ((TextView) Details.this.findViewById(R.id.description)).getViewTreeObserver().removeOnGlobalLayoutListener(this);
                                }
                            }
                        });
                        ((TextView) findViewById(R.id.description)).setText(response.getString("description"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Stock does not exist", Toast.LENGTH_SHORT).show();
                        finish();
                    } finally {
                        num_requests_done++;
                        if (num_requests_done == total_requests) {
                            findViewById(R.id.progress).setVisibility(View.GONE);
                            findViewById(R.id.scrollview).setVisibility(View.VISIBLE);
                            invalidateOptionsMenu();
                        }
                    }

                }, error -> {
                    Toast.makeText(this, "Stock does not exist", Toast.LENGTH_SHORT).show();
                    finish();
                    Log.d("Response: ", error.toString());
                    num_requests_done++;
                    if (num_requests_done == total_requests) {
                        findViewById(R.id.progress).setVisibility(View.GONE);
                        findViewById(R.id.scrollview).setVisibility(View.VISIBLE);
                        invalidateOptionsMenu();
                    }
                });
        url = "https://api.tiingo.com/iex?tickers=" + query + "&token=b4e95182bcfae3fa31c6d095f26de94e3514a17b";
        JsonArrayRequest jsonArrayRequest1 = new JsonArrayRequest
                (Request.Method.GET, url, null, response -> {
//[{"timestamp":"2020-11-25T21:00:00+00:00","bidSize":null,"lastSaleTimestamp":"2020-11-25T21:00:00+00:00","low":3140.26,"bidPrice":null,"prevClose":3118.06,"quoteTimestamp":"2020-11-25T21:00:00+00:00","last":3185.07,"askSize":null,"volume":3790403,"lastSize":null,"ticker":"AMZN","high":3198,"mid":null,"askPrice":null,"open":3141.87,"tngoLast":3185.07}]
                    try {
                        JSONObject response1 = response.getJSONObject(0);

                        double change = 0, low = 0, bidPrice = 0, open = 0, mid = 0, high = 0, volume = 0;

                        if (!response1.isNull("last"))
                            last = response1.getDouble("last");
                        if (!response1.isNull("low"))
                            low = response1.getDouble("low");
                        if (!response1.isNull("bidPrice"))
                            bidPrice = response1.getDouble("bidPrice");
                        if (!response1.isNull("open"))
                            open = response1.getDouble("open");
                        if (!response1.isNull("mid"))
                            mid = response1.getDouble("mid");
                        if (!response1.isNull("high"))
                            high = response1.getDouble("high");
                        if (!response1.isNull("volume"))
                            volume = response1.getDouble("volume");

                        last = Math.round(last * Math.pow(10, 2)) / Math.pow(10, 2);
                        low = Math.round(low * Math.pow(10, 2)) / Math.pow(10, 2);
                        bidPrice = Math.round(bidPrice * Math.pow(10, 2)) / Math.pow(10, 2);
                        open = Math.round(open * Math.pow(10, 2)) / Math.pow(10, 2);
                        mid = Math.round(mid * Math.pow(10, 2)) / Math.pow(10, 2);
                        high = Math.round(high * Math.pow(10, 2)) / Math.pow(10, 2);


                        ((TextView) findViewById(R.id.last)).setText(String.format(Locale.getDefault(), "$%,.2f", last));


                        if (response1.has("prevClose"))
                            change = response1.getDouble("last") - response1.getDouble("prevClose");

                        change = Math.round(change * Math.pow(10, 2)) / Math.pow(10, 2);

                        if (change > 0) {
                            ((TextView) findViewById(R.id.change)).setTextColor(getColor(R.color.green));
                            ((TextView) findViewById(R.id.change)).setText(String.format(Locale.getDefault(), "%,.2f", change));
                        } else if (change < 0) {
                            ((TextView) findViewById(R.id.change)).setTextColor(getColor(R.color.red));
                            ((TextView) findViewById(R.id.change)).setText(String.format(Locale.getDefault(), "-$%,.2f", Math.abs(change)));
                        } else {
                            ((TextView) findViewById(R.id.change)).setTextColor(getColor(R.color.gray));
                            ((TextView) findViewById(R.id.change)).setText(String.format(Locale.getDefault(), "%,.2f", change));
                        }

                        ArrayList<String> arrayList = new ArrayList<>();
                        arrayList.add(String.format(Locale.getDefault(), "Current Price:%,.2f", last));
                        arrayList.add(String.format(Locale.getDefault(), "Low:%,.2f", low));
                        arrayList.add(String.format(Locale.getDefault(), "Bid Price:%,.2f", bidPrice));
                        arrayList.add(String.format(Locale.getDefault(), "Open Price:%,.2f", open));
                        arrayList.add(String.format(Locale.getDefault(), "Mid:%,.2f", mid));
                        arrayList.add(String.format(Locale.getDefault(), "High:%,.2f", high));
                        arrayList.add(String.format(Locale.getDefault(), "Volume: %,.0f", volume));

                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.simple_item, arrayList);

                        ((GridView) findViewById(R.id.stats)).setAdapter(adapter);

                        updateViews();

                    } catch (JSONException e) {
                        e.printStackTrace();
                    } finally {
                        num_requests_done++;
                        if (num_requests_done == total_requests) {
                            findViewById(R.id.progress).setVisibility(View.GONE);
                            findViewById(R.id.scrollview).setVisibility(View.VISIBLE);
                            invalidateOptionsMenu();
                        }
                    }

                }, error -> {
                    Log.d("Response: ", error.toString());
                    num_requests_done++;
                    if (num_requests_done == total_requests) {
                        findViewById(R.id.progress).setVisibility(View.GONE);
                        findViewById(R.id.scrollview).setVisibility(View.VISIBLE);
                        invalidateOptionsMenu();
                    }
                });

        queue.add(jsonObjectRequest);
        queue.add(jsonArrayRequest1);
        fetchNews(tick, sectionAdapter, newsArrayList);
    }

    void fetchNews(String query, NewsRecyclerViewAdapter sectionAdapter, ArrayList<News> newsArrayList) {
        String url = "http://newsapi.org/v2/everything?apiKey=f3346f1e651545cb8b4d2a1320357fd4&q=" + query;

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.GET, url, null, response -> {
                    try {
                        JSONArray jsonArray = response.getJSONArray("articles");
                        ArrayList<News> arrayList = new ArrayList<>();
                        for (int i = 0; i < jsonArray.length(); i++) {

                            JSONObject object = jsonArray.getJSONObject(i);
                            String title = object.getString("title");
                            String imgUrl = object.getString("urlToImage");
                            String newsUrl = object.getString("url");
                            String date = object.getString("publishedAt");
                            String src = object.getJSONObject("source").getString("name");

                            if (!object.isNull("title") && !object.isNull("urlToImage")) {
                                arrayList.add(new News(title, src, date, imgUrl, newsUrl));
                            }

                        }
                        newsArrayList.clear();
                        newsArrayList.addAll(arrayList);
                        sectionAdapter.notifyDataSetChanged();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } finally {
                        num_requests_done++;
                        if (num_requests_done == total_requests) {
                            findViewById(R.id.progress).setVisibility(View.GONE);
                            findViewById(R.id.scrollview).setVisibility(View.VISIBLE);
                            invalidateOptionsMenu();
                        }
                    }
                }, error -> {
                    error.printStackTrace();
                    num_requests_done++;
                    if (num_requests_done == total_requests) {
                        findViewById(R.id.progress).setVisibility(View.GONE);
                        findViewById(R.id.scrollview).setVisibility(View.VISIBLE);
                        invalidateOptionsMenu();
                    }
                }){
            @Override
            public Map<String, String> getHeaders() throws AuthFailureError {
                Map<String, String>  params = new HashMap<String, String>();
                params.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.88 Safari/537.36");
                return params;
            }
        };

        queue.add(jsonObjectRequest);

        fetchChart(tick);
    }

    void fetchChart(String query) {
        Date d = new Date();
        int year = d.getYear();
        int month = d.getMonth();
        int day = d.getDate();

        Date fulldate = new Date(year - 2, month, day);

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String toDate = dateFormat.format(fulldate);

        String url = "https://api.tiingo.com/tiingo/daily/" + query + "/prices?startDate=" + toDate + "&columns=open,high,low,close,volume&resampleFreq=daily&token=b4e95182bcfae3fa31c6d095f26de94e3514a17b";

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest
                (Request.Method.GET, url, null, response -> {
                    WebView webview;
                    webview = findViewById(R.id.chart_view);
                    WebSettings settings = webview.getSettings();
                    settings.setJavaScriptEnabled(true);
                    webview.clearCache(true);
                    settings.setDomStorageEnabled(true);
                    webview.setWebViewClient(new customWeb(query, response));
                    webview.loadUrl("file:///android_asset/chart.html");
                    webview.setBackgroundColor(Color.parseColor("#FBF9FC"));
                }, error -> {
                    Log.d("Response: ", error.toString());
                });

        queue.add(jsonArrayRequest);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            if (changesMade)
                setResult(RESULT_OK);
            else
                setResult(RESULT_CANCELED);
            finish();
//            super.onBackPressed();
        }

        return true;
    }

    @Override
    public void onBackPressed() {
        if (changesMade)
            setResult(RESULT_OK);
        else
            setResult(RESULT_CANCELED);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (queue != null)
            queue.stop();
        super.onDestroy();
    }

    static public class MyGridView extends GridView {

        public MyGridView(Context context, AttributeSet attrs) {
            super(context, attrs);
        }

        public MyGridView(Context context) {
            super(context);
        }

        public MyGridView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
        }

        @Override
        public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            int expandSpec = MeasureSpec.makeMeasureSpec(1073741823,
                    MeasureSpec.AT_MOST);
            super.onMeasure(widthMeasureSpec, expandSpec);
        }
    }

    static class customWeb extends WebViewClient {

        String text, query;

        public customWeb(String query, JSONArray response) {
            text = response.toString();
            this.query = query;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            view.loadUrl("javascript:setTick('" + query + "')");
            view.loadUrl("javascript:loadChart(" + text + ")");
        }
    }
}