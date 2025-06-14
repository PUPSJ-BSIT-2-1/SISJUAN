package com.example.pupsis_main_dashboard.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Student {
    private final StringProperty studentId;
    private StringProperty studentNa;
    private final StringProperty firstName;
    private final StringProperty middleName;
    private final StringProperty lastName;
    private final StringProperty email;
    private final StringProperty birthday;
    private final StringProperty address;
    private final StringProperty status;
    private final StringProperty program;
    private final StringProperty yearLevel;
    private final StringProperty studentNo; // Added for EditGradesPageController
    private final StringProperty finalGrade; // Added for EditGradesPageController
    private final StringProperty loadId; // New field
    private final StringProperty gradeId; // New field

    // Constructor for the old properties (maintained for backward compatibility)
    public Student(String no, String id, String studentNa, String code, String grade, String status) {
        this.studentId = new SimpleStringProperty(id);
        this.studentNa = new SimpleStringProperty(studentNa);
        this.firstName = new SimpleStringProperty("");
        this.middleName = new SimpleStringProperty("");
        this.lastName = new SimpleStringProperty("");
        this.email = new SimpleStringProperty("");
        this.birthday = new SimpleStringProperty("");
        this.address = new SimpleStringProperty("");
        this.status = new SimpleStringProperty(status);
        this.program = new SimpleStringProperty(code);
        this.yearLevel = new SimpleStringProperty(grade);
        this.studentNo = new SimpleStringProperty(no);
        this.finalGrade = new SimpleStringProperty(grade);
        this.loadId = new SimpleStringProperty("");
        this.gradeId = new SimpleStringProperty("");
    }

    // Constructor for student management with comprehensive properties
    public Student(String studentId, String firstName, String middleName, String lastName,
                   String email, String birthday, String address, String status) {
        this.studentId = new SimpleStringProperty(studentId);
        this.firstName = new SimpleStringProperty(firstName);
        this.middleName = new SimpleStringProperty(middleName != null ? middleName : "");
        this.lastName = new SimpleStringProperty(lastName);
        this.email = new SimpleStringProperty(email);
        this.birthday = new SimpleStringProperty(birthday);
        this.address = new SimpleStringProperty(address);
        this.status = new SimpleStringProperty(status);
        this.program = new SimpleStringProperty("");
        this.yearLevel = new SimpleStringProperty("");
        this.studentNo = new SimpleStringProperty("");
        this.finalGrade = new SimpleStringProperty("");
        this.loadId = new SimpleStringProperty("");
        this.gradeId = new SimpleStringProperty("");
    }

    // Full constructor with all properties
    public Student(String studentId, String firstName, String middleName, String lastName,
                   String email, String birthday, String address, String status,
                   String program, String yearLevel) {
        this.studentId = new SimpleStringProperty(studentId);
        this.firstName = new SimpleStringProperty(firstName);
        this.middleName = new SimpleStringProperty(middleName != null ? middleName : "");
        this.lastName = new SimpleStringProperty(lastName);
        this.email = new SimpleStringProperty(email);
        this.birthday = new SimpleStringProperty(birthday);
        this.address = new SimpleStringProperty(address);
        this.status = new SimpleStringProperty(status);
        this.program = new SimpleStringProperty(program);
        this.yearLevel = new SimpleStringProperty(yearLevel);
        this.studentNo = new SimpleStringProperty("");
        this.finalGrade = new SimpleStringProperty(yearLevel);
        this.loadId = new SimpleStringProperty("");
        this.gradeId = new SimpleStringProperty("");
    }

    // Getters and setters for each property
    public String getStudentId() {
        return studentId.get();
    }

    public StringProperty studentIdProperty() {
        return studentId;
    }

    public void setStudentId(String id) {
        this.studentId.set(id);
    }

    public String getFirstName() {
        return firstName.get();
    }

    public StringProperty firstNameProperty() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName.set(firstName);
    }

    public String getMiddleName() {
        return middleName.get();
    }

    public StringProperty middleNameProperty() {
        return middleName;
    }

    public void setMiddleName(String middleName) {
        this.middleName.set(middleName);
    }

    public String getLastName() {
        return lastName.get();
    }

    public StringProperty lastNameProperty() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName.set(lastName);
    }

    public String getEmail() {
        return email.get();
    }

    public StringProperty emailProperty() {
        return email;
    }

    public void setEmail(String email) {
        this.email.set(email);
    }

    public String getBirthday() {
        return birthday.get();
    }

    public StringProperty birthdayProperty() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday.set(birthday);
    }

    public String getAddress() {
        return address.get();
    }

    public StringProperty addressProperty() {
        return address;
    }

    public void setAddress(String address) {
        this.address.set(address);
    }

    public String getStatus() {
        return status.get();
    }

    public StringProperty statusProperty() {
        return status;
    }

    public void setStatus(String status) {
        this.status.set(status);
    }

    public String getProgram() {
        return program.get();
    }

    public StringProperty programProperty() {
        return program;
    }

    public void setProgram(String program) {
        this.program.set(program);
    }

    public String getYearLevel() {
        return yearLevel.get();
    }

    public StringProperty yearLevelProperty() {
        return yearLevel;
    }

    public void setYearLevel(String yearLevel) {
        this.yearLevel.set(yearLevel);
    }

    // Gets the full name of the student (firstName + middleName + lastName)
    public String getFullName() {
        StringBuilder fullName = new StringBuilder(firstName.get());
        if (middleName.get() != null && !middleName.get().isEmpty()) {
            fullName.append(" ").append(middleName.get());
        }
        fullName.append(" ").append(lastName.get());
        return fullName.toString();
    }

    // Added getters and setters for compatibility with EditGradesPageController

    public String getStudentNo() {
        return studentNo.get();
    }

    public StringProperty studentNoProperty() {
        return studentNo;
    }

    public void setStudentNo(String no) {
        this.studentNo.set(no);
    }

    public String getStudentNa() {
        return studentNa.get();
    }

    public StringProperty studentNaProperty() {
        return studentNa;
    }

    public void setStudentNa(String no) {
        this.studentNa.set(no);
    }

    public String getSubjCode() {
        return program.get();
    }

    public void setSubjCode(String code) {
        this.program.set(code);
    }

    public String getFinalGrade() {
        return finalGrade.get();
    }

    public StringProperty finalGradeProperty() {
        return finalGrade;
    }

    public void setFinalGrade(String grade) {
        this.finalGrade.set(grade);
        // Also update yearLevel for consistency
        this.yearLevel.set(grade);
    }

    public String getGradeStatus() {
        return status.get();
    }

    public void setGradeStatus(String status) {
        this.status.set(status);
    }

    // New getters and setters for loadId and gradeId
    public String getLoadId() {
        return loadId.get();
    }

    public StringProperty loadIdProperty() {
        return loadId;
    }

    public void setLoadId(String loadId) {
        this.loadId.set(loadId);
    }

    public String getGradeId() {
        return gradeId.get();
    }

    public StringProperty gradeIdProperty() {
        return gradeId;
    }

    public void setGradeId(String gradeId) {
        this.gradeId.set(gradeId);
    }
}