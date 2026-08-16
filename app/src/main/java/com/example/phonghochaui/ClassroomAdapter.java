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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ClassroomAdapter extends BaseAdapter {

    private final Context context;
    private final LayoutInflater inflater;

    private final List<Classroom> classrooms =
            new ArrayList<>();

    public ClassroomAdapter(Context context) {
        this.context = context;
        inflater = LayoutInflater.from(context);
    }

    public void updateData(List<Classroom> newData) {
        classrooms.clear();

        if (newData != null) {
            classrooms.addAll(newData);
        }

        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return classrooms.size();
    }

    @Override
    public Classroom getItem(int position) {
        return classrooms.get(position);
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
                    R.layout.item_classroom,
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
            Classroom classroom
    ) {
        Classroom.Building building =
                classroom.getBuilding();

        Classroom.Campus campus =
                building == null
                        ? null
                        : building.getCampus();

        holder.tvRoomCode.setText(
                classroom.getRoomCode()
        );

        holder.tvRoomInfo.setText(
                context.getString(
                        R.string.room_floor_capacity,
                        classroom.getFloor(),
                        classroom.getCapacity()
                )
        );

        String buildingText =
                building == null
                        ? context.getString(
                        R.string.information_not_available
                )
                        : joinName(
                        building.getBuildingCode(),
                        building.getBuildingName()
                );

        holder.tvBuilding.setText(
                context.getString(
                        R.string.room_building,
                        buildingText
                )
        );

        String campusText =
                campus == null
                        ? context.getString(
                        R.string.information_not_available
                )
                        : joinName(
                        campus.getCampusCode(),
                        campus.getCampusName()
                );

        holder.tvCampus.setText(
                context.getString(
                        R.string.room_campus,
                        campusText
                )
        );

        String statusLabel = statusLabel(
                context,
                classroom.getOperationalStatus()
        );

        holder.tvStatus.setText(statusLabel);

        holder.tvStatus.setTextColor(
                statusColor(
                        classroom.getOperationalStatus()
                )
        );

        String description =
                classroom.getDescription().trim();

        holder.tvDescription.setText(description);

        holder.tvDescription.setVisibility(
                description.isEmpty()
                        ? View.GONE
                        : View.VISIBLE
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

    public static String statusLabel(
            Context context,
            String status
    ) {
        String normalized =
                status == null
                        ? ""
                        : status.trim()
                        .toLowerCase(Locale.ROOT);

        switch (normalized) {
            case "active":
            case "available":
            case "operational":
            case "in_service":
                return context.getString(
                        R.string.room_status_active
                );

            case "maintenance":
            case "under_maintenance":
                return context.getString(
                        R.string.room_status_maintenance
                );

            case "inactive":
            case "unavailable":
            case "out_of_service":
                return context.getString(
                        R.string.room_status_inactive
                );

            default:
                return normalized.isEmpty()
                        ? context.getString(
                        R.string.information_not_available
                )
                        : status.trim();
        }
    }

    private int statusColor(String status) {
        String normalized =
                status == null
                        ? ""
                        : status.trim()
                        .toLowerCase(Locale.ROOT);

        if (normalized.equals("active")
                || normalized.equals("available")
                || normalized.equals("operational")
                || normalized.equals("in_service")) {

            return Color.rgb(24, 133, 81);
        }

        if (normalized.equals("maintenance")
                || normalized.equals("under_maintenance")
                || normalized.equals("inactive")
                || normalized.equals("unavailable")
                || normalized.equals("out_of_service")) {

            return ContextCompat.getColor(
                    context,
                    R.color.haui_red
            );
        }

        return ContextCompat.getColor(
                context,
                R.color.haui_blue
        );
    }

    private static class ViewHolder {

        final TextView tvRoomCode;
        final TextView tvStatus;
        final TextView tvRoomInfo;
        final TextView tvBuilding;
        final TextView tvCampus;
        final TextView tvDescription;

        ViewHolder(View view) {
            tvRoomCode = view.findViewById(
                    R.id.tvItemRoomCode
            );

            tvStatus = view.findViewById(
                    R.id.tvItemStatus
            );

            tvRoomInfo = view.findViewById(
                    R.id.tvItemRoomInfo
            );

            tvBuilding = view.findViewById(
                    R.id.tvItemBuilding
            );

            tvCampus = view.findViewById(
                    R.id.tvItemCampus
            );

            tvDescription = view.findViewById(
                    R.id.tvItemDescription
            );
        }
    }
}