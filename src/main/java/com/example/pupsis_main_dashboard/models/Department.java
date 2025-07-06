package com.example.pupsis_main_dashboard.models;

public class Department {
    private final int departmentId;
    private final String departmentName;

    public Department(int id, String name) {
        this.departmentId = id;
        this.departmentName = name;
    }

    public int getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    @Override
    public String toString() {
        return departmentName;
    } // For ComboBox display
}