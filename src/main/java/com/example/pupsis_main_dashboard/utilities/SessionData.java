package com.example.pupsis_main_dashboard.utilities;

public class SessionData {
    private static SessionData instance = new SessionData();
    private String studentId;
    private String facultyId;

    private SessionData() {}

    public static synchronized SessionData getInstance() {
        if (instance == null) {
            instance = new SessionData();
        }
        return instance;
    }

    public String getFacultyId() {
        return facultyId;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty");
        }
        this.studentId = studentId.trim();
    }

    public void setFacultyId(String facultyId) {
        if (facultyId == null || facultyId.trim().isEmpty()) {
            throw new IllegalArgumentException("Faculty ID cannot be null or empty");
        }
        this.facultyId = facultyId.trim();
    }

    public void clear() {
        studentId = null;
        facultyId = null;
    }
}