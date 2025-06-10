package com.example.pupsis_main_dashboard.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;

public class Schedule {
    private final IntegerProperty loadID;
    private final StringProperty faculty;
    private final StringProperty facultyID;
    private final StringProperty subjectID;
    private final StringProperty subCode;
    private final StringProperty subDesc;
    private final StringProperty facultyName;
    private final StringProperty facultyNumber;
    private final StringProperty yearSection;
    private final StringProperty days;
    private final StringProperty startTime;
    private final StringProperty endTime;
    private final StringProperty room;
    private final IntegerProperty units;
    private final IntegerProperty lectureHour;
    private final IntegerProperty laboratoryHour;
    private final StringProperty stringLectureHour;
    private final StringProperty stringLaboratoryHour;
    private final StringProperty schedule;
    private final StringProperty scheduleForFaculty;
    private final StringProperty scheduleWithFaculty;
    private final Button editButton;
    private final StringProperty stringUnits;

    // Existing constructor for detailed/admin view
    public Schedule(int loadID, String faculty, String subjectID, String facultyNumber,
                    String subCode, String subDesc, String facultyName, String facultyID,
                    String yearSection, String days, String startTime, String endTime,
                    String room, String units, Integer lectureHour, Integer laboratoryHour,
                    Button editButton) {

        this.loadID = new SimpleIntegerProperty(loadID);
        this.faculty = new SimpleStringProperty(nonNull(faculty));
        this.subjectID = new SimpleStringProperty(nonNull(subjectID));
        this.facultyNumber = new SimpleStringProperty(nonNull(facultyNumber));
        this.subCode = new SimpleStringProperty(nonNull(subCode));
        this.subDesc = new SimpleStringProperty(nonNull(subDesc));
        this.facultyName = new SimpleStringProperty(nonNull(facultyName));
        this.facultyID = new SimpleStringProperty(nonNull(facultyID));
        this.yearSection = new SimpleStringProperty(nonNull(yearSection));
        this.days = new SimpleStringProperty(nonNull(days));
        this.startTime = new SimpleStringProperty(nonNull(startTime));
        this.endTime = new SimpleStringProperty(nonNull(endTime));
        this.room = new SimpleStringProperty(nonNull(room));
        this.units = new SimpleIntegerProperty(nonNullInt(Integer.valueOf(units)));
        this.lectureHour = new SimpleIntegerProperty(nonNullInt(lectureHour));
        this.laboratoryHour = new SimpleIntegerProperty(nonNullInt(laboratoryHour));
        this.stringLectureHour = new SimpleStringProperty(String.valueOf(nonNullInt(lectureHour)));
        this.stringLaboratoryHour = new SimpleStringProperty(String.valueOf(nonNullInt(laboratoryHour)));
        this.stringUnits = new SimpleStringProperty(String.valueOf(nonNullInt(Integer.valueOf(units))));

        // Calculated properties
        // Ensure yearSection is non-null before using in concatenation for scheduleWithFaculty
        String displayYearSection = this.yearSection.get() != null ? this.yearSection.get() : "";
        this.schedule = new SimpleStringProperty(displayYearSection + " " + this.days.get() + " " + this.startTime.get() + " - " + this.endTime.get());
        this.scheduleForFaculty = new SimpleStringProperty(this.startTime.get() + " - " + this.endTime.get());
        this.scheduleWithFaculty = new SimpleStringProperty(displayYearSection + " " + this.days.get() + " " + this.startTime.get() + " - " + this.endTime.get() + " " + this.facultyName.get());

        this.editButton = editButton;
    }

    // New constructor for student view (10 arguments)
    public Schedule(String subCode, String subDesc, Integer units, String days, 
                    String startTime, String endTime, String room, String facultyName, 
                    Integer lectureHour, Integer laboratoryHour) {
        
        this.loadID = new SimpleIntegerProperty(0); // Default
        this.faculty = new SimpleStringProperty(""); // Default
        this.subjectID = new SimpleStringProperty(""); // Default
        this.facultyNumber = new SimpleStringProperty(""); // Default
        this.facultyID = new SimpleStringProperty(""); // Default
        this.yearSection = new SimpleStringProperty(""); // Default - year/section context comes from student, not per schedule item here

        this.subCode = new SimpleStringProperty(nonNull(subCode));
        this.subDesc = new SimpleStringProperty(nonNull(subDesc));
        this.units = new SimpleIntegerProperty(nonNullInt(units));
        this.days = new SimpleStringProperty(nonNull(days));
        this.startTime = new SimpleStringProperty(nonNull(startTime));
        this.endTime = new SimpleStringProperty(nonNull(endTime));
        this.room = new SimpleStringProperty(nonNull(room));
        this.facultyName = new SimpleStringProperty(nonNull(facultyName));
        this.lectureHour = new SimpleIntegerProperty(nonNullInt(lectureHour));
        this.laboratoryHour = new SimpleIntegerProperty(nonNullInt(laboratoryHour));

        // Derived string properties for display
        this.stringUnits = new SimpleStringProperty(String.valueOf(nonNullInt(units)));
        this.stringLectureHour = new SimpleStringProperty(String.valueOf(nonNullInt(lectureHour)));
        this.stringLaboratoryHour = new SimpleStringProperty(String.valueOf(nonNullInt(laboratoryHour)));
        
        // Calculated schedule strings - adapt as needed for student view
        // For student view, yearSection is not directly part of this constructor's individual schedule items
        // It's a general context for the student. So, scheduleWithFaculty might not include yearSection here.
        String scheduleDisplay = String.format("%s %s - %s", 
            nonNull(days), nonNull(startTime), nonNull(endTime)).trim();
        this.schedule = new SimpleStringProperty(scheduleDisplay);
        this.scheduleForFaculty = new SimpleStringProperty(scheduleDisplay); // Or adapt if faculty view is different
        this.scheduleWithFaculty = new SimpleStringProperty(String.format("%s (%s)", scheduleDisplay, nonNull(facultyName)).trim());

        this.editButton = null; // No edit button for student view
    }

    private String nonNull(String value) {
        return value != null ? value : "";
    }

    private int nonNullInt(Integer value) {
        return value != null ? value : 0;
    }

    //Setters
    public void setLoadID(int loadID) { this.loadID.set(loadID); }
    public void setFaculty(String faculty) { this.faculty.set(faculty != null ? faculty : ""); }
    public void setSubCode(String subCode) { this.subCode.set(subCode != null ? subCode : ""); }
    public void setSubDesc(String subDesc) { this.subDesc.set(subDesc != null ? subDesc : ""); }
    public void setFacultyName(String facultyName) { this.facultyName.set(facultyName != null ? facultyName : ""); }
    public void setFacultyID(String facultyID) { this.facultyID.set(facultyID != null ? facultyID : ""); }
    public void setYearSection(String yearSection) { this.yearSection.set(yearSection != null ? yearSection : ""); }
    public void setDays(String days) { this.days.set(days != null ? days : ""); }
    public void setStartTime(String startTime) { this.startTime.set(startTime != null ? startTime : ""); }
    public void setEndTime(String endTime) { this.endTime.set(endTime != null ? endTime : ""); }
    public void setRoom(String room) { this.room.set(room != null ? room : ""); }
    public void setUnits(int units) { this.units.set(units); }
    public void setLectureHour(int lectureHour) { this.lectureHour.set(lectureHour); }
    public void setLaboratoryHour(int laboratoryHour) { this.laboratoryHour.set(laboratoryHour); }
    public void setSubID(String subjectID) { this.subjectID.set(subjectID != null ? subjectID : ""); }
    public void setFacultyNumber(String facultyNumber) { this.facultyNumber.set(facultyNumber != null ? facultyNumber : ""); }
    public void setSchedule(String schedule) { this.schedule.set(this.yearSection.get() + " " + this.days.get() + " " + this.startTime.get() + " - " + this.endTime.get()); }

    // Getters
    public int getLoadID() { return loadID.get(); }
    public String getFaculty() { return faculty.get(); }
    public String getSubjectID() { return subjectID.get(); }
    public String getFacultyNumber() { return facultyNumber.get(); }
    public String getSubCode() { return subCode.get(); }
    public String getSubDesc() { return subDesc.get(); }
    public String getFacultyName() { return facultyName.get(); }
    public String getFacultyID() { return facultyID.get(); }
    public String getYearSection() { return yearSection.get(); }
    public String getDays() { return days.get(); }
    public String getStartTime() { return startTime.get(); }
    public String getEndTime() { return endTime.get(); }
    public String getRoom() { return room.get(); }
    public int getUnits() { return units.get(); }
    public int getLectureHour() { return lectureHour.get(); }
    public int getLaboratoryHour() { return laboratoryHour.get(); }
    public String getStringLectureHour() { return stringLectureHour.get(); }
    public String getStringLaboratoryHour() { return stringLaboratoryHour.get(); }
    public String getStringUnits() { return stringUnits.get(); }
    public String getSchedule() { return schedule.get(); }
    public String getScheduleForFaculty() { return scheduleForFaculty.get(); }
    public String getScheduleWithFaculty() { return scheduleWithFaculty.get(); }
    public Button getEditButton() { return editButton; }

    // Property accessors (if needed for JavaFX binding)
    public IntegerProperty loadIDProperty() { return loadID; }
    public StringProperty facultyProperty() { return faculty; }
    public StringProperty subjectIDProperty() { return subjectID; }
    public StringProperty facultyNumberProperty() { return facultyNumber; }
    public StringProperty subCodeProperty() { return subCode; }
    public StringProperty subDescProperty() { return subDesc; }
    public StringProperty facultyNameProperty() { return facultyName; }
    public StringProperty facultyIDProperty() { return facultyID; }
    public StringProperty yearSectionProperty() { return yearSection; }
    public StringProperty daysProperty() { return days; }
    public StringProperty startTimeProperty() { return startTime; }
    public StringProperty endTimeProperty() { return endTime; }
    public StringProperty roomProperty() { return room; }
    public IntegerProperty unitsProperty() { return units; }
    public IntegerProperty lectureHourProperty() { return lectureHour; }
    public IntegerProperty laboratoryHourProperty() { return laboratoryHour; }
    public StringProperty stringLectureHourProperty() { return stringLectureHour; }
    public StringProperty stringLaboratoryHourProperty() { return stringLaboratoryHour; }
    public StringProperty stringUnitsProperty() { return stringUnits; }
    public StringProperty scheduleProperty() { return schedule; }
    public StringProperty scheduleForFacultyProperty() { return scheduleForFaculty; }
    public StringProperty scheduleWithFacultyProperty() { return scheduleWithFaculty; }
}
