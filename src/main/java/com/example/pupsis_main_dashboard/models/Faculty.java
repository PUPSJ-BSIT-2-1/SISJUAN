package com.example.pupsis_main_dashboard.models;

import java.time.LocalDate;

public class Faculty {
    private String facultyId;
    private String firstName;
    private String middleName;
    private String lastName; // Corresponds to DB column: 'lastname'
    private String department;
    private String email;
    private String contactNumber;
    private LocalDate birthdate;
    private String status;
    private LocalDate dateJoined;

    public Faculty() {}

    // Full constructor
    public Faculty(String facultyId, String firstName, String middleName, String lastName,
                   String department, String email, String contactNumber,
                   LocalDate birthdate, String status, LocalDate dateJoined) {
        this.facultyId = facultyId;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.department = department;
        this.email = email;
        this.contactNumber = contactNumber;
        this.birthdate = birthdate;
        this.status = status;
        this.dateJoined = dateJoined;
    }

    // Getters
    public String getFacultyId() {
        return facultyId;
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

    public String getDepartment() {
        return department;
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

    public String getStatus() {
        return status;
    }

    public LocalDate getDateJoined() {
        return dateJoined;
    }

    // Setters
    public void setFacultyId(String facultyId) {
        this.facultyId = facultyId;
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

    public void setDepartment(String department) {
        this.department = department;
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

    public void setStatus(String status) {
        this.status = status;
    }

    public void setDateJoined(LocalDate dateJoined) {
        this.dateJoined = dateJoined;
    }

    // Optional: Useful for debugging/logging
    @Override
    public String toString() {
        return String.format("Faculty[id=%s, name=%s %s %s, dept=%s, email=%s]",
                facultyId, firstName, middleName, lastName, department, email);
    }
}
