package com.example.phonghochaui.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class DashboardStatistics {

    @SerializedName("generated_at")
    private String generatedAt;

    @SerializedName("timezone")
    private String timezone;

    @SerializedName("summary")
    private Summary summary;

    @SerializedName("classroom_status")
    private List<StatusStat> classroomStatus;

    @SerializedName("booking_status")
    private List<StatusStat> bookingStatus;

    @SerializedName("booking_activity")
    private BookingActivity bookingActivity;

    public String getGeneratedAt() {
        return generatedAt == null ? "" : generatedAt;
    }

    public String getTimezone() {
        return timezone == null ? "" : timezone;
    }

    public Summary getSummary() {
        return summary == null ? new Summary() : summary;
    }

    public List<StatusStat> getClassroomStatus() {
        return classroomStatus == null ? new ArrayList<>() : classroomStatus;
    }

    public List<StatusStat> getBookingStatus() {
        return bookingStatus == null ? new ArrayList<>() : bookingStatus;
    }

    public BookingActivity getBookingActivity() {
        return bookingActivity == null ? new BookingActivity() : bookingActivity;
    }

    public StatusStat findClassroomStatus(String status) {
        return findStatus(getClassroomStatus(), status);
    }

    public StatusStat findBookingStatus(String status) {
        return findStatus(getBookingStatus(), status);
    }

    private StatusStat findStatus(List<StatusStat> values, String status) {
        for (StatusStat value : values) {
            if (status.equalsIgnoreCase(value.getStatus())) {
                return value;
            }
        }
        return new StatusStat();
    }

    public static class Summary {

        @SerializedName("campuses_total")
        private long campusesTotal;

        @SerializedName("buildings_total")
        private long buildingsTotal;

        @SerializedName("classrooms_total")
        private long classroomsTotal;

        @SerializedName("users_total")
        private long usersTotal;

        @SerializedName("students_total")
        private long studentsTotal;

        @SerializedName("admins_total")
        private long adminsTotal;

        @SerializedName("locked_users_total")
        private long lockedUsersTotal;

        @SerializedName("bookings_total")
        private long bookingsTotal;

        @SerializedName("pending_bookings_total")
        private long pendingBookingsTotal;

        @SerializedName("bookings_last_7_days")
        private long bookingsLast7Days;

        @SerializedName("bookings_last_30_days")
        private long bookingsLast30Days;

        public long getCampusesTotal() {
            return campusesTotal;
        }

        public long getBuildingsTotal() {
            return buildingsTotal;
        }

        public long getClassroomsTotal() {
            return classroomsTotal;
        }

        public long getUsersTotal() {
            return usersTotal;
        }

        public long getStudentsTotal() {
            return studentsTotal;
        }

        public long getAdminsTotal() {
            return adminsTotal;
        }

        public long getLockedUsersTotal() {
            return lockedUsersTotal;
        }

        public long getBookingsTotal() {
            return bookingsTotal;
        }

        public long getPendingBookingsTotal() {
            return pendingBookingsTotal;
        }

        public long getBookingsLast7Days() {
            return bookingsLast7Days;
        }

        public long getBookingsLast30Days() {
            return bookingsLast30Days;
        }
    }

    public static class StatusStat {

        @SerializedName("status")
        private String status;

        @SerializedName("total")
        private long total;

        @SerializedName("percentage")
        private float percentage;

        public String getStatus() {
            return status == null ? "" : status;
        }

        public long getTotal() {
            return total;
        }

        public float getPercentage() {
            return percentage;
        }
    }

    public static class BookingActivity {

        @SerializedName("last_7_days")
        private long last7Days;

        @SerializedName("last_30_days")
        private long last30Days;

        @SerializedName("trend_30_days")
        private List<TrendPoint> trend30Days;

        public long getLast7Days() {
            return last7Days;
        }

        public long getLast30Days() {
            return last30Days;
        }

        public List<TrendPoint> getTrend30Days() {
            return trend30Days == null ? new ArrayList<>() : trend30Days;
        }
    }

    public static class TrendPoint {

        @SerializedName("date")
        private String date;

        @SerializedName("total")
        private long total;

        public String getDate() {
            return date == null ? "" : date;
        }

        public long getTotal() {
            return total;
        }
    }
}