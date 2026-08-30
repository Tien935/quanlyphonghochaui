package com.example.phonghochaui.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class IncidentReport implements Serializable {

    @SerializedName("id")
    private Long id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("classroom_id")
    private long classroomId;

    @SerializedName("equipment_id")
    private Long equipmentId;

    @SerializedName("issue_type")
    private String issueType;

    @SerializedName("priority")
    private String priority;

    @SerializedName("description")
    private String description;

    @SerializedName("image_path")
    private String imagePath;

    @SerializedName("status")
    private String status;

    @SerializedName("handled_by")
    private String handledBy;

    @SerializedName("handled_at")
    private String handledAt;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("updated_at")
    private String updatedAt;

    // Supabase can return joined data if requested like "classroom:classrooms(...)"
    @SerializedName("classroom")
    private Classroom classroom;

    public IncidentReport() {
    }

    public IncidentReport(String userId, long classroomId, String issueType, String priority, String description) {
        this.userId = userId;
        this.classroomId = classroomId;
        this.issueType = issueType;
        this.priority = priority;
        this.description = description;
    }

    public long getId() { return id == null ? 0 : id; }
    public String getUserId() { return userId; }
    public long getClassroomId() { return classroomId; }
    public Long getEquipmentId() { return equipmentId; }
    public String getIssueType() { return issueType; }
    public String getPriority() { return priority; }
    public String getDescription() { return description; }
    public String getImagePath() { return imagePath; }
    public String getStatus() { return status; }
    public String getHandledBy() { return handledBy; }
    public String getHandledAt() { return handledAt; }
    public String getCreatedAt() { return createdAt; }
    public String getUpdatedAt() { return updatedAt; }

    public Classroom getClassroom() { return classroom; }
    public void setClassroom(Classroom classroom) { this.classroom = classroom; }
}
