package com.example.pupsis_main_dashboard.models;

import javafx.beans.property.*;

public class SubjectManagement {
    private final StringProperty subjectCode = new SimpleStringProperty();
    private final StringProperty preRequisites = new SimpleStringProperty(); // Changed from prerequisite
    private final StringProperty equivSubjectCode = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final DoubleProperty units = new SimpleDoubleProperty(); // Changed from unit
    private final StringProperty yearLevel = new SimpleStringProperty();
    private final StringProperty semester = new SimpleStringProperty();

    public SubjectManagement (String subjectCode, String preRequisites, String equivSubjectCode,
                              String description, double units, String yearLevel, String semester) {
        this.subjectCode.set(subjectCode);
        this.preRequisites.set(preRequisites);
        this.equivSubjectCode.set(equivSubjectCode != null ? equivSubjectCode : subjectCode); // Default to subject_code if null
        this.description.set(description);
        this.units.set(units);
        this.yearLevel.set(yearLevel);
        this.semester.set(semester);
    }

    public StringProperty subjectCodeProperty() { return subjectCode; }
    public StringProperty preRequisitesProperty() { return preRequisites; } // Changed from prerequisiteProperty
    public StringProperty equivSubjectCodeProperty() { return equivSubjectCode; }
    public StringProperty descriptionProperty() { return description; }
    public DoubleProperty unitsProperty() { return units; } // Changed from unitProperty
    public StringProperty yearLevelProperty() { return yearLevel; }
    public StringProperty semesterProperty() { return semester; }

    public String getSubjectCode() { return subjectCode.get(); }
    public void setSubjectCode(String value) { subjectCode.set(value); }

    public String getPreRequisites() { return preRequisites.get(); } // Changed from getPrerequisite
    public void setPreRequisites(String value) { preRequisites.set(value); } // Changed from setPrerequisite

    public String getEquivSubjectCode() { return equivSubjectCode.get(); }
    public void setEquivSubjectCode(String value) { equivSubjectCode.set(value); }

    public String getDescription() { return description.get(); }
    public void setDescription(String value) { description.set(value); }

    public double getUnits() { return units.get(); } // Changed from getUnit
    public void setUnits(double value) { units.set(value); } // Changed from setUnit

    public String getYearLevel() { return yearLevel.get(); }
    public void setYearLevel(String value) { yearLevel.set(value); }

    public String getSemester() { return semester.get(); }
    public void setSemester(String value) { semester.set(value); }

    // Compatibility methods for code using the old property names
    public String getPrerequisite() { return preRequisites.get(); }
    public void setPrerequisite(String value) { preRequisites.set(value); }
    public double getUnit() { return units.get(); }
    public void setUnit(double value) { units.set(value); }
    public StringProperty prerequisiteProperty() { return preRequisites; }
    public DoubleProperty unitProperty() { return units; }
}