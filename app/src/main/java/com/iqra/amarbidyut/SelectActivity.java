package com.iqra.amarbidyut;

import static android.view.Gravity.apply;
import static android.view.View.GONE;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class SelectActivity extends AppCompatActivity {

    EditText etSearch;
    ListView lvResult;
    CardView cvSubstation, cvFeeder, cvArea, cvDoneBtn;
    TextView tvSubstation, tvFeeder, tvArea;
    TextView tvStep1, tvStep2, tvStep3;

    String selSub = "", selFeed = "", selArea = "";

    //==============================================

    ArrayList<HashMap<String, String>> substation_arraylist = new ArrayList<>();
    ArrayList<HashMap<String, String>> feeder_arraylist = new ArrayList<>();
    ArrayList<HashMap<String, String>> area_arraylist = new ArrayList<>();
    ArrayList<HashMap<String, String>> search_arraylist = new ArrayList<>();
    HashMap<String, String> hashMap;

    String sub_id, feeder_id, area_id;

    // ── Search debounce ──
    Handler searchHandler = new Handler(Looper.getMainLooper());
    Runnable searchRunnable;
    RequestQueue requestQueue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select);

        // আগে setup হয়েছে? তাহলে সরাসরি Home যাও
        SharedPreferences prefs = getSharedPreferences(
                "amarbidyut", MODE_PRIVATE);
        if (prefs.getBoolean("setup_done", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        etSearch     = findViewById(R.id.etSearch);
        lvResult     = findViewById(R.id.lvSearchResult);
        cvSubstation = findViewById(R.id.cvSubstation);
        cvFeeder     = findViewById(R.id.cvFeeder);
        cvArea       = findViewById(R.id.cvArea);
        cvDoneBtn    = findViewById(R.id.cvDoneBtn);
        tvSubstation = findViewById(R.id.tvSubstation);
        tvFeeder     = findViewById(R.id.tvFeeder);
        tvArea       = findViewById(R.id.tvArea);
        tvStep1      = findViewById(R.id.tvStep1);
        tvStep2      = findViewById(R.id.tvStep2);
        tvStep3      = findViewById(R.id.tvStep3);

        cvDoneBtn.setAlpha(0.4f);
        cvDoneBtn.setEnabled(false);

        requestQueue = Volley.newRequestQueue(SelectActivity.this);

        // ── Search (live, debounced) ──
        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) {}
            public void afterTextChanged(Editable s) {
                String q = s.toString().trim();

                // আগের pending search cancel
                if (searchRunnable != null) searchHandler.removeCallbacks(searchRunnable);

                if (q.isEmpty()) {
                    lvResult.setVisibility(View.GONE);
                    return;
                }

                // 350ms পর সার্চ করবে (typing থামলে), অতিরিক্ত API call কমানোর জন্য
                searchRunnable = () -> searchArea(q);
                searchHandler.postDelayed(searchRunnable, 350);
            }
        });

        cvSubstation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                substation_arraylist.clear();

                String url = "https://dainikbhorerbarta.com/bidyut_apps/get_substation.php";

                JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray jsonArray) {

                        for (int x = 0; jsonArray.length() > x; x++) {

                            try {
                                JSONObject jsonObject = jsonArray.getJSONObject(x);

                                String id = jsonObject.getString("id");
                                String name = jsonObject.getString("name");

                                hashMap = new HashMap<>();
                                hashMap.put("id", id);
                                hashMap.put("name", name);
                                substation_arraylist.add(hashMap);


                            } catch (JSONException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        showBottomSheet("sub");
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                    }
                });

                jsonArrayRequest.setShouldCache(false);
                requestQueue.add(jsonArrayRequest);
            }
        });

        cvFeeder.setOnClickListener(v -> {

            feeder_arraylist.clear();

            String url = "https://dainikbhorerbarta.com/bidyut_apps/get_feeder.php?substation_id=" + sub_id;

            JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray jsonArray) {

                    for (int x = 0; jsonArray.length() > x; x++) {

                        try {
                            JSONObject jsonObject = jsonArray.getJSONObject(x);

                            String id = jsonObject.getString("id");
                            String name = jsonObject.getString("name");

                            hashMap = new HashMap<>();
                            hashMap.put("id", id);
                            hashMap.put("name", name);
                            feeder_arraylist.add(hashMap);

                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    if (!selSub.isEmpty()) showBottomSheet("feed");
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                }
            });

            jsonArrayRequest.setShouldCache(false);
            requestQueue.add(jsonArrayRequest);
        });

        cvArea.setOnClickListener(v -> {

            area_arraylist.clear();

            String url = "https://dainikbhorerbarta.com/bidyut_apps/get_areas.php?feeder_id=" + feeder_id;

            JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
                @Override
                public void onResponse(JSONArray jsonArray) {

                    for (int x = 0; jsonArray.length() > x; x++) {

                        try {
                            JSONObject jsonObject = jsonArray.getJSONObject(x);

                            String id = jsonObject.getString("id");
                            String name = jsonObject.getString("name");

                            hashMap = new HashMap<>();
                            hashMap.put("id", id);
                            hashMap.put("name", name);
                            area_arraylist.add(hashMap);

                        } catch (JSONException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    if (!selFeed.isEmpty()) showBottomSheet("area");
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                }
            });

            jsonArrayRequest.setShouldCache(false);
            requestQueue.add(jsonArrayRequest);
        });

        cvDoneBtn.setOnClickListener(v -> {

            prefs.edit()
                    .putString("sub", selSub)
                    .putString("feed", selFeed)
                    .putString("area", selArea)
                    .putBoolean("setup_done", true)
                    .putString("sub_id", sub_id)
                    .putString("feeder_id", feeder_id)
                    .putString("area_id", area_id)
                    .apply();

            startActivity(new Intent(this, MainActivity.class));
            finish();
        });
    }

    // ════════════════════════════════════════════
    // ── LIVE AREA SEARCH (API) ──
    // এলাকার নাম লিখলে এখান থেকে সাবস্টেশন + ফিডার + এলাকা
    // সবগুলোর id ও name একসাথে নিয়ে আসে।
    // ════════════════════════════════════════════
    private void searchArea(String q) {

        search_arraylist.clear();

        String url = "https://dainikbhorerbarta.com/bidyut_apps/search_area.php?q=" + Uri.encode(q);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, url, null, new Response.Listener<JSONArray>() {
            @Override
            public void onResponse(JSONArray jsonArray) {

                for (int x = 0; jsonArray.length() > x; x++) {

                    try {
                        JSONObject o = jsonArray.getJSONObject(x);

                        hashMap = new HashMap<>();
                        hashMap.put("area_id",    o.getString("area_id"));
                        hashMap.put("area_name",  o.getString("area_name"));
                        hashMap.put("feeder_id",   o.getString("feeder_id"));
                        hashMap.put("feeder_name", o.getString("feeder_name"));
                        hashMap.put("sub_id",      o.getString("sub_id"));
                        hashMap.put("sub_name",    o.getString("sub_name"));
                        search_arraylist.add(hashMap);

                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                }

                showSearchResults();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                lvResult.setVisibility(View.GONE);
            }
        });

        jsonArrayRequest.setShouldCache(false);
        requestQueue.add(jsonArrayRequest);
    }

    // ── Search result list দেখানো ──
    private void showSearchResults() {

        if (search_arraylist.isEmpty()) {
            lvResult.setVisibility(View.GONE);
            return;
        }

        ArrayAdapter<HashMap<String, String>> adapter = new ArrayAdapter<HashMap<String, String>>(
                this, R.layout.item_search_result, search_arraylist) {
            @Override
            public View getView(int pos, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext())
                            .inflate(R.layout.item_search_result, parent, false);
                }
                HashMap<String, String> row = search_arraylist.get(pos);
                ((TextView) convertView.findViewById(R.id.tvAreaName))
                        .setText(row.get("area_name"));
                ((TextView) convertView.findViewById(R.id.tvAreaPath))
                        .setText(row.get("sub_name") + " সাবস্টেশন → " + row.get("feeder_name") + " ফিডার");
                return convertView;
            }
        };

        lvResult.setAdapter(adapter);
        lvResult.setVisibility(View.VISIBLE);

        lvResult.setOnItemClickListener((p, v, pos, id) -> {
            HashMap<String, String> row = search_arraylist.get(pos);
            selectFromSearch(row);
            lvResult.setVisibility(View.GONE);
            etSearch.setText("");
        });
    }

    // ── Search থেকে সিলেক্ট করলে সব auto fill হবে ──
    private void selectFromSearch(HashMap<String, String> row) {

        sub_id    = row.get("sub_id");
        feeder_id = row.get("feeder_id");
        area_id   = row.get("area_id");

        selSub  = row.get("sub_name");
        selFeed = row.get("feeder_name");
        selArea = row.get("area_name");

        // পরের ধাপে (cvFeeder/cvArea চাপলে) আগের তালিকা ব্যবহার না হয়,
        // নতুন করে যেন API থেকেই লোড হয়
        feeder_arraylist.clear();
        area_arraylist.clear();

        allDone();
    }

    // ── BottomSheet ──
    private void showBottomSheet(String type) {


        ArrayList<String[]> items = new ArrayList<>();

        if (type.equals("sub")) {

            for (int i = 0; substation_arraylist.size() > i; i++) {
                String name = substation_arraylist.get(i).get("name");
                String id = substation_arraylist.get(i).get("id");
                items.add(new String[]{name, id, ""});
            }

        } else if (type.equals("feed")) {
            ArrayList<String> seen = new ArrayList<>();

            for (int x = 0; feeder_arraylist.size() > x; x++) {

                String id = feeder_arraylist.get(x).get("id");
                String name = feeder_arraylist.get(x).get("name");

                if (!seen.contains(id)) {
                    items.add(new String[]{name, id, ""});
                    seen.add(id);
                }
            }

        } else {

            for (int x = 0; area_arraylist.size() > x; x++) {

                String name = area_arraylist.get(x).get("name");
                String id = area_arraylist.get(x).get("id");
                items.add(new String[]{name, id, ""});
            }
        }

        View sheetView = LayoutInflater.from(this)
                .inflate(R.layout.bottom_sheet_select, null);

        TextView tvTitle = sheetView.findViewById(R.id.tvSheetTitle);
        ListView lv = sheetView.findViewById(R.id.lvSheet);

        if (type.equals("sub")) tvTitle.setText("সাব-স্টেশন বাছাই করুন");
        else if (type.equals("feed")) tvTitle.setText("ফিডার বাছাই করুন");
        else tvTitle.setText("এলাকা বাছাই করুন");

        ArrayAdapter<String[]> adapter = new ArrayAdapter<String[]>(
                this, R.layout.item_bottom_sheet, items) {
            @Override
            public View getView(int pos, View cv, ViewGroup parent) {
                if (cv == null)
                    cv = LayoutInflater.from(getContext())
                            .inflate(R.layout.item_bottom_sheet, parent, false);
                String[] item = items.get(pos);
                ((TextView) cv.findViewById(R.id.tvItemName)).setText(item[0]);
                ((TextView) cv.findViewById(R.id.tvItemSub)).setText(item[1]);
                return cv;
            }
        };
        lv.setAdapter(adapter);

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(sheetView);
        dialog.show();

        lv.setOnItemClickListener((p, v, pos, id) -> {
            String name = items.get(pos)[0];
            String selectedId = items.get(pos)[1];
            dialog.dismiss();

            if (type.equals("sub")) {

                sub_id = selectedId;

                feeder_arraylist.clear();
                area_arraylist.clear();

                selSub  = name.replace(" সাবস্টেশন", "");
                selFeed = "";
                selArea = "";
                stepDone(tvSubstation, tvStep1, name);
                stepActive(tvFeeder, tvStep2, "ফিডার বাছাই করুন");
                stepReset(tvArea, tvStep3, cvArea, "আগে ফিডার বাছাই করুন");
                cvFeeder.setAlpha(1f);
                cvDoneBtn.setAlpha(0.4f);
                cvDoneBtn.setEnabled(false);

            } else if (type.equals("feed")) {

                feeder_id = selectedId;

                area_arraylist.clear();

                selFeed = name.replace(" ফিডার", "");
                selArea = "";
                stepDone(tvFeeder, tvStep2, name);
                stepActive(tvArea, tvStep3, "এলাকা বাছাই করুন");
                cvArea.setAlpha(1f);
                cvDoneBtn.setAlpha(0.4f);
                cvDoneBtn.setEnabled(false);

            } else {

                area_id = selectedId;
                selArea = name;
                stepDone(tvArea, tvStep3, name);
                cvDoneBtn.setAlpha(1f);
                cvDoneBtn.setEnabled(true);
            }
        });
    }

    // ── Step helpers ──
    private void stepDone(TextView tv, TextView num, String text) {
        tv.setText(text);
        tv.setTextColor(Color.parseColor("#16A34A"));
        num.setBackgroundResource(R.drawable.circle_green);
        num.setText("✓");
    }
    private void stepActive(TextView tv, TextView num, String hint) {
        tv.setText(hint);
        tv.setTextColor(Color.parseColor("#94A3B8"));
        num.setBackgroundResource(R.drawable.circle_brand);
        num.setText(num.getText().toString()
                .equals("✓") ? "২" : num.getText().toString());
    }
    private void stepReset(TextView tv, TextView num, CardView cv, String hint) {
        tv.setText(hint);
        tv.setTextColor(Color.parseColor("#94A3B8"));
        num.setBackgroundResource(R.drawable.circle_gray);
        cv.setAlpha(0.5f);
    }

    // ── Search থেকে সিলেক্ট করার পর সব ৩টা ধাপ ✓ করে দেয় (auto fillup) ──
    private void allDone() {
        stepDone(tvSubstation, tvStep1, selSub + " সাবস্টেশন");
        stepDone(tvFeeder, tvStep2, selFeed + " ফিডার");
        stepDone(tvArea, tvStep3, selArea);
        cvFeeder.setAlpha(1f);
        cvArea.setAlpha(1f);
        cvDoneBtn.setAlpha(1f);
        cvDoneBtn.setEnabled(true);
    }
}