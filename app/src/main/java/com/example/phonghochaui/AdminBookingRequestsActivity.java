package com.example.phonghochaui;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.phonghochaui.data.model.AdminBooking;
import com.example.phonghochaui.data.remote.RetrofitClient;
import com.example.phonghochaui.data.remote.SessionManager;
import com.example.phonghochaui.data.remote.SupabaseApiService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminBookingRequestsActivity
        extends AppCompatActivity {

    private final String[] statusValues = {
            "pending",
            "all",
            "approved",
            "rejected",
            "cancelled"
    };

    private AutoCompleteTextView actStatus;
    private ListView listBookings;
    private TextView tvCount;
    private TextView tvState;
    private LinearProgressIndicator progress;
    private MaterialButton btnRefresh;

    private AdminBookingAdapter adapter;
    private SupabaseApiService apiService;
    private SessionManager sessionManager;

    private String selectedStatus = "pending";

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
                R.layout.activity_admin_booking_requests
        );

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.adminBookingRoot),
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
        setupStatusFilter();

        apiService =
                RetrofitClient.getApiService(this);

        adapter =
                new AdminBookingAdapter(this);

        listBookings.setAdapter(adapter);

        findViewById(R.id.btnAdminBookingBack)
                .setOnClickListener(
                        view -> finish()
                );

        btnRefresh.setOnClickListener(
                view -> loadBookings()
        );

        listBookings.setOnItemClickListener(
                (parent, view, position, id) -> {
                    AdminBooking booking =
                            adapter.getItem(position);

                    if ("pending".equalsIgnoreCase(
                            booking.getStatus()
                    )) {
                        showReviewDialog(booking);
                    } else {
                        Toast.makeText(
                                this,
                                R.string.admin_booking_already_reviewed,
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                }
        );

        loadBookings();
    }

    private void bindViews() {
        actStatus = findViewById(
                R.id.actAdminBookingStatus
        );

        listBookings = findViewById(
                R.id.listAdminBookings
        );

        tvCount = findViewById(
                R.id.tvAdminBookingCount
        );

        tvState = findViewById(
                R.id.tvAdminBookingState
        );

        progress = findViewById(
                R.id.adminBookingProgress
        );

        btnRefresh = findViewById(
                R.id.btnRefreshAdminBookings
        );
    }

    private void setupStatusFilter() {
        List<String> labels = Arrays.asList(
                getString(
                        R.string.booking_status_pending
                ),
                getString(
                        R.string.filter_all
                ),
                getString(
                        R.string.booking_status_approved
                ),
                getString(
                        R.string.booking_status_rejected
                ),
                getString(
                        R.string.booking_status_cancelled
                )
        );

        actStatus.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                labels
        ));

        actStatus.setText(
                labels.get(0),
                false
        );

        actStatus.setOnItemClickListener(
                (parent, view, position, id) -> {
                    if (position >= 0
                            && position < statusValues.length) {
                        selectedStatus =
                                statusValues[position];

                        loadBookings();
                    }
                }
        );
    }

    private void loadBookings() {
        setLoading(true);

        JsonObject request = new JsonObject();

        if ("all".equals(selectedStatus)) {
            request.add(
                    "p_status",
                    JsonNull.INSTANCE
            );
        } else {
            request.addProperty(
                    "p_status",
                    selectedStatus
            );
        }

        apiService.listAdminBookings(request)
                .enqueue(
                        new Callback<List<AdminBooking>>() {
                            @Override
                            public void onResponse(
                                    Call<List<AdminBooking>> call,
                                    Response<List<AdminBooking>> response
                            ) {
                                setLoading(false);

                                if (response.code() == 401) {
                                    Toast.makeText(
                                            AdminBookingRequestsActivity.this,
                                            R.string.session_expired_message,
                                            Toast.LENGTH_LONG
                                    ).show();

                                    openLogin();
                                    return;
                                }

                                if (!response.isSuccessful()
                                        || response.body() == null) {

                                    showState(readApiError(
                                            response.errorBody(),
                                            getString(
                                                    R.string.admin_booking_load_error,
                                                    response.code()
                                            )
                                    ));

                                    return;
                                }

                                adapter.updateData(
                                        response.body()
                                );

                                tvCount.setText(getString(
                                        R.string.admin_booking_count,
                                        response.body().size()
                                ));

                                if (response.body().isEmpty()) {
                                    showState(getString(
                                            R.string.admin_booking_empty
                                    ));
                                } else {
                                    tvState.setVisibility(
                                            View.GONE
                                    );

                                    listBookings.setVisibility(
                                            View.VISIBLE
                                    );
                                }
                            }

                            @Override
                            public void onFailure(
                                    Call<List<AdminBooking>> call,
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

    private void showReviewDialog(
            AdminBooking booking
    ) {
        int horizontalPadding =
                dpToPx(24);

        int verticalPadding =
                dpToPx(8);

        LinearLayout container =
                new LinearLayout(this);

        container.setOrientation(
                LinearLayout.VERTICAL
        );

        container.setPadding(
                horizontalPadding,
                verticalPadding,
                horizontalPadding,
                0
        );

        TextView tvSummary =
                new TextView(this);

        tvSummary.setText(
                reviewSummary(booking)
        );

        tvSummary.setTextColor(
                getColor(R.color.haui_text)
        );

        tvSummary.setTextSize(14);
        tvSummary.setLineSpacing(0, 1.15f);

        container.addView(tvSummary);

        TextInputLayout tilNote =
                new TextInputLayout(this);

        tilNote.setHint(getString(
                R.string.admin_note_label
        ));

        tilNote.setBoxBackgroundMode(
                TextInputLayout.BOX_BACKGROUND_OUTLINE
        );

        LinearLayout.LayoutParams noteLayoutParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        noteLayoutParams.topMargin =
                dpToPx(16);

        tilNote.setLayoutParams(
                noteLayoutParams
        );

        TextInputEditText etNote =
                new TextInputEditText(this);

        etNote.setMinLines(2);
        etNote.setMaxLines(4);

        etNote.setGravity(
                Gravity.TOP | Gravity.START
        );

        etNote.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                        | InputType.TYPE_TEXT_FLAG_MULTI_LINE
        );

        etNote.setFilters(
                new InputFilter[]{
                        new InputFilter.LengthFilter(500)
                }
        );

        etNote.setText(
                booking.getAdminNote()
        );

        tilNote.addView(etNote);
        container.addView(tilNote);

        AlertDialog dialog =
                new MaterialAlertDialogBuilder(this)
                        .setTitle(getString(
                                R.string.admin_review_booking_title,
                                booking.getId()
                        ))
                        .setView(container)
                        .setPositiveButton(
                                R.string.approve,
                                null
                        )
                        .setNegativeButton(
                                R.string.reject,
                                null
                        )
                        .setNeutralButton(
                                R.string.close,
                                null
                        )
                        .create();

        dialog.setOnShowListener(ignored -> {
            dialog.getButton(
                    AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener(view -> {
                tilNote.setError(null);

                reviewBooking(
                        booking,
                        "approved",
                        textOf(etNote),
                        dialog,
                        tilNote
                );
            });

            dialog.getButton(
                    AlertDialog.BUTTON_NEGATIVE
            ).setOnClickListener(view -> {
                String note =
                        textOf(etNote);

                if (note.isEmpty()) {
                    tilNote.setError(getString(
                            R.string.admin_reject_note_required
                    ));
                    return;
                }

                tilNote.setError(null);

                reviewBooking(
                        booking,
                        "rejected",
                        note,
                        dialog,
                        tilNote
                );
            });
        });

        dialog.show();
    }

    private void reviewBooking(
            AdminBooking booking,
            String decision,
            String note,
            AlertDialog dialog,
            TextInputLayout tilNote
    ) {
        setDialogButtonsEnabled(
                dialog,
                false
        );

        JsonObject request =
                new JsonObject();

        request.addProperty(
                "p_booking_id",
                booking.getId()
        );

        request.addProperty(
                "p_decision",
                decision
        );

        if (note.isEmpty()) {
            request.add(
                    "p_admin_note",
                    JsonNull.INSTANCE
            );
        } else {
            request.addProperty(
                    "p_admin_note",
                    note
            );
        }

        apiService.reviewAdminBooking(request)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(
                            Call<JsonObject> call,
                            Response<JsonObject> response
                    ) {
                        if (response.code() == 401) {
                            dialog.dismiss();

                            Toast.makeText(
                                    AdminBookingRequestsActivity.this,
                                    R.string.session_expired_message,
                                    Toast.LENGTH_LONG
                            ).show();

                            openLogin();
                            return;
                        }

                        if (!response.isSuccessful()) {
                            setDialogButtonsEnabled(
                                    dialog,
                                    true
                            );

                            tilNote.setError(
                                    readApiError(
                                            response.errorBody(),
                                            getString(
                                                    R.string.admin_review_failed
                                            )
                                    )
                            );

                            return;
                        }

                        String message = getString(
                                "approved".equals(decision)
                                        ? R.string.admin_approve_success
                                        : R.string.admin_reject_success
                        );

                        JsonObject body =
                                response.body();

                        if (body != null
                                && body.has("message")
                                && !body.get("message")
                                .isJsonNull()) {

                            message = body.get(
                                    "message"
                            ).getAsString();
                        }

                        dialog.dismiss();

                        Toast.makeText(
                                AdminBookingRequestsActivity.this,
                                message,
                                Toast.LENGTH_SHORT
                        ).show();

                        loadBookings();
                    }

                    @Override
                    public void onFailure(
                            Call<JsonObject> call,
                            Throwable throwable
                    ) {
                        setDialogButtonsEnabled(
                                dialog,
                                true
                        );

                        tilNote.setError(
                                networkError(throwable)
                        );
                    }
                });
    }

    private String reviewSummary(
            AdminBooking booking
    ) {
        return getString(
                R.string.admin_review_summary,

                booking.getStudentFullName().isEmpty()
                        ? getString(
                        R.string.information_not_available
                )
                        : booking.getStudentFullName(),

                booking.getStudentHauiCode().isEmpty()
                        ? getString(
                        R.string.information_not_available
                )
                        : booking.getStudentHauiCode(),

                booking.getRoomCode(),

                formatDate(
                        booking.getBookingDate()
                ),

                formatTime(
                        booking.getStartTime()
                ),

                formatTime(
                        booking.getEndTime()
                ),

                booking.getHeadcount(),

                booking.getPurpose()
        );
    }

    private void setDialogButtonsEnabled(
            AlertDialog dialog,
            boolean enabled
    ) {
        dialog.getButton(
                AlertDialog.BUTTON_POSITIVE
        ).setEnabled(enabled);

        dialog.getButton(
                AlertDialog.BUTTON_NEGATIVE
        ).setEnabled(enabled);

        dialog.getButton(
                AlertDialog.BUTTON_NEUTRAL
        ).setEnabled(enabled);
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        btnRefresh.setEnabled(!loading);

        listBookings.setVisibility(
                loading ? View.GONE : View.VISIBLE
        );

        if (loading) {
            tvState.setVisibility(View.GONE);

            tvCount.setText(
                    R.string.admin_booking_loading
            );
        }
    }

    private void showState(String message) {
        listBookings.setVisibility(View.GONE);

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

                    String message =
                            value.getAsString().trim();

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

    private String textOf(
            TextInputEditText editText
    ) {
        return editText.getText() == null
                ? ""
                : editText.getText()
                .toString()
                .trim();
    }

    private String formatDate(
            String apiDate
    ) {
        if (apiDate == null
                || apiDate.length() != 10) {
            return apiDate == null
                    ? ""
                    : apiDate;
        }

        return apiDate.substring(8, 10)
                + "/"
                + apiDate.substring(5, 7)
                + "/"
                + apiDate.substring(0, 4);
    }

    private String formatTime(
            String apiTime
    ) {
        return apiTime != null
                && apiTime.length() >= 5
                ? apiTime.substring(0, 5)
                : apiTime;
    }

    private int dpToPx(int dp) {
        return Math.round(
                dp * getResources()
                        .getDisplayMetrics()
                        .density
        );
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