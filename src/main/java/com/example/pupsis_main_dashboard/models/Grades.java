package com.example.pupsis_main_dashboard.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Grades {

    private final StringProperty scholasticStatus;
    private final StringProperty subCode;
    private final StringProperty subDesc;
    private final StringProperty facultyName;
    private final StringProperty units;
    private final StringProperty sectionCode;
    private final StringProperty finalGrade;
    private final StringProperty remarks;

    public Grades(String scholasticStatus, String subCode, String subDesc, String facultyName, String units, String sectionCode, String finalGrade, String remarks) {
        this.scholasticStatus = new SimpleStringProperty(scholasticStatus);
        this.subCode = new SimpleStringProperty(subCode);
        this.subDesc = new SimpleStringProperty(subDesc);
        this.facultyName = new SimpleStringProperty(facultyName);
        this.units = new SimpleStringProperty(units);
        this.sectionCode = new SimpleStringProperty(sectionCode);
        this.finalGrade = new SimpleStringProperty(finalGrade != null ? finalGrade : "");
        this.remarks = new SimpleStringProperty(remarks != null ? remarks : "");
    }

    // setters
    public void setScholasticStatus(String scholasticStatus) {
        this.scholasticStatus.set(scholasticStatus);
    }

    public void setSubCode(String subCode) {
        this.subCode.set(subCode);
    }

    public void setSubDesc(String subDesc) {
        this.subDesc.set(subDesc);
    }

    public void setFacultyName(String facultyName) {
        this.facultyName.set(facultyName);
    }

    public void setUnits(String units) {
        this.units.set(units);
    }

    public void setSectionCode(String sectionCode) {
        this.sectionCode.set(sectionCode);
    }

    public void setFinalGrade(String finalGrade) {
        this.finalGrade.set(finalGrade);
    }

    public void setRemarks(String remarks) {
        this.remarks.set(remarks);
    }

    // getters
    public String getScholasticStatus() {
        return scholasticStatus.get();
    }

    public String getSubCode() {
        return subCode.get();
    }

    public String getSubDesc() {
        return subDesc.get();
    }

    public String getFacultyName() {
        return facultyName.get();
    }

    public String getUnits() {
        return units.get();
    }

    public String getSectionCode() {
        return sectionCode.get();
    }

    public String getFinalGrade() {
        return finalGrade.get();
    }

    public String getRemarks() {
        return remarks.get();
    }

    // string properties
    public StringProperty scholasticStatusProperty() {
        return scholasticStatus;
    }

    public StringProperty subCodeProperty() {
        return subCode;
    }

    public StringProperty subDescProperty() {
        return subDesc;
    }

    public StringProperty facultyNameProperty() {
        return facultyName;
    }

    public StringProperty unitsProperty() {
        return units;
    }

    public StringProperty sectionCodeProperty() {
        return sectionCode;
    }

    public StringProperty finalGradeProperty() {
        return finalGrade;
    }

    public StringProperty remarksProperty() {
        return remarks;
    }
}
