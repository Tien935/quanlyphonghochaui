package com.example.phonghochaui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.phonghochaui.data.model.AdminUser;
import com.example.phonghochaui.data.remote.RetrofitClient;
import com.example.phonghochaui.data.remote.SessionManager;
import com.example.phonghochaui.data.remote.SupabaseApiService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserManagementActivity extends AppCompatActivity {

    private final List<AdminUser> allUsers =
            new ArrayList<>();

    private final List<String> roleValues =
            Arrays.asList(
                    "all",
                    "admin",
                    "student"
            );

    private TextInputEditText etSearch;
    private AutoCompleteTextView actRole;
    private TextView tvCount;
    private TextView tvState;
    private ListView listUsers;
    private LinearProgressIndicator progress;
    private MaterialButton btnRefresh;

    private SessionManager sessionManager;
    private SupabaseApiService apiService;
    private AdminUserAdapter adapter;

    private String selectedRole = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        if (!sessionManager.hasSession()
                || !"admin".equals(
                sessionManager.getUserRole()
        )) {
            openLogin();
            return;
        }

        EdgeToEdge.enable(this);

        setContentView(
                R.layout.activity_user_management
        );

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(
                        R.id.userManagementRoot
                ),
                (view, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
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
        setupRoleFilter();

        apiService =
                RetrofitClient.getApiService(this);

        adapter = new AdminUserAdapter(
                this,
                sessionManager.getUserId(),
                this::showLockConfirmation
        );

        listUsers.setAdapter(adapter);

        findViewById(
                R.id.btnUserManagementBack
        ).setOnClickListener(
                view -> finish()
        );

        btnRefresh.setOnClickListener(
                view -> loadUsers()
        );

        etSearch.addTextChangedListener(
                new TextWatcher() {
                    @Override
                    public void beforeTextChanged(
                            CharSequence value,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence value,
                            int start,
                            int before,
                            int count
                    ) {
                        applyFilters();
                    }

                    @Override
                    public void afterTextChanged(
                            Editable value
                    ) {
                    }
                }
        );

        loadUsers();
    }

    private void bindViews() {
        etSearch = findViewById(
                R.id.etAdminUserSearch
        );

        actRole = findViewById(
                R.id.actAdminUserRole
        );

        tvCount = findViewById(
                R.id.tvAdminUserCount
        );

        tvState = findViewById(
                R.id.tvAdminUserState
        );

        listUsers = findViewById(
                R.id.listAdminUsers
        );

        progress = findViewById(
                R.id.adminUserProgress
        );

        btnRefresh = findViewById(
                R.id.btnRefreshAdminUsers
        );
    }

    private void setupRoleFilter() {
        List<String> labels = Arrays.asList(
                getString(R.string.filter_all),
                getString(
                        R.string.admin_user_role_admin
                ),
                getString(
                        R.string.admin_user_role_student
                )
        );

        actRole.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                labels
        ));

        actRole.setText(
                labels.get(0),
                false
        );

        actRole.setOnItemClickListener(
                (parent, view, position, id) -> {
                    selectedRole =
                            roleValues.get(position);

                    applyFilters();
                }
        );
    }

    private void loadUsers() {
        setLoading(true);

        JsonObject request = new JsonObject();

        request.add(
                "p_role",
                JsonNull.INSTANCE
        );

        request.add(
                "p_query",
                JsonNull.INSTANCE
        );

        apiService.listAdminUsers(request)
                .enqueue(
                        new Callback<List<AdminUser>>() {
                            @Override
                            public void onResponse(
                                    Call<List<AdminUser>> call,
                                    Response<List<AdminUser>> response
                            ) {
                                setLoading(false);

                                if (response.code() == 401) {
                                    handleExpiredSession();
                                    return;
                                }

                                if (!response.isSuccessful()
                                        || response.body() == null) {
                                    showState(readApiError(
                                            response.errorBody(),
                                            getString(
                                                    R.string.admin_user_load_error,
                                                    response.code()
                                            )
                                    ));

                                    return;
                                }

                                allUsers.clear();
                                allUsers.addAll(
                                        response.body()
                                );

                                applyFilters();
                            }

                            @Override
                            public void onFailure(
                                    Call<List<AdminUser>> call,
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

    private void applyFilters() {
        if (adapter == null) {
            return;
        }

        String query =
                etSearch.getText() == null
                        ? ""
                        : etSearch.getText()
                        .toString()
                        .trim()
                        .toLowerCase(Locale.ROOT);

        List<AdminUser> filtered =
                new ArrayList<>();

        for (AdminUser user : allUsers) {
            String searchable = (
                    user.getFullName() + " "
                            + user.getHauiCode() + " "
                            + user.getEmail() + " "
                            + user.getId()
            ).toLowerCase(Locale.ROOT);

            boolean matchesQuery =
                    query.isEmpty()
                            || searchable.contains(query);

            boolean matchesRole =
                    "all".equals(selectedRole)
                            || selectedRole.equalsIgnoreCase(
                            user.getRole()
                    );

            if (matchesQuery && matchesRole) {
                filtered.add(user);
            }
        }

        adapter.updateData(filtered);

        tvCount.setText(getString(
                R.string.admin_user_count,
                filtered.size()
        ));

        if (filtered.isEmpty()) {
            showState(getString(
                    R.string.admin_user_empty
            ));
        } else {
            tvState.setVisibility(View.GONE);
            listUsers.setVisibility(View.VISIBLE);
        }
    }

    private void showLockConfirmation(
            AdminUser user
    ) {
        boolean willLock = !user.isLocked();

        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        willLock
                                ? R.string.lock_account_title
                                : R.string.unlock_account_title
                )
                .setMessage(getString(
                        willLock
                                ? R.string.lock_account_message
                                : R.string.unlock_account_message,
                        user.getDisplayName()
                ))
                .setNegativeButton(
                        R.string.cancel,
                        null
                )
                .setPositiveButton(
                        willLock
                                ? R.string.lock_account
                                : R.string.unlock_account,
                        (dialog, which) ->
                                setUserLocked(
                                        user,
                                        willLock
                                )
                )
                .show();
    }

    private void setUserLocked(
            AdminUser user,
            boolean locked
    ) {
        setLoading(true);

        JsonObject request = new JsonObject();

        request.addProperty(
                "p_user_id",
                user.getId()
        );

        request.addProperty(
                "p_locked",
                locked
        );

        apiService.setAdminUserLocked(request)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(
                            Call<JsonObject> call,
                            Response<JsonObject> response
                    ) {
                        if (response.code() == 401) {
                            setLoading(false);
                            handleExpiredSession();
                            return;
                        }

                        if (!response.isSuccessful()) {
                            setLoading(false);

                            Toast.makeText(
                                    UserManagementActivity.this,
                                    readApiError(
                                            response.errorBody(),
                                            getString(
                                                    R.string.admin_user_status_update_failed
                                            )
                                    ),
                                    Toast.LENGTH_LONG
                            ).show();

                            applyFilters();
                            return;
                        }

                        String message = getString(
                                locked
                                        ? R.string.lock_account_success
                                        : R.string.unlock_account_success
                        );

                        JsonObject body =
                                response.body();

                        if (body != null
                                && body.has("message")
                                && !body.get("message")
                                .isJsonNull()) {
                            message = body
                                    .get("message")
                                    .getAsString();
                        }

                        Toast.makeText(
                                UserManagementActivity.this,
                                message,
                                Toast.LENGTH_SHORT
                        ).show();

                        loadUsers();
                    }

                    @Override
                    public void onFailure(
                            Call<JsonObject> call,
                            Throwable throwable
                    ) {
                        setLoading(false);

                        Toast.makeText(
                                UserManagementActivity.this,
                                networkError(throwable),
                                Toast.LENGTH_LONG
                        ).show();

                        applyFilters();
                    }
                });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        btnRefresh.setEnabled(!loading);

        listUsers.setVisibility(
                loading ? View.GONE : View.VISIBLE
        );

        if (loading) {
            tvState.setVisibility(View.GONE);

            tvCount.setText(
                    R.string.admin_user_loading
            );
        }
    }

    private void showState(String message) {
        listUsers.setVisibility(View.GONE);

        tvState.setText(message);
        tvState.setVisibility(View.VISIBLE);
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
                    String message = value
                            .getAsString()
                            .trim();

                    if (!message.isEmpty()) {
                        return message;
                    }
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // Sử dụng thông báo dự phòng.
        }

        return fallback;
    }

    private String networkError(
            Throwable throwable
    ) {
        String detail = throwable.getMessage();

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