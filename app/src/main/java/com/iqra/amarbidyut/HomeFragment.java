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
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.content.Intent;
import android.widget.Toast;

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
    CardView complainBtn, complainForm, complainsubmitBtn;
    Spinner spinnerComplain;
    EditText etComplain;
    TextView tvComplainSuccess;
    CardView ticketCheckCard, btnCheckTicket;
    EditText etTicket;
    TextView tvTicketResult;

    // Spinner এ অভিযোগের ধরন
    String[] types = {
            "অভিযোগের ধরন বাছাই করুন",
            "বিদ্যুৎ বিল সমস্যা",
            "লো ভোল্টেজ",
            "ঘন ঘন বিদ্যুৎ বিচ্ছিন্ন",
            "ট্রান্সফরমার সমস্যা",
            "মিটার সমস্যা",
            "তার ছেঁড়া / বিপজ্জনক",
            "অন্যান্য"
    };

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
        complainBtn        = view.findViewById(R.id.complainBtn);
        complainForm       = view.findViewById(R.id.complainForm);
        complainsubmitBtn  = view.findViewById(R.id.complainsubmitBtn);
        spinnerComplain    = view.findViewById(R.id.spinnerComplain);
        etComplain         = view.findViewById(R.id.etComplain);
        tvComplainSuccess  = view.findViewById(R.id.tvComplainSuccess);
        ticketCheckCard = view.findViewById(R.id.ticketCheckCard);
        btnCheckTicket  = view.findViewById(R.id.btnCheckTicket);
        etTicket        = view.findViewById(R.id.etTicket);
        tvTicketResult  = view.findViewById(R.id.tvTicketResult);

// শুরুতে form ও success message লুকানো থাকবে
        complainForm.setVisibility(GONE);
        tvComplainSuccess.setVisibility(GONE);

        TextView tvSavedTicket = view.findViewById(R.id.tvSavedTicket);
        String savedTicket = requireContext()
                .getSharedPreferences("complaint", MODE_PRIVATE)
                .getString("last_ticket", "");

        if (!savedTicket.isEmpty()) {
            tvSavedTicket.setText("আপনার শেষ টিকেট: " + savedTicket + " (চাপ দিন)");
            tvSavedTicket.setVisibility(View.VISIBLE);
            // চাপলে auto fill হবে
            tvSavedTicket.setOnClickListener(v -> {
                etTicket.setText(savedTicket);
            });
        }


        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                types
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerComplain.setAdapter(adapter);

// Button click — form দেখাও/লুকাও
        complainBtn.setOnClickListener(v -> {
            if (complainForm.getVisibility() == VISIBLE) {
                complainForm.setVisibility(GONE);
            } else {
                complainForm.setVisibility(VISIBLE);
                tvComplainSuccess.setVisibility(GONE);
            }
        });

// Submit button
        complainsubmitBtn.setOnClickListener(v -> submitComplaint());
        btnCheckTicket.setOnClickListener(v -> checkTicket());



        notificationCard.setVisibility(GONE);
        isFragmentActive = true;

        requestQueue = Volley.newRequestQueue(requireContext());

        // SharedPreferences থেকে user-এর selected info
        SharedPreferences prefs = requireContext().getSharedPreferences("amarbidyut", MODE_PRIVATE);
        String sub    = prefs.getString("sub", "");
        String feed   = prefs.getString("feed", "");
        String area   = prefs.getString("area", "");
        String feederId = prefs.getString("feeder_id", "0");

        tvsubTitle.setText("▼ " +area + " · " + feed + " · " + sub);

        // এলাকা পরিবর্তন করতে চাইলে
        tvsubTitle.setOnClickListener(v -> {
            prefs.edit().clear().apply();
            startActivity(new Intent(requireContext(), SelectActivity.class));
            getActivity().finish();
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
                tvUpdatedBy.setText("পরিবর্তন: " + updatedBy + " · " + formatEta(lastUpdated));
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
                tvReason.setText("কারণ: " + offReason);
                tvComing.setText("⚡ বিদ্যুৎ আসার সম্ভাব্য সময় বাকি");
                tvReason.setVisibility(VISIBLE);
            } else {
                tvReason.setVisibility(GONE);
            }

            // ETA
            if (!etaAt.isEmpty() && !etaAt.equals("null")) {
                tvEta.setText("সম্ভাব্য আসার সময়: "+ formatEta(etaAt));
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
                tvUpdatedBy.setText("পরিবর্তন: " + updatedBy + " · " + formatEta(lastUpdated));
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
                        tvEta.setText("সার্ভার থেকে আপডেট নিচ্ছে...");
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
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("amarbidyut", MODE_PRIVATE);
        String subId  = prefs.getString("sub_id", "");
        String areaId = prefs.getString("area_id", "");

        String url = "https://dainikbhorerbarta.com/bidyut_super_admin/get_user_notices.php"
                + "?feeder_id=" + feederId
                + "&sub_id=" + subId
                + "&area_id=" + areaId;

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (!response.getBoolean("success")) {
                            notificationCard.setVisibility(GONE);
                            return;
                        }

                        org.json.JSONArray notices = response.getJSONArray("notices");
                        if (notices.length() == 0) {
                            notificationCard.setVisibility(GONE);
                            return;
                        }

                        // সবচেয়ে নতুন notice দেখাও
                        JSONObject notice = notices.getJSONObject(0);
                        String noticeId = notice.optString("id", "");

                        SharedPreferences noticePref = requireContext()
                                .getSharedPreferences("notice", MODE_PRIVATE);
                        String seenId = noticePref.getString("seen_notice_id", "");

                        if (!noticeId.equals(seenId) && !noticeId.isEmpty()) {
                            currentNoticeId = noticeId;

                            // Title
                            String title = notice.optString("title", notice.optString("type", "নোটিশ"));
                            tvNoticeTitle.setText(title);

                            // তারিখ ও সময়
                            tvNoticeDate.setText(notice.optString("notice_date", ""));
                            tvNoticeTime.setText(notice.optString("notice_time", ""));

                            // বার্তা
                            tvNoticeReason.setText(notice.optString("message", ""));

                            // এলাকা
                            String area = notice.optString("feeder_name", "");
                            if (!notice.optString("area_name", "").isEmpty()) {
                                area += " · " + notice.optString("area_name", "");
                            }
                            tvNoticeArea.setText(area);

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

            // আজকের date কিনা check করো
            java.util.Calendar etaCal = java.util.Calendar.getInstance();
            etaCal.setTime(date);
            java.util.Calendar today = java.util.Calendar.getInstance();

            String[] months = {"জানু", "ফেব্রু", "মার্চ", "এপ্রিল", "মে", "জুন",
                    "জুলাই", "আগস্ট", "সেপ্টে", "অক্টো", "নভে", "ডিসে"};

            SimpleDateFormat timeFmt = new SimpleDateFormat("hh:mm a", Locale.ENGLISH);
            timeFmt.setTimeZone(TimeZone.getTimeZone("Asia/Dhaka"));
            String timeStr = timeFmt.format(date);

            // আজকে হলে শুধু সময়, অন্যদিন হলে তারিখ + সময়
            if (etaCal.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR)
                    && etaCal.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR)) {
                return "আজ " + timeStr;
            } else {
                int day = etaCal.get(java.util.Calendar.DAY_OF_MONTH);
                String month = months[etaCal.get(java.util.Calendar.MONTH)];
                return day + " " + month + " " + timeStr;
            }
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

    private void submitComplaint() {
        // Validation
        if (spinnerComplain.getSelectedItemPosition() == 0) {
            android.widget.Toast.makeText(requireContext(),
                    "অভিযোগের ধরন বাছাই করুন", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

        EditText etPhone = requireView().findViewById(R.id.etPhone);
        String phone = etPhone.getText().toString().trim();

        if (phone.isEmpty() || phone.length() < 11) {
            android.widget.Toast.makeText(requireContext(),
                    "সঠিক মোবাইল নম্বর দিন", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }

// params-এ যোগ করো

        String complaintType = spinnerComplain.getSelectedItem().toString();
        String description   = etComplain.getText().toString().trim();

        SharedPreferences prefs = requireContext()
                .getSharedPreferences("amarbidyut", MODE_PRIVATE);
        String feederId   = prefs.getString("feeder_id", "0");
        String subId      = prefs.getString("sub_id", "0");
        String areaId     = prefs.getString("area_id", "0");
        String areaName   = prefs.getString("area", "");
        String feederName = prefs.getString("feed", "");

        String url = "https://dainikbhorerbarta.com/bidyut_apps/submit_complaint.php";

        com.android.volley.toolbox.StringRequest req = new com.android.volley.toolbox.StringRequest(
                com.android.volley.Request.Method.POST, url,
                response -> {
                    try {
                        JSONObject json = new JSONObject(response);
                        if (json.getBoolean("success")) {
                            // Success দেখাও
                            String ticket = json.optString("ticket", "");
                            tvComplainSuccess.setText("✓ অভিযোগ গৃহীত হয়েছে!\nটিকেট নম্বর: " + ticket + "\nএই নম্বর দিয়ে পরে status জানতে পারবেন\n(চাপ দিয়ে ধরুন copy করতে)");
                            tvComplainSuccess.setVisibility(VISIBLE);



                            // ← এখানে বসাও
                            tvComplainSuccess.setOnLongClickListener(v -> {
                                android.content.ClipboardManager clipboard =
                                        (android.content.ClipboardManager) requireContext()
                                                .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                                android.content.ClipData clip =
                                        android.content.ClipData.newPlainText("ticket", ticket);
                                clipboard.setPrimaryClip(clip);
                                android.widget.Toast.makeText(requireContext(),
                                        "✓ টিকেট নম্বর copy হয়েছে: " + ticket,
                                        android.widget.Toast.LENGTH_LONG).show();
                                return true;
                            });

                            // Ticket SharedPreferences-এ save করো
                            requireContext()
                                    .getSharedPreferences("complaint", MODE_PRIVATE)
                                    .edit()
                                    .putString("last_ticket", ticket)
                                    .apply();

                            // Form reset করো
                            spinnerComplain.setSelection(0);
                            etComplain.setText("");
                            // ১০ সেকেন্ড পর form লুকাও
                            handler.postDelayed(() -> {
                                if (!isFragmentActive) return;
                                complainForm.setVisibility(GONE);
                                tvComplainSuccess.setVisibility(GONE);
                            }, 10000);
                        } else {
                            android.widget.Toast.makeText(requireContext(),
                                    json.optString("message", "সমস্যা হয়েছে"),
                                    android.widget.Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        android.widget.Toast.makeText(requireContext(),
                                "সমস্যা হয়েছে", android.widget.Toast.LENGTH_SHORT).show();
                    }
                },
                error -> android.widget.Toast.makeText(requireContext(),
                        "নেটওয়ার্ক সমস্যা", android.widget.Toast.LENGTH_SHORT).show()
        ) {
            @Override
            protected java.util.Map<String, String> getParams() {
                java.util.Map<String, String> params = new java.util.HashMap<>();
                params.put("feeder_id",      feederId);
                params.put("sub_id",         subId);
                params.put("area_id",        areaId);
                params.put("area_name",      areaName);
                params.put("feeder_name",    feederName);
                params.put("complaint_type", complaintType);
                params.put("description",    description);
                params.put("phone", phone);

                return params;
            }
        };

        req.setShouldCache(false);
        requestQueue.add(req);
    }


    private void checkTicket() {
        String ticket = etTicket.getText().toString().trim().toUpperCase();

        if (ticket.isEmpty()) {
            Toast.makeText(requireContext(), "টিকেট নম্বর লিখুন", Toast.LENGTH_SHORT).show();
            return;
        }

        String url = "https://dainikbhorerbarta.com/bidyut_apps/check_complaint.php?ticket=" + ticket;

        JsonObjectRequest req = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (!response.getBoolean("success")) {
                            tvTicketResult.setText("❌ টিকেট পাওয়া যায়নি");
                            tvTicketResult.setBackgroundColor(Color.parseColor("#FEE2E2"));
                            tvTicketResult.setTextColor(Color.parseColor("#991B1B"));
                            tvTicketResult.setVisibility(View.VISIBLE);
                            return;
                        }

                        String statusText = response.optString("status_text", "");
                        String type       = response.optString("type", "");
                        String createdAt  = response.optString("created_at", "");
                        String status     = response.optString("status", "");

                        int bgColor, textColor;
                        switch (status) {
                            case "resolved":
                                bgColor = Color.parseColor("#DCFCE7");
                                textColor = Color.parseColor("#166534");
                                break;
                            case "seen":
                                bgColor = Color.parseColor("#FEF9C3");
                                textColor = Color.parseColor("#92400E");
                                break;
                            default:
                                bgColor = Color.parseColor("#FEE2E2");
                                textColor = Color.parseColor("#991B1B");
                                break;
                        }

                        tvTicketResult.setText(
                                "🎫 টিকেট: " + ticket + "\n" +
                                        "📋 ধরন: " + type + "\n" +
                                        "📌 অবস্থা: " + statusText + "\n" +
                                        "🕐 জমা: " + createdAt
                        );
                        tvTicketResult.setBackgroundColor(bgColor);
                        tvTicketResult.setTextColor(textColor);
                        tvTicketResult.setVisibility(View.VISIBLE);

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                },
                error -> {
                    tvTicketResult.setText("❌ নেটওয়ার্ক সমস্যা");
                    tvTicketResult.setVisibility(View.VISIBLE);
                }
        );

        req.setShouldCache(false);
        requestQueue.add(req);
    }

}