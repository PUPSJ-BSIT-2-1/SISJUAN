package com.sisjuan.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class FacultyAssignment {
    private final StringProperty subjectCode;
    private final StringProperty subjectDesc;
    private final StringProperty section;
    private final StringProperty semester;
    private final StringProperty yearLevel;

    public FacultyAssignment(String subjectCode, String subjectDesc, String section, String semester, String yearLevel) {
        this.subjectCode = new SimpleStringProperty(subjectCode);
        this.subjectDesc = new SimpleStringProperty(subjectDesc);
        this.section = new SimpleStringProperty(section);
        this.semester = new SimpleStringProperty(semester);
        this.yearLevel = new SimpleStringProperty(yearLevel);
    }

    // Property getters (for TableView)
    public StringProperty subjectCodeProperty() { return subjectCode; }
    public StringProperty subjectDescProperty() { return subjectDesc; }
    public StringProperty sectionProperty() { return section; }
    public StringProperty semesterProperty() { return semester; }
    public StringProperty yearLevelProperty() { return yearLevel; }

    // Value getters
    public String getSubjectCode() { return subjectCode.get(); }
    public String getSubjectDesc() { return subjectDesc.get(); }
    public String getSection() { return section.get(); }
    public String getSemester() { return semester.get(); }
    public String getYearLevel() { return yearLevel.get(); }
}
