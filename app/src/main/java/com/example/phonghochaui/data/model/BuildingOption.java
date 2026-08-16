package com.example.phonghochaui.data.model;

import com.google.gson.annotations.SerializedName;

public class BuildingOption {

    @SerializedName("id")
    private long id;

    @SerializedName("campus_id")
    private long campusId;

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

    public long getCampusId() {
        return campusId;
    }

    public String getBuildingCode() {
        return safe(buildingCode);
    }

    public String getBuildingName() {
        return safe(buildingName);
    }

    public Integer getTotalFloors() {
        return totalFloors;
    }

    public Campus getCampus() {
        return campus;
    }

    @Override
    public String toString() {
        String building = join(getBuildingCode(), getBuildingName());
        String campusCode = campus == null ? "" : campus.getCampusCode();

        if (campusCode.isEmpty()) {
            return building;
        }

        if (building.isEmpty()) {
            return campusCode;
        }

        return campusCode + " • " + building;
    }

    private String join(String code, String name) {
        if (code.isEmpty()) {
            return name;
        }

        if (name.isEmpty()) {
            return code;
        }

        return code + " - " + name;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static class Campus {

        @SerializedName("id")
        private long id;

        @SerializedName("campus_code")
        private String campusCode;

        @SerializedName("campus_name")
        private String campusName;

        public long getId() {
            return id;
        }

        public String getCampusCode() {
            return campusCode == null ? "" : campusCode.trim();
        }

        public String getCampusName() {
            return campusName == null ? "" : campusName.trim();
        }
    }
}