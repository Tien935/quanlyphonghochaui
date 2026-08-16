package com.example.phonghochaui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.phonghochaui.data.model.Classroom;
import com.example.phonghochaui.data.model.RoomBooking;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RoomBookingAdapter extends BaseAdapter {

    private final Context context;
    private final LayoutInflater inflater;

    private final List<RoomBooking> bookings =
            new ArrayList<>();

    public RoomBookingAdapter(Context context) {
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    public void updateData(
            List<RoomBooking> newData
    ) {
        bookings.clear();

        if (newData != null) {
            bookings.addAll(newData);
        }

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return bookings.size();
    }

    @Override
    public RoomBooking getItem(int position) {
        return bookings.get(position);
    }

    @Override
    public long getItemId(int position) {
        return getItem(position).getId();
    }

    @Override
    public View getView(
            int position,
            View convertView,
            ViewGroup parent
    ) {
        ViewHolder holder;

        if (convertView == null) {
            convertView = inflater.inflate(
                    R.layout.item_room_booking,
                    parent,
                    false
            );

            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder =
                    (ViewHolder) convertView.getTag();
        }

        bind(holder, getItem(position));

        return convertView;
    }

    private void bind(
            ViewHolder holder,
            RoomBooking booking
    ) {
        Classroom classroom =
                booking.getClassroom();

        Classroom.Building building =
                classroom == null
                        ? null
                        : classroom.getBuilding();

        Classroom.Campus campus =
                building == null
                        ? null
                        : building.getCampus();

        holder.tvBookingCode.setText(
                context.getString(
                        R.string.my_booking_code,
                        booking.getId()
                )
        );

        holder.tvRoomCode.setText(
                classroom == null
                        ? context.getString(
                        R.string.information_not_available
                )
                        : classroom.getRoomCode()
        );

        holder.tvDateTime.setText(
                context.getString(
                        R.string.my_booking_date_time,
                        formatDate(
                                booking.getBookingDate()
                        ),
                        formatTime(
                                booking.getStartTime()
                        ),
                        formatTime(
                                booking.getEndTime()
                        )
                )
        );

        holder.tvLocation.setText(
                locationText(building, campus)
        );

        holder.tvHeadcount.setText(
                context.getString(
                        R.string.my_booking_headcount,
                        booking.getHeadcount()
                )
        );

        holder.tvPurpose.setText(
                context.getString(
                        R.string.my_booking_purpose,
                        booking.getPurpose()
                )
        );

        holder.tvStatus.setText(
                statusLabel(
                        context,
                        booking.getStatus()
                )
        );

        holder.tvStatus.setTextColor(
                statusColor(
                        booking.getStatus()
                )
        );

        String adminNote =
                booking.getAdminNote().trim();

        holder.tvAdminNote.setText(
                context.getString(
                        R.string.my_booking_admin_note,
                        adminNote
                )
        );

        holder.tvAdminNote.setVisibility(
                adminNote.isEmpty()
                        ? View.GONE
                        : View.VISIBLE
        );
    }

    private String locationText(
            Classroom.Building building,
            Classroom.Campus campus
    ) {
        String buildingCode =
                building == null
                        ? ""
                        : building.getBuildingCode();

        String buildingName =
                building == null
                        ? ""
                        : building.getBuildingName();

        String campusName =
                campus == null
                        ? ""
                        : campus.getCampusName();

        String buildingText =
                joinName(
                        buildingCode,
                        buildingName
                );

        if (buildingText.isEmpty()
                && campusName.isEmpty()) {
            return context.getString(
                    R.string.information_not_available
            );
        }

        if (campusName.isEmpty()) {
            return buildingText;
        }

        if (buildingText.isEmpty()) {
            return campusName;
        }

        return buildingText
                + " • "
                + campusName;
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

    private String formatDate(
            String apiDate
    ) {
        try {
            SimpleDateFormat apiFormat =
                    new SimpleDateFormat(
                            "yyyy-MM-dd",
                            Locale.ROOT
                    );

            Date date =
                    apiFormat.parse(apiDate);

            if (date == null) {
                return apiDate;
            }

            SimpleDateFormat displayFormat =
                    new SimpleDateFormat(
                            "dd/MM/yyyy",
                            Locale.getDefault()
                    );

            return displayFormat.format(date);

        } catch (ParseException exception) {
            return apiDate;
        }
    }

    private String formatTime(
            String apiTime
    ) {
        return apiTime != null
                && apiTime.length() >= 5
                ? apiTime.substring(0, 5)
                : apiTime;
    }

    public static String statusLabel(
            Context context,
            String status
    ) {
        String normalized =
                normalize(status);

        switch (normalized) {
            case "pending":
                return context.getString(
                        R.string.booking_status_pending
                );

            case "approved":
                return context.getString(
                        R.string.booking_status_approved
                );

            case "rejected":
                return context.getString(
                        R.string.booking_status_rejected
                );

            case "cancelled":
                return context.getString(
                        R.string.booking_status_cancelled
                );

            default:
                return status == null
                        || status.trim().isEmpty()
                        ? context.getString(
                        R.string.information_not_available
                )
                        : status.trim();
        }
    }

    private int statusColor(String status) {
        switch (normalize(status)) {
            case "approved":
                return Color.rgb(
                        24,
                        133,
                        81
                );

            case "rejected":
                return ContextCompat.getColor(
                        context,
                        R.color.haui_red
                );

            case "pending":
                return Color.rgb(
                        173,
                        112,
                        0
                );

            case "cancelled":
                return ContextCompat.getColor(
                        context,
                        R.color.haui_text_muted
                );

            default:
                return ContextCompat.getColor(
                        context,
                        R.color.haui_blue
                );
        }
    }

    private static String normalize(
            String status
    ) {
        return status == null
                ? ""
                : status.trim()
                .toLowerCase(Locale.ROOT);
    }

    private static class ViewHolder {

        final TextView tvBookingCode;
        final TextView tvStatus;
        final TextView tvRoomCode;
        final TextView tvDateTime;
        final TextView tvLocation;
        final TextView tvHeadcount;
        final TextView tvPurpose;
        final TextView tvAdminNote;

        ViewHolder(View view) {
            tvBookingCode =
                    view.findViewById(
                            R.id.tvItemBookingCode
                    );

            tvStatus =
                    view.findViewById(
                            R.id.tvItemBookingStatus
                    );

            tvRoomCode =
                    view.findViewById(
                            R.id.tvItemBookingRoom
                    );

            tvDateTime =
                    view.findViewById(
                            R.id.tvItemBookingDateTime
                    );

            tvLocation =
                    view.findViewById(
                            R.id.tvItemBookingLocation
                    );

            tvHeadcount =
                    view.findViewById(
                            R.id.tvItemBookingHeadcount
                    );

            tvPurpose =
                    view.findViewById(
                            R.id.tvItemBookingPurpose
                    );

            tvAdminNote =
                    view.findViewById(
                            R.id.tvItemBookingAdminNote
                    );
        }
    }
}