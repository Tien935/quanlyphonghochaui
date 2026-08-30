package com.example.phonghochaui.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.phonghochaui.R;
import com.example.phonghochaui.data.model.Classroom;
import com.example.phonghochaui.data.model.IncidentReport;
import com.example.phonghochaui.data.remote.RetrofitClient;
import com.example.phonghochaui.data.remote.SessionManager;
import com.example.phonghochaui.data.remote.SupabaseApiService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ReportIncidentFragment extends Fragment {

    private SessionManager sessionManager;
    private SupabaseApiService apiService;

    private AutoCompleteTextView actRoom;
    private AutoCompleteTextView actIssueType;
    private AutoCompleteTextView actPriority;
    private TextInputEditText etDesc;
    private TextView tvError;
    private MaterialButton btnSubmit;
    private LinearProgressIndicator progressIndicator;

    private List<Classroom> availableRooms = new ArrayList<>();
    private Classroom selectedRoom = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_report_incident, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager = new SessionManager(requireContext());
        apiService = RetrofitClient.getApiService(requireContext());

        actRoom = view.findViewById(R.id.actIncidentRoom);
        actIssueType = view.findViewById(R.id.actIncidentIssueType);
        actPriority = view.findViewById(R.id.actIncidentPriority);
        etDesc = view.findViewById(R.id.etIncidentDesc);
        tvError = view.findViewById(R.id.tvIncidentError);
        btnSubmit = view.findViewById(R.id.btnSubmitIncident);
        progressIndicator = view.findViewById(R.id.incidentProgress);

        view.findViewById(R.id.btnIncidentBack).setOnClickListener(v -> {
            Navigation.findNavController(v).navigateUp();
        });

        btnSubmit.setOnClickListener(v -> attemptSubmit(view));

        setupDropdowns();
        loadRooms();
    }

    private void setupDropdowns() {
        // Issue Types
        String[] issueTypes = {"Cơ sở vật chất (Bàn ghế, cửa, đèn...)", "Thiết bị điện tử (Máy chiếu, điều hòa, loa...)", "Phần mềm/Máy tính", "Khác"};
        ArrayAdapter<String> issueAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, issueTypes);
        actIssueType.setAdapter(issueAdapter);
        actIssueType.setText(issueTypes[1], false); // Default

        // Priorities
        // Giữ giá trị tiếng Anh để map với database default ('low', 'medium', 'high')
        // Giao diện hiển thị tiếng Việt, mapping xử lý lúc submit
        String[] priorities = {"Thấp", "Trung bình", "Cao"};
        ArrayAdapter<String> priorityAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, priorities);
        actPriority.setAdapter(priorityAdapter);
        actPriority.setText(priorities[1], false); // Default "Trung bình"
    }

    private void loadRooms() {
        setLoading(true, getString(R.string.room_loading));
        
        apiService.getClassrooms(
                "*,building:buildings(*)",
                "room_code.asc"
        ).enqueue(new Callback<List<Classroom>>() {
            @Override
            public void onResponse(Call<List<Classroom>> call, Response<List<Classroom>> response) {
                setLoading(false, getString(R.string.incident_submit));
                if (response.isSuccessful() && response.body() != null) {
                    availableRooms.clear();
                    List<String> roomNames = new ArrayList<>();
                    for (Classroom c : response.body()) {
                        if ("active".equalsIgnoreCase(c.getOperationalStatus())) {
                            availableRooms.add(c);
                            String buildingName = c.getBuilding() != null ? c.getBuilding().getBuildingName() : "Unknown";
                            roomNames.add(c.getRoomCode() + " (Tòa " + buildingName + ")");
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_dropdown_item_1line,
                            roomNames
                    );
                    actRoom.setAdapter(adapter);
                    actRoom.setOnItemClickListener((parent, view, position, id) -> {
                        selectedRoom = availableRooms.get(position);
                        tvError.setVisibility(View.GONE);
                    });
                } else {
                    showError(getString(R.string.room_load_error_http, response.code()));
                }
            }

            @Override
            public void onFailure(Call<List<Classroom>> call, Throwable t) {
                setLoading(false, getString(R.string.incident_submit));
                showError(getString(R.string.room_load_error_network, t.getMessage()));
            }
        });
    }

    private void attemptSubmit(View view) {
        tvError.setVisibility(View.GONE);

        if (selectedRoom == null) {
            showError(getString(R.string.incident_error_room));
            return;
        }

        String desc = etDesc.getText() != null ? etDesc.getText().toString().trim() : "";
        if (desc.length() < 10) {
            showError(getString(R.string.incident_error_desc));
            return;
        }

        String displayPriority = actPriority.getText().toString();
        String priorityCode = "medium";
        if (displayPriority.equals("Thấp")) priorityCode = "low";
        else if (displayPriority.equals("Cao")) priorityCode = "high";

        String issueType = actIssueType.getText().toString();

        setLoading(true, getString(R.string.incident_submitting));

        IncidentReport report = new IncidentReport(
                sessionManager.getUserId(),
                selectedRoom.getId(),
                issueType,
                priorityCode,
                desc
        );

        apiService.createIncidentReport(report).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                setLoading(false, getString(R.string.incident_submit));
                if (response.isSuccessful()) {
                    new MaterialAlertDialogBuilder(requireContext())
                            .setTitle(R.string.incident_success_title)
                            .setMessage(R.string.incident_success_message)
                            .setPositiveButton(R.string.back_to_home, (dialog, which) -> {
                                Navigation.findNavController(view).navigateUp();
                            })
                            .setCancelable(false)
                            .show();
                } else {
                    String errorMsg = getString(R.string.incident_error_submit);
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += " Chi tiết: HTTP " + response.code() + " - " + response.errorBody().string();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    showError(errorMsg);
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                setLoading(false, getString(R.string.incident_submit));
                showError(getString(R.string.room_load_error_network, t.getMessage()));
            }
        });
    }

    private void setLoading(boolean isLoading, String buttonText) {
        actRoom.setEnabled(!isLoading);
        actIssueType.setEnabled(!isLoading);
        actPriority.setEnabled(!isLoading);
        etDesc.setEnabled(!isLoading);
        btnSubmit.setEnabled(!isLoading);
        btnSubmit.setText(buttonText);
        progressIndicator.setVisibility(isLoading ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        tvError.setText(message);
        tvError.setVisibility(View.VISIBLE);
    }
}
