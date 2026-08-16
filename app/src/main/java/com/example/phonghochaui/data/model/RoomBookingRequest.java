package com.example.phonghochaui.data.model;

import com.google.gson.annotations.SerializedName;

public class RoomBookingRequest {

    @SerializedName("p_classroom_id")
    private final long classroomId;

    @SerializedName("p_booking_date")
    private final String bookingDate;

    @SerializedName("p_start_time")
    private final String startTime;

    @SerializedName("p_end_time")
    private final String endTime;

    @SerializedName("p_headcount")
    private final int headcount;

    @SerializedName("p_purpose")
    private final String purpose;

    public RoomBookingRequest(
            long classroomId,
            String bookingDate,
            String startTime,
            String endTime,
            int headcount,
            String purpose
    ) {
        this.classroomId = classroomId;
        this.bookingDate = bookingDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.headcount = headcount;
        this.purpose = purpose;
    }
}