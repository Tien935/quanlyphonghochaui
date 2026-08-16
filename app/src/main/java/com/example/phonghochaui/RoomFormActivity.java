package com.example.phonghochaui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.phonghochaui.data.model.BuildingOption;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoomFormActivity extends AppCompatActivity {

    public static final String EXTRA_CLASSROOM_ID = "classroom_id";
    public static final String EXTRA_BUILDING_ID = "building_id";
    public static final String EXTRA_ROOM_CODE = "room_code";
    public static final String EXTRA_FLOOR = "floor";
    public static final String EXTRA_CAPACITY = "capacity";
    public static final String EXTRA_STATUS = "status";
    public static final String EXTRA_DESCRIPTION = "description";

    private static final String BUILDING_SELECT =
            "id,campus_id,building_code,building_name,total_floors,"
                    + "campus:campuses(id,campus_code,campus_name)";

    private final List<BuildingOption> buildings = new ArrayList<>();

    private final List<String> statusValues = Arrays.asList(
            "active",
            "maintenance",
            "inactive"
    );

    private TextView tvTitle;
    private TextInputLayout tilBuilding;
    private TextInputLayout tilRoomCode;
    private TextInputLayout tilFloor;
    private TextInputLayout tilCapacity;
    private TextInputLayout tilStatus;

    private TextInputEditText etRoomCode;
    private TextInputEditText etFloor;
    private TextInputEditText etCapacity;
    private TextInputEditText etDescription;

    private AutoCompleteTextView actBuilding;
    private AutoCompleteTextView actStatus;

    private TextView tvError;
    private MaterialButton btnSave;
    private MaterialButton btnDelete;
    private LinearProgressIndicator progress;

    private SessionManager sessionManager;
    private SupabaseApiService apiService;
    private BuildingOption selectedBuilding;

    private String selectedStatus = "active";
    private long classroomId = -1L;
    private long initialBuildingId = -1L;
    private boolean editMode;

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
        setContentView(R.layout.activity_room_form);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.roomFormRoot),
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
        readFormMode();
        setupStatusDropdown();
        fillInitialValues();

        apiService = RetrofitClient.getApiService(this);

        findViewById(R.id.btnRoomFormBack)
                .setOnClickListener(view -> finish());

        btnSave.setOnClickListener(
                view -> validateAndSave()
        );

        btnDelete.setOnClickListener(
                view -> showDeleteConfirmation()
        );

        actBuilding.setOnItemClickListener(
                (parent, view, position, id) -> {
                    if (position >= 0
                            && position < buildings.size()) {
                        selectedBuilding = buildings.get(position);
                        tilBuilding.setError(null);
                        updateFloorHelper();
                    }
                }
        );

        loadBuildings();
    }

    private void bindViews() {
        tvTitle = findViewById(R.id.tvRoomFormTitle);

        tilBuilding = findViewById(R.id.tilRoomFormBuilding);
        tilRoomCode = findViewById(R.id.tilRoomFormCode);
        tilFloor = findViewById(R.id.tilRoomFormFloor);
        tilCapacity = findViewById(R.id.tilRoomFormCapacity);
        tilStatus = findViewById(R.id.tilRoomFormStatus);

        actBuilding = findViewById(R.id.actRoomFormBuilding);
        etRoomCode = findViewById(R.id.etRoomFormCode);
        etFloor = findViewById(R.id.etRoomFormFloor);
        etCapacity = findViewById(R.id.etRoomFormCapacity);
        actStatus = findViewById(R.id.actRoomFormStatus);
        etDescription = findViewById(R.id.etRoomFormDescription);

        tvError = findViewById(R.id.tvRoomFormError);
        btnSave = findViewById(R.id.btnSaveRoom);
        btnDelete = findViewById(R.id.btnDeleteRoom);
        progress = findViewById(R.id.roomFormProgress);
    }

    private void readFormMode() {
        Intent intent = getIntent();

        classroomId = intent.getLongExtra(
                EXTRA_CLASSROOM_ID,
                -1L
        );

        initialBuildingId = intent.getLongExtra(
                EXTRA_BUILDING_ID,
                -1L
        );

        editMode = classroomId > 0L;

        tvTitle.setText(
                editMode
                        ? R.string.edit_room_title
                        : R.string.add_room_title
        );

        btnSave.setText(
                editMode
                        ? R.string.update_room
                        : R.string.create_room
        );

        btnDelete.setVisibility(
                editMode ? View.VISIBLE : View.GONE
        );
    }

    private void setupStatusDropdown() {
        List<String> labels = Arrays.asList(
                getString(R.string.room_status_active),
                getString(R.string.room_status_maintenance),
                getString(R.string.room_status_inactive)
        );

        actStatus.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                labels
        ));

        actStatus.setOnItemClickListener(
                (parent, view, position, id) -> {
                    if (position >= 0
                            && position < statusValues.size()) {
                        selectedStatus = statusValues.get(position);
                        tilStatus.setError(null);
                    }
                }
        );
    }

    private void fillInitialValues() {
        Intent intent = getIntent();

        if (editMode) {
            etRoomCode.setText(
                    intent.getStringExtra(EXTRA_ROOM_CODE)
            );

            etFloor.setText(String.valueOf(
                    intent.getIntExtra(EXTRA_FLOOR, 0)
            ));

            etCapacity.setText(String.valueOf(
                    intent.getIntExtra(EXTRA_CAPACITY, 0)
            ));

            etDescription.setText(
                    intent.getStringExtra(EXTRA_DESCRIPTION)
            );

            String status = intent.getStringExtra(EXTRA_STATUS);

            if (statusValues.contains(status)) {
                selectedStatus = status;
            }
        }

        int statusIndex = statusValues.indexOf(selectedStatus);

        if (statusIndex < 0) {
            statusIndex = 0;
            selectedStatus = statusValues.get(0);
        }

        String[] labels = {
                getString(R.string.room_status_active),
                getString(R.string.room_status_maintenance),
                getString(R.string.room_status_inactive)
        };

        actStatus.setText(labels[statusIndex], false);
    }

    private void loadBuildings() {
        setBusy(
                true,
                R.string.room_form_loading_buildings
        );

        apiService.getBuildingOptions(
                BUILDING_SELECT,
                "building_code.asc"
        ).enqueue(new Callback<List<BuildingOption>>() {
            @Override
            public void onResponse(
                    Call<List<BuildingOption>> call,
                    Response<List<BuildingOption>> response
            ) {
                setBusy(
                        false,
                        editMode
                                ? R.string.update_room
                                : R.string.create_room
                );

                if (response.code() == 401) {
                    handleExpiredSession();
                    return;
                }

                if (!response.isSuccessful()
                        || response.body() == null) {
                    showError(readApiError(
                            response.errorBody(),
                            getString(
                                    R.string.room_form_building_load_error,
                                    response.code()
                            )
                    ));

                    btnSave.setEnabled(false);
                    return;
                }

                buildings.clear();
                buildings.addAll(response.body());

                actBuilding.setAdapter(new ArrayAdapter<>(
                        RoomFormActivity.this,
                        android.R.layout.simple_dropdown_item_1line,
                        buildings
                ));

                if (buildings.isEmpty()) {
                    showError(getString(
                            R.string.room_form_no_buildings
                    ));

                    btnSave.setEnabled(false);
                    return;
                }

                if (editMode) {
                    selectInitialBuilding();
                }
            }

            @Override
            public void onFailure(
                    Call<List<BuildingOption>> call,
                    Throwable throwable
            ) {
                setBusy(
                        false,
                        editMode
                                ? R.string.update_room
                                : R.string.create_room
                );

                btnSave.setEnabled(false);
                showError(networkError(throwable));
            }
        });
    }

    private void selectInitialBuilding() {
        for (BuildingOption building : buildings) {
            if (building.getId() == initialBuildingId) {
                selectedBuilding = building;

                actBuilding.setText(
                        building.toString(),
                        false
                );

                updateFloorHelper();
                return;
            }
        }

        showError(getString(
                R.string.room_form_building_not_found
        ));

        btnSave.setEnabled(false);
    }

    private void updateFloorHelper() {
        if (selectedBuilding == null
                || selectedBuilding.getTotalFloors() == null) {
            tilFloor.setHelperText(null);
            return;
        }

        tilFloor.setHelperText(getString(
                R.string.room_form_floor_helper,
                selectedBuilding.getTotalFloors()
        ));
    }

    private void validateAndSave() {
        clearErrors();

        String roomCode = textOf(etRoomCode)
                .toUpperCase(Locale.ROOT);

        String floorText = textOf(etFloor);
        String capacityText = textOf(etCapacity);
        String description = textOf(etDescription);

        boolean valid = true;

        if (selectedBuilding == null) {
            tilBuilding.setError(getString(
                    R.string.room_form_building_required
            ));

            valid = false;
        }

        if (roomCode.isEmpty()) {
            tilRoomCode.setError(getString(
                    R.string.room_form_code_required
            ));

            valid = false;
        }

        Integer floor = parseInteger(floorText);

        if (floor == null) {
            tilFloor.setError(getString(
                    R.string.room_form_floor_invalid
            ));

            valid = false;
        } else if (floor < 0) {
            tilFloor.setError(getString(
                    R.string.room_form_floor_negative
            ));

            valid = false;
        } else if (selectedBuilding != null
                && selectedBuilding.getTotalFloors() != null
                && floor > selectedBuilding.getTotalFloors()) {
            tilFloor.setError(getString(
                    R.string.room_form_floor_too_large,
                    selectedBuilding.getTotalFloors()
            ));

            valid = false;
        }

        Integer capacity = parseInteger(capacityText);

        if (capacity == null || capacity <= 0) {
            tilCapacity.setError(getString(
                    R.string.room_form_capacity_invalid
            ));

            valid = false;
        }

        if (!statusValues.contains(selectedStatus)) {
            tilStatus.setError(getString(
                    R.string.room_form_status_required
            ));

            valid = false;
        }

        if (!valid || floor == null || capacity == null) {
            return;
        }

        JsonObject request = new JsonObject();

        if (editMode) {
            request.addProperty(
                    "p_classroom_id",
                    classroomId
            );
        }

        request.addProperty(
                "p_building_id",
                selectedBuilding.getId()
        );

        request.addProperty(
                "p_room_code",
                roomCode
        );

        request.addProperty(
                "p_floor",
                floor
        );

        request.addProperty(
                "p_capacity",
                capacity
        );

        request.addProperty(
                "p_operational_status",
                selectedStatus
        );

        if (description.isEmpty()) {
            request.add(
                    "p_description",
                    JsonNull.INSTANCE
            );
        } else {
            request.addProperty(
                    "p_description",
                    description
            );
        }

        saveRoom(request);
    }

    private void saveRoom(JsonObject request) {
        setBusy(
                true,
                R.string.room_form_saving
        );

        Call<JsonObject> call = editMode
                ? apiService.updateClassroom(request)
                : apiService.createClassroom(request);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(
                    Call<JsonObject> call,
                    Response<JsonObject> response
            ) {
                setBusy(
                        false,
                        editMode
                                ? R.string.update_room
                                : R.string.create_room
                );

                if (response.code() == 401) {
                    handleExpiredSession();
                    return;
                }

                if (!response.isSuccessful()) {
                    showError(readApiError(
                            response.errorBody(),
                            getString(
                                    R.string.room_form_save_failed
                            )
                    ));

                    return;
                }

                String message = getString(
                        editMode
                                ? R.string.room_form_update_success
                                : R.string.room_form_create_success
                );

                JsonObject body = response.body();

                if (body != null
                        && body.has("message")
                        && !body.get("message").isJsonNull()) {
                    message = body
                            .get("message")
                            .getAsString();
                }

                Toast.makeText(
                        RoomFormActivity.this,
                        message,
                        Toast.LENGTH_SHORT
                ).show();

                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(
                    Call<JsonObject> call,
                    Throwable throwable
            ) {
                setBusy(
                        false,
                        editMode
                                ? R.string.update_room
                                : R.string.create_room
                );

                showError(networkError(throwable));
            }
        });
    }

    private void showDeleteConfirmation() {
        if (!editMode || classroomId <= 0L) {
            return;
        }

        String roomCode = textOf(etRoomCode);

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.delete_room_title)
                .setMessage(getString(
                        R.string.delete_room_message,
                        roomCode.isEmpty()
                                ? getString(
                                R.string.information_not_available
                        )
                                : roomCode
                ))
                .setNegativeButton(
                        R.string.cancel,
                        null
                )
                .setPositiveButton(
                        R.string.delete_room,
                        (dialog, which) -> deleteRoom()
                )
                .show();
    }

    private void deleteRoom() {
        clearErrors();

        setBusy(
                true,
                R.string.room_form_deleting
        );

        JsonObject request = new JsonObject();

        request.addProperty(
                "p_classroom_id",
                classroomId
        );

        apiService.deleteClassroom(request)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(
                            Call<JsonObject> call,
                            Response<JsonObject> response
                    ) {
                        setBusy(
                                false,
                                R.string.update_room
                        );

                        if (response.code() == 401) {
                            handleExpiredSession();
                            return;
                        }

                        if (!response.isSuccessful()) {
                            showError(readApiError(
                                    response.errorBody(),
                                    getString(
                                            R.string.delete_room_failed
                                    )
                            ));

                            return;
                        }

                        String message = getString(
                                R.string.delete_room_success
                        );

                        JsonObject body = response.body();

                        if (body != null
                                && body.has("message")
                                && !body.get("message").isJsonNull()) {
                            message = body
                                    .get("message")
                                    .getAsString();
                        }

                        Toast.makeText(
                                RoomFormActivity.this,
                                message,
                                Toast.LENGTH_SHORT
                        ).show();

                        setResult(RESULT_OK);
                        finish();
                    }

                    @Override
                    public void onFailure(
                            Call<JsonObject> call,
                            Throwable throwable
                    ) {
                        setBusy(
                                false,
                                R.string.update_room
                        );

                        showError(networkError(throwable));
                    }
                });
    }

    private Integer parseInteger(String value) {
        if (value.isEmpty()) {
            return null;
        }

        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void clearErrors() {
        tilBuilding.setError(null);
        tilRoomCode.setError(null);
        tilFloor.setError(null);
        tilCapacity.setError(null);
        tilStatus.setError(null);

        tvError.setText("");
        tvError.setVisibility(View.GONE);
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void setBusy(
            boolean busy,
            int buttonText
    ) {
        progress.setVisibility(
                busy ? View.VISIBLE : View.GONE
        );

        actBuilding.setEnabled(!busy);
        etRoomCode.setEnabled(!busy);
        etFloor.setEnabled(!busy);
        etCapacity.setEnabled(!busy);
        actStatus.setEnabled(!busy);
        etDescription.setEnabled(!busy);
        btnSave.setEnabled(!busy);
        btnDelete.setEnabled(!busy);
        btnSave.setText(buttonText);
    }

    private String readApiError(
            ResponseBody errorBody,
            String fallback
    ) {
        if (errorBody == null) {
            return fallback;
        }

        try {
            JsonElement element = JsonParser.parseString(
                    errorBody.string()
            );

            if (!element.isJsonObject()) {
                return fallback;
            }

            JsonObject error = element.getAsJsonObject();

            String[] fields = {
                    "message",
                    "msg",
                    "error_description",
                    "error"
            };

            for (String field : fields) {
                JsonElement value = error.get(field);

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

    private String networkError(Throwable throwable) {
        String detail = throwable.getMessage();

        return getString(
                R.string.room_load_error_network,
                detail == null || detail.trim().isEmpty()
                        ? getString(R.string.unknown_error)
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