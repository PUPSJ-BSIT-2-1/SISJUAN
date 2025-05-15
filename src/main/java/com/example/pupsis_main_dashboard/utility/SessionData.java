package com.example.pupsis_main_dashboard.utility;

public class SessionData {
    private static final SessionData instance = new SessionData();
    private String studentId;

    private SessionData() {}

    public static SessionData getInstance() {
        return instance;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }
}

