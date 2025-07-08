package com.sisjuan.utilities;

public class SessionData {
    private static SessionData instance = new SessionData();
    private String studentId;
    private String studentNumber;
    private String facultyId;
    private int currentAcademicYearId;
    private int unitsEnrolled;

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

    public String getStudentNumber() {
        return studentNumber;
    }

    public int getUnitsEnrolled() {
        return unitsEnrolled;
    }

    public void setUnitsEnrolled(int unitsEnrolled) {
        if (unitsEnrolled < 0) {
            throw new IllegalArgumentException("Units enrolled cannot be negative");
        }
        this.unitsEnrolled = unitsEnrolled;
    }

    public void setStudentId(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            throw new IllegalArgumentException("Student ID cannot be null or empty");
        }
        this.studentId = studentId.trim();
    }

    public void setStudentNumber(String studentNumber) {
        if (studentNumber == null || studentNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Student Number cannot be null or empty");
        }
        this.studentNumber = studentNumber.trim();
    }

    public void setFacultyId(String facultyId) {
        if (facultyId == null || facultyId.trim().isEmpty()) {
            throw new IllegalArgumentException("Faculty ID cannot be null or empty");
        }
        this.facultyId = facultyId.trim();
    }

    public int getCurrentAcademicYearId() {
        return currentAcademicYearId;
    }

    public void setCurrentAcademicYearId(int academicYearId) {
        this.currentAcademicYearId = academicYearId;
    }

    public void clear() {
        studentId = null;
        facultyId = null;
        studentNumber = null;
        currentAcademicYearId = 0;
    }
}