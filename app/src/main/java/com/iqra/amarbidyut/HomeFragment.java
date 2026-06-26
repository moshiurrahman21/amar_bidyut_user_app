package com.iqra.amarbidyut;

import static android.content.Context.MODE_PRIVATE;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.content.Intent;

import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class HomeFragment extends Fragment {

    // ── Views ──
    TextView tvsubTitle, tvStatus, tvReason, tvEta, tvUpdatedBy,tvComing;
    ;
    TextView tvHour, tvMinute, tvSecond;
    CardView notificationCard, infoCard;
    TextView tvNoticeTitle, tvNoticeDate, tvNoticeTime, tvNoticeReason, tvNoticeArea;
    android.widget.ImageView close;
    android.widget.RelativeLayout topBar;
    android.widget.LinearLayout notificationLay;

    // ── Timer ──
    Handler handler = new Handler(Looper.getMainLooper());
    Runnable countUpRunnable;
    long offTimeMillis = 0;

    // ── Auto refresh ──
    Handler refreshHandler = new Handler(Looper.getMainLooper());
    Runnable refreshRunnable;

    // ── Notice ──
    String currentNoticeId = "";

    RequestQueue requestQueue;
    private boolean isFragmentActive = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // View bind
        tvsubTitle       = view.findViewById(R.id.tvsubTitle);
        tvStatus         = view.findViewById(R.id.tvStatus);
        tvReason         = view.findViewById(R.id.tvReason);
        tvEta            = view.findViewById(R.id.tvEta);
        tvHour           = view.findViewById(R.id.tvHour);
        tvMinute         = view.findViewById(R.id.tvMinute);
        tvSecond         = view.findViewById(R.id.tvSecond);
        notificationCard = view.findViewById(R.id.notificationCard);
        close            = view.findViewById(R.id.close);
        tvNoticeTitle    = view.findViewById(R.id.title);
        tvNoticeDate     = view.findViewById(R.id.date);
        tvNoticeTime     = view.findViewById(R.id.time);
        tvNoticeReason   = view.findViewById(R.id.reason);
        tvNoticeArea     = view.findViewById(R.id.area);
        tvUpdatedBy     = view.findViewById(R.id.tvUpdatedBy);
        tvComing     = view.findViewById(R.id.tvComing);

        notificationCard.setVisibility(GONE);
        isFragmentActive = true;

        requestQueue = Volley.newRequestQueue(requireContext());

        // SharedPreferences থেকে user-এর selected info
        SharedPreferences prefs = requireContext().getSharedPreferences("amarbidyut", MODE_PRIVATE);
        String sub    = prefs.getString("sub", "");
        String feed   = prefs.getString("feed", "");
        String area   = prefs.getString("area", "");
        String feederId = prefs.getString("feeder_id", "0");

        tvsubTitle.setText(area + " · " + feed + " · " + sub);

        // এলাকা পরিবর্তন করতে চাইলে
        tvsubTitle.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            startActivity(new Intent(requireContext(), SelectActivity.class));
        });

        // Notice close button
        close.setOnClickListener(v -> {
            notificationCard.setVisibility(GONE);
            requireContext()
                    .getSharedPreferences("notice", MODE_PRIVATE)
                    .edit().putString("seen_notice_id", currentNoticeId).apply();
        });

        // প্রথমবার load
        loadStatus(feederId);
        loadNotice(feederId);

        // প্রতি ৩০ সেকেন্ডে auto refresh
        refreshRunnable = new Runnable() {
            @Override
            public void run() {
                loadStatus(feederId);
                loadNotice(feederId);
                refreshHandler.postDelayed(this, 30_000);
            }
        };
        refreshHandler.postDelayed(refreshRunnable, 30_000);

        return view;
    }

    // ══════════════════════════════════════════
    // Feeder Status API call
    // ══════════════════════════════════════════
    private void loadStatus(String feederId) {
        String url = "https://dainikbhorerbarta.com/bidyut_apps/get_status.php?feeder_id=" + feederId;

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (!response.getBoolean("success")) return;

                        JSONObject data = response.getJSONObject("data");

                        String status    = data.optString("status", "on");
                        String offReason = data.optString("off_reason", "");
                        String etaAt     = data.optString("eta_at", "");
                        String autoOffAt = data.optString("auto_off_at", "");
                        String lastUpdated = data.optString("last_updated", "");

                        String updatedBy = data.optString("updated_by", "");
                        updateStatusUI(status, offReason, etaAt, autoOffAt, lastUpdated, updatedBy);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> { /* network error — পুরনো UI রেখে দাও */ }
        );

        req.setShouldCache(false);
        requestQueue.add(req);
    }

    // ══════════════════════════════════════════
    // UI আপডেট করো
    // ══════════════════════════════════════════
    private void updateStatusUI(String status, String offReason, String etaAt, String autoOffAt, String lastUpdated, String updatedBy) {

        // পুরনো timer বন্ধ করো
        if (countUpRunnable != null) handler.removeCallbacks(countUpRunnable);

        if (status.equals("on")) {

            if (!autoOffAt.isEmpty() && !autoOffAt.equals("null")) {
                // ── চালু আছে কিন্তু Scheduled OFF আছে ──
                tvStatus.setText("⚡ বিদ্যুৎ আছে · শীঘ্রই বন্ধ হবে");
                tvStatus.setTextColor(Color.parseColor("#D97706"));
                tvReason.setText("⚠️ " + formatEta(autoOffAt) + " এ বন্ধ হবে");
                tvComing.setText("⚡ বিদ্যুৎ যাবার সম্ভাব্য সময় বাকি");
                tvReason.setVisibility(VISIBLE);
                tvEta.setVisibility(GONE);

                // Countdown — auto_off_at পর্যন্ত
                startCountDownTimer(autoOffAt, true);

            } else {
                // ── স্বাভাবিক চালু ──
                tvStatus.setText("বিদ্যুৎ আছে ⚡");
                tvStatus.setTextColor(Color.parseColor("#16A34A"));
                tvReason.setText("সব ঠিকঠাক চলছে");
                tvComing.setText("⚡ বিদ্যুৎ সরবরাহ স্বাভাবিক রয়েছে");
                tvReason.setVisibility(VISIBLE);
                tvEta.setVisibility(GONE);

                tvHour.setText("০০");
                tvMinute.setText("০০");
                tvSecond.setText("০০");
            }

            // Updated by
            if (!updatedBy.isEmpty() && !updatedBy.equals("null")) {
                tvUpdatedBy.setText("✏️ " + updatedBy + " · " + formatEta(lastUpdated));
                tvUpdatedBy.setVisibility(VISIBLE);
            } else {
                tvUpdatedBy.setVisibility(GONE);
            }

        } else {
            // ── বিদ্যুৎ নেই ──
            tvStatus.setText("বিদ্যুৎ নেই");
            tvStatus.setTextColor(Color.parseColor("#E03B3B"));

            // কারণ
            if (!offReason.isEmpty() && !offReason.equals("null")) {
                tvReason.setText("🔧 " + offReason);
                tvComing.setText("⚡ বিদ্যুৎ আসার সম্ভাব্য সময় বাকি");
                tvReason.setVisibility(VISIBLE);
            } else {
                tvReason.setVisibility(GONE);
            }

            // ETA
            if (!etaAt.isEmpty() && !etaAt.equals("null")) {
                tvEta.setText("⏰ সম্ভাব্য আসার সময়: " + formatEta(etaAt));
                tvEta.setVisibility(VISIBLE);
                startCountDownTimer(etaAt, false);
            } else {
                tvEta.setVisibility(GONE);
                tvHour.setText("০০");
                tvMinute.setText("০০");
                tvSecond.setText("০০");
            }

            // Updated by
            if (!updatedBy.isEmpty() && !updatedBy.equals("null")) {
                tvUpdatedBy.setText("✏️ " + updatedBy + " · " + formatEta(lastUpdated));
                tvUpdatedBy.setVisibility(VISIBLE);
            } else {
                tvUpdatedBy.setVisibility(GONE);
            }
        }
    }
    // ══════════════════════════════════════════
    // Count-up timer (কতক্ষণ ধরে বন্ধ)
    // ══════════════════════════════════════════
    private void startCountDownTimer(String targetTime, boolean isOffTimer) {
        long targetMillis = parseDateTime(targetTime);
        if (targetMillis <= 0) return;

        countUpRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isFragmentActive) return;

                long diff = targetMillis - System.currentTimeMillis();

                if (diff <= 0) {
                    tvHour.setText("০০");
                    tvMinute.setText("০০");
                    tvSecond.setText("০০");

                    if (isOffTimer) {
                        tvStatus.setText("বন্ধ হওয়ার সময় হয়েছে...");
                        tvStatus.setTextColor(Color.parseColor("#E03B3B"));
                        tvReason.setVisibility(GONE);
                    } else {
                        tvStatus.setText("বিদ্যুৎ আসার সময় হয়েছে ⚡");
                        tvStatus.setTextColor(Color.parseColor("#D97706"));
                        tvEta.setText("⏰ সার্ভার থেকে আপডেট নিচ্ছে...");
                        tvEta.setVisibility(VISIBLE);
                    }

                    // ১০ সেকেন্ড পর পর server check করতে থাকবে
                    scheduleStatusCheck();
                    return;
                }

                long totalSeconds = diff / 1000;
                long hours   = totalSeconds / 3600;
                long minutes = (totalSeconds % 3600) / 60;
                long seconds = totalSeconds % 60;

                tvHour.setText(toBn(String.format("%02d", hours)));
                tvMinute.setText(toBn(String.format("%02d", minutes)));
                tvSecond.setText(toBn(String.format("%02d", seconds)));

                handler.postDelayed(this, 1000);
            }
        };
        handler.post(countUpRunnable);
    }
    //========================================================

    private void scheduleStatusCheck() {
        if (!isFragmentActive) return;

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("amarbidyut", MODE_PRIVATE);
        String feederId = prefs.getString("feeder_id", "0");

        handler.postDelayed(() -> {
            if (!isFragmentActive) return;
            loadStatus(feederId);
        }, 10_000);
    }

    //========================================================

    // ══════════════════════════════════════════
    // Notice API call
    // ══════════════════════════════════════════
    private void loadNotice(String feederId) {
        String url = "https://dainikbhorerbarta.com/bidyut_super_admin/get_user_notices.php?feeder_id=" + feederId;

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (!response.getBoolean("success")) {
                            notificationCard.setVisibility(GONE);
                            return;
                        }

                        JSONObject notice = response.getJSONObject("notice");
                        String noticeId = notice.optString("id", "");

                        SharedPreferences noticePref = requireContext()
                                .getSharedPreferences("notice", MODE_PRIVATE);
                        String seenId = noticePref.getString("seen_notice_id", "");

                        if (!noticeId.equals(seenId) && !noticeId.isEmpty()) {
                            currentNoticeId = noticeId;
                            tvNoticeTitle.setText(notice.optString("title", ""));
                            tvNoticeDate.setText(notice.optString("date", ""));
                            tvNoticeTime.setText(notice.optString("time", ""));
                            tvNoticeReason.setText(notice.optString("reason", ""));
                            tvNoticeArea.setText(notice.optString("area", ""));
                            notificationCard.setVisibility(VISIBLE);
                        } else {
                            notificationCard.setVisibility(GONE);
                        }

                    } catch (Exception e) {
                        notificationCard.setVisibility(GONE);
                    }
                },
                error -> notificationCard.setVisibility(GONE)
        );

        req.setShouldCache(false);
        requestQueue.add(req);
    }

    // ══════════════════════════════════════════
    // Helper: datetime parse করো
    // ══════════════════════════════════════════
    private long parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty() || dateTimeStr.equals("null")) return 0;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("Asia/Dhaka"));
            Date date = sdf.parse(dateTimeStr);
            return date != null ? date.getTime() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // ══════════════════════════════════════════
    // Helper: ETA সুন্দর format-এ দেখাও
    // ══════════════════════════════════════════
    private String formatEta(String etaAt) {
        if (etaAt == null || etaAt.isEmpty() || etaAt.equals("null")) return "";
        try {
            SimpleDateFormat inFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            inFmt.setTimeZone(TimeZone.getTimeZone("Asia/Dhaka"));
            Date date = inFmt.parse(etaAt);
            if (date == null) return etaAt;
            SimpleDateFormat outFmt = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            outFmt.setTimeZone(TimeZone.getTimeZone("Asia/Dhaka"));
            return outFmt.format(date);
        } catch (Exception e) {
            return etaAt;
        }
    }

    // ══════════════════════════════════════════
    // Helper: English digit → Bengali digit
    // ══════════════════════════════════════════
    private String toBn(String input) {
        return input
                .replace("0","০").replace("1","১").replace("2","২")
                .replace("3","৩").replace("4","৪").replace("5","৫")
                .replace("6","৬").replace("7","৭").replace("8","৮")
                .replace("9","৯");
    }

    //===========================================
    // ------onResume Methood--------------

    @Override
    public void onResume() {
        super.onResume();
        if (!isFragmentActive) return;

        // পুরনো timer বন্ধ করো
        if (countUpRunnable != null) handler.removeCallbacks(countUpRunnable);

        // Server থেকে fresh data আনো
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("amarbidyut", MODE_PRIVATE);
        String feederId = prefs.getString("feeder_id", "0");
        loadStatus(feederId);
        loadNotice(feederId);
    }


    //===========================================


    // ══════════════════════════════════════════
    // Fragment বন্ধ হলে সব timer বন্ধ করো
    // ══════════════════════════════════════════
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        isFragmentActive = false;
        if (countUpRunnable != null) handler.removeCallbacks(countUpRunnable);
        if (refreshRunnable != null) refreshHandler.removeCallbacks(refreshRunnable);
    }
}