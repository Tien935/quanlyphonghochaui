package com.example.phonghochaui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonghochaui.adapter.IncidentAdapter;
import com.example.phonghochaui.data.model.IncidentReport;
import com.example.phonghochaui.data.remote.RetrofitClient;
import com.example.phonghochaui.data.remote.SupabaseApiService;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminIncidentsActivity extends AppCompatActivity {

    private SupabaseApiService apiService;
    private RecyclerView rvIncidents;
    private IncidentAdapter adapter;
    private View llLoading;
    private View llEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_incidents);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rvIncidents).getRootView(), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        apiService = RetrofitClient.getApiService(this);

        rvIncidents = findViewById(R.id.rvIncidents);
        llLoading = findViewById(R.id.llIncidentsLoading);
        llEmpty = findViewById(R.id.llIncidentsEmpty);

        rvIncidents.setLayoutManager(new LinearLayoutManager(this));
        adapter = new IncidentAdapter(new ArrayList<>(), incident -> {
            Intent intent = new Intent(this, AdminIncidentDetailActivity.class);
            intent.putExtra("INCIDENT", incident);
            startActivity(intent);
        });
        rvIncidents.setAdapter(adapter);

        findViewById(R.id.btnIncidentsBack).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadIncidents();
    }

    private void loadIncidents() {
        llLoading.setVisibility(View.VISIBLE);
        llEmpty.setVisibility(View.GONE);
        rvIncidents.setVisibility(View.GONE);

        // Fetch reports and join with classrooms and buildings
        apiService.getIncidentReports(
                "*,classroom:classrooms(*,building:buildings(*))",
                "status.asc,created_at.desc" // pending -> in_progress -> resolved
        ).enqueue(new Callback<List<IncidentReport>>() {
            @Override
            public void onResponse(Call<List<IncidentReport>> call, Response<List<IncidentReport>> response) {
                llLoading.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    List<IncidentReport> list = response.body();
                    if (list.isEmpty()) {
                        llEmpty.setVisibility(View.VISIBLE);
                    } else {
                        rvIncidents.setVisibility(View.VISIBLE);
                        // Sort: pending -> processing -> resolved
                        list.sort((a, b) -> {
                            int statusA = getStatusWeight(a.getStatus());
                            int statusB = getStatusWeight(b.getStatus());
                            if (statusA != statusB) return Integer.compare(statusA, statusB);
                            return b.getCreatedAt().compareTo(a.getCreatedAt()); // newer first
                        });
                        
                        adapter.updateData(list);
                    }
                } else {
                    llEmpty.setVisibility(View.VISIBLE);
                    String errorMsg = "Không thể tải danh sách sự cố";
                    try {
                        if (response.errorBody() != null) {
                            errorMsg += ": HTTP " + response.code() + " - " + response.errorBody().string();
                        }
                    } catch (Exception e) {}
                    Snackbar.make(rvIncidents, errorMsg, Snackbar.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<List<IncidentReport>> call, Throwable t) {
                llLoading.setVisibility(View.GONE);
                llEmpty.setVisibility(View.VISIBLE);
                Snackbar.make(rvIncidents, "Lỗi kết nối: " + t.getMessage(), Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private int getStatusWeight(String status) {
        if ("pending".equalsIgnoreCase(status)) return 0;
        if ("processing".equalsIgnoreCase(status)) return 1;
        if ("resolved".equalsIgnoreCase(status)) return 2;
        return 3;
    }
}
