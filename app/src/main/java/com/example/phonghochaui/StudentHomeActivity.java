package com.example.phonghochaui;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class StudentHomeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_student_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.studentHomeRoot), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0); // Bottom padding is handled by BottomNavigationView
            return insets;
        });

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment_student);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            BottomNavigationView bottomNav = findViewById(R.id.bottom_nav_student);
            NavigationUI.setupWithNavController(bottomNav, navController);

            // Ghi đè hành vi chọn tab mới (ngăn khôi phục lịch sử cũ)
            bottomNav.setOnItemSelectedListener(item -> {
                androidx.navigation.NavOptions options = new androidx.navigation.NavOptions.Builder()
                        .setLaunchSingleTop(true)
                        .setRestoreState(false) // KHÔNG khôi phục trạng thái cũ
                        .setPopUpTo(navController.getGraph().getStartDestinationId(), false, false) // Xóa lịch sử hiện tại
                        .build();
                try {
                    navController.navigate(item.getItemId(), null, options);
                    return true;
                } catch (IllegalArgumentException e) {
                    return false;
                }
            });

            // Ghi đè hành vi nhấn lại tab hiện tại (để đưa về trang gốc nếu đang ở trang con)
            bottomNav.setOnItemReselectedListener(item -> {
                navController.popBackStack(navController.getGraph().getStartDestinationId(), false);
            });
        }
    }
}