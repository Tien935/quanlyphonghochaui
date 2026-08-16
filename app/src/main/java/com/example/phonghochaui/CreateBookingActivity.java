package com.example.phonghochaui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.phonghochaui.data.model.Classroom;
import com.example.phonghochaui.data.model.RoomBookingRequest;
import com.example.phonghochaui.data.remote.RetrofitClient;
import com.example.phonghochaui.data.remote.SessionManager;
import com.example.phonghochaui.data.remote.SupabaseApiService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CreateBookingActivity extends AppCompatActivity {

    private static final String CLASSROOM_SELECT =
            "id,room_code,floor,capacity,"
                    + "operational_status,description,"
                    + "building:buildings("
                    + "id,building_code,building_name,"
                    + "total_floors,"
                    + "campus:campuses("
                    + "id,campus_code,campus_name,address"
                    + "))";

    private final List<Classroom> activeClassrooms =
            new ArrayList<>();

    private TextInputLayout tilRoom;
    private TextInputLayout tilDate;
    private TextInputLayout tilStart;
    private TextInputLayout tilEnd;
    private TextInputLayout tilHeadcount;
    private TextInputLayout tilPurpose;

    private AutoCompleteTextView actRoom;
    private TextInputEditText etDate;
    private TextInputEditText etStart;
    private TextInputEditText etEnd;
    private TextInputEditText etHeadcount;
    private TextInputEditText etPurpose;

    private TextView tvRoomHint;
    private TextView tvError;
    private MaterialButton btnSubmit;
    private LinearProgressIndicator progress;

    private SupabaseApiService apiService;
    private Classroom selectedClassroom;
    private Calendar selectedDate;

    private int startHour = -1;
    private int startMinute = -1;
    private int endHour = -1;
    private int endMinute = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SessionManager sessionManager =
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
                R.layout.activity_create_booking
        );

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.createBookingRoot),
                (view, insets) -> {
                    Insets bars = insets.getInsets(
                            WindowInsetsCompat
                                    .Type
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

        findViewById(R.id.btnCreateBookingBack)
                .setOnClickListener(
                        view -> finish()
                );

        etDate.setOnClickListener(
                view -> showDatePicker()
        );

        etStart.setOnClickListener(
                view -> showTimePicker(true)
        );

        etEnd.setOnClickListener(
                view -> showTimePicker(false)
        );

        btnSubmit.setOnClickListener(
                view -> submitBooking()
        );

        actRoom.setOnItemClickListener(
                (parent, view, position, id) -> {
                    if (position >= 0
                            && position < activeClassrooms.size()) {

                        selectedClassroom =
                                activeClassrooms.get(position);

                        tilRoom.setError(null);

                        tvRoomHint.setText(
                                getString(
                                        R.string.booking_room_capacity_hint,
                                        selectedClassroom.getRoomCode(),
                                        selectedClassroom.getCapacity()
                                )
                        );
                    }
                }
        );

        loadActiveClassrooms();
    }

    private void bindViews() {
        tilRoom = findViewById(
                R.id.tilBookingRoom
        );

        tilDate = findViewById(
                R.id.tilBookingDate
        );

        tilStart = findViewById(
                R.id.tilBookingStart
        );

        tilEnd = findViewById(
                R.id.tilBookingEnd
        );

        tilHeadcount = findViewById(
                R.id.tilBookingHeadcount
        );

        tilPurpose = findViewById(
                R.id.tilBookingPurpose
        );

        actRoom = findViewById(
                R.id.actBookingRoom
        );

        etDate = findViewById(
                R.id.etBookingDate
        );

        etStart = findViewById(
                R.id.etBookingStart
        );

        etEnd = findViewById(
                R.id.etBookingEnd
        );

        etHeadcount = findViewById(
                R.id.etBookingHeadcount
        );

        etPurpose = findViewById(
                R.id.etBookingPurpose
        );

        tvRoomHint = findViewById(
                R.id.tvBookingRoomHint
        );

        tvError = findViewById(
                R.id.tvBookingError
        );

        btnSubmit = findViewById(
                R.id.btnSubmitBooking
        );

        progress = findViewById(
                R.id.bookingProgress
        );
    }

    private void loadActiveClassrooms() {
        setLoading(
                true,
                R.string.booking_loading_rooms
        );

        apiService.getClassrooms(
                CLASSROOM_SELECT,
                "room_code.asc"
        ).enqueue(
                new Callback<List<Classroom>>() {
                    @Override
                    public void onResponse(
                            Call<List<Classroom>> call,
                            Response<List<Classroom>> response
                    ) {
                        setLoading(
                                false,
                                R.string.submit_booking
                        );

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            showError(
                                    getString(
                                            R.string.booking_room_load_error,
                                            response.code()
                                    )
                            );

                            return;
                        }

                        activeClassrooms.clear();

                        for (Classroom classroom
                                : response.body()) {

                            if ("active".equalsIgnoreCase(
                                    classroom
                                            .getOperationalStatus()
                            )) {
                                activeClassrooms.add(
                                        classroom
                                );
                            }
                        }

                        List<String> labels =
                                new ArrayList<>();

                        for (Classroom classroom
                                : activeClassrooms) {

                            labels.add(
                                    roomLabel(classroom)
                            );
                        }

                        actRoom.setAdapter(
                                new ArrayAdapter<>(
                                        CreateBookingActivity.this,
                                        android.R.layout
                                                .simple_dropdown_item_1line,
                                        labels
                                )
                        );

                        if (activeClassrooms.isEmpty()) {
                            showError(
                                    getString(
                                            R.string.booking_no_active_rooms
                                    )
                            );

                            btnSubmit.setEnabled(false);
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<List<Classroom>> call,
                            Throwable throwable
                    ) {
                        setLoading(
                                false,
                                R.string.submit_booking
                        );

                        showError(
                                networkError(throwable)
                        );
                    }
                }
        );
    }

    private void showDatePicker() {
        Calendar initial =
                selectedDate == null
                        ? Calendar.getInstance()
                        : selectedDate;

        DatePickerDialog dialog =
                new DatePickerDialog(
                        this,
                        (view, year, month, day) -> {
                            selectedDate =
                                    Calendar.getInstance();

                            selectedDate.set(
                                    year,
                                    month,
                                    day,
                                    0,
                                    0,
                                    0
                            );

                            selectedDate.set(
                                    Calendar.MILLISECOND,
                                    0
                            );

                            SimpleDateFormat displayFormat =
                                    new SimpleDateFormat(
                                            "dd/MM/yyyy",
                                            Locale.getDefault()
                                    );

                            etDate.setText(
                                    displayFormat.format(
                                            selectedDate.getTime()
                                    )
                            );

                            tilDate.setError(null);
                        },
                        initial.get(Calendar.YEAR),
                        initial.get(Calendar.MONTH),
                        initial.get(Calendar.DAY_OF_MONTH)
                );

        Calendar minimum =
                Calendar.getInstance();

        minimum.set(
                Calendar.HOUR_OF_DAY,
                0
        );

        minimum.set(
                Calendar.MINUTE,
                0
        );

        minimum.set(
                Calendar.SECOND,
                0
        );

        minimum.set(
                Calendar.MILLISECOND,
                0
        );

        dialog.getDatePicker().setMinDate(
                minimum.getTimeInMillis()
        );

        dialog.show();
    }

    private void showTimePicker(
            boolean isStart
    ) {
        Calendar now =
                Calendar.getInstance();

        int hour =
                isStart && startHour >= 0
                        ? startHour
                        : (
                        !isStart && endHour >= 0
                                ? endHour
                                : now.get(
                                Calendar.HOUR_OF_DAY
                        )
                );

        int minute =
                isStart && startMinute >= 0
                        ? startMinute
                        : (
                        !isStart && endMinute >= 0
                                ? endMinute
                                : 0
                );

        new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    String value =
                            String.format(
                                    Locale.getDefault(),
                                    "%02d:%02d",
                                    selectedHour,
                                    selectedMinute
                            );

                    if (isStart) {
                        startHour = selectedHour;
                        startMinute = selectedMinute;

                        etStart.setText(value);
                        tilStart.setError(null);
                    } else {
                        endHour = selectedHour;
                        endMinute = selectedMinute;

                        etEnd.setText(value);
                        tilEnd.setError(null);
                    }
                },
                hour,
                minute,
                true
        ).show();
    }

    private void submitBooking() {
        clearErrors();

        String headcountText =
                textOf(etHeadcount);

        String purpose =
                textOf(etPurpose);

        boolean valid = true;

        if (selectedClassroom == null) {
            tilRoom.setError(
                    getString(
                            R.string.booking_room_required
                    )
            );

            valid = false;
        }

        if (selectedDate == null) {
            tilDate.setError(
                    getString(
                            R.string.booking_date_required
                    )
            );

            valid = false;
        }

        if (startHour < 0) {
            tilStart.setError(
                    getString(
                            R.string.booking_start_required
                    )
            );

            valid = false;
        }

        if (endHour < 0) {
            tilEnd.setError(
                    getString(
                            R.string.booking_end_required
                    )
            );

            valid = false;
        }

        int startTotalMinutes =
                startHour * 60 + startMinute;

        int endTotalMinutes =
                endHour * 60 + endMinute;

        if (startHour >= 0
                && endHour >= 0
                && startTotalMinutes >= endTotalMinutes) {

            tilEnd.setError(
                    getString(
                            R.string.booking_time_invalid
                    )
            );

            valid = false;
        }

        int headcount = 0;

        if (headcountText.isEmpty()) {
            tilHeadcount.setError(
                    getString(
                            R.string.booking_headcount_required
                    )
            );

            valid = false;
        } else {
            try {
                headcount =
                        Integer.parseInt(
                                headcountText
                        );

                if (headcount <= 0) {
                    tilHeadcount.setError(
                            getString(
                                    R.string.booking_headcount_positive
                            )
                    );

                    valid = false;
                } else if (selectedClassroom != null
                        && headcount
                        > selectedClassroom.getCapacity()) {

                    tilHeadcount.setError(
                            getString(
                                    R.string.booking_headcount_capacity,
                                    selectedClassroom.getCapacity()
                            )
                    );

                    valid = false;
                }
            } catch (NumberFormatException exception) {
                tilHeadcount.setError(
                        getString(
                                R.string.booking_headcount_invalid
                        )
                );

                valid = false;
            }
        }

        if (purpose.isEmpty()) {
            tilPurpose.setError(
                    getString(
                            R.string.booking_purpose_required
                    )
            );

            valid = false;
        } else if (purpose.length() < 5) {
            tilPurpose.setError(
                    getString(
                            R.string.booking_purpose_short
                    )
            );

            valid = false;
        }

        if (!valid) {
            return;
        }

        SimpleDateFormat apiDateFormat =
                new SimpleDateFormat(
                        "yyyy-MM-dd",
                        Locale.ROOT
                );

        String bookingDate =
                apiDateFormat.format(
                        selectedDate.getTime()
                );

        String startTime =
                String.format(
                        Locale.ROOT,
                        "%02d:%02d:00",
                        startHour,
                        startMinute
                );

        String endTime =
                String.format(
                        Locale.ROOT,
                        "%02d:%02d:00",
                        endHour,
                        endMinute
                );

        RoomBookingRequest request =
                new RoomBookingRequest(
                        selectedClassroom.getId(),
                        bookingDate,
                        startTime,
                        endTime,
                        headcount,
                        purpose
                );

        setLoading(
                true,
                R.string.booking_submitting
        );

        apiService.createRoomBooking(request)
                .enqueue(
                        new Callback<JsonObject>() {
                            @Override
                            public void onResponse(
                                    Call<JsonObject> call,
                                    Response<JsonObject> response
                            ) {
                                setLoading(
                                        false,
                                        R.string.submit_booking
                                );

                                JsonObject result =
                                        response.body();

                                if (!response.isSuccessful()
                                        || result == null) {

                                    showError(
                                            readApiError(
                                                    response.errorBody()
                                            )
                                    );

                                    return;
                                }

                                boolean success =
                                        result.has("success")
                                                && result
                                                .get("success")
                                                .getAsBoolean();

                                if (!success) {
                                    showError(
                                            getString(
                                                    R.string.booking_submit_failed
                                            )
                                    );

                                    return;
                                }

                                long bookingId =
                                        result.has("booking_id")
                                                ? result
                                                .get("booking_id")
                                                .getAsLong()
                                                : 0L;

                                showSuccess(bookingId);
                            }

                            @Override
                            public void onFailure(
                                    Call<JsonObject> call,
                                    Throwable throwable
                            ) {
                                setLoading(
                                        false,
                                        R.string.submit_booking
                                );

                                showError(
                                        networkError(throwable)
                                );
                            }
                        }
                );
    }

    private void showSuccess(long bookingId) {
        String message =
                bookingId > 0
                        ? getString(
                        R.string.booking_success_message,
                        bookingId
                )
                        : getString(
                        R.string.booking_success_message_without_id
                );

        new MaterialAlertDialogBuilder(this)
                .setTitle(
                        R.string.booking_success_title
                )
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(
                        R.string.back_to_home,
                        (dialog, which) -> finish()
                )
                .show();
    }

    private void clearErrors() {
        tilRoom.setError(null);
        tilDate.setError(null);
        tilStart.setError(null);
        tilEnd.setError(null);
        tilHeadcount.setError(null);
        tilPurpose.setError(null);

        tvError.setText("");
        tvError.setVisibility(View.GONE);
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void setLoading(
            boolean loading,
            int buttonText
    ) {
        actRoom.setEnabled(!loading);
        etDate.setEnabled(!loading);
        etStart.setEnabled(!loading);
        etEnd.setEnabled(!loading);
        etHeadcount.setEnabled(!loading);
        etPurpose.setEnabled(!loading);
        btnSubmit.setEnabled(!loading);
        btnSubmit.setText(buttonText);

        progress.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );
    }

    private String roomLabel(
            Classroom classroom
    ) {
        Classroom.Building building =
                classroom.getBuilding();

        Classroom.Campus campus =
                building == null
                        ? null
                        : building.getCampus();

        String buildingCode =
                building == null
                        ? ""
                        : building.getBuildingCode();

        String campusCode =
                campus == null
                        ? ""
                        : campus.getCampusCode();

        return getString(
                R.string.booking_room_dropdown_item,
                classroom.getRoomCode(),
                buildingCode,
                campusCode,
                classroom.getCapacity()
        );
    }

    private String readApiError(
            ResponseBody errorBody
    ) {
        if (errorBody == null) {
            return getString(
                    R.string.booking_submit_failed
            );
        }

        try {
            JsonElement element =
                    JsonParser.parseString(
                            errorBody.string()
                    );

            if (element.isJsonObject()) {
                JsonElement message =
                        element
                                .getAsJsonObject()
                                .get("message");

                if (message != null
                        && !message.isJsonNull()) {

                    return message.getAsString();
                }
            }
        } catch (IOException
                 | RuntimeException ignored) {
            // Sử dụng thông báo mặc định.
        }

        return getString(
                R.string.booking_submit_failed
        );
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
                : editText
                .getText()
                .toString()
                .trim();
    }

    private void openLogin() {
        Intent intent =
                new Intent(
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