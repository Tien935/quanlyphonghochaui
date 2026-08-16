package com.example.phonghochaui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.phonghochaui.data.model.AdminBooking;
import com.example.phonghochaui.data.remote.RetrofitClient;
import com.example.phonghochaui.data.remote.SessionManager;
import com.example.phonghochaui.data.remote.SupabaseApiService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.gson.JsonObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminHomeActivity extends AppCompatActivity {

    private static final long BADGE_REFRESH_INTERVAL_MS = 15_000L;

    private SessionManager sessionManager;
    private SupabaseApiService apiService;
    private TextView tvPendingBadge;
    private boolean badgeRequestRunning;

    private final Handler badgeHandler = new Handler(Looper.getMainLooper());
    private final Runnable badgeRefreshTask = new Runnable() {
        @Override
        public void run() {
            loadPendingBookingCount();
            badgeHandler.postDelayed(this, BADGE_REFRESH_INTERVAL_MS);
        }
    };

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
        setContentView(R.layout.activity_admin_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.adminHomeRoot), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        TextView tvAdminEmail = findViewById(R.id.tvAdminEmail);
        tvAdminEmail.setText(sessionManager.getUserEmail());
        tvPendingBadge = findViewById(R.id.tvAdminPendingBadge);
        apiService = RetrofitClient.getApiService(this);

        findViewById(R.id.btnAdminRooms).setOnClickListener(
                view -> startActivity(new Intent(
                        this,
                        RoomManagementActivity.class
                ))
        );
        findViewById(R.id.btnAdminRequests).setOnClickListener(
                view -> startActivity(new Intent(
                        this,
                        AdminBookingRequestsActivity.class
                ))
        );
        findViewById(R.id.btnAdminUsers).setOnClickListener(
                view -> startActivity(new Intent(
                        this,
                        UserManagementActivity.class
                ))
        );
        findViewById(R.id.btnAdminReports).setOnClickListener(
                view -> startActivity(new Intent(
                        this,
                        AdminReportsActivity.class
                ))
        );
        findViewById(R.id.btnAdminLogout).setOnClickListener(
                view -> showLogoutConfirmation()
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        badgeHandler.removeCallbacks(badgeRefreshTask);
        badgeRefreshTask.run();
    }

    @Override
    protected void onPause() {
        badgeHandler.removeCallbacks(badgeRefreshTask);
        super.onPause();
    }

    private void loadPendingBookingCount() {
        if (apiService == null || badgeRequestRunning) {
            return;
        }

        badgeRequestRunning = true;

        JsonObject request = new JsonObject();
        request.addProperty("p_status", "pending");

        apiService.listAdminBookings(request)
                .enqueue(new Callback<List<AdminBooking>>() {
                    @Override
                    public void onResponse(
                            Call<List<AdminBooking>> call,
                            Response<List<AdminBooking>> response
                    ) {
                        badgeRequestRunning = false;

                        if (response.isSuccessful() && response.body() != null) {
                            showBadge(tvPendingBadge, response.body().size());
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<List<AdminBooking>> call,
                            Throwable throwable
                    ) {
                        badgeRequestRunning = false;
                    }
                });
    }

    private void showBadge(TextView badge, int count) {
        if (count <= 0) {
            badge.setText("");
            badge.setVisibility(View.GONE);
            return;
        }

        badge.setText(count > 99 ? "99+" : String.valueOf(count));
        badge.setVisibility(View.VISIBLE);
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.logout, (dialog, which) -> logout())
                .show();
    }

    private void logout() {
        sessionManager.clearSession();
        openLogin();
    }

    private void openLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}