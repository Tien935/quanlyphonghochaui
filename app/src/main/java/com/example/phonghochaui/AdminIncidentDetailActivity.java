package com.example.phonghochaui;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.phonghochaui.data.model.IncidentReport;
import com.example.phonghochaui.data.remote.RetrofitClient;
import com.example.phonghochaui.data.remote.SessionManager;
import com.example.phonghochaui.data.remote.SupabaseApiService;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.gson.JsonObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminIncidentDetailActivity extends AppCompatActivity {

    private IncidentReport incident;
    private SessionManager sessionManager;
    private SupabaseApiService apiService;

    private TextView tvRoomName, tvIssueType, tvPriority, tvStatus, tvDate, tvDesc;
    private MaterialButton btnAction;
    private LinearProgressIndicator progressIndicator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_incident_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.btnIncidentAction).getRootView(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sessionManager = new SessionManager(this);
        apiService = RetrofitClient.getApiService(this);

        incident = (IncidentReport) getIntent().getSerializableExtra("INCIDENT");
        if (incident == null) {
            finish();
            return;
        }

        bindViews();
        displayData();

        findViewById(R.id.btnIncidentDetailBack).setOnClickListener(v -> finish());
        btnAction.setOnClickListener(v -> handleAction());
    }

    private void bindViews() {
        tvRoomName = findViewById(R.id.tvDetailRoomName);
        tvIssueType = findViewById(R.id.tvDetailIssueType);
        tvPriority = findViewById(R.id.tvDetailPriority);
        tvStatus = findViewById(R.id.tvDetailStatus);
        tvDate = findViewById(R.id.tvDetailDate);
        tvDesc = findViewById(R.id.tvDetailDesc);
        btnAction = findViewById(R.id.btnIncidentAction);
        progressIndicator = findViewById(R.id.incidentActionProgress);
    }

    private void displayData() {
        if (incident.getClassroom() != null) {
            String bName = incident.getClassroom().getBuilding() != null ? incident.getClassroom().getBuilding().getBuildingName() : "?";
            tvRoomName.setText(incident.getClassroom().getRoomCode() + " (Tòa " + bName + ")");
        } else {
            tvRoomName.setText("ID Phòng: " + incident.getClassroomId());
        }

        tvIssueType.setText("Loại sự cố: " + incident.getIssueType());
        
        String p = "Trung bình";
        if ("low".equalsIgnoreCase(incident.getPriority())) p = "Thấp";
        else if ("high".equalsIgnoreCase(incident.getPriority())) p = "Cao";
        tvPriority.setText("Mức độ ưu tiên: " + p);
        
        tvDate.setText("Ngày gửi: " + formatDate(incident.getCreatedAt()));
        tvDesc.setText(incident.getDescription());

        setupStatusAndButton();
    }

    private void setupStatusAndButton() {
        String status = incident.getStatus();
        if ("pending".equalsIgnoreCase(status)) {
            tvStatus.setText("Trạng thái: Chờ xử lý");
            btnAction.setText(getString(R.string.admin_incident_accept));
            btnAction.setVisibility(View.VISIBLE);
        } else if ("processing".equalsIgnoreCase(status)) {
            tvStatus.setText("Trạng thái: Đang bảo trì");
            btnAction.setText(getString(R.string.admin_incident_resolve));
            btnAction.setVisibility(View.VISIBLE);
        } else {
            tvStatus.setText("Trạng thái: Đã xử lý");
            btnAction.setVisibility(View.GONE);
        }
    }

    private void handleAction() {
        String currentStatus = incident.getStatus();
        if ("pending".equalsIgnoreCase(currentStatus)) {
            // Update to processing + maintenance
            updateIncidentAndRoom("processing", "maintenance");
        } else if ("processing".equalsIgnoreCase(currentStatus)) {
            // Update to resolved + active
            updateIncidentAndRoom("resolved", "active");
        }
    }

    private void updateIncidentAndRoom(String newIncidentStatus, String newRoomStatus) {
        setLoading(true);

        JsonObject incidentUpdates = new JsonObject();
        incidentUpdates.addProperty("status", newIncidentStatus);
        incidentUpdates.addProperty("handled_by", sessionManager.getUserId());
        if ("resolved".equalsIgnoreCase(newIncidentStatus)) {
            // Just use a simple ISO string or let DB handle it? We can pass current time
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            incidentUpdates.addProperty("handled_at", sdf.format(new Date()));
        }

        // 1. Update Incident
        apiService.updateIncidentReport("eq." + incident.getId(), incidentUpdates).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    // 2. Update Classroom
                    JsonObject roomUpdates = new JsonObject();
                    roomUpdates.addProperty("operational_status", newRoomStatus);
                    
                    apiService.updateClassroomStatus("eq." + incident.getClassroomId(), roomUpdates).enqueue(new Callback<Void>() {
                        @Override
                        public void onResponse(Call<Void> call, Response<Void> response2) {
                            setLoading(false);
                            if (response2.isSuccessful()) {
                                Toast.makeText(AdminIncidentDetailActivity.this, R.string.admin_incident_update_success, Toast.LENGTH_SHORT).show();
                                finish(); // Go back to list, list will refresh onResume
                            } else {
                                Toast.makeText(AdminIncidentDetailActivity.this, "Lỗi cập nhật phòng: " + response2.code(), Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Void> call, Throwable t) {
                            setLoading(false);
                            Toast.makeText(AdminIncidentDetailActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
                        }
                    });

                } else {
                    setLoading(false);
                    String errMsg = "Lỗi cập nhật sự cố: " + response.code();
                    try {
                        if (response.errorBody() != null) errMsg += "\n\nChi tiết:\n" + response.errorBody().string();
                    } catch (Exception e) {}
                    
                    new com.google.android.material.dialog.MaterialAlertDialogBuilder(AdminIncidentDetailActivity.this)
                        .setTitle("Lỗi cập nhật")
                        .setMessage(errMsg)
                        .setPositiveButton("Đóng", null)
                        .show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                setLoading(false);
                Toast.makeText(AdminIncidentDetailActivity.this, "Lỗi mạng", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setLoading(boolean loading) {
        btnAction.setEnabled(!loading);
        progressIndicator.setVisibility(loading ? View.VISIBLE : View.GONE);
    }

    private String formatDate(String isoString) {
        if (isoString == null || isoString.isEmpty()) return "";
        try {
            SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            inFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = inFormat.parse(isoString);
            
            SimpleDateFormat outFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
            return outFormat.format(date);
        } catch (ParseException e) {
            return isoString.split("T")[0]; // fallback
        }
    }
}
