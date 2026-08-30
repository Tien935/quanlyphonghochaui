package com.example.phonghochaui.data.model;

import com.google.gson.annotations.SerializedName;

public class SubjectOption {

    @SerializedName("subject_code")
    private String subjectCode;

    @SerializedName("subject_name")
    private String subjectName;

    public String getSubjectCode() {
        return safe(subjectCode);
    }

    public String getSubjectName() {
        return safe(subjectName);
    }

    @Override
    public String toString() {
        if (getSubjectCode().isEmpty()) {
            return getSubjectName();
        }
        return getSubjectName() + " • " + getSubjectCode();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
