package com.example.pupsis_main_dashboard.models;

import javafx.beans.property.*;

public class SubjectManagement {
    private final StringProperty subjectCode = new SimpleStringProperty();
    private final StringProperty prerequisite = new SimpleStringProperty();
    private final StringProperty equivSubjectCode = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final DoubleProperty unit = new SimpleDoubleProperty();
    private final StringProperty yearLevel = new SimpleStringProperty();
    private final StringProperty semester = new SimpleStringProperty();

    public SubjectManagement (String subjectCode, String prerequisite, String equivSubjectCode,
                   String description, double unit, String yearLevel ,String semester) {
        this.subjectCode.set(subjectCode);
        this.prerequisite.set(prerequisite);
        this.equivSubjectCode.set(equivSubjectCode);
        this.description.set(description);
        this.unit.set(unit);
        this.yearLevel.set(yearLevel);
        this.semester.set(semester);
    }

    public StringProperty subjectCodeProperty() { return subjectCode; }
    public StringProperty prerequisiteProperty() { return prerequisite; }
    public StringProperty equivSubjectCodeProperty() { return equivSubjectCode; }
    public StringProperty descriptionProperty() { return description; }
    public DoubleProperty unitProperty() { return unit; }
    public StringProperty yearLevelProperty() { return yearLevel; }
    public StringProperty semesterProperty() { return semester; }

    public String getSubjectCode() { return subjectCode.get(); }
    public void setSubjectCode(String value) { subjectCode.set(value); }

    public String getPrerequisite() { return prerequisite.get(); }
    public void setPrerequisite(String value) { prerequisite.set(value); }

    public String getEquivSubjectCode() { return equivSubjectCode.get(); }
    public void setEquivSubjectCode(String value) { equivSubjectCode.set(value); }

    public String getDescription() { return description.get(); }
    public void setDescription(String value) { description.set(value); }

    public double getUnit() { return unit.get(); }
    public void setUnit(double value) { unit.set(value); }

    public String getYearLevel() { return yearLevel.get(); }
    public void setYearLevel(String value) { yearLevel.set(value); }

    public String getSemester() { return semester.get(); }
    public void setSemester(String value) { semester.set(value); }
}
