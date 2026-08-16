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

import com.example.phonghochaui.data.model.NotificationItem;
import com.example.phonghochaui.data.remote.RetrofitClient;
import com.example.phonghochaui.data.remote.SessionManager;
import com.example.phonghochaui.data.remote.SupabaseApiService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentHomeActivity
        extends AppCompatActivity {

    /*
     * Kiểm tra số thông báo mới sau mỗi 15 giây
     * khi màn hình trang chủ đang hiển thị.
     */
    private static final long
            BADGE_REFRESH_INTERVAL_MS = 15_000L;

    private SessionManager sessionManager;
    private SupabaseApiService apiService;

    private TextView tvNotificationBadge;

    private boolean badgeRequestRunning;

    private final Handler badgeHandler =
            new Handler(
                    Looper.getMainLooper()
            );

    private final Runnable badgeRefreshTask =
            new Runnable() {
                @Override
                public void run() {
                    loadUnreadNotificationCount();

                    badgeHandler.postDelayed(
                            this,
                            BADGE_REFRESH_INTERVAL_MS
                    );
                }
            };

    @Override
    protected void onCreate(
            Bundle savedInstanceState
    ) {
        super.onCreate(savedInstanceState);

        sessionManager =
                new SessionManager(this);

        if (!sessionManager.hasSession()
                || !"student".equals(
                sessionManager.getUserRole()
        )) {
            openLogin();
            return;
        }

        EdgeToEdge.enable(this);

        setContentView(
                R.layout.activity_student_home
        );

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(
                        R.id.studentHomeRoot
                ),
                (view, insets) -> {
                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type
                                            .systemBars()
                            );

                    view.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        TextView tvStudentEmail =
                findViewById(
                        R.id.tvStudentEmail
                );

        tvStudentEmail.setText(
                sessionManager.getUserEmail()
        );

        tvNotificationBadge =
                findViewById(
                        R.id.tvStudentNotificationBadge
                );

        apiService =
                RetrofitClient.getApiService(this);

        findViewById(R.id.btnStudentFindRoom)
                .setOnClickListener(
                        view -> startActivity(
                                new Intent(
                                        this,
                                        FindRoomActivity.class
                                )
                        )
                );

        findViewById(R.id.btnStudentCreateBooking)
                .setOnClickListener(
                        view -> startActivity(
                                new Intent(
                                        this,
                                        CreateBookingActivity.class
                                )
                        )
                );

        findViewById(R.id.btnStudentBookings)
                .setOnClickListener(
                        view -> startActivity(
                                new Intent(
                                        this,
                                        MyBookingsActivity.class
                                )
                        )
                );

        findViewById(R.id.btnStudentNotifications)
                .setOnClickListener(
                        view -> startActivity(
                                new Intent(
                                        this,
                                        NotificationsActivity.class
                                )
                        )
                );

        findViewById(R.id.btnStudentLogout)
                .setOnClickListener(
                        view -> showLogoutConfirmation()
                );
    }

    @Override
    protected void onResume() {
        super.onResume();

        badgeHandler.removeCallbacks(
                badgeRefreshTask
        );

        /*
         * Cập nhật ngay khi quay lại trang chủ.
         */
        badgeRefreshTask.run();
    }

    @Override
    protected void onPause() {
        badgeHandler.removeCallbacks(
                badgeRefreshTask
        );

        super.onPause();
    }

    private void loadUnreadNotificationCount() {
        if (apiService == null
                || badgeRequestRunning) {
            return;
        }

        String userId =
                sessionManager.getUserId();

        if (userId.isEmpty()) {
            showBadge(
                    tvNotificationBadge,
                    0
            );
            return;
        }

        badgeRequestRunning = true;

        apiService.getUnreadNotifications(
                "id",
                "eq." + userId,
                "eq.false"
        ).enqueue(
                new Callback<List<NotificationItem>>() {
                    @Override
                    public void onResponse(
                            Call<List<NotificationItem>> call,
                            Response<List<NotificationItem>> response
                    ) {
                        badgeRequestRunning = false;

                        if (response.isSuccessful()
                                && response.body() != null) {

                            showBadge(
                                    tvNotificationBadge,
                                    response.body().size()
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<List<NotificationItem>> call,
                            Throwable throwable
                    ) {
                        badgeRequestRunning = false;
                    }
                }
        );
    }

    private void showBadge(
            TextView badge,
            int count
    ) {
        if (count <= 0) {
            badge.setText("");

            badge.setVisibility(
                    View.GONE
            );

            return;
        }

        badge.setText(
                count > 99
                        ? "99+"
                        : String.valueOf(count)
        );

        badge.setVisibility(
                View.VISIBLE
        );
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        R.string.logout_title
                )
                .setMessage(
                        R.string.logout_message
                )
                .setNegativeButton(
                        R.string.cancel,
                        null
                )
                .setPositiveButton(
                        R.string.logout,
                        (dialog, which) -> logout()
                )
                .show();
    }

    private void logout() {
        badgeHandler.removeCallbacks(
                badgeRefreshTask
        );

        sessionManager.clearSession();

        openLogin();
    }

    private void openLogin() {
        Intent intent = new Intent(
                this,
                LoginActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}