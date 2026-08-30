package com.example.phonghochaui;

import android.content.Intent;
import android.os.Bundle;
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

import com.example.phonghochaui.data.model.RoomBooking;
import com.example.phonghochaui.data.remote.RetrofitClient;
import com.example.phonghochaui.data.remote.SessionManager;
import com.example.phonghochaui.data.remote.SupabaseApiService;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MyBookingsFragment extends Fragment {

    private static final String BOOKING_SELECT =
            "id,booking_date,start_time,end_time,"
                    + "headcount,purpose,status,admin_note,"
                    + "classroom:classrooms("
                    + "id,room_code,floor,capacity,"
                    + "operational_status,"
                    + "building:buildings("
                    + "id,building_code,building_name,"
                    + "campus:campuses("
                    + "id,campus_code,campus_name,address"
                    + ")))";

    private final List<RoomBooking> allBookings =
            new ArrayList<>();

    private AutoCompleteTextView actStatus;
    private ListView listBookings;
    private TextView tvCount;
    private TextView tvState;
    private LinearProgressIndicator progress;

    private RoomBookingAdapter adapter;
    private SupabaseApiService apiService;
    private SessionManager sessionManager;

    private String filterAll;
    private String selectedStatus;

    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.activity_my_bookings, container, false);
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sessionManager =
                new SessionManager(requireContext());

        if (!sessionManager.hasSession()
                || !"student".equals(
                sessionManager.getUserRole()
        )) {
            openLogin();
            return;
        }

        // EdgeToEdge.enable(requireActivity());

        

        ViewCompat.setOnApplyWindowInsetsListener(
                rootView.findViewById(R.id.myBookingsRoot),
                (v, insets) -> {
                    Insets bars =
                            insets.getInsets(
                                    WindowInsetsCompat
                                            .Type
                                            .systemBars()
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

        apiService =
                RetrofitClient.getApiService(requireContext());

        adapter =
                new RoomBookingAdapter(requireContext());

        listBookings.setAdapter(adapter);

        rootView.findViewById(R.id.btnMyBookingsBack)
                .setOnClickListener(
                        v -> requireActivity().onBackPressed()
                );

        rootView.findViewById(R.id.btnRefreshBookings)
                .setOnClickListener(
                        v -> loadBookings()
                );

        loadBookings();
    }

    private void bindViews() {
        actStatus = rootView.findViewById(
                R.id.actMyBookingStatus
        );

        listBookings = rootView.findViewById(
                R.id.listMyBookings
        );

        tvCount = rootView.findViewById(
                R.id.tvMyBookingCount
        );

        tvState = rootView.findViewById(
                R.id.tvMyBookingState
        );

        progress = rootView.findViewById(
                R.id.myBookingProgress
        );
    }

    private void setupStatusFilter() {
        filterAll =
                getString(R.string.filter_all);

        selectedStatus = filterAll;

        List<String> options =
                Arrays.asList(
                        filterAll,
                        getString(
                                R.string.booking_status_pending
                        ),
                        getString(
                                R.string.booking_status_approved
                        ),
                        getString(
                                R.string.booking_status_rejected
                        ),
                        getString(
                                R.string.booking_status_cancelled
                        )
                );

        actStatus.setAdapter(
                new ArrayAdapter<>(
                        requireContext(),
android.R.layout
                                .simple_dropdown_item_1line,
                        options
                )
        );

        actStatus.setText(
                filterAll,
                false
        );

        actStatus.setOnItemClickListener(
                (parent, v, position, id) -> {
                    selectedStatus =
                            String.valueOf(
                                    parent.getItemAtPosition(
                                            position
                                    )
                            );

                    applyFilter();
                }
        );
    }

    private void loadBookings() {
        setLoading(true);

        String userId =
                sessionManager.getUserId();

        if (userId.isEmpty()) {
            sessionManager.clearSession();
            openLogin();
            return;
        }

        apiService.getMyBookings(
                BOOKING_SELECT,
                "eq." + userId,
                "booking_date.desc,start_time.desc"
        ).enqueue(
                new Callback<List<RoomBooking>>() {
                    @Override
                    public void onResponse(
                            Call<List<RoomBooking>> call,
                            Response<List<RoomBooking>> response
                    ) {
                        setLoading(false);

                        if (!response.isSuccessful()
                                || response.body() == null) {

                            if (response.code() == 401) {
                                showState(
                                        getString(
                                                R.string.session_expired_message
                                        )
                                );
                            } else {
                                showState(
                                        getString(
                                                R.string.my_bookings_load_error,
                                                response.code()
                                        )
                                );
                            }

                            return;
                        }

                        allBookings.clear();

                        allBookings.addAll(
                                response.body()
                        );

                        applyFilter();
                    }

                    @Override
                    public void onFailure(
                            Call<List<RoomBooking>> call,
                            Throwable throwable
                    ) {
                        setLoading(false);

                        String detail =
                                throwable.getMessage();

                        showState(
                                getString(
                                        R.string.room_load_error_network,
                                        detail == null
                                                || detail.trim().isEmpty()
                                                ? getString(
                                                R.string.unknown_error
                                        )
                                                : detail
                                )
                        );
                    }
                }
        );
    }

    private void applyFilter() {
        if (adapter == null) {
            return;
        }

        List<RoomBooking> filtered =
                new ArrayList<>();

        for (RoomBooking booking
                : allBookings) {

            String label =
                    RoomBookingAdapter.statusLabel(
                            requireContext(),
booking.getStatus()
                    );

            if (filterAll.equals(selectedStatus)
                    || selectedStatus.equals(label)) {

                filtered.add(booking);
            }
        }

        adapter.updateData(filtered);

        tvCount.setText(
                getString(
                        R.string.my_bookings_count,
                        filtered.size()
                )
        );

        if (filtered.isEmpty()) {
            showState(
                    getString(
                            R.string.my_bookings_empty
                    )
            );
        } else {
            tvState.setVisibility(View.GONE);

            listBookings.setVisibility(
                    View.VISIBLE
            );
        }
    }

    private void setLoading(
            boolean loading
    ) {
        progress.setVisibility(
                loading
                        ? View.VISIBLE
                        : View.GONE
        );

        listBookings.setVisibility(
                loading
                        ? View.GONE
                        : View.VISIBLE
        );

        if (loading) {
            tvState.setVisibility(View.GONE);

            tvCount.setText(
                    R.string.my_bookings_loading
            );
        }
    }

    private void showState(
            String message
    ) {
        listBookings.setVisibility(View.GONE);

        tvState.setText(message);

        tvState.setVisibility(View.VISIBLE);
    }

    private void openLogin() {
        Intent intent =
                new Intent(
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