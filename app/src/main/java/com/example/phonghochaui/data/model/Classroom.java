package com.example.phonghochaui.data.model;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

public class Classroom implements Serializable {

    @SerializedName("id")
    private long id;

    @SerializedName("room_code")
    private String roomCode;

    @SerializedName("floor")
    private int floor;

    @SerializedName("capacity")
    private int capacity;

    @SerializedName("operational_status")
    private String operationalStatus;

    @SerializedName("description")
    private String description;

    @SerializedName("building")
    private Building building;

    public long getId() {
        return id;
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

    public String getOperationalStatus() {
        return safe(operationalStatus);
    }

    public String getDescription() {
        return safe(description);
    }

    public Building getBuilding() {
        return building;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    public static class Building implements Serializable {

        @SerializedName("id")
        private long id;

        @SerializedName("building_code")
        private String buildingCode;

        @SerializedName("building_name")
        private String buildingName;

        @SerializedName("total_floors")
        private Integer totalFloors;

        @SerializedName("campus")
        private Campus campus;

        public long getId() {
            return id;
        }

        public String getBuildingCode() {
            return buildingCode == null
                    ? ""
                    : buildingCode;
        }

        public String getBuildingName() {
            return buildingName == null
                    ? ""
                    : buildingName;
        }

        public Integer getTotalFloors() {
            return totalFloors;
        }

        public Campus getCampus() {
            return campus;
        }
    }

    public static class Campus implements Serializable {

        @SerializedName("id")
        private long id;

        @SerializedName("campus_code")
        private String campusCode;

        @SerializedName("campus_name")
        private String campusName;

        @SerializedName("address")
        private String address;

        public long getId() {
            return id;
        }

        public String getCampusCode() {
            return campusCode == null
                    ? ""
                    : campusCode;
        }

        public String getCampusName() {
            return campusName == null
                    ? ""
                    : campusName;
        }

        public String getAddress() {
            return address == null
                    ? ""
                    : address;
        }
    }
}