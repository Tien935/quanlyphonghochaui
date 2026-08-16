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

import com.example.phonghochaui.data.model.Classroom;
import com.example.phonghochaui.data.remote.RetrofitClient;
import com.example.phonghochaui.data.remote.SessionManager;
import com.example.phonghochaui.data.remote.SupabaseApiService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RoomManagementActivity extends AppCompatActivity {

    private static final String CLASSROOM_SELECT =
            "id,room_code,floor,capacity,operational_status,description,"
                    + "building:buildings(id,building_code,building_name,total_floors,"
                    + "campus:campuses(id,campus_code,campus_name,address))";

    private final List<Classroom> allClassrooms = new ArrayList<>();

    private final List<String> statusValues = Arrays.asList(
            "all",
            "active",
            "maintenance",
            "inactive"
    );

    private TextInputEditText etSearch;
    private AutoCompleteTextView actStatus;
    private ListView listRooms;
    private TextView tvCount;
    private TextView tvState;
    private LinearProgressIndicator progress;
    private MaterialButton btnRefresh;
    private MaterialButton btnAdd;

    private ClassroomAdapter adapter;
    private SupabaseApiService apiService;
    private SessionManager sessionManager;

    private String selectedStatus = "all";

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
        setContentView(R.layout.activity_room_management);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.roomManagementRoot),
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

        apiService = RetrofitClient.getApiService(this);
        adapter = new ClassroomAdapter(this);
        listRooms.setAdapter(adapter);

        findViewById(R.id.btnRoomManagementBack)
                .setOnClickListener(view -> finish());

        btnRefresh.setOnClickListener(view -> loadClassrooms());

        btnAdd.setOnClickListener(
                view -> startActivity(
                        new Intent(this, RoomFormActivity.class)
                )
        );

        listRooms.setOnItemClickListener(
                (parent, view, position, id) ->
                        openEditForm(adapter.getItem(position))
        );

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
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable value) {
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (apiService != null) {
            loadClassrooms();
        }
    }

    private void bindViews() {
        etSearch = findViewById(R.id.etAdminRoomSearch);
        actStatus = findViewById(R.id.actAdminRoomStatus);
        listRooms = findViewById(R.id.listAdminRooms);
        tvCount = findViewById(R.id.tvAdminRoomCount);
        tvState = findViewById(R.id.tvAdminRoomState);
        progress = findViewById(R.id.adminRoomProgress);
        btnRefresh = findViewById(R.id.btnRefreshAdminRooms);
        btnAdd = findViewById(R.id.btnAddAdminRoom);
    }

    private void setupStatusFilter() {
        List<String> labels = Arrays.asList(
                getString(R.string.filter_all),
                getString(R.string.room_status_active),
                getString(R.string.room_status_maintenance),
                getString(R.string.room_status_inactive)
        );

        actStatus.setAdapter(new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                labels
        ));

        actStatus.setText(labels.get(0), false);

        actStatus.setOnItemClickListener(
                (parent, view, position, id) -> {
                    selectedStatus = statusValues.get(position);
                    applyFilters();
                }
        );
    }

    private void loadClassrooms() {
        setLoading(true);

        apiService.getClassrooms(
                CLASSROOM_SELECT,
                "room_code.asc"
        ).enqueue(new Callback<List<Classroom>>() {
            @Override
            public void onResponse(
                    Call<List<Classroom>> call,
                    Response<List<Classroom>> response
            ) {
                setLoading(false);

                if (response.code() == 401) {
                    sessionManager.clearSession();

                    Toast.makeText(
                            RoomManagementActivity.this,
                            R.string.session_expired_message,
                            Toast.LENGTH_LONG
                    ).show();

                    openLogin();
                    return;
                }

                if (!response.isSuccessful()
                        || response.body() == null) {
                    showState(getString(
                            R.string.admin_room_load_error,
                            response.code()
                    ));
                    return;
                }

                allClassrooms.clear();
                allClassrooms.addAll(response.body());
                applyFilters();
            }

            @Override
            public void onFailure(
                    Call<List<Classroom>> call,
                    Throwable throwable
            ) {
                setLoading(false);

                String detail = throwable.getMessage();

                showState(getString(
                        R.string.room_load_error_network,
                        detail == null
                                ? getString(R.string.unknown_error)
                                : detail
                ));
            }
        });
    }

    private void applyFilters() {
        if (adapter == null) {
            return;
        }

        String query = etSearch.getText() == null
                ? ""
                : etSearch.getText()
                .toString()
                .trim()
                .toLowerCase(Locale.ROOT);

        List<Classroom> filtered = new ArrayList<>();

        for (Classroom classroom : allClassrooms) {
            String searchable = buildSearchableText(classroom)
                    .toLowerCase(Locale.ROOT);

            boolean matchesText =
                    query.isEmpty() || searchable.contains(query);

            boolean matchesStatus =
                    "all".equals(selectedStatus)
                            || selectedStatus.equalsIgnoreCase(
                            classroom.getOperationalStatus()
                    );

            if (matchesText && matchesStatus) {
                filtered.add(classroom);
            }
        }

        adapter.updateData(filtered);

        tvCount.setText(getString(
                R.string.admin_room_count,
                filtered.size()
        ));

        if (filtered.isEmpty()) {
            showState(getString(R.string.admin_room_empty));
        } else {
            tvState.setVisibility(View.GONE);
            listRooms.setVisibility(View.VISIBLE);
        }
    }

    private String buildSearchableText(Classroom classroom) {
        Classroom.Building building = classroom.getBuilding();

        Classroom.Campus campus =
                building == null ? null : building.getCampus();

        return classroom.getRoomCode() + " "
                + classroom.getDescription() + " "
                + (building == null
                ? ""
                : building.getBuildingCode()) + " "
                + (building == null
                ? ""
                : building.getBuildingName()) + " "
                + (campus == null
                ? ""
                : campus.getCampusCode()) + " "
                + (campus == null
                ? ""
                : campus.getCampusName());
    }

    private void openEditForm(Classroom classroom) {
        Intent intent = new Intent(
                this,
                RoomFormActivity.class
        );

        intent.putExtra(
                RoomFormActivity.EXTRA_CLASSROOM_ID,
                classroom.getId()
        );

        intent.putExtra(
                RoomFormActivity.EXTRA_ROOM_CODE,
                classroom.getRoomCode()
        );

        intent.putExtra(
                RoomFormActivity.EXTRA_FLOOR,
                classroom.getFloor()
        );

        intent.putExtra(
                RoomFormActivity.EXTRA_CAPACITY,
                classroom.getCapacity()
        );

        intent.putExtra(
                RoomFormActivity.EXTRA_STATUS,
                classroom.getOperationalStatus()
        );

        intent.putExtra(
                RoomFormActivity.EXTRA_DESCRIPTION,
                classroom.getDescription()
        );

        if (classroom.getBuilding() != null) {
            intent.putExtra(
                    RoomFormActivity.EXTRA_BUILDING_ID,
                    classroom.getBuilding().getId()
            );
        }

        startActivity(intent);
    }

    private void setLoading(boolean loading) {
        progress.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        btnRefresh.setEnabled(!loading);
        btnAdd.setEnabled(!loading);

        listRooms.setVisibility(
                loading ? View.GONE : View.VISIBLE
        );

        if (loading) {
            tvState.setVisibility(View.GONE);
            tvCount.setText(R.string.admin_room_loading);
        }
    }

    private void showState(String message) {
        listRooms.setVisibility(View.GONE);
        tvState.setText(message);
        tvState.setVisibility(View.VISIBLE);
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