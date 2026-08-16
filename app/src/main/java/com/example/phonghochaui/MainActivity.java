package com.example.phonghochaui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.phonghochaui.data.remote.RetrofitClient;
import com.example.phonghochaui.data.remote.SupabaseApiService;
import com.example.phonghochaui.data.remote.SupabaseConfig;
import com.google.gson.JsonArray;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private Button btnTestConnection;
    private TextView tvConfigurationStatus;
    private TextView tvConnectionResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnTestConnection = findViewById(R.id.btnTestConnection);
        tvConfigurationStatus = findViewById(R.id.tvConfigurationStatus);
        tvConnectionResult = findViewById(R.id.tvConnectionResult);

        updateConfigurationStatus();
        btnTestConnection.setOnClickListener(view -> testSupabaseConnection());
    }

    private void updateConfigurationStatus() {
        int message = SupabaseConfig.isConfigured()
                ? R.string.configuration_ready
                : R.string.configuration_missing;
        tvConfigurationStatus.setText(message);
    }

    private void testSupabaseConnection() {
        if (!SupabaseConfig.isConfigured()) {
            tvConnectionResult.setText(R.string.configuration_missing);
            return;
        }

        setLoading(true);

        SupabaseApiService apiService = RetrofitClient.getApiService(this);
        apiService.getCampuses("*", 1).enqueue(new Callback<JsonArray>() {
            @Override
            public void onResponse(Call<JsonArray> call, Response<JsonArray> response) {
                setLoading(false);

                if (response.isSuccessful()) {
                    int rowCount = response.body() == null ? 0 : response.body().size();
                    tvConnectionResult.setText(
                            "Kết nối thành công.\n"
                                    + "HTTP: " + response.code() + "\n"
                                    + "Bảng: campuses\n"
                                    + "Số bản ghi nhận được: " + rowCount
                    );
                    return;
                }

                tvConnectionResult.setText(
                        "Kết nối tới Supabase nhưng API trả về lỗi.\n"
                                + "HTTP: " + response.code() + "\n"
                                + "Chi tiết: " + readErrorBody(response.errorBody())
                );
            }

            @Override
            public void onFailure(Call<JsonArray> call, Throwable throwable) {
                setLoading(false);
                String message = throwable.getMessage() == null
                        ? throwable.getClass().getSimpleName()
                        : throwable.getMessage();
                tvConnectionResult.setText(
                        "Không thể kết nối.\n"
                                + "Kiểm tra Internet, Project URL và Logcat.\n"
                                + "Chi tiết: " + message
                );
            }
        });
    }

    private void setLoading(boolean loading) {
        btnTestConnection.setEnabled(!loading);
        btnTestConnection.setText(
                loading ? R.string.testing_connection : R.string.test_connection
        );
    }

    private String readErrorBody(ResponseBody errorBody) {
        if (errorBody == null) {
            return "Không có nội dung lỗi.";
        }

        try {
            return errorBody.string();
        } catch (IOException exception) {
            return "Không đọc được nội dung lỗi.";
        }
    }
}
