package com.example.phonghochaui.data.model;

import com.google.gson.annotations.SerializedName;

public class NotificationItem {

    @SerializedName("id")
    private long id;

    @SerializedName("title")
    private String title;

    @SerializedName("content")
    private String content;

    @SerializedName("notification_type")
    private String notificationType;

    @SerializedName("is_read")
    private boolean read;

    @SerializedName("created_at")
    private String createdAt;

    public long getId() {
        return id;
    }

    public String getTitle() {
        return safe(title);
    }

    public String getContent() {
        return safe(content);
    }

    public String getNotificationType() {
        return safe(notificationType);
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getCreatedAt() {
        return safe(createdAt);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}