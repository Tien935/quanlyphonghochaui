package com.example.phonghochaui;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.phonghochaui.data.model.Classroom;
import com.example.phonghochaui.data.model.ScheduleItem;
import com.example.phonghochaui.data.remote.RetrofitClient;
import com.example.phonghochaui.data.remote.SessionManager;
import com.example.phonghochaui.data.remote.SupabaseApiService;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminScheduleManagementActivity extends AppCompatActivity {

    private static final String CLASSROOM_SELECT =
            "id,room_code,floor,capacity,operational_status,description,"
                    + "building:buildings(id,building_code,building_name,total_floors,"
                    + "campus:campuses(id,campus_code,campus_name,address))";
    private static final long SEARCH_DELAY_MS = 400L;
    private static final int PRESET_CUSTOM = 0;
    private static final int PRESET_TODAY = 1;
    private static final int PRESET_SEVEN_DAYS = 2;

    private final List<Classroom> classrooms = new ArrayList<>();
    private final Handler searchHandler = new Handler(Looper.getMainLooper());

    private TextInputEditText etSearch;
    private MaterialButton btnToday;
    private MaterialButton btnSevenDays;
    private MaterialButton btnFilters;
    private MaterialButton btnRefresh;
    private MaterialButton btnClearFilters;
    private ExtendedFloatingActionButton fabAdd;
    private TextView tvCount;
    private TextView tvState;
    private ListView listSchedules;
    private LinearProgressIndicator progress;

    private SessionManager sessionManager;
    private SupabaseApiService apiService;
    private AdminScheduleAdapter adapter;

    private String dateFrom;
    private String dateTo;
    private Long selectedClassroomId;
    private String selectedScheduleType;
    private int activePreset = PRESET_SEVEN_DAYS;
    private int requestVersion;
    private boolean suppressSearchCallback;
    private boolean firstResume = true;

    private final Runnable searchTask = this::loadSchedules;

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
        setContentView(R.layout.activity_admin_schedule_management);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.scheduleManagementRoot),
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
        apiService = RetrofitClient.getApiService(this);
        adapter = new AdminScheduleAdapter(
                this,
                new AdminScheduleAdapter.ScheduleActionListener() {
                    @Override
                    public void onEdit(ScheduleItem schedule) {
                        openScheduleForm(schedule);
                    }

                    @Override
                    public void onDelete(ScheduleItem schedule) {
                        showDeleteConfirmation(schedule);
                    }
                }
        );
        listSchedules.setAdapter(adapter);

        findViewById(R.id.btnScheduleBack)
                .setOnClickListener(view -> finish());
        btnRefresh.setOnClickListener(view -> {
            loadClassroomOptions();
            loadSchedules();
        });
        btnToday.setOnClickListener(view -> applyTodayPreset());
        btnSevenDays.setOnClickListener(view -> applySevenDayPreset());
        btnFilters.setOnClickListener(view -> showFilterSheet());
        btnClearFilters.setOnClickListener(view -> clearAllFilters());
        fabAdd.setOnClickListener(view -> openScheduleForm(null));

        etSearch.addTextChangedListener(new TextWatcher() {
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
                if (suppressSearchCallback) {
                    return;
                }
                searchHandler.removeCallbacks(searchTask);
                searchHandler.postDelayed(searchTask, SEARCH_DELAY_MS);
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        });

        dateFrom = todayIso();
        dateTo = addDays(dateFrom, 6);
        updateFilterButtons();
        loadClassroomOptions();
        loadSchedules();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (firstResume) {
            firstResume = false;
        } else if (apiService != null) {
            loadSchedules();
        }
    }

    @Override
    protected void onDestroy() {
        searchHandler.removeCallbacks(searchTask);
        super.onDestroy();
    }

    private void bindViews() {
        etSearch = findViewById(R.id.etScheduleSearch);
        btnToday = findViewById(R.id.btnScheduleToday);
        btnSevenDays = findViewById(R.id.btnScheduleSevenDays);
        btnFilters = findViewById(R.id.btnScheduleFilters);
        btnRefresh = findViewById(R.id.btnScheduleRefresh);
        btnClearFilters = findViewById(R.id.btnScheduleClearFilters);
        fabAdd = findViewById(R.id.fabAddSchedule);
        tvCount = findViewById(R.id.tvScheduleCount);
        tvState = findViewById(R.id.tvScheduleState);
        listSchedules = findViewById(R.id.listAdminSchedules);
        progress = findViewById(R.id.scheduleProgress);
    }

    private void loadClassroomOptions() {
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
                            return;
                        }
                        classrooms.clear();
                        classrooms.addAll(response.body());

                        if (selectedClassroomId != null
                                && findClassroom(selectedClassroomId) == null) {
                            selectedClassroomId = null;
                            updateFilterButtons();
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<List<Classroom>> call,
                            Throwable throwable
                    ) {
                        // Danh sách lịch vẫn có thể tải; chỉ bộ lọc phòng bị hạn chế.
                    }
                });
    }

    private void loadSchedules() {
        searchHandler.removeCallbacks(searchTask);
        int currentRequest = ++requestVersion;
        setLoading(true);

        JsonObject request = new JsonObject();
        addNullable(request, "p_date_from", dateFrom);
        addNullable(request, "p_date_to", dateTo);
        if (selectedClassroomId == null) {
            request.add("p_classroom_id", JsonNull.INSTANCE);
        } else {
            request.addProperty("p_classroom_id", selectedClassroomId);
        }
        addNullable(request, "p_schedule_type", selectedScheduleType);
        addNullable(request, "p_query", textOf(etSearch));

        apiService.listAdminSchedules(request)
                .enqueue(new Callback<List<ScheduleItem>>() {
                    @Override
                    public void onResponse(
                            Call<List<ScheduleItem>> call,
                            Response<List<ScheduleItem>> response
                    ) {
                        if (currentRequest != requestVersion) {
                            return;
                        }
                        setLoading(false);

                        if (response.code() == 401) {
                            handleExpiredSession();
                            return;
                        }
                        if (!response.isSuccessful() || response.body() == null) {
                            showState(readApiError(
                                    response.errorBody(),
                                    getString(
                                            R.string.schedule_load_failed_http,
                                            response.code()
                                    )
                            ));
                            return;
                        }

                        List<ScheduleItem> result = new ArrayList<>(response.body());
                        sortSchedules(result);
                        adapter.updateData(result);
                        tvCount.setText(getString(
                                R.string.schedule_count,
                                result.size()
                        ));

                        if (result.isEmpty()) {
                            showState(getString(R.string.schedule_empty));
                        } else {
                            tvState.setVisibility(View.GONE);
                            listSchedules.setVisibility(View.VISIBLE);
                        }
                    }

                    @Override
                    public void onFailure(
                            Call<List<ScheduleItem>> call,
                            Throwable throwable
                    ) {
                        if (currentRequest != requestVersion) {
                            return;
                        }
                        setLoading(false);
                        showState(networkError(throwable));
                    }
                });
    }

    private void sortSchedules(List<ScheduleItem> result) {
        boolean ascendingDate = dateFrom != null || dateTo != null;
        Collections.sort(result, (left, right) -> {
            int dateCompare = left.getStudyDate().compareTo(right.getStudyDate());
            if (!ascendingDate) {
                dateCompare = -dateCompare;
            }
            if (dateCompare != 0) {
                return dateCompare;
            }
            return left.getStartTime().compareTo(right.getStartTime());
        });
    }

    private void applyTodayPreset() {
        dateFrom = todayIso();
        dateTo = dateFrom;
        activePreset = PRESET_TODAY;
        updateFilterButtons();
        loadSchedules();
    }

    private void applySevenDayPreset() {
        dateFrom = todayIso();
        dateTo = addDays(dateFrom, 6);
        activePreset = PRESET_SEVEN_DAYS;
        updateFilterButtons();
        loadSchedules();
    }

    private void clearAllFilters() {
        dateFrom = null;
        dateTo = null;
        selectedClassroomId = null;
        selectedScheduleType = null;
        activePreset = PRESET_CUSTOM;
        suppressSearchCallback = true;
        etSearch.setText("");
        suppressSearchCallback = false;
        updateFilterButtons();
        loadSchedules();
    }

    private void updateFilterButtons() {
        styleFilterButton(btnToday, activePreset == PRESET_TODAY);
        styleFilterButton(btnSevenDays, activePreset == PRESET_SEVEN_DAYS);

        boolean hasAdvancedFilter = selectedClassroomId != null
                || selectedScheduleType != null
                || (activePreset == PRESET_CUSTOM
                && (dateFrom != null || dateTo != null));
        styleFilterButton(btnFilters, hasAdvancedFilter);
    }

    private void styleFilterButton(MaterialButton button, boolean active) {
        int background = ContextCompat.getColor(
                this,
                active ? R.color.haui_blue : R.color.haui_card
        );
        int foreground = ContextCompat.getColor(
                this,
                active ? R.color.white : R.color.haui_blue
        );
        int stroke = ContextCompat.getColor(this, R.color.haui_blue);
        button.setBackgroundTintList(ColorStateList.valueOf(background));
        button.setTextColor(foreground);
        button.setStrokeColor(ColorStateList.valueOf(stroke));
    }

    private void showFilterSheet() {
        new ScheduleFilterSheetController().show();
    }

    private void openScheduleForm(ScheduleItem schedule) {
        Intent intent = new Intent(this, AdminScheduleFormActivity.class);
        if (schedule != null) {
            intent.putExtra(
                    AdminScheduleFormActivity.EXTRA_SCHEDULE_ID,
                    schedule.getId()
            );
            intent.putExtra(
                    AdminScheduleFormActivity.EXTRA_CLASSROOM_ID,
                    schedule.getClassroomId()
            );
            intent.putExtra(
                    AdminScheduleFormActivity.EXTRA_SUBJECT_CODE,
                    schedule.getSubjectCode()
            );
            intent.putExtra(
                    AdminScheduleFormActivity.EXTRA_SUBJECT_NAME,
                    schedule.getSubjectName()
            );
            intent.putExtra(
                    AdminScheduleFormActivity.EXTRA_LECTURER_ID,
                    schedule.getLecturerId()
            );
            intent.putExtra(
                    AdminScheduleFormActivity.EXTRA_STUDY_DATE,
                    schedule.getStudyDate()
            );
            intent.putExtra(
                    AdminScheduleFormActivity.EXTRA_START_TIME,
                    schedule.getStartTime()
            );
            intent.putExtra(
                    AdminScheduleFormActivity.EXTRA_END_TIME,
                    schedule.getEndTime()
            );
            intent.putExtra(
                    AdminScheduleFormActivity.EXTRA_SCHEDULE_TYPE,
                    schedule.getScheduleType()
            );
            intent.putExtra(
                    AdminScheduleFormActivity.EXTRA_ROOM_CODE,
                    schedule.getRoomCode()
            );
        }
        startActivity(intent);
    }

    private void showDeleteConfirmation(ScheduleItem schedule) {
        String subject = schedule.getSubjectName().isEmpty()
                ? getString(R.string.schedule_subject_unknown)
                : schedule.getSubjectName();
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.schedule_delete_title)
                .setMessage(getString(
                        R.string.schedule_delete_message,
                        subject,
                        schedule.getRoomCode(),
                        displayDate(schedule.getStudyDate()),
                        shortTime(schedule.getStartTime()),
                        shortTime(schedule.getEndTime())
                ))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(
                        R.string.schedule_delete_action,
                        (dialog, which) -> deleteSchedule(schedule.getId())
                )
                .show();
    }

    private void deleteSchedule(long scheduleId) {
        setLoading(true);
        JsonObject request = new JsonObject();
        request.addProperty("p_schedule_id", scheduleId);

        apiService.deleteAdminSchedule(request)
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
                                    AdminScheduleManagementActivity.this,
                                    readApiError(
                                            response.errorBody(),
                                            getString(R.string.schedule_delete_failed)
                                    ),
                                    Toast.LENGTH_LONG
                            ).show();
                            return;
                        }
                        Toast.makeText(
                                AdminScheduleManagementActivity.this,
                                R.string.schedule_delete_success,
                                Toast.LENGTH_SHORT
                        ).show();
                        loadSchedules();
                    }

                    @Override
                    public void onFailure(
                            Call<JsonObject> call,
                            Throwable throwable
                    ) {
                        setLoading(false);
                        Toast.makeText(
                                AdminScheduleManagementActivity.this,
                                networkError(throwable),
                                Toast.LENGTH_LONG
                        ).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRefresh.setEnabled(!loading);
        btnToday.setEnabled(!loading);
        btnSevenDays.setEnabled(!loading);
        btnFilters.setEnabled(!loading);
        btnClearFilters.setEnabled(!loading);
        fabAdd.setEnabled(!loading);
        if (loading) {
            tvCount.setText(R.string.schedule_loading);
            tvState.setVisibility(View.GONE);
            listSchedules.setVisibility(View.GONE);
        }
    }

    private void showState(String message) {
        listSchedules.setVisibility(View.GONE);
        tvState.setText(message);
        tvState.setVisibility(View.VISIBLE);
    }

    private Classroom findClassroom(long classroomId) {
        for (Classroom classroom : classrooms) {
            if (classroom.getId() == classroomId) {
                return classroom;
            }
        }
        return null;
    }

    private void addNullable(JsonObject object, String key, String value) {
        if (value == null || value.trim().isEmpty()) {
            object.add(key, JsonNull.INSTANCE);
        } else {
            object.addProperty(key, value.trim());
        }
    }

    private String textOf(TextInputEditText input) {
        return input.getText() == null
                ? ""
                : input.getText().toString().trim();
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

    private String todayIso() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                .format(Calendar.getInstance().getTime());
    }

    private String addDays(String isoDate, int days) {
        Calendar calendar = calendarFromIso(isoDate);
        calendar.add(Calendar.DAY_OF_MONTH, days);
        return new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT)
                .format(calendar.getTime());
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

    private String shortTime(String value) {
        return value == null
                ? ""
                : (value.length() >= 5 ? value.substring(0, 5) : value);
    }

    private class ScheduleFilterSheetController {

        private final BottomSheetDialog dialog;
        private final View content;
        private final TextInputEditText etFrom;
        private final TextInputEditText etTo;
        private final AutoCompleteTextView actCampus;
        private final AutoCompleteTextView actBuilding;
        private final AutoCompleteTextView actRoom;
        private final ChipGroup chipTypes;
        private final TextView tvError;

        private final List<Classroom.Campus> campusOptions = new ArrayList<>();
        private final List<Classroom.Building> buildingOptions = new ArrayList<>();
        private final List<Classroom> roomOptions = new ArrayList<>();

        private String workingFrom = dateFrom;
        private String workingTo = dateTo;
        private Long workingCampusId;
        private Long workingBuildingId;
        private Long workingRoomId = selectedClassroomId;

        ScheduleFilterSheetController() {
            dialog = new BottomSheetDialog(
                    AdminScheduleManagementActivity.this
            );
            content = LayoutInflater.from(AdminScheduleManagementActivity.this)
                    .inflate(R.layout.bottom_sheet_schedule_filter, null);
            dialog.setContentView(content);

            etFrom = content.findViewById(R.id.etScheduleFilterFrom);
            etTo = content.findViewById(R.id.etScheduleFilterTo);
            actCampus = content.findViewById(R.id.actScheduleFilterCampus);
            actBuilding = content.findViewById(R.id.actScheduleFilterBuilding);
            actRoom = content.findViewById(R.id.actScheduleFilterRoom);
            chipTypes = content.findViewById(R.id.chipGroupScheduleType);
            tvError = content.findViewById(R.id.tvScheduleFilterError);

            Classroom selectedRoom = workingRoomId == null
                    ? null
                    : findClassroom(workingRoomId);
            if (selectedRoom != null && selectedRoom.getBuilding() != null) {
                workingBuildingId = selectedRoom.getBuilding().getId();
                if (selectedRoom.getBuilding().getCampus() != null) {
                    workingCampusId = selectedRoom.getBuilding()
                            .getCampus().getId();
                }
            }

            etFrom.setText(displayDate(workingFrom));
            etTo.setText(displayDate(workingTo));
            populateCampuses();
            populateBuildings();
            populateRooms();
            selectTypeChip();
            bindListeners();
        }

        void show() {
            dialog.show();
        }

        private void bindListeners() {
            etFrom.setOnClickListener(view -> showDatePicker(true));
            etTo.setOnClickListener(view -> showDatePicker(false));

            actCampus.setOnItemClickListener((parent, view, position, id) -> {
                workingCampusId = position == 0
                        ? null
                        : campusOptions.get(position - 1).getId();
                workingBuildingId = null;
                workingRoomId = null;
                populateBuildings();
                populateRooms();
            });

            actBuilding.setOnItemClickListener((parent, view, position, id) -> {
                workingBuildingId = position == 0
                        ? null
                        : buildingOptions.get(position - 1).getId();
                workingRoomId = null;
                populateRooms();
            });

            actRoom.setOnItemClickListener((parent, view, position, id) -> {
                workingRoomId = position == 0
                        ? null
                        : roomOptions.get(position - 1).getId();
            });

            content.findViewById(R.id.btnScheduleFilterReset)
                    .setOnClickListener(view -> reset());
            content.findViewById(R.id.btnScheduleFilterApply)
                    .setOnClickListener(view -> apply());
        }

        private void populateCampuses() {
            LinkedHashMap<Long, Classroom.Campus> unique = new LinkedHashMap<>();
            for (Classroom classroom : classrooms) {
                Classroom.Building building = classroom.getBuilding();
                Classroom.Campus campus = building == null
                        ? null
                        : building.getCampus();
                if (campus != null) {
                    unique.put(campus.getId(), campus);
                }
            }
            campusOptions.clear();
            campusOptions.addAll(unique.values());

            List<String> labels = new ArrayList<>();
            labels.add(getString(R.string.schedule_filter_all_campuses));
            int selectedIndex = 0;
            for (int i = 0; i < campusOptions.size(); i++) {
                Classroom.Campus campus = campusOptions.get(i);
                labels.add(joinCodeName(
                        campus.getCampusCode(),
                        campus.getCampusName()
                ));
                if (workingCampusId != null
                        && campus.getId() == workingCampusId) {
                    selectedIndex = i + 1;
                }
            }
            actCampus.setAdapter(simpleAdapter(labels));
            actCampus.setText(labels.get(selectedIndex), false);
        }

        private void populateBuildings() {
            LinkedHashMap<Long, Classroom.Building> unique = new LinkedHashMap<>();
            for (Classroom classroom : classrooms) {
                Classroom.Building building = classroom.getBuilding();
                Classroom.Campus campus = building == null
                        ? null
                        : building.getCampus();
                if (building == null) {
                    continue;
                }
                if (workingCampusId == null
                        || (campus != null && campus.getId() == workingCampusId)) {
                    unique.put(building.getId(), building);
                }
            }
            buildingOptions.clear();
            buildingOptions.addAll(unique.values());

            List<String> labels = new ArrayList<>();
            labels.add(getString(R.string.schedule_filter_all_buildings));
            int selectedIndex = 0;
            boolean found = workingBuildingId == null;
            for (int i = 0; i < buildingOptions.size(); i++) {
                Classroom.Building building = buildingOptions.get(i);
                labels.add(joinCodeName(
                        building.getBuildingCode(),
                        building.getBuildingName()
                ));
                if (workingBuildingId != null
                        && building.getId() == workingBuildingId) {
                    selectedIndex = i + 1;
                    found = true;
                }
            }
            if (!found) {
                workingBuildingId = null;
                workingRoomId = null;
            }
            actBuilding.setAdapter(simpleAdapter(labels));
            actBuilding.setText(labels.get(selectedIndex), false);
        }

        private void populateRooms() {
            roomOptions.clear();
            for (Classroom classroom : classrooms) {
                Classroom.Building building = classroom.getBuilding();
                Classroom.Campus campus = building == null
                        ? null
                        : building.getCampus();
                boolean matchesCampus = workingCampusId == null
                        || (campus != null && campus.getId() == workingCampusId);
                boolean matchesBuilding = workingBuildingId == null
                        || (building != null
                        && building.getId() == workingBuildingId);
                if (matchesCampus && matchesBuilding) {
                    roomOptions.add(classroom);
                }
            }

            List<String> labels = new ArrayList<>();
            labels.add(getString(R.string.schedule_filter_all_rooms));
            int selectedIndex = 0;
            boolean found = workingRoomId == null;
            for (int i = 0; i < roomOptions.size(); i++) {
                Classroom room = roomOptions.get(i);
                labels.add(roomLabel(room));
                if (workingRoomId != null && room.getId() == workingRoomId) {
                    selectedIndex = i + 1;
                    found = true;
                }
            }
            if (!found) {
                workingRoomId = null;
            }
            actRoom.setAdapter(simpleAdapter(labels));
            actRoom.setText(labels.get(selectedIndex), false);
        }

        private void selectTypeChip() {
            if ("study".equals(selectedScheduleType)) {
                chipTypes.check(R.id.chipScheduleTypeStudy);
            } else if ("self_study".equals(selectedScheduleType)) {
                chipTypes.check(R.id.chipScheduleTypeSelfStudy);
            } else if ("other".equals(selectedScheduleType)) {
                chipTypes.check(R.id.chipScheduleTypeOther);
            } else {
                chipTypes.check(R.id.chipScheduleTypeAll);
            }
        }

        private void showDatePicker(boolean fromDate) {
            String value = fromDate ? workingFrom : workingTo;
            Calendar initial = calendarFromIso(value);
            new DatePickerDialog(
                    AdminScheduleManagementActivity.this,
                    (picker, year, month, day) -> {
                        Calendar selected = Calendar.getInstance();
                        selected.set(year, month, day, 0, 0, 0);
                        selected.set(Calendar.MILLISECOND, 0);
                        String iso = new SimpleDateFormat(
                                "yyyy-MM-dd",
                                Locale.ROOT
                        ).format(selected.getTime());
                        if (fromDate) {
                            workingFrom = iso;
                            etFrom.setText(displayDate(iso));
                        } else {
                            workingTo = iso;
                            etTo.setText(displayDate(iso));
                        }
                        tvError.setVisibility(View.GONE);
                    },
                    initial.get(Calendar.YEAR),
                    initial.get(Calendar.MONTH),
                    initial.get(Calendar.DAY_OF_MONTH)
            ).show();
        }

        private void reset() {
            workingFrom = null;
            workingTo = null;
            workingCampusId = null;
            workingBuildingId = null;
            workingRoomId = null;
            etFrom.setText("");
            etTo.setText("");
            populateCampuses();
            populateBuildings();
            populateRooms();
            chipTypes.check(R.id.chipScheduleTypeAll);
            tvError.setVisibility(View.GONE);
        }

        private void apply() {
            if (workingFrom != null
                    && workingTo != null
                    && workingFrom.compareTo(workingTo) > 0) {
                tvError.setText(R.string.schedule_filter_date_invalid);
                tvError.setVisibility(View.VISIBLE);
                return;
            }

            dateFrom = workingFrom;
            dateTo = workingTo;
            selectedClassroomId = workingRoomId;

            int checked = chipTypes.getCheckedChipId();
            if (checked == R.id.chipScheduleTypeStudy) {
                selectedScheduleType = "study";
            } else if (checked == R.id.chipScheduleTypeSelfStudy) {
                selectedScheduleType = "self_study";
            } else if (checked == R.id.chipScheduleTypeOther) {
                selectedScheduleType = "other";
            } else {
                selectedScheduleType = null;
            }

            String today = todayIso();
            if (today.equals(dateFrom) && today.equals(dateTo)) {
                activePreset = PRESET_TODAY;
            } else if (today.equals(dateFrom)
                    && addDays(today, 6).equals(dateTo)) {
                activePreset = PRESET_SEVEN_DAYS;
            } else {
                activePreset = PRESET_CUSTOM;
            }

            updateFilterButtons();
            dialog.dismiss();
            loadSchedules();
        }

        private ArrayAdapter<String> simpleAdapter(List<String> labels) {
            return new ArrayAdapter<>(
                    AdminScheduleManagementActivity.this,
                    android.R.layout.simple_dropdown_item_1line,
                    labels
            );
        }

        private String roomLabel(Classroom room) {
            Classroom.Building building = room.getBuilding();
            String buildingCode = building == null
                    ? ""
                    : building.getBuildingCode();
            if (buildingCode.isEmpty()) {
                return room.getRoomCode();
            }
            return room.getRoomCode() + " • Tòa " + buildingCode;
        }

        private String joinCodeName(String code, String name) {
            String safeCode = code == null ? "" : code.trim();
            String safeName = name == null ? "" : name.trim();
            if (safeCode.isEmpty()) {
                return safeName;
            }
            if (safeName.isEmpty()) {
                return safeCode;
            }
            return safeCode + " - " + safeName;
        }
    }
}

