package com.example.phonghochaui.data.model;

import com.google.gson.annotations.SerializedName;

public class AdminBooking {

    @SerializedName("id")
    private long id;

    @SerializedName("user_id")
    private String userId;

    @SerializedName("student_haui_code")
    private String studentHauiCode;

    @SerializedName("student_full_name")
    private String studentFullName;

    @SerializedName("classroom_id")
    private long classroomId;

    @SerializedName("room_code")
    private String roomCode;

    @SerializedName("floor")
    private int floor;

    @SerializedName("capacity")
    private int capacity;

    @SerializedName("building_code")
    private String buildingCode;

    @SerializedName("building_name")
    private String buildingName;

    @SerializedName("campus_code")
    private String campusCode;

    @SerializedName("campus_name")
    private String campusName;

    @SerializedName("booking_date")
    private String bookingDate;

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("end_time")
    private String endTime;

    @SerializedName("headcount")
    private int headcount;

    @SerializedName("purpose")
    private String purpose;

    @SerializedName("status")
    private String status;

    @SerializedName("admin_note")
    private String adminNote;

    @SerializedName("created_at")
    private String createdAt;

    public long getId() {
        return id;
    }

    public String getUserId() {
        return safe(userId);
    }

    public String getStudentHauiCode() {
        return safe(studentHauiCode);
    }

    public String getStudentFullName() {
        return safe(studentFullName);
    }

    public long getClassroomId() {
        return classroomId;
    }

    public String getRoomCode() {
        return safe(roomCode);
    }

    public int getFloor() {
        return floor;
    }

    public int getCapacity() {
        return capacity;
    }

    public String getBuildingCode() {
        return safe(buildingCode);
    }

    public String getBuildingName() {
        return safe(buildingName);
    }

    public String getCampusCode() {
        return safe(campusCode);
    }

    public String getCampusName() {
        return safe(campusName);
    }

    public String getBookingDate() {
        return safe(bookingDate);
    }

    public String getStartTime() {
        return safe(startTime);
    }

    public String getEndTime() {
        return safe(endTime);
    }

    public int getHeadcount() {
        return headcount;
    }

    public String getPurpose() {
        return safe(purpose);
    }

    public String getStatus() {
        return safe(status);
    }

    public String getAdminNote() {
        return safe(adminNote);
    }

    public String getCreatedAt() {
        return safe(createdAt);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}