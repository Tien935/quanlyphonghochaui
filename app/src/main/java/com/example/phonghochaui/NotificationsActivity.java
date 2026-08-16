package com.example.phonghochaui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.phonghochaui.data.model.NotificationItem;
import com.example.phonghochaui.data.remote.RetrofitClient;
import com.example.phonghochaui.data.remote.SessionManager;
import com.example.phonghochaui.data.remote.SupabaseApiService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationsActivity
        extends AppCompatActivity {

    private static final String NOTIFICATION_SELECT =
            "id,title,content,"
                    + "notification_type,is_read,created_at";

    private final List<NotificationItem> notifications =
            new ArrayList<>();

    private ListView listNotifications;
    private TextView tvCount;
    private TextView tvState;
    private LinearProgressIndicator progress;
    private MaterialButton btnRefresh;
    private MaterialButton btnMarkAllRead;

    private NotificationAdapter adapter;
    private SupabaseApiService apiService;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
                R.layout.activity_notifications
        );

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(
                        R.id.notificationsRoot
                ),
                (view, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type
                                    .systemBars()
                    );

                    view.setPadding(
                            bars.left,
                            bars.top,
                            bars.right,
                            bars.bottom
                    );

                    return insets;
                }
        );

        bindViews();

        apiService =
                RetrofitClient.getApiService(this);

        adapter =
                new NotificationAdapter(this);

        listNotifications.setAdapter(adapter);

        findViewById(R.id.btnNotificationsBack)
                .setOnClickListener(
                        view -> finish()
                );

        btnRefresh.setOnClickListener(
                view -> loadNotifications()
        );

        btnMarkAllRead.setOnClickListener(
                view -> markAllAsRead()
        );

        listNotifications.setOnItemClickListener(
                (parent, view, position, id) -> {
                    NotificationItem item =
                            adapter.getItem(position);

                    if (!item.isRead()) {
                        markOneAsRead(item);
                    }
                }
        );

        loadNotifications();
    }

    private void bindViews() {
        listNotifications = findViewById(
                R.id.listNotifications
        );

        tvCount = findViewById(
                R.id.tvNotificationCount
        );

        tvState = findViewById(
                R.id.tvNotificationState
        );

        progress = findViewById(
                R.id.notificationProgress
        );

        btnRefresh = findViewById(
                R.id.btnRefreshNotifications
        );

        btnMarkAllRead = findViewById(
                R.id.btnMarkAllNotificationsRead
        );
    }

    private void loadNotifications() {
        String userId =
                sessionManager.getUserId();

        if (userId.isEmpty()) {
            sessionManager.clearSession();
            openLogin();
            return;
        }

        setLoading(true);

        apiService.getNotifications(
                NOTIFICATION_SELECT,
                "eq." + userId,
                "created_at.desc"
        ).enqueue(
                new Callback<List<NotificationItem>>() {
                    @Override
                    public void onResponse(
                            Call<List<NotificationItem>> call,
                            Response<List<NotificationItem>> response
                    ) {
                        setLoading(false);

                        if (response.code() == 401) {
                            showSessionExpired();
                            return;
                        }

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            showState(readApiError(
                                    response.errorBody(),
                                    getString(
                                            R.string.notification_load_error,
                                            response.code()
                                    )
                            ));

                            return;
                        }

                        notifications.clear();

                        notifications.addAll(
                                response.body()
                        );

                        adapter.updateData(
                                notifications
                        );

                        updateSummary();

                        if (notifications.isEmpty()) {
                            showState(getString(
                                    R.string.notification_empty
                            ));

                        } else {
                            tvState.setVisibility(
                                    View.GONE
                            );

                            listNotifications.setVisibility(
                                    View.VISIBLE
                            );
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<List<NotificationItem>> call,
                            Throwable throwable
                    ) {
                        setLoading(false);

                        showState(
                                networkError(throwable)
                        );
                    }
                }
        );
    }

    private void markOneAsRead(
            NotificationItem item
    ) {
        JsonObject body =
                new JsonObject();

        body.addProperty(
                "is_read",
                true
        );

        apiService.updateNotifications(
                "eq." + item.getId(),
                "eq." + sessionManager.getUserId(),
                null,
                body
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(
                    Call<Void> call,
                    Response<Void> response
            ) {
                if (response.code() == 401) {
                    showSessionExpired();
                    return;
                }

                if (!response.isSuccessful()) {
                    Toast.makeText(
                            NotificationsActivity.this,

                            readApiError(
                                    response.errorBody(),

                                    getString(
                                            R.string.notification_update_error
                                    )
                            ),

                            Toast.LENGTH_LONG
                    ).show();

                    return;
                }

                item.setRead(true);

                adapter.notifyDataSetChanged();

                updateSummary();
            }

            @Override
            public void onFailure(
                    Call<Void> call,
                    Throwable throwable
            ) {
                Toast.makeText(
                        NotificationsActivity.this,
                        networkError(throwable),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void markAllAsRead() {
        if (unreadCount() == 0) {
            return;
        }

        btnMarkAllRead.setEnabled(false);

        JsonObject body =
                new JsonObject();

        body.addProperty(
                "is_read",
                true
        );

        apiService.updateNotifications(
                null,
                "eq." + sessionManager.getUserId(),
                "eq.false",
                body
        ).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(
                    Call<Void> call,
                    Response<Void> response
            ) {
                if (response.code() == 401) {
                    showSessionExpired();
                    return;
                }

                if (!response.isSuccessful()) {
                    btnMarkAllRead.setEnabled(true);

                    Toast.makeText(
                            NotificationsActivity.this,

                            readApiError(
                                    response.errorBody(),

                                    getString(
                                            R.string.notification_update_error
                                    )
                            ),

                            Toast.LENGTH_LONG
                    ).show();

                    return;
                }

                for (
                        NotificationItem item
                        : notifications
                ) {
                    item.setRead(true);
                }

                adapter.notifyDataSetChanged();

                updateSummary();

                Toast.makeText(
                        NotificationsActivity.this,
                        R.string.notification_all_read_success,
                        Toast.LENGTH_SHORT
                ).show();
            }

            @Override
            public void onFailure(
                    Call<Void> call,
                    Throwable throwable
            ) {
                btnMarkAllRead.setEnabled(true);

                Toast.makeText(
                        NotificationsActivity.this,
                        networkError(throwable),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void updateSummary() {
        int unread = unreadCount();

        tvCount.setText(getString(
                R.string.notification_count,
                notifications.size(),
                unread
        ));

        btnMarkAllRead.setEnabled(
                unread > 0
        );
    }

    private int unreadCount() {
        int count = 0;

        for (
                NotificationItem item
                : notifications
        ) {
            if (!item.isRead()) {
                count++;
            }
        }

        return count;
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        btnRefresh.setEnabled(!loading);

        btnMarkAllRead.setEnabled(
                !loading && unreadCount() > 0
        );

        listNotifications.setVisibility(
                loading
                        ? View.GONE
                        : View.VISIBLE
        );

        if (loading) {
            tvState.setVisibility(View.GONE);

            tvCount.setText(
                    R.string.notification_loading
            );
        }
    }

    private void showState(String message) {
        listNotifications.setVisibility(
                View.GONE
        );

        tvState.setText(message);

        tvState.setVisibility(
                View.VISIBLE
        );

        btnMarkAllRead.setEnabled(false);
    }

    private String readApiError(
            ResponseBody errorBody,
            String fallback
    ) {
        if (errorBody == null) {
            return fallback;
        }

        try {
            JsonElement element =
                    JsonParser.parseString(
                            errorBody.string()
                    );

            if (!element.isJsonObject()) {
                return fallback;
            }

            JsonObject error =
                    element.getAsJsonObject();

            String[] fields = {
                    "message",
                    "msg",
                    "error_description",
                    "error"
            };

            for (String field : fields) {
                JsonElement value =
                        error.get(field);

                if (value != null
                        && !value.isJsonNull()) {

                    String message =
                            value.getAsString()
                                    .trim();

                    if (!message.isEmpty()) {
                        return message;
                    }
                }
            }

        } catch (
                IOException
                | RuntimeException ignored
        ) {
            // Dùng thông báo dự phòng.
        }

        return fallback;
    }

    private String networkError(
            Throwable throwable
    ) {
        String detail =
                throwable.getMessage();

        return getString(
                R.string.room_load_error_network,

                detail == null
                        || detail.trim().isEmpty()
                        ? getString(
                        R.string.unknown_error
                )
                        : detail
        );
    }

    private void showSessionExpired() {
        sessionManager.clearSession();

        Toast.makeText(
                this,
                R.string.session_expired_message,
                Toast.LENGTH_LONG
        ).show();

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