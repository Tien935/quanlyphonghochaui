package com.example.phonghochaui.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.phonghochaui.LoginActivity;
import com.example.phonghochaui.R;
import com.example.phonghochaui.data.model.NotificationItem;
import com.example.phonghochaui.data.remote.RetrofitClient;
import com.example.phonghochaui.data.remote.SessionManager;
import com.example.phonghochaui.data.remote.SupabaseApiService;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentHomeFragment extends Fragment {

    private static final long BADGE_REFRESH_INTERVAL_MS = 15_000L;

    private SessionManager sessionManager;
    private SupabaseApiService apiService;

    private TextView tvNotificationBadge;
    private boolean badgeRequestRunning;

    private final Handler badgeHandler = new Handler(Looper.getMainLooper());
    private final Runnable badgeRefreshTask = new Runnable() {
        @Override
        public void run() {
            loadUnreadNotificationCount();
            badgeHandler.postDelayed(this, BADGE_REFRESH_INTERVAL_MS);
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_student_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());

        if (!sessionManager.hasSession() || !"student".equals(sessionManager.getUserRole())) {
            openLogin();
            return;
        }

        TextView tvStudentEmail = view.findViewById(R.id.tvStudentEmail);
        tvStudentEmail.setText(sessionManager.getUserEmail());

        tvNotificationBadge = view.findViewById(R.id.tvStudentNotificationBadge);
        apiService = RetrofitClient.getApiService(requireContext());

        // Fetch BottomNavigationView from the Activity
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav =
                requireActivity().findViewById(R.id.bottom_nav_student);

        // Buttons click listeners
        view.findViewById(R.id.btnStudentFindRoom).setOnClickListener(v -> {
            Navigation.findNavController(v).navigate(R.id.nav_student_find_room);
        });

        // Use BottomNavigationView to switch tabs to ensure proper back stack management
        view.findViewById(R.id.btnStudentCreateBooking).setOnClickListener(v -> {
            if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_student_booking);
        });

        view.findViewById(R.id.btnStudentBookings).setOnClickListener(v -> {
            if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_student_schedule);
        });

        view.findViewById(R.id.btnStudentNotifications).setOnClickListener(v -> {
            if (bottomNav != null) bottomNav.setSelectedItemId(R.id.nav_student_notifications);
        });

        view.findViewById(R.id.btnStudentLogout).setOnClickListener(v -> showLogoutConfirmation());
    }

    @Override
    public void onResume() {
        super.onResume();
        badgeHandler.removeCallbacks(badgeRefreshTask);
        badgeRefreshTask.run();
    }

    @Override
    public void onPause() {
        badgeHandler.removeCallbacks(badgeRefreshTask);
        super.onPause();
    }

    private void loadUnreadNotificationCount() {
        if (apiService == null || badgeRequestRunning) {
            return;
        }
        String userId = sessionManager.getUserId();
        if (userId.isEmpty()) {
            showBadge(tvNotificationBadge, 0);
            return;
        }
        badgeRequestRunning = true;
        apiService.getUnreadNotifications("id", "eq." + userId, "eq.false")
            .enqueue(new Callback<List<NotificationItem>>() {
                @Override
                public void onResponse(Call<List<NotificationItem>> call, Response<List<NotificationItem>> response) {
                    badgeRequestRunning = false;
                    if (response.isSuccessful() && response.body() != null) {
                        showBadge(tvNotificationBadge, response.body().size());
                    }
                }
                @Override
                public void onFailure(Call<List<NotificationItem>> call, Throwable throwable) {
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
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.logout_title)
                .setMessage(R.string.logout_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.logout, (dialog, which) -> logout())
                .show();
    }

    private void logout() {
        badgeHandler.removeCallbacks(badgeRefreshTask);
        sessionManager.clearSession();
        openLogin();
    }

    private void openLogin() {
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finish();
    }
}
