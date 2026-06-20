package com.iqra.amarbidyut;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class HomeFragment extends Fragment {

    TextView title, date, time, reason, area, tvsubTitle;
    RelativeLayout topBar;
    LinearLayout notificationLay;
    CardView notificationCard;
    ImageView close;
    String currentNoticeId;
    TextView tvHour, tvMinute, tvSecond;
    android.os.Handler handler = new android.os.Handler();
    Runnable timerRunnable;
    long powerOffMillis;
    MaterialToolbar materialToolbar;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,Bundle savedInstanceState) {
        View view =  inflater.inflate(R.layout.fragment_home, container, false);

        title = view.findViewById(R.id.title);
        date = view.findViewById(R.id.date);
        time = view.findViewById(R.id.time);
        reason = view.findViewById(R.id.reason);
        area = view.findViewById(R.id.area);
        topBar = view.findViewById(R.id.topBar);
        notificationLay = view.findViewById(R.id.notificationLay);
        notificationCard = view.findViewById(R.id.notificationCard);
        close = view.findViewById(R.id.close);
        tvHour = view.findViewById(R.id.tvHour);
        tvMinute = view.findViewById(R.id.tvMinute);
        tvSecond = view.findViewById(R.id.tvSecond);
        tvsubTitle = view.findViewById(R.id.tvsubTitle);
        materialToolbar = view.findViewById(R.id.materialToolbar);

        notificationCard.setVisibility(GONE);

        SharedPreferences sharedPreferences = getContext().getSharedPreferences("amarbidyut", MODE_PRIVATE);
        String sub = sharedPreferences.getString("sub", "");
        String feed = sharedPreferences.getString("feed", "");
        String selarea = sharedPreferences.getString("area", "");

        tvsubTitle.setText(""+selarea+" - "+feed+" - "+sub);

        tvsubTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                SharedPreferences prefs = getContext().getSharedPreferences("amarbidyut", MODE_PRIVATE);
                prefs.edit().clear().apply();

                Intent intent = new Intent(getContext(), SelectActivity.class);
                startActivity(intent);


            }
        });

        close.setOnClickListener(v -> {
            // Banner বন্ধ করো
            notificationCard.setVisibility(View.GONE);

            // এই notice দেখেছি — save করো
            SharedPreferences prefs = getContext().getSharedPreferences(
                    "notice", MODE_PRIVATE);
            prefs.edit()
                    .putString("seen_notice_id", currentNoticeId)
                    .apply();
        });


        String url = "https://dainikbhorerbarta.com/bidyut_apps/get_data.php";

        JsonObjectRequest jsonObjectRequest  = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {


            @Override
            public void onResponse(JSONObject jsonObject) {



                String string_title = jsonObject.optString("title");
                String string_date = jsonObject.optString("date");
                String string_time = jsonObject.optString("start_time");
                String string_reason = jsonObject.optString("notice");
                String string_area = jsonObject.optString("area");
                String newNoticeId = jsonObject.optString("notice_id");
//                String powerOffTime = jsonObject.optString("power_off_time");


//                if (!powerOffTime.isEmpty()) {
//                    startRunningTimer(powerOffTime);
//                }

                Log.d("response: ", "onResponse: " + jsonObject);

                SharedPreferences prefs = getContext().getSharedPreferences(
                        "notice", MODE_PRIVATE);
                String seenId = prefs.getString("seen_notice_id", "");


                if (!newNoticeId.equals(seenId)) {
                    // নতুন notice — দেখা
                    notificationCard.setVisibility(View.VISIBLE);
                    currentNoticeId = newNoticeId; // ← এই লাইন নেই তোমার code এ

                    // Data set
                    title.setText(string_title);
                    date.setText(string_date);
                    time.setText(string_time);
                    reason.setText(string_reason);
                    area.setText(string_area);

                } else {
                    // আগে দেখেছে — লুকাও
                    notificationCard.setVisibility(View.GONE);
                }

               /* if (string_reason.equals("শিডিউল লোডশেডিং")) {
                    topBar.setBackgroundColor(
                            Color.parseColor("#BA7517"));
                    notificationLay.setBackgroundColor(
                            Color.parseColor("#FAEEDA"));

                } else if (string_reason.equals("জরুরি মেরামত")) {
                    topBar.setBackgroundColor(
                            Color.parseColor("#A32D2D"));
                    notificationLay.setBackgroundColor(
                            Color.parseColor("#FCEBEB"));

                } else {
                    topBar.setBackgroundColor(
                            Color.parseColor("#185FA5"));
                    notificationLay.setBackgroundColor(
                            Color.parseColor("#E6F1FB"));
                }*/


            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

            }


        }



        );
        jsonObjectRequest.setShouldCache(false); // ← এই লাইন

        RequestQueue requestQueue = Volley.newRequestQueue(getContext());
        requestQueue.add(jsonObjectRequest);


        return view;
    }

    private void startRunningTimer(String dateTimeString) {

        try {
            java.text.SimpleDateFormat sdf =
                    new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            sdf.setTimeZone(java.util.TimeZone.getDefault());

            java.util.Date powerOffDate = sdf.parse(dateTimeString);
            powerOffMillis = powerOffDate.getTime();

            if (timerRunnable != null) {
                handler.removeCallbacks(timerRunnable);
            }

            timerRunnable = new Runnable() {
                @Override
                public void run() {

                    long now = System.currentTimeMillis();
                    long diff = now - powerOffMillis;

                    if (diff < 0) diff = 0;

                    long totalSeconds = diff / 1000;

                    long hours = totalSeconds / 3600;
                    long minutes = (totalSeconds % 3600) / 60;
                    long seconds = totalSeconds % 60;

                    tvHour.setText(String.format("%02d", hours));
                    tvMinute.setText(String.format("%02d", minutes));
                    tvSecond.setText(String.format("%02d", seconds));

                    handler.postDelayed(this, 1000);
                }
            };

            handler.post(timerRunnable);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}