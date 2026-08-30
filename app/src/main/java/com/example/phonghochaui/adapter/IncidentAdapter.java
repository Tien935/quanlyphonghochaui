package com.example.phonghochaui.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.phonghochaui.R;
import com.example.phonghochaui.data.model.IncidentReport;
import com.google.android.material.card.MaterialCardView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class IncidentAdapter extends RecyclerView.Adapter<IncidentAdapter.IncidentViewHolder> {

    private List<IncidentReport> incidents;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(IncidentReport incident);
    }

    public IncidentAdapter(List<IncidentReport> incidents, OnItemClickListener listener) {
        this.incidents = incidents;
        this.listener = listener;
    }

    public void updateData(List<IncidentReport> newIncidents) {
        this.incidents = newIncidents;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public IncidentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_incident, parent, false);
        return new IncidentViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull IncidentViewHolder holder, int position) {
        IncidentReport incident = incidents.get(position);
        Context ctx = holder.itemView.getContext();

        // Room
        if (incident.getClassroom() != null) {
            String bName = incident.getClassroom().getBuilding() != null ? incident.getClassroom().getBuilding().getBuildingName() : "?";
            holder.tvRoom.setText(incident.getClassroom().getRoomCode() + " (Tòa " + bName + ")");
        } else {
            holder.tvRoom.setText("ID Phòng: " + incident.getClassroomId());
        }

        // Issue Type
        holder.tvIssueType.setText(incident.getIssueType());

        // Description
        holder.tvDesc.setText(incident.getDescription());

        // Priority
        String priorityText = "Trung bình";
        if ("low".equalsIgnoreCase(incident.getPriority())) priorityText = "Thấp";
        else if ("high".equalsIgnoreCase(incident.getPriority())) priorityText = "Cao";
        holder.tvPriority.setText("Ưu tiên: " + priorityText);

        // Date
        holder.tvDate.setText(formatDate(incident.getCreatedAt()));

        // Status
        String status = incident.getStatus();
        if ("processing".equalsIgnoreCase(status)) {
            holder.tvStatus.setText(ctx.getString(R.string.admin_incident_status_in_progress));
            holder.tvStatus.setTextColor(Color.parseColor("#1565C0")); // Blue
            holder.cvStatus.setCardBackgroundColor(Color.parseColor("#E3F2FD"));
        } else if ("resolved".equalsIgnoreCase(status)) {
            holder.tvStatus.setText(ctx.getString(R.string.admin_incident_status_resolved));
            holder.tvStatus.setTextColor(Color.parseColor("#2E7D32")); // Green
            holder.cvStatus.setCardBackgroundColor(Color.parseColor("#E8F5E9"));
        } else { // pending
            holder.tvStatus.setText(ctx.getString(R.string.admin_incident_status_pending));
            holder.tvStatus.setTextColor(Color.parseColor("#E65100")); // Orange
            holder.cvStatus.setCardBackgroundColor(Color.parseColor("#FFF3E0"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(incident);
        });
    }

    @Override
    public int getItemCount() {
        return incidents == null ? 0 : incidents.size();
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

    static class IncidentViewHolder extends RecyclerView.ViewHolder {
        TextView tvRoom, tvIssueType, tvDesc, tvPriority, tvDate, tvStatus;
        MaterialCardView cvStatus;

        public IncidentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRoom = itemView.findViewById(R.id.tvIncidentRoom);
            tvIssueType = itemView.findViewById(R.id.tvIncidentIssueType);
            tvDesc = itemView.findViewById(R.id.tvIncidentDesc);
            tvPriority = itemView.findViewById(R.id.tvIncidentPriority);
            tvDate = itemView.findViewById(R.id.tvIncidentDate);
            tvStatus = itemView.findViewById(R.id.tvIncidentStatus);
            cvStatus = itemView.findViewById(R.id.cvIncidentStatus);
        }
    }
}
