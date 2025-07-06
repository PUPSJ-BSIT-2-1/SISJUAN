package com.example.pupsis_main_dashboard.models;

public class FacultyStatus {
    private final int facultyStatusId;
    private final String statusName;

    public FacultyStatus(int id, String name) {
        this.facultyStatusId = id;
        this.statusName = name;
    }
    public int getFacultyStatusId() { return facultyStatusId; }
    public String getStatusName() { return statusName; }
    @Override public String toString() { return statusName; } // For ComboBox display
}