package com.example.phonghochaui;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
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

import com.example.phonghochaui.data.model.AdminUser;
import com.example.phonghochaui.data.model.Classroom;
import com.example.phonghochaui.data.model.SubjectOption;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminScheduleFormActivity extends AppCompatActivity {

    public static final String EXTRA_SCHEDULE_ID = "schedule_id";
    public static final String EXTRA_CLASSROOM_ID = "classroom_id";
    public static final String EXTRA_SUBJECT_CODE = "subject_code";
    public static final String EXTRA_SUBJECT_NAME = "subject_name";
    public static final String EXTRA_LECTURER_ID = "lecturer_id";
    public static final String EXTRA_STUDY_DATE = "study_date";
    public static final String EXTRA_START_TIME = "start_time";
    public static final String EXTRA_END_TIME = "end_time";
    public static final String EXTRA_SCHEDULE_TYPE = "schedule_type";
    public static final String EXTRA_ROOM_CODE = "room_code";

    private static final String CLASSROOM_SELECT =
            "id,room_code,floor,capacity,operational_status,description,"
                    + "building:buildings(id,building_code,building_name,total_floors,"
                    + "campus:campuses(id,campus_code,campus_name,address))";

    private final List<Classroom> activeClassrooms = new ArrayList<>();
    private final List<AdminUser> users = new ArrayList<>();
    private final List<SubjectOption> subjects = new ArrayList<>();
    private final List<Classroom.Campus> campusOptions = new ArrayList<>();
    private final List<Classroom.Building> buildingOptions = new ArrayList<>();
    private final List<Classroom> roomOptions = new ArrayList<>();
    private final List<String> typeValues = Arrays.asList(
            "study",
            "self_study",
            "other"
    );

    private TextView tvTitle;
    private TextInputLayout tilCampus;
    private TextInputLayout tilBuilding;
    private TextInputLayout tilRoom;
    private TextInputLayout tilSubjectCode;
    private TextInputLayout tilSubjectName;
    private TextInputLayout tilLecturer;
    private TextInputLayout tilDate;
    private TextInputLayout tilStart;
    private TextInputLayout tilEnd;
    private TextInputLayout tilType;
    private AutoCompleteTextView actCampus;
    private AutoCompleteTextView actBuilding;
    private AutoCompleteTextView actRoom;
    private AutoCompleteTextView actSubjectName;
    private AutoCompleteTextView actLecturer;
    private AutoCompleteTextView actType;
    private TextInputEditText etSubjectCode;
    private TextInputEditText etDate;
    private TextInputEditText etStart;
    private TextInputEditText etEnd;
    private TextView tvError;
    private MaterialButton btnSave;
    private MaterialButton btnDelete;
    private LinearProgressIndicator progress;

    private SessionManager sessionManager;
    private SupabaseApiService apiService;
    private Classroom.Campus selectedCampus;
    private Classroom.Building selectedBuilding;
    private Classroom selectedClassroom;
    private SubjectOption selectedSubject;
    private AdminUser selectedLecturer;
    private String selectedDateIso;
    private String selectedType = "study";
    private int startMinutes = -1;
    private int endMinutes = -1;

    private long scheduleId = -1L;
    private long initialClassroomId = -1L;
    private String initialSubjectCode = "";
    private String initialSubjectName = "";
    private String initialLecturerId = "";
    private String initialRoomCode = "";
    private boolean editMode;
    private int pendingOptionRequests;
    private boolean optionLoadFailed;

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
        setContentView(R.layout.activity_admin_schedule_form);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.scheduleFormRoot),
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
        setupTypeDropdown();
        fillInitialValues();

        apiService = RetrofitClient.getApiService(this);
        findViewById(R.id.btnScheduleFormBack)
                .setOnClickListener(view -> finish());
        etDate.setOnClickListener(view -> showDatePicker());
        etStart.setOnClickListener(view -> showTimePicker(true));
        etEnd.setOnClickListener(view -> showTimePicker(false));
        btnSave.setOnClickListener(view -> validateAndSave());
        btnDelete.setOnClickListener(view -> showDeleteConfirmation());

        loadFormOptions();
    }

    private void bindViews() {
        tvTitle = findViewById(R.id.tvScheduleFormTitle);
        tilCampus = findViewById(R.id.tilScheduleCampus);
        tilBuilding = findViewById(R.id.tilScheduleBuilding);
        tilRoom = findViewById(R.id.tilScheduleRoom);
        tilSubjectCode = findViewById(R.id.tilScheduleSubjectCode);
        tilSubjectName = findViewById(R.id.tilScheduleSubjectName);
        tilLecturer = findViewById(R.id.tilScheduleLecturer);
        tilDate = findViewById(R.id.tilScheduleDate);
        tilStart = findViewById(R.id.tilScheduleStart);
        tilEnd = findViewById(R.id.tilScheduleEnd);
        tilType = findViewById(R.id.tilScheduleType);
        actCampus = findViewById(R.id.actScheduleCampus);
        actBuilding = findViewById(R.id.actScheduleBuilding);
        actRoom = findViewById(R.id.actScheduleRoom);
        actSubjectName = findViewById(R.id.actScheduleSubjectName);
        actLecturer = findViewById(R.id.actScheduleLecturer);
        actType = findViewById(R.id.actScheduleType);
        etSubjectCode = findViewById(R.id.etScheduleSubjectCode);
        etDate = findViewById(R.id.etScheduleDate);
        etStart = findViewById(R.id.etScheduleStart);
        etEnd = findViewById(R.id.etScheduleEnd);
        tvError = findViewById(R.id.tvScheduleFormError);
        btnSave = findViewById(R.id.btnSaveSchedule);
        btnDelete = findViewById(R.id.btnDeleteSchedule);
        progress = findViewById(R.id.scheduleFormProgress);
    }

    private void readFormMode() {
        Intent intent = getIntent();
        scheduleId = intent.getLongExtra(EXTRA_SCHEDULE_ID, -1L);
        initialClassroomId = intent.getLongExtra(EXTRA_CLASSROOM_ID, -1L);
        initialSubjectCode = safe(intent.getStringExtra(EXTRA_SUBJECT_CODE));
        initialSubjectName = safe(intent.getStringExtra(EXTRA_SUBJECT_NAME));
        initialLecturerId = safe(intent.getStringExtra(EXTRA_LECTURER_ID));
        initialRoomCode = safe(intent.getStringExtra(EXTRA_ROOM_CODE));
        editMode = scheduleId > 0L;

        tvTitle.setText(editMode
                ? R.string.schedule_edit_title
                : R.string.schedule_add_title);
        btnSave.setText(editMode
                ? R.string.schedule_update
                : R.string.schedule_save);
        btnDelete.setVisibility(editMode ? View.VISIBLE : View.GONE);
    }

    private void setupTypeDropdown() {
        List<String> labels = Arrays.asList(
                getString(R.string.schedule_type_study),
                getString(R.string.schedule_type_self_study),
                getString(R.string.schedule_type_other)
        );
        actType.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                labels
        ));
        actType.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < typeValues.size()) {
                selectedType = typeValues.get(position);
                tilType.setError(null);
            }
        });
    }

    private void fillInitialValues() {
        Intent intent = getIntent();
        if (editMode) {
            etSubjectCode.setText(initialSubjectCode);
            actSubjectName.setText(initialSubjectName, false);
            selectedDateIso = safe(intent.getStringExtra(EXTRA_STUDY_DATE));
            startMinutes = parseTimeMinutes(
                    intent.getStringExtra(EXTRA_START_TIME)
            );
            endMinutes = parseTimeMinutes(intent.getStringExtra(EXTRA_END_TIME));

            String type = safe(intent.getStringExtra(EXTRA_SCHEDULE_TYPE));
            if (typeValues.contains(type)) {
                selectedType = type;
            }
        }

        etDate.setText(displayDate(selectedDateIso));
        etStart.setText(displayTime(startMinutes));
        etEnd.setText(displayTime(endMinutes));
        int typeIndex = typeValues.indexOf(selectedType);
        String[] labels = {
                getString(R.string.schedule_type_study),
                getString(R.string.schedule_type_self_study),
                getString(R.string.schedule_type_other)
        };
        actType.setText(labels[typeIndex < 0 ? 0 : typeIndex], false);
    }

    private void loadFormOptions() {
        pendingOptionRequests = 3;
        optionLoadFailed = false;
        setBusy(true, R.string.schedule_loading_options);

        apiService.getClassrooms(CLASSROOM_SELECT, "room_code.asc")
                .enqueue(new Callback<List<Classroom>>() {
                    @Override
                    public void onResponse(
                            Call<List<Classroom>> call,
                            Response<List<Classroom>> response
                    ) {
                        if (response.code() == 401) {
                            handleExpiredSession();
                            return;
                        }
                        if (!response.isSuccessful() || response.body() == null) {
                            optionLoadFailed = true;
                            showError(getString(
                                    R.string.schedule_form_options_failed
                            ));
                        } else {
                            activeClassrooms.clear();
                            for (Classroom classroom : response.body()) {
                                if ("active".equalsIgnoreCase(
                                        classroom.getOperationalStatus()
                                )) {
                                    activeClassrooms.add(classroom);
                                }
                            }
                        }
                        completeOptionRequest();
                    }

                    @Override
                    public void onFailure(
                            Call<List<Classroom>> call,
                            Throwable throwable
                    ) {
                        optionLoadFailed = true;
                        showError(networkError(throwable));
                        completeOptionRequest();
                    }
                });

        apiService.listAdminSubjectOptions(new JsonObject())
                .enqueue(new Callback<List<SubjectOption>>() {
                    @Override
                    public void onResponse(
                            Call<List<SubjectOption>> call,
                            Response<List<SubjectOption>> response
                    ) {
                        if (response.code() == 401) {
                            handleExpiredSession();
                            return;
                        }
                        if (!response.isSuccessful() || response.body() == null) {
                            optionLoadFailed = true;
                            showError(readApiError(
                                    response.errorBody(),
                                    getString(R.string.schedule_form_options_failed)
                            ));
                        } else {
                            subjects.clear();
                            subjects.addAll(response.body());
                        }
                        completeOptionRequest();
                    }

                    @Override
                    public void onFailure(
                            Call<List<SubjectOption>> call,
                            Throwable throwable
                    ) {
                        optionLoadFailed = true;
                        showError(networkError(throwable));
                        completeOptionRequest();
                    }
                });

        JsonObject userRequest = new JsonObject();
        userRequest.add("p_role", JsonNull.INSTANCE);
        userRequest.add("p_query", JsonNull.INSTANCE);
        apiService.listAdminUsers(userRequest)
                .enqueue(new Callback<List<AdminUser>>() {
                    @Override
                    public void onResponse(
                            Call<List<AdminUser>> call,
                            Response<List<AdminUser>> response
                    ) {
                        if (response.code() == 401) {
                            handleExpiredSession();
                            return;
                        }
                        if (!response.isSuccessful() || response.body() == null) {
                            optionLoadFailed = true;
                            showError(getString(
                                    R.string.schedule_form_options_failed
                            ));
                        } else {
                            users.clear();
                            users.addAll(response.body());
                        }
                        completeOptionRequest();
                    }

                    @Override
                    public void onFailure(
                            Call<List<AdminUser>> call,
                            Throwable throwable
                    ) {
                        optionLoadFailed = true;
                        showError(networkError(throwable));
                        completeOptionRequest();
                    }
                });
    }

    private void completeOptionRequest() {
        pendingOptionRequests--;
        if (pendingOptionRequests > 0 || isFinishing()) {
            return;
        }

        setBusy(false, editMode
                ? R.string.schedule_update
                : R.string.schedule_save);

        if (optionLoadFailed) {
            btnSave.setEnabled(false);
            return;
        }

        setupSubjectDropdown();
        setupLecturerDropdown();
        setupClassroomDropdowns();

        if (activeClassrooms.isEmpty()) {
            showError(getString(R.string.schedule_form_no_active_rooms));
            btnSave.setEnabled(false);
            return;
        }

        if (subjects.isEmpty()) {
            showError(getString(R.string.schedule_form_no_subjects));
            btnSave.setEnabled(false);
            return;
        }

        if (editMode) {
            restoreInitialClassroom();
            restoreInitialSubject();
            restoreInitialLecturer();
        }
    }

    private void setupSubjectDropdown() {
        List<String> labels = new ArrayList<>();
        for (SubjectOption subject : subjects) {
            labels.add(subject.toString());
        }

        actSubjectName.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                labels
        ));
        actSubjectName.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < subjects.size()) {
                selectedSubject = subjects.get(position);
                actSubjectName.setText(selectedSubject.getSubjectName(), false);
                etSubjectCode.setText(selectedSubject.getSubjectCode());
                tilSubjectName.setError(null);
                tilSubjectCode.setError(null);
            }
        });
    }

    private void setupLecturerDropdown() {
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.schedule_no_lecturer_option));
        for (AdminUser user : users) {
            labels.add(lecturerLabel(user));
        }

        actLecturer.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                labels
        ));
        actLecturer.setText(labels.get(0), false);
        actLecturer.setOnItemClickListener((parent, view, position, id) -> {
            selectedLecturer = position == 0
                    ? null
                    : users.get(position - 1);
            tilLecturer.setError(null);
        });
    }

    private void setupClassroomDropdowns() {
        LinkedHashMap<Long, Classroom.Campus> uniqueCampuses =
                new LinkedHashMap<>();
        for (Classroom classroom : activeClassrooms) {
            Classroom.Building building = classroom.getBuilding();
            Classroom.Campus campus = building == null
                    ? null
                    : building.getCampus();
            if (campus != null) {
                uniqueCampuses.put(campus.getId(), campus);
            }
        }
        campusOptions.clear();
        campusOptions.addAll(uniqueCampuses.values());

        List<String> campusLabels = new ArrayList<>();
        for (Classroom.Campus campus : campusOptions) {
            campusLabels.add(joinCodeName(
                    campus.getCampusCode(),
                    campus.getCampusName()
            ));
        }
        actCampus.setAdapter(simpleAdapter(campusLabels));

        actCampus.setOnItemClickListener((parent, view, position, id) -> {
            selectedCampus = campusOptions.get(position);
            selectedBuilding = null;
            selectedClassroom = null;
            actBuilding.setText("", false);
            actRoom.setText("", false);
            tilCampus.setError(null);
            populateBuildings(null);
        });

        actBuilding.setOnItemClickListener((parent, view, position, id) -> {
            selectedBuilding = buildingOptions.get(position);
            selectedClassroom = null;
            actRoom.setText("", false);
            tilBuilding.setError(null);
            populateRooms(null);
        });

        actRoom.setOnItemClickListener((parent, view, position, id) -> {
            selectedClassroom = roomOptions.get(position);
            tilRoom.setError(null);
        });
    }

    private void populateBuildings(Long preferredBuildingId) {
        LinkedHashMap<Long, Classroom.Building> unique = new LinkedHashMap<>();
        for (Classroom classroom : activeClassrooms) {
            Classroom.Building building = classroom.getBuilding();
            Classroom.Campus campus = building == null
                    ? null
                    : building.getCampus();
            if (building != null
                    && selectedCampus != null
                    && campus != null
                    && campus.getId() == selectedCampus.getId()) {
                unique.put(building.getId(), building);
            }
        }
        buildingOptions.clear();
        buildingOptions.addAll(unique.values());

        List<String> labels = new ArrayList<>();
        int selectedIndex = -1;
        for (int i = 0; i < buildingOptions.size(); i++) {
            Classroom.Building building = buildingOptions.get(i);
            labels.add(joinCodeName(
                    building.getBuildingCode(),
                    building.getBuildingName()
            ));
            if (preferredBuildingId != null
                    && building.getId() == preferredBuildingId) {
                selectedIndex = i;
            }
        }
        actBuilding.setAdapter(simpleAdapter(labels));

        if (selectedIndex >= 0) {
            selectedBuilding = buildingOptions.get(selectedIndex);
            actBuilding.setText(labels.get(selectedIndex), false);
        } else {
            selectedBuilding = null;
            actBuilding.setText("", false);
        }
        populateRooms(null);
    }

    private void populateRooms(Long preferredRoomId) {
        roomOptions.clear();
        for (Classroom classroom : activeClassrooms) {
            Classroom.Building building = classroom.getBuilding();
            if (building != null
                    && selectedBuilding != null
                    && building.getId() == selectedBuilding.getId()) {
                roomOptions.add(classroom);
            }
        }

        List<String> labels = new ArrayList<>();
        int selectedIndex = -1;
        for (int i = 0; i < roomOptions.size(); i++) {
            Classroom room = roomOptions.get(i);
            labels.add(roomLabel(room));
            if (preferredRoomId != null && room.getId() == preferredRoomId) {
                selectedIndex = i;
            }
        }
        actRoom.setAdapter(simpleAdapter(labels));

        if (selectedIndex >= 0) {
            selectedClassroom = roomOptions.get(selectedIndex);
            actRoom.setText(labels.get(selectedIndex), false);
        } else {
            selectedClassroom = null;
            actRoom.setText("", false);
        }
    }

    private void restoreInitialClassroom() {
        Classroom room = null;
        for (Classroom classroom : activeClassrooms) {
            if (classroom.getId() == initialClassroomId) {
                room = classroom;
                break;
            }
        }
        if (room == null || room.getBuilding() == null
                || room.getBuilding().getCampus() == null) {
            showError(getString(R.string.schedule_form_room_missing));
            return;
        }

        long campusId = room.getBuilding().getCampus().getId();
        for (int i = 0; i < campusOptions.size(); i++) {
            Classroom.Campus campus = campusOptions.get(i);
            if (campus.getId() == campusId) {
                selectedCampus = campus;
                actCampus.setText(joinCodeName(
                        campus.getCampusCode(),
                        campus.getCampusName()
                ), false);
                populateBuildings(room.getBuilding().getId());
                populateRooms(room.getId());
                return;
            }
        }

        showError(getString(R.string.schedule_form_room_missing));
    }

    private void restoreInitialLecturer() {
        if (initialLecturerId.isEmpty()) {
            return;
        }
        for (AdminUser user : users) {
            if (initialLecturerId.equals(user.getId())) {
                selectedLecturer = user;
                actLecturer.setText(lecturerLabel(user), false);
                return;
            }
        }
    }

    private void restoreInitialSubject() {
        for (SubjectOption subject : subjects) {
            boolean matchesCode = !initialSubjectCode.isEmpty()
                    && initialSubjectCode.equalsIgnoreCase(
                    subject.getSubjectCode()
            );
            boolean matchesName = initialSubjectCode.isEmpty()
                    && initialSubjectName.equalsIgnoreCase(
                    subject.getSubjectName()
            );
            if (matchesCode || matchesName) {
                selectedSubject = subject;
                actSubjectName.setText(subject.getSubjectName(), false);
                etSubjectCode.setText(subject.getSubjectCode());
                return;
            }
        }

        selectedSubject = null;
        etSubjectCode.setText(initialSubjectCode);
        actSubjectName.setText(initialSubjectName, false);
        showError(getString(R.string.schedule_form_subject_missing));
    }

    private void showDatePicker() {
        Calendar initial = calendarFromIso(selectedDateIso);
        new DatePickerDialog(
                this,
                (view, year, month, day) -> {
                    Calendar selected = Calendar.getInstance();
                    selected.set(year, month, day, 0, 0, 0);
                    selected.set(Calendar.MILLISECOND, 0);
                    selectedDateIso = new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.ROOT
                    ).format(selected.getTime());
                    etDate.setText(displayDate(selectedDateIso));
                    tilDate.setError(null);
                },
                initial.get(Calendar.YEAR),
                initial.get(Calendar.MONTH),
                initial.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void showTimePicker(boolean start) {
        Calendar now = Calendar.getInstance();
        int currentMinutes = start ? startMinutes : endMinutes;
        int hour = currentMinutes >= 0
                ? currentMinutes / 60
                : now.get(Calendar.HOUR_OF_DAY);
        int minute = currentMinutes >= 0 ? currentMinutes % 60 : 0;

        new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    int value = selectedHour * 60 + selectedMinute;
                    if (start) {
                        startMinutes = value;
                        etStart.setText(displayTime(value));
                        tilStart.setError(null);
                    } else {
                        endMinutes = value;
                        etEnd.setText(displayTime(value));
                        tilEnd.setError(null);
                    }
                },
                hour,
                minute,
                true
        ).show();
    }

    private void validateAndSave() {
        clearErrors();
        boolean valid = true;

        if (selectedCampus == null) {
            tilCampus.setError(getString(R.string.schedule_form_room_required));
            valid = false;
        }
        if (selectedBuilding == null) {
            tilBuilding.setError(getString(R.string.schedule_form_room_required));
            valid = false;
        }
        if (selectedClassroom == null) {
            tilRoom.setError(getString(R.string.schedule_form_room_required));
            valid = false;
        }
        if (selectedSubject == null) {
            tilSubjectName.setError(getString(
                    R.string.schedule_form_subject_required
            ));
            valid = false;
        }
        if (selectedDateIso == null || selectedDateIso.isEmpty()) {
            tilDate.setError(getString(R.string.schedule_form_date_required));
            valid = false;
        }
        if (startMinutes < 0) {
            tilStart.setError(getString(R.string.schedule_form_start_required));
            valid = false;
        }
        if (endMinutes < 0) {
            tilEnd.setError(getString(R.string.schedule_form_end_required));
            valid = false;
        }
        if (startMinutes >= 0 && endMinutes >= 0
                && startMinutes >= endMinutes) {
            tilEnd.setError(getString(R.string.schedule_form_time_invalid));
            valid = false;
        }
        if (!typeValues.contains(selectedType)) {
            tilType.setError(getString(R.string.schedule_form_type_required));
            valid = false;
        }

        if (!valid) {
            return;
        }

        String subjectCode = selectedSubject.getSubjectCode()
                .toUpperCase(Locale.ROOT);
        String subjectName = selectedSubject.getSubjectName();

        JsonObject request = new JsonObject();
        if (editMode) {
            request.addProperty("p_schedule_id", scheduleId);
        }
        request.addProperty("p_classroom_id", selectedClassroom.getId());
        addNullable(request, "p_subject_code", subjectCode);
        request.addProperty("p_subject_name", subjectName);
        if (selectedLecturer == null) {
            request.add("p_lecturer_id", JsonNull.INSTANCE);
        } else {
            request.addProperty("p_lecturer_id", selectedLecturer.getId());
        }
        request.addProperty("p_study_date", selectedDateIso);
        request.addProperty("p_start_time", rpcTime(startMinutes));
        request.addProperty("p_end_time", rpcTime(endMinutes));
        request.addProperty("p_schedule_type", selectedType);

        saveSchedule(request);
    }

    private void saveSchedule(JsonObject request) {
        setBusy(true, R.string.schedule_saving);
        Call<JsonObject> call = editMode
                ? apiService.updateAdminSchedule(request)
                : apiService.createAdminSchedule(request);

        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(
                    Call<JsonObject> call,
                    Response<JsonObject> response
            ) {
                setBusy(false, editMode
                        ? R.string.schedule_update
                        : R.string.schedule_save);

                if (response.code() == 401) {
                    handleExpiredSession();
                    return;
                }
                JsonObject result = response.body();
                if (!response.isSuccessful() || result == null) {
                    showError(readApiError(
                            response.errorBody(),
                            getString(R.string.schedule_save_failed)
                    ));
                    return;
                }
                if (result.has("success")
                        && !result.get("success").getAsBoolean()) {
                    showError(getString(R.string.schedule_save_failed));
                    return;
                }

                String message = getString(editMode
                        ? R.string.schedule_update_success
                        : R.string.schedule_create_success);
                if (result.has("message")
                        && !result.get("message").isJsonNull()) {
                    message = result.get("message").getAsString();
                }
                Toast.makeText(
                        AdminScheduleFormActivity.this,
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
                setBusy(false, editMode
                        ? R.string.schedule_update
                        : R.string.schedule_save);
                showError(networkError(throwable));
            }
        });
    }

    private void showDeleteConfirmation() {
        String subject = selectedSubject == null
                ? safe(actSubjectName.getText().toString())
                : selectedSubject.getSubjectName();
        if (subject.isEmpty()) {
            subject = getString(R.string.schedule_subject_unknown);
        }
        String roomCode = selectedClassroom == null
                ? initialRoomCode
                : selectedClassroom.getRoomCode();
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.schedule_delete_title)
                .setMessage(getString(
                        R.string.schedule_delete_message,
                        subject,
                        roomCode,
                        displayDate(selectedDateIso),
                        displayTime(startMinutes),
                        displayTime(endMinutes)
                ))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(
                        R.string.schedule_delete_action,
                        (dialog, which) -> deleteSchedule()
                )
                .show();
    }

    private void deleteSchedule() {
        setBusy(true, R.string.schedule_deleting);
        JsonObject request = new JsonObject();
        request.addProperty("p_schedule_id", scheduleId);
        apiService.deleteAdminSchedule(request)
                .enqueue(new Callback<JsonObject>() {
                    @Override
                    public void onResponse(
                            Call<JsonObject> call,
                            Response<JsonObject> response
                    ) {
                        setBusy(false, R.string.schedule_update);
                        if (response.code() == 401) {
                            handleExpiredSession();
                            return;
                        }
                        if (!response.isSuccessful()) {
                            showError(readApiError(
                                    response.errorBody(),
                                    getString(R.string.schedule_delete_failed)
                            ));
                            return;
                        }
                        Toast.makeText(
                                AdminScheduleFormActivity.this,
                                R.string.schedule_delete_success,
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
                        setBusy(false, R.string.schedule_update);
                        showError(networkError(throwable));
                    }
                });
    }

    private void clearErrors() {
        tilCampus.setError(null);
        tilBuilding.setError(null);
        tilRoom.setError(null);
        tilSubjectCode.setError(null);
        tilSubjectName.setError(null);
        tilLecturer.setError(null);
        tilDate.setError(null);
        tilStart.setError(null);
        tilEnd.setError(null);
        tilType.setError(null);
        tvError.setText("");
        tvError.setVisibility(View.GONE);
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }

    private void setBusy(boolean busy, int buttonText) {
        actCampus.setEnabled(!busy);
        actBuilding.setEnabled(!busy);
        actRoom.setEnabled(!busy);
        etSubjectCode.setEnabled(!busy);
        actSubjectName.setEnabled(!busy);
        actLecturer.setEnabled(!busy);
        etDate.setEnabled(!busy);
        etStart.setEnabled(!busy);
        etEnd.setEnabled(!busy);
        actType.setEnabled(!busy);
        btnSave.setEnabled(!busy);
        btnDelete.setEnabled(!busy);
        btnSave.setText(buttonText);
        progress.setVisibility(busy ? View.VISIBLE : View.GONE);
    }

    private String lecturerLabel(AdminUser user) {
        String name = user.getDisplayName();
        String code = user.getHauiCode();
        if (code.isEmpty()) {
            return name;
        }
        return name + " • " + code;
    }

    private String roomLabel(Classroom room) {
        return room.getRoomCode() + " • "
                + getString(R.string.room_floor_capacity,
                room.getFloor(), room.getCapacity());
    }

    private String joinCodeName(String code, String name) {
        String safeCode = safe(code);
        String safeName = safe(name);
        if (safeCode.isEmpty()) {
            return safeName;
        }
        if (safeName.isEmpty()) {
            return safeCode;
        }
        return safeCode + " - " + safeName;
    }

    private ArrayAdapter<String> simpleAdapter(List<String> labels) {
        return new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                labels
        );
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null
                ? ""
                : input.getText().toString().trim();
    }

    private void addNullable(JsonObject object, String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            object.add(key, JsonNull.INSTANCE);
        } else {
            object.addProperty(key, value.trim());
        }
    }

    private int parseTimeMinutes(String value) {
        String safeValue = safe(value);
        if (safeValue.length() < 5) {
            return -1;
        }
        try {
            int hour = Integer.parseInt(safeValue.substring(0, 2));
            int minute = Integer.parseInt(safeValue.substring(3, 5));
            if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
                return -1;
            }
            return hour * 60 + minute;
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    private String displayTime(int minutes) {
        if (minutes < 0) {
            return "";
        }
        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                minutes / 60,
                minutes % 60
        );
    }

    private String rpcTime(int minutes) {
        return String.format(
                Locale.ROOT,
                "%02d:%02d:00",
                minutes / 60,
                minutes % 60
        );
    }

    private Calendar calendarFromIso(String isoDate) {
        Calendar calendar = Calendar.getInstance();
        if (isoDate == null || isoDate.isEmpty()) {
            return calendar;
        }
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                    .parse(isoDate);
            if (date != null) {
                calendar.setTime(date);
            }
        } catch (ParseException ignored) {
            // Dùng ngày hiện tại.
        }
        return calendar;
    }

    private String displayDate(String isoDate) {
        if (isoDate == null || isoDate.isEmpty()) {
            return "";
        }
        try {
            Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                    .parse(isoDate);
            if (date != null) {
                return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(date);
            }
        } catch (ParseException ignored) {
            // Trả lại dữ liệu gốc.
        }
        return isoDate;
    }

    private String readApiError(ResponseBody errorBody, String fallback) {
        if (errorBody == null) {
            return fallback;
        }
        try {
            JsonElement root = JsonParser.parseString(errorBody.string());
            if (!root.isJsonObject()) {
                return fallback;
            }
            JsonObject error = root.getAsJsonObject();
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

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private void handleExpiredSession() {
        sessionManager.clearSession();
        Toast.makeText(
                this,
                R.string.session_expired_message,
                Toast.LENGTH_LONG
        ).show();
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
