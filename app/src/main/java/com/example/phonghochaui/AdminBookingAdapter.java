package com.example.phonghochaui;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.example.phonghochaui.data.model.AdminBooking;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdminBookingAdapter extends BaseAdapter {

    private final Context context;
    private final LayoutInflater inflater;
    private final List<AdminBooking> bookings =
            new ArrayList<>();

    public AdminBookingAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    public void updateData(List<AdminBooking> newData) {
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
    public AdminBooking getItem(int position) {
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
                    R.layout.item_admin_booking,
                    parent,
                    false
            );

            holder = new ViewHolder(convertView);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        bind(holder, getItem(position));

        return convertView;
    }

    private void bind(
            ViewHolder holder,
            AdminBooking booking
    ) {
        holder.tvCode.setText(context.getString(
                R.string.my_booking_code,
                booking.getId()
        ));

        holder.tvStatus.setText(
                statusLabel(
                        context,
                        booking.getStatus()
                )
        );

        holder.tvStatus.setTextColor(
                statusColor(booking.getStatus())
        );

        String fullName =
                booking.getStudentFullName().trim();

        if (fullName.isEmpty()) {
            fullName = context.getString(
                    R.string.information_not_available
            );
        }

        String hauiCode =
                booking.getStudentHauiCode().trim();

        holder.tvStudent.setText(context.getString(
                R.string.admin_booking_student,
                fullName,
                hauiCode.isEmpty()
                        ? context.getString(
                        R.string.information_not_available
                )
                        : hauiCode
        ));

        holder.tvRoom.setText(context.getString(
                R.string.admin_booking_room,
                booking.getRoomCode(),
                booking.getFloor(),
                booking.getCapacity()
        ));

        holder.tvDateTime.setText(context.getString(
                R.string.my_booking_date_time,
                formatDate(booking.getBookingDate()),
                formatTime(booking.getStartTime()),
                formatTime(booking.getEndTime())
        ));

        holder.tvLocation.setText(
                locationText(booking)
        );

        holder.tvHeadcount.setText(context.getString(
                R.string.my_booking_headcount,
                booking.getHeadcount()
        ));

        holder.tvPurpose.setText(context.getString(
                R.string.my_booking_purpose,
                booking.getPurpose()
        ));

        String note =
                booking.getAdminNote().trim();

        holder.tvNote.setText(context.getString(
                R.string.my_booking_admin_note,
                note
        ));

        holder.tvNote.setVisibility(
                note.isEmpty()
                        ? View.GONE
                        : View.VISIBLE
        );

        boolean pending = "pending".equals(
                normalize(booking.getStatus())
        );

        holder.tvAction.setVisibility(
                pending ? View.VISIBLE : View.GONE
        );
    }

    private String locationText(
            AdminBooking booking
    ) {
        String building = joinName(
                booking.getBuildingCode(),
                booking.getBuildingName()
        );

        String campus = joinName(
                booking.getCampusCode(),
                booking.getCampusName()
        );

        if (building.isEmpty() && campus.isEmpty()) {
            return context.getString(
                    R.string.information_not_available
            );
        }

        if (building.isEmpty()) {
            return campus;
        }

        if (campus.isEmpty()) {
            return building;
        }

        return building + " • " + campus;
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

    private String formatDate(String apiDate) {
        try {
            Date date = new SimpleDateFormat(
                    "yyyy-MM-dd",
                    Locale.ROOT
            ).parse(apiDate);

            return date == null
                    ? apiDate
                    : new SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
            ).format(date);

        } catch (ParseException exception) {
            return apiDate;
        }
    }

    private String formatTime(String apiTime) {
        return apiTime != null
                && apiTime.length() >= 5
                ? apiTime.substring(0, 5)
                : apiTime;
    }

    public static String statusLabel(
            Context context,
            String status
    ) {
        switch (normalize(status)) {
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
                return context.getString(
                        R.string.information_not_available
                );
        }
    }

    private int statusColor(String status) {
        switch (normalize(status)) {
            case "approved":
                return Color.rgb(24, 133, 81);

            case "rejected":
                return ContextCompat.getColor(
                        context,
                        R.color.haui_red
                );

            case "pending":
                return Color.rgb(173, 112, 0);

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

    private static String normalize(String value) {
        return value == null
                ? ""
                : value.trim().toLowerCase(
                Locale.ROOT
        );
    }

    private static class ViewHolder {
        final TextView tvCode;
        final TextView tvStatus;
        final TextView tvStudent;
        final TextView tvRoom;
        final TextView tvDateTime;
        final TextView tvLocation;
        final TextView tvHeadcount;
        final TextView tvPurpose;
        final TextView tvNote;
        final TextView tvAction;

        ViewHolder(View view) {
            tvCode = view.findViewById(
                    R.id.tvAdminBookingCode
            );

            tvStatus = view.findViewById(
                    R.id.tvAdminBookingStatus
            );

            tvStudent = view.findViewById(
                    R.id.tvAdminBookingStudent
            );

            tvRoom = view.findViewById(
                    R.id.tvAdminBookingRoom
            );

            tvDateTime = view.findViewById(
                    R.id.tvAdminBookingDateTime
            );

            tvLocation = view.findViewById(
                    R.id.tvAdminBookingLocation
            );

            tvHeadcount = view.findViewById(
                    R.id.tvAdminBookingHeadcount
            );

            tvPurpose = view.findViewById(
                    R.id.tvAdminBookingPurpose
            );

            tvNote = view.findViewById(
                    R.id.tvAdminBookingNote
            );

            tvAction = view.findViewById(
                    R.id.tvAdminBookingAction
            );
        }
    }
}