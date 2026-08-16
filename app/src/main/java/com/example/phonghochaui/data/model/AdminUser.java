package com.example.phonghochaui.data.model;

import com.google.gson.annotations.SerializedName;

public class AdminUser {

    @SerializedName("id")
    private String id;

    @SerializedName("email")
    private String email;

    @SerializedName("haui_code")
    private String hauiCode;

    @SerializedName("full_name")
    private String fullName;

    @SerializedName("role")
    private String role;

    @SerializedName("is_locked")
    private boolean locked;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("last_sign_in_at")
    private String lastSignInAt;

    public String getId() {
        return safe(id);
    }

    public String getEmail() {
        return safe(email);
    }

    public String getHauiCode() {
        return safe(hauiCode);
    }

    public String getFullName() {
        return safe(fullName);
    }

    public String getRole() {
        return safe(role);
    }

    public boolean isLocked() {
        return locked;
    }

    public String getCreatedAt() {
        return safe(createdAt);
    }

    public String getLastSignInAt() {
        return safe(lastSignInAt);
    }

    public String getDisplayName() {
        if (!getFullName().isEmpty()) {
            return getFullName();
        }

        if (!getEmail().isEmpty()) {
            return getEmail();
        }

        return getId();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}