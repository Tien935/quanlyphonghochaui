package com.example.phonghochaui.data.model;

import com.google.gson.annotations.SerializedName;

public class RoomBooking {

    @SerializedName("id")
    private long id;

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

    @SerializedName("classroom")
    private Classroom classroom;

    public long getId() {
        return id;
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

    public Classroom getClassroom() {
        return classroom;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}