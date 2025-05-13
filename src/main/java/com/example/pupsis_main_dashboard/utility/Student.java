package com.example.GradingModule;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Student {
    private final StringProperty studentNo;
    private final StringProperty studentId;
    private final StringProperty studentName;
    private final StringProperty subjCode;
    private final StringProperty finalGrade;
    private final StringProperty gradeStatus;

    public Student(String no, String id, String name, String code, String grade, String status) {
        this.studentNo = new SimpleStringProperty(no);
        this.studentId = new SimpleStringProperty(id);
        this.studentName = new SimpleStringProperty(name);
        this.subjCode = new SimpleStringProperty(code);
        this.finalGrade = new SimpleStringProperty(grade);
        this.gradeStatus = new SimpleStringProperty(status);
    }

    // Getters and setters for each property
    public String getStudentNo() {
        return studentNo.get();
    }

    public StringProperty studentNoProperty() {
        return studentNo;
    }

    public void setStudentNo(String no) {
        this.studentNo.set(no);
    }

    public String getStudentId() {
        return studentId.get();
    }

    public StringProperty studentIdProperty() {
        return studentId;
    }

    public void setStudentId(String id) {
        this.studentId.set(id);
    }

    public String getStudentName() {
        return studentName.get();
    }

    public StringProperty studentNameProperty() {
        return studentName;
    }

    public void setStudentName(String name) {
        this.studentName.set(name);
    }

    public String getSubjCode() {
        return subjCode.get();
    }

    public StringProperty subjCodeProperty() {
        return subjCode;
    }

    public void setSubjCode(String code) {
        this.subjCode.set(code);
    }

    public String getFinalGrade() {
        return finalGrade.get();
    }

    public StringProperty finalGradeProperty() {
        return finalGrade;
    }

    public void setFinalGrade(String grade) {
        this.finalGrade.set(grade);
    }

    public String getGradeStatus() {
        return gradeStatus.get();
    }

    public StringProperty gradeStatusProperty() {
        return gradeStatus;
    }

    public void setGradeStatus(String status) {
        this.gradeStatus.set(status);
    }
}