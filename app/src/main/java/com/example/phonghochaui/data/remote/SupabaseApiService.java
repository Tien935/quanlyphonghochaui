package com.example.phonghochaui.data.remote;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.example.phonghochaui.data.model.AuthResponse;
import com.example.phonghochaui.data.model.AdminBooking;
import com.example.phonghochaui.data.model.AdminUser;
import com.example.phonghochaui.data.model.BuildingOption;
import com.example.phonghochaui.data.model.Classroom;
import com.example.phonghochaui.data.model.DashboardStatistics;
import com.example.phonghochaui.data.model.LoginRequest;
import com.example.phonghochaui.data.model.NotificationItem;
import com.example.phonghochaui.data.model.RoomBooking;
import com.example.phonghochaui.data.model.RoomBookingRequest;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PATCH;
import retrofit2.http.Query;

public interface SupabaseApiService {

    @GET("rest/v1/campuses")
    Call<JsonArray> getCampuses(
            @Query("select") String columns,
            @Query("limit") int limit
    );

    @POST("auth/v1/token")
    Call<AuthResponse> signInWithPassword(
            @Query("grant_type") String grantType,
            @Body LoginRequest request
    );

    @GET("rest/v1/profiles")
    Call<JsonArray> getProfileRole(
            @Query("id") String idFilter,
            @Query("select") String columns,
            @Query("limit") int limit
    );

    @GET("rest/v1/classrooms")
    Call<List<Classroom>> getClassrooms(
            @Query("select") String columns,
            @Query("order") String order
    );

    @GET("rest/v1/buildings")
    Call<List<BuildingOption>> getBuildingOptions(
            @Query("select") String columns,
            @Query("order") String order
    );

    @POST("rest/v1/rpc/admin_create_classroom_v1")
    Call<JsonObject> createClassroom(
            @Body JsonObject request
    );

    @POST("rest/v1/rpc/admin_update_classroom_v1")
    Call<JsonObject> updateClassroom(
            @Body JsonObject request
    );

    @POST("rest/v1/rpc/admin_delete_classroom_v1")
    Call<JsonObject> deleteClassroom(
            @Body JsonObject request
    );

    @POST("rest/v1/rpc/admin_list_users_v1")
    Call<List<AdminUser>> listAdminUsers(
            @Body JsonObject request
    );

    @POST("rest/v1/rpc/admin_set_user_locked_v1")
    Call<JsonObject> setAdminUserLocked(
            @Body JsonObject request
    );

    @POST("rest/v1/rpc/admin_get_dashboard_statistics_v1")
    Call<DashboardStatistics> getAdminDashboardStatistics(
            @Body JsonObject request
    );

    @POST("rest/v1/rpc/student_create_room_booking_v1")
    Call<JsonObject> createRoomBooking(
            @Body RoomBookingRequest request
    );

    @GET("rest/v1/room_bookings")
    Call<List<RoomBooking>> getMyBookings(
            @Query("select") String columns,
            @Query("user_id") String userFilter,
            @Query("order") String order
    );

    @POST("rest/v1/rpc/admin_list_room_bookings_v1")
    Call<List<AdminBooking>> listAdminBookings(
            @Body JsonObject request
    );

    @POST("rest/v1/rpc/admin_review_room_booking_v1")
    Call<JsonObject> reviewAdminBooking(
            @Body JsonObject request
    );

    @GET("rest/v1/notifications")
    Call<List<NotificationItem>> getNotifications(
            @Query("select") String columns,
            @Query("user_id") String userFilter,
            @Query("order") String order
    );

    @GET("rest/v1/notifications")
    Call<List<NotificationItem>> getUnreadNotifications(
            @Query("select") String columns,
            @Query("user_id") String userFilter,
            @Query("is_read") String readFilter
    );

    @PATCH("rest/v1/notifications")
    Call<Void> updateNotifications(
            @Query("id") String idFilter,
            @Query("user_id") String userFilter,
            @Query("is_read") String readFilter,
            @Body JsonObject request
    );
}