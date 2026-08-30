package com.example.phonghochaui.data.model;

import com.google.gson.annotations.SerializedName;
public class ScheduleItem {
    @SerializedName("id")
    private long id;

    @SerializedName("classroom_id")
    private long classroomId;

    @SerializedName("room_code")
    private String roomCode;

    @SerializedName("room_capacity")
    private int roomCapacity;

    @SerializedName("building_id")
    private long buildingId;

    @SerializedName("building_code")
    private String buildingCode;

    @SerializedName("building_name")
    private String buildingName;

    @SerializedName("campus_id")
    private long campusId;

    @SerializedName("campus_code")
    private String campusCode;

    @SerializedName("campus_name")
    private String campusName;

    @SerializedName("subject_code")
    private String subjectCode;

    @SerializedName("subject_name")
    private String subjectName;

    @SerializedName("lecturer_id")
    private String lecturerId;

    @SerializedName("lecturer_name")
    private String lecturerName;

    @SerializedName("lecturer_haui_code")
    private String lecturerHauiCode;

    @SerializedName("study_date")
    private String studyDate;

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("end_time")
    private String endTime;

    @SerializedName("schedule_type")
    private String scheduleType;

    @SerializedName("created_at")
    private String createdAt;

    public long getId() {
        return id;
    }

    public long getClassroomId() {
        return classroomId;
    }

    public String getRoomCode() {
        return safe(roomCode);
    }

    public int getRoomCapacity() {
        return roomCapacity;
    }

    public long getBuildingId() {
        return buildingId;
    }

    public String getBuildingCode() {
        return safe(buildingCode);
    }

    public String getBuildingName() {
        return safe(buildingName);
    }

    public long getCampusId() {
        return campusId;
    }

    public String getCampusCode() {
        return safe(campusCode);
    }

    public String getCampusName() {
        return safe(campusName);
    }

    public String getSubjectCode() {
        return safe(subjectCode);
    }

    public String getSubjectName() {
        return safe(subjectName);
    }

    public String getLecturerId() {
        return safe(lecturerId);
    }

    public String getLecturerName() {
        return safe(lecturerName);
    }

    public String getLecturerHauiCode() {
        return safe(lecturerHauiCode);
    }

    public String getStudyDate() {
        return safe(studyDate);
    }

    public String getStartTime() {
        return safe(startTime);
    }

    public String getEndTime() {
        return safe(endTime);
    }

    public String getScheduleType() {
        return safe(scheduleType);
    }

    public String getCreatedAt() {
        return safe(createdAt);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
