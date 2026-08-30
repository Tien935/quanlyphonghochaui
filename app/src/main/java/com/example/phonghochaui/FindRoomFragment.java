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

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.phonghochaui.data.model.Classroom;
import com.example.phonghochaui.data.remote.RetrofitClient;
import com.example.phonghochaui.data.remote.SessionManager;
import com.example.phonghochaui.data.remote.SupabaseApiService;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FindRoomFragment extends Fragment {

    private static final String CLASSROOM_SELECT =
            "id,room_code,floor,capacity,operational_status,description,"
                    + "building:buildings(id,building_code,building_name,total_floors,"
                    + "campus:campuses(id,campus_code,campus_name,address))";

    private final List<Classroom> allClassrooms = new ArrayList<>();

    private TextInputEditText etSearchRoom;
    private AutoCompleteTextView actCampus;
    private AutoCompleteTextView actBuilding;
    private AutoCompleteTextView actStatus;
    private ListView listClassrooms;
    private TextView tvResultCount;
    private TextView tvRoomState;
    private LinearProgressIndicator roomProgress;

    private ClassroomAdapter classroomAdapter;
    private SupabaseApiService apiService;

    private String filterAll;
    private String selectedCampus;
    private String selectedBuilding;
    private String selectedStatus;

    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.activity_find_room, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SessionManager sessionManager = new SessionManager(requireContext());

        if (!sessionManager.hasSession()
                || !"student".equals(sessionManager.getUserRole())) {
            openLogin();
            return;
        }

        // EdgeToEdge.enable(requireActivity());
        

        ViewCompat.setOnApplyWindowInsetsListener(
                rootView.findViewById(R.id.findRoomRoot),
                (v, insets) -> {
                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
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

        bindViews();
        setupFilters();

        apiService = RetrofitClient.getApiService(requireContext());

        classroomAdapter = new ClassroomAdapter(requireContext());
        listClassrooms.setAdapter(classroomAdapter);

        /*
         * Sự kiện nhấn vào một dòng trong danh sách.
         * Thông tin phòng sẽ được đưa lên khu vực nhập liệu.
         */
        listClassrooms.setOnItemClickListener(
                (parent, v, position, id) -> {
                    Classroom selectedClassroom =
                            classroomAdapter.getItem(position);

                    showClassroomInInputArea(selectedClassroom);
                }
        );

        rootView.findViewById(R.id.btnFindRoomBack)
                .setOnClickListener(v -> requireActivity().onBackPressed());

        loadClassrooms();
    }

    private void bindViews() {
        etSearchRoom = rootView.findViewById(R.id.etSearchRoom);
        actCampus = rootView.findViewById(R.id.actCampusFilter);
        actBuilding = rootView.findViewById(R.id.actBuildingFilter);
        actStatus = rootView.findViewById(R.id.actStatusFilter);
        listClassrooms = rootView.findViewById(R.id.listClassrooms);
        tvResultCount = rootView.findViewById(R.id.tvRoomResultCount);
        tvRoomState = rootView.findViewById(R.id.tvRoomState);
        roomProgress = rootView.findViewById(R.id.roomProgress);
    }

    private void setupFilters() {
        filterAll = getString(R.string.filter_all);

        selectedCampus = filterAll;
        selectedBuilding = filterAll;
        selectedStatus = filterAll;

        setDropdownItems(
                actCampus,
                Collections.singletonList(filterAll)
        );

        setDropdownItems(
                actBuilding,
                Collections.singletonList(filterAll)
        );

        setDropdownItems(
                actStatus,
                Collections.singletonList(filterAll)
        );

        actCampus.setText(filterAll, false);
        actBuilding.setText(filterAll, false);
        actStatus.setText(filterAll, false);

        etSearchRoom.addTextChangedListener(new TextWatcher() {
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

        actCampus.setOnItemClickListener(
                (parent, v, position, id) -> {
                    selectedCampus = String.valueOf(
                            parent.getItemAtPosition(position)
                    );

                    selectedBuilding = filterAll;
                    actBuilding.setText(filterAll, false);

                    updateBuildingDropdown();
                    applyFilters();
                }
        );

        actBuilding.setOnItemClickListener(
                (parent, v, position, id) -> {
                    selectedBuilding = String.valueOf(
                            parent.getItemAtPosition(position)
                    );

                    applyFilters();
                }
        );

        actStatus.setOnItemClickListener(
                (parent, v, position, id) -> {
                    selectedStatus = String.valueOf(
                            parent.getItemAtPosition(position)
                    );

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

                if (!response.isSuccessful()
                        || response.body() == null) {
                    showState(getString(
                            R.string.room_load_error_http,
                            response.code()
                    ));
                    return;
                }

                allClassrooms.clear();
                allClassrooms.addAll(response.body());

                updateAllDropdowns();
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

    private void updateAllDropdowns() {
        List<String> campuses = new ArrayList<>();
        List<String> statuses = new ArrayList<>();

        campuses.add(filterAll);
        statuses.add(filterAll);

        Set<String> uniqueCampuses = new LinkedHashSet<>();
        Set<String> uniqueStatuses = new LinkedHashSet<>();

        for (Classroom classroom : allClassrooms) {
            String campus = campusLabel(classroom);

            if (!campus.isEmpty()) {
                uniqueCampuses.add(campus);
            }

            uniqueStatuses.add(
                    ClassroomAdapter.statusLabel(
                            requireContext(),
classroom.getOperationalStatus()
                    )
            );
        }

        List<String> sortedCampuses =
                new ArrayList<>(uniqueCampuses);

        List<String> sortedStatuses =
                new ArrayList<>(uniqueStatuses);

        Collections.sort(sortedCampuses);
        Collections.sort(sortedStatuses);

        campuses.addAll(sortedCampuses);
        statuses.addAll(sortedStatuses);

        setDropdownItems(actCampus, campuses);
        setDropdownItems(actStatus, statuses);

        updateBuildingDropdown();
    }

    private void updateBuildingDropdown() {
        List<String> buildings = new ArrayList<>();
        buildings.add(filterAll);

        Set<String> uniqueBuildings = new LinkedHashSet<>();

        for (Classroom classroom : allClassrooms) {
            if (!filterAll.equals(selectedCampus)
                    && !selectedCampus.equals(
                    campusLabel(classroom)
            )) {
                continue;
            }

            String building = buildingLabel(classroom);

            if (!building.isEmpty()) {
                uniqueBuildings.add(building);
            }
        }

        List<String> sortedBuildings =
                new ArrayList<>(uniqueBuildings);

        Collections.sort(sortedBuildings);
        buildings.addAll(sortedBuildings);

        setDropdownItems(actBuilding, buildings);
    }

    private void setDropdownItems(
            AutoCompleteTextView view,
            List<String> values
    ) {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
android.R.layout.simple_dropdown_item_1line,
                values
        );

        view.setAdapter(adapter);
    }

    private void applyFilters() {
        if (classroomAdapter == null) {
            return;
        }

        String query = etSearchRoom.getText() == null
                ? ""
                : etSearchRoom.getText()
                .toString()
                .trim()
                .toLowerCase(Locale.ROOT);

        List<Classroom> filtered = new ArrayList<>();

        for (Classroom classroom : allClassrooms) {
            String searchableText = (
                    classroom.getRoomCode()
                            + " "
                            + buildingLabel(classroom)
                            + " "
                            + campusLabel(classroom)
            ).toLowerCase(Locale.ROOT);

            boolean matchesSearch =
                    query.isEmpty()
                            || searchableText.contains(query);

            boolean matchesCampus =
                    filterAll.equals(selectedCampus)
                            || selectedCampus.equals(
                            campusLabel(classroom)
                    );

            boolean matchesBuilding =
                    filterAll.equals(selectedBuilding)
                            || selectedBuilding.equals(
                            buildingLabel(classroom)
                    );

            boolean matchesStatus =
                    filterAll.equals(selectedStatus)
                            || selectedStatus.equals(
                            ClassroomAdapter.statusLabel(
                                    requireContext(),
classroom.getOperationalStatus()
                            )
                    );

            if (matchesSearch
                    && matchesCampus
                    && matchesBuilding
                    && matchesStatus) {
                filtered.add(classroom);
            }
        }

        classroomAdapter.updateData(filtered);

        tvResultCount.setText(getString(
                R.string.room_result_count,
                filtered.size()
        ));

        if (filtered.isEmpty()) {
            showState(getString(
                    R.string.room_no_results
            ));
        } else {
            tvRoomState.setVisibility(View.GONE);
            listClassrooms.setVisibility(View.VISIBLE);
        }
    }

    /*
     * Hiển thị thông tin của phòng được chọn
     * lên khu vực nhập liệu phía trên danh sách.
     */
    private void showClassroomInInputArea(
            Classroom classroom
    ) {
        if (classroom == null) {
            return;
        }

        String campus = campusLabel(classroom);
        String building = buildingLabel(classroom);

        String status = ClassroomAdapter.statusLabel(
                requireContext(),
classroom.getOperationalStatus()
        );

        selectedCampus = campus.isEmpty()
                ? filterAll
                : campus;

        selectedBuilding = building.isEmpty()
                ? filterAll
                : building;

        selectedStatus = status.isEmpty()
                ? filterAll
                : status;

        // Hiển thị cơ sở.
        actCampus.setText(
                selectedCampus,
                false
        );

        // Cập nhật danh sách tòa nhà theo cơ sở vừa chọn.
        updateBuildingDropdown();

        // Hiển thị tòa nhà.
        actBuilding.setText(
                selectedBuilding,
                false
        );

        // Hiển thị trạng thái.
        actStatus.setText(
                selectedStatus,
                false
        );

        // Hiển thị mã phòng trong ô tìm kiếm.
        etSearchRoom.setText(
                classroom.getRoomCode()
        );

        etSearchRoom.setSelection(
                etSearchRoom.length()
        );

        etSearchRoom.clearFocus();

        // Lọc lại danh sách theo phòng vừa chọn.
        applyFilters();
    }

    private String campusLabel(
            Classroom classroom
    ) {
        Classroom.Building building =
                classroom.getBuilding();

        Classroom.Campus campus =
                building == null
                        ? null
                        : building.getCampus();

        return campus == null
                ? ""
                : joinName(
                campus.getCampusCode(),
                campus.getCampusName()
        );
    }

    private String buildingLabel(
            Classroom classroom
    ) {
        Classroom.Building building =
                classroom.getBuilding();

        return building == null
                ? ""
                : joinName(
                building.getBuildingCode(),
                building.getBuildingName()
        );
    }

    private String joinName(
            String code,
            String name
    ) {
        String safeCode =
                code == null ? "" : code.trim();

        String safeName =
                name == null ? "" : name.trim();

        if (safeCode.isEmpty()) {
            return safeName;
        }

        if (safeName.isEmpty()) {
            return safeCode;
        }

        return safeCode + " - " + safeName;
    }

    private void setLoading(boolean loading) {
        roomProgress.setVisibility(
                loading ? View.VISIBLE : View.GONE
        );

        listClassrooms.setVisibility(
                loading ? View.GONE : View.VISIBLE
        );

        if (loading) {
            tvRoomState.setVisibility(View.GONE);
            tvResultCount.setText(
                    R.string.room_loading
            );
        }
    }

    private void showState(String message) {
        listClassrooms.setVisibility(View.GONE);

        tvRoomState.setText(message);
        tvRoomState.setVisibility(View.VISIBLE);
    }

    private void openLogin() {
        Intent intent = new Intent(
                requireContext(),
LoginActivity.class
        );

        intent.addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        requireActivity().onBackPressed();
    }
}