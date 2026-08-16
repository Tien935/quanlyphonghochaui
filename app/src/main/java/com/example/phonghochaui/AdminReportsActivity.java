package com.example.phonghochaui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.phonghochaui.data.model.DashboardStatistics;
import com.example.phonghochaui.data.remote.RetrofitClient;
import com.example.phonghochaui.data.remote.SessionManager;
import com.example.phonghochaui.data.remote.SupabaseApiService;
import com.example.phonghochaui.ui.components.BarChartView;
import com.example.phonghochaui.ui.components.DonutChartView;
import com.example.phonghochaui.ui.components.LineChartView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.text.NumberFormat;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminReportsActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private SupabaseApiService apiService;
    private DashboardStatistics statistics;

    private View reportContent;
    private LinearProgressIndicator progress;
    private MaterialButton btnRefresh;
    private MaterialButton btnTrend7Days;
    private MaterialButton btnTrend30Days;
    private TextView tvError;
    private TextView tvGeneratedAt;
    private TextView tvCampuses;
    private TextView tvBuildings;
    private TextView tvClassrooms;
    private TextView tvUsers;
    private TextView tvUsersDetail;
    private TextView tvBookings;
    private TextView tvBookingsDetail;
    private TextView tvPending;
    private TextView tvRoomActiveLegend;
    private TextView tvRoomMaintenanceLegend;
    private TextView tvRoomInactiveLegend;
    private TextView tvTrendTotal;
    private DonutChartView roomDonutChart;
    private BarChartView bookingBarChart;
    private LineChartView bookingLineChart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);
        if (!sessionManager.hasSession()
                || !"admin".equals(sessionManager.getUserRole())) {
            openLogin();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_reports);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.adminReportsRoot),
                (view, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );
                    view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
                    return insets;
                }
        );

        bindViews();
        apiService = RetrofitClient.getApiService(this);

        findViewById(R.id.btnAdminReportsBack)
                .setOnClickListener(view -> finish());
        btnRefresh.setOnClickListener(view -> loadStatistics());
        btnTrend7Days.setOnClickListener(view -> updateTrend(7));
        btnTrend30Days.setOnClickListener(view -> updateTrend(30));

        btnTrend7Days.setChecked(true);
        loadStatistics();
    }

    private void bindViews() {
        reportContent = findViewById(R.id.reportContentScroll);
        progress = findViewById(R.id.adminReportsProgress);
        btnRefresh = findViewById(R.id.btnRefreshReports);
        btnTrend7Days = findViewById(R.id.btnTrend7Days);
        btnTrend30Days = findViewById(R.id.btnTrend30Days);
        tvError = findViewById(R.id.tvAdminReportsError);
        tvGeneratedAt = findViewById(R.id.tvReportGeneratedAt);
        tvCampuses = findViewById(R.id.tvReportCampuses);
        tvBuildings = findViewById(R.id.tvReportBuildings);
        tvClassrooms = findViewById(R.id.tvReportClassrooms);
        tvUsers = findViewById(R.id.tvReportUsers);
        tvUsersDetail = findViewById(R.id.tvReportUsersDetail);
        tvBookings = findViewById(R.id.tvReportBookings);
        tvBookingsDetail = findViewById(R.id.tvReportBookingsDetail);
        tvPending = findViewById(R.id.tvReportPending);
        tvRoomActiveLegend = findViewById(R.id.tvRoomActiveLegend);
        tvRoomMaintenanceLegend = findViewById(R.id.tvRoomMaintenanceLegend);
        tvRoomInactiveLegend = findViewById(R.id.tvRoomInactiveLegend);
        tvTrendTotal = findViewById(R.id.tvReportTrendTotal);
        roomDonutChart = findViewById(R.id.reportRoomDonut);
        bookingBarChart = findViewById(R.id.reportBookingBar);
        bookingLineChart = findViewById(R.id.reportBookingTrend);
    }

    private void loadStatistics() {
        setLoading(true);

        apiService.getAdminDashboardStatistics(new JsonObject())
                .enqueue(new Callback<DashboardStatistics>() {
                    @Override
                    public void onResponse(
                            Call<DashboardStatistics> call,
                            Response<DashboardStatistics> response
                    ) {
                        setLoading(false);

                        if (response.code() == 401) {
                            handleExpiredSession();
                            return;
                        }

                        if (!response.isSuccessful() || response.body() == null) {
                            showError(readApiError(
                                    response.errorBody(),
                                    getString(
                                            R.string.report_load_error_http,
                                            response.code()
                                    )
                            ));
                            return;
                        }

                        statistics = response.body();
                        renderStatistics();
                    }

                    @Override
                    public void onFailure(
                            Call<DashboardStatistics> call,
                            Throwable throwable
                    ) {
                        setLoading(false);
                        showError(networkError(throwable));
                    }
                });
    }

    private void renderStatistics() {
        if (statistics == null) {
            return;
        }

        DashboardStatistics.Summary summary = statistics.getSummary();
        DashboardStatistics.BookingActivity activity =
                statistics.getBookingActivity();

        tvGeneratedAt.setText(getString(
                R.string.report_generated_at,
                formatGeneratedAt(statistics.getGeneratedAt()),
                statistics.getTimezone()
        ));
        tvCampuses.setText(formatNumber(summary.getCampusesTotal()));
        tvBuildings.setText(formatNumber(summary.getBuildingsTotal()));
        tvClassrooms.setText(formatNumber(summary.getClassroomsTotal()));
        tvUsers.setText(formatNumber(summary.getUsersTotal()));
        tvUsersDetail.setText(getString(
                R.string.report_users_detail,
                summary.getStudentsTotal(),
                summary.getAdminsTotal(),
                summary.getLockedUsersTotal()
        ));
        tvBookings.setText(formatNumber(summary.getBookingsTotal()));
        tvBookingsDetail.setText(getString(
                R.string.report_bookings_detail,
                activity.getLast7Days(),
                activity.getLast30Days()
        ));
        tvPending.setText(formatNumber(summary.getPendingBookingsTotal()));

        DashboardStatistics.StatusStat active =
                statistics.findClassroomStatus("active");
        DashboardStatistics.StatusStat maintenance =
                statistics.findClassroomStatus("maintenance");
        DashboardStatistics.StatusStat inactive =
                statistics.findClassroomStatus("inactive");

        roomDonutChart.setData(
                active.getTotal(),
                maintenance.getTotal(),
                inactive.getTotal()
        );
        tvRoomActiveLegend.setText(legendText(
                getString(R.string.report_status_active),
                active
        ));
        tvRoomMaintenanceLegend.setText(legendText(
                getString(R.string.report_status_maintenance),
                maintenance
        ));
        tvRoomInactiveLegend.setText(legendText(
                getString(R.string.report_status_inactive),
                inactive
        ));

        DashboardStatistics.StatusStat pending =
                statistics.findBookingStatus("pending");
        DashboardStatistics.StatusStat approved =
                statistics.findBookingStatus("approved");
        DashboardStatistics.StatusStat rejected =
                statistics.findBookingStatus("rejected");
        DashboardStatistics.StatusStat cancelled =
                statistics.findBookingStatus("cancelled");

        bookingBarChart.setData(
                new long[]{
                        pending.getTotal(),
                        approved.getTotal(),
                        rejected.getTotal(),
                        cancelled.getTotal()
                },
                new String[]{
                        getString(R.string.report_pending_short),
                        getString(R.string.report_approved_short),
                        getString(R.string.report_rejected_short),
                        getString(R.string.report_cancelled_short)
                }
        );

        reportContent.setVisibility(View.VISIBLE);
        tvError.setVisibility(View.GONE);
        updateTrend(btnTrend30Days.isChecked() ? 30 : 7);
    }

    private void updateTrend(int days) {
        if (statistics == null) {
            return;
        }

        if (days == 30) {
            btnTrend30Days.setChecked(true);
        } else {
            btnTrend7Days.setChecked(true);
        }

        DashboardStatistics.BookingActivity activity =
                statistics.getBookingActivity();
        bookingLineChart.setData(activity.getTrend30Days(), days);

        long total = days == 30
                ? activity.getLast30Days()
                : activity.getLast7Days();
        tvTrendTotal.setText(getString(
                R.string.report_trend_total,
                days,
                total
        ));
    }

    private String legendText(
            String label,
            DashboardStatistics.StatusStat stat
    ) {
        return getString(
                R.string.report_legend_format,
                label,
                stat.getTotal(),
                stat.getPercentage()
        );
    }

    private String formatNumber(long value) {
        return NumberFormat.getIntegerInstance(Locale.getDefault()).format(value);
    }

    private String formatGeneratedAt(String value) {
        if (value == null || value.trim().isEmpty()) {
            return getString(R.string.information_not_available);
        }

        String result = value.trim().replace('T', ' ');
        if (result.length() >= 16) {
            return result.substring(0, 16);
        }
        return result;
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRefresh.setEnabled(!loading);

        if (loading && statistics == null) {
            reportContent.setVisibility(View.GONE);
            tvError.setVisibility(View.GONE);
        }
    }

    private void showError(String message) {
        reportContent.setVisibility(View.GONE);
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private String readApiError(ResponseBody errorBody, String fallback) {
        if (errorBody == null) {
            return fallback;
        }

        try {
            JsonElement element = JsonParser.parseString(errorBody.string());
            if (!element.isJsonObject()) {
                return fallback;
            }

            JsonObject error = element.getAsJsonObject();
            String[] fields = {"message", "msg", "error_description", "error"};
            for (String field : fields) {
                JsonElement value = error.get(field);
                if (value != null && !value.isJsonNull()) {
                    String message = value.getAsString().trim();
                    if (!message.isEmpty()) {
                        return message;
                    }
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // Dùng thông báo dự phòng.
        }

        return fallback;
    }

    private String networkError(Throwable throwable) {
        String detail = throwable.getMessage();
        return getString(
                R.string.room_load_error_network,
                detail == null || detail.trim().isEmpty()
                        ? getString(R.string.unknown_error)
                        : detail
        );
    }

    private void handleExpiredSession() {
        Toast.makeText(
                this,
                R.string.session_expired_message,
                Toast.LENGTH_LONG
        ).show();
        sessionManager.clearSession();
        openLogin();
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );
        startActivity(intent);
        finish();
    }
}