package com.example.pupsis_main_dashboard.models;

import java.time.LocalDate;

public class Faculty {
    private String facultyId; // This is likely faculty_number (text), PK is an int in DB
    private Integer actualFacultyId; // Assuming the DB primary key 'faculty_id' is an int
    private String firstName;
    private String middleName;
    private String lastName; // Corresponds to DB column: 'lastname'
    private Integer departmentId; // Changed from String department
    private String departmentName; // Added for display
    private String email;
    private String contactNumber;
    private LocalDate birthdate;
    private Integer facultyStatusId; // Changed from String status
    private String facultyStatusName; // Added for display
    private LocalDate dateJoined;

    public Faculty() {}

    // Full constructor - updated for new ID fields
    public Faculty(String facultyId, Integer actualFacultyId, String firstName, String middleName, String lastName,
                   Integer departmentId, String departmentName, String email, String contactNumber,
                   LocalDate birthdate, Integer facultyStatusId, String facultyStatusName, LocalDate dateJoined) {
        this.facultyId = facultyId; // This might be the textual faculty_number
        this.actualFacultyId = actualFacultyId; // The integer primary key from DB
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.departmentId = departmentId;
        this.departmentName = departmentName;
        this.email = email;
        this.contactNumber = contactNumber;
        this.birthdate = birthdate;
        this.facultyStatusId = facultyStatusId;
        this.facultyStatusName = facultyStatusName;
        this.dateJoined = dateJoined;
    }

    // Getters
    public String getFacultyId() { // This is likely faculty_number
        return facultyId;
    }

    public Integer getActualFacultyId() {
        return actualFacultyId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getMiddleName() {
        return middleName;
    }

    public String getLastName() {
        return lastName;
    }

    public Integer getDepartmentId() {
        return departmentId;
    }

    public String getDepartmentName() {
        return departmentName;
    }

    public String getEmail() {
        return email;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public Integer getFacultyStatusId() {
        return facultyStatusId;
    }

    public String getFacultyStatusName() {
        return facultyStatusName;
    }

    public LocalDate getDateJoined() {
        return dateJoined;
    }

    // Setters
    public void setFacultyId(String facultyId) { // This is likely faculty_number
        this.facultyId = facultyId;
    }

    public void setActualFacultyId(Integer actualFacultyId) {
        this.actualFacultyId = actualFacultyId;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setMiddleName(String middleName) {
        this.middleName = middleName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setDepartmentId(Integer departmentId) {
        this.departmentId = departmentId;
    }

    public void setDepartmentName(String departmentName) {
        this.departmentName = departmentName;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public void setBirthdate(LocalDate birthdate) {
        this.birthdate = birthdate;
    }

    public void setFacultyStatusId(Integer facultyStatusId) {
        this.facultyStatusId = facultyStatusId;
    }

    public void setFacultyStatusName(String facultyStatusName) {
        this.facultyStatusName = facultyStatusName;
    }

    public void setDateJoined(LocalDate dateJoined) {
        this.dateJoined = dateJoined;
    }

    // Optional: Useful for debugging/logging
    @Override
    public String toString() {
        return String.format("Faculty[actualId=%d, facultyNumber=%s, name=%s %s %s, deptId=%d (%s), statusId=%d (%s), email=%s]",
                actualFacultyId, facultyId, firstName, middleName, lastName, departmentId, departmentName, facultyStatusId, facultyStatusName, email);
    }
}
