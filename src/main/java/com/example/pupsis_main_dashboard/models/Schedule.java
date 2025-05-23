package com.example.pupsis_main_dashboard.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.shape.SVGPath;

import java.util.Objects;

public class Schedule {
    private final StringProperty subCode;
    private final StringProperty subDesc;
    private final StringProperty facultyName;
    private final StringProperty facultyID;
    private final StringProperty yearSection;
    private final StringProperty days;
    private final StringProperty startTime;
    private final StringProperty endTime;
    private final StringProperty room;
    private final IntegerProperty units;
    private final IntegerProperty lectureHour;
    private final IntegerProperty laboratoryHour;
    private final Button editButton;
    private final StringProperty schedule;
    private final StringProperty scheduleWithFaculty;
    private final StringProperty hoursWithIcon;
    private final StringProperty unitsWithIcon;
    private final StringProperty scheduleForFaculty;

    public Schedule(String subCode, String subDesc, String facultyName, String facultyID, String yearSection, String days, String startTime, String endTime, String room, Integer units, Integer lectureHour, Integer laboratoryHour, Button editButton, String[] iconNames) {
        this.subCode = new SimpleStringProperty(subCode);
        this.subDesc = new SimpleStringProperty(subDesc);
        this.facultyName = new SimpleStringProperty(facultyName);
        this.facultyID = new SimpleStringProperty(facultyID);
        this.yearSection = new SimpleStringProperty(yearSection);
        this.days = new SimpleStringProperty(days);
        this.units = new SimpleIntegerProperty(units);
        this.startTime = new SimpleStringProperty(startTime);
        this.endTime = new SimpleStringProperty(endTime);
        this.room = new SimpleStringProperty(room);
        this.lectureHour = new SimpleIntegerProperty(lectureHour);
        this.laboratoryHour = new SimpleIntegerProperty(laboratoryHour);
        this.editButton = editButton;

        SVGPath lectureIcon = new SVGPath();
        SVGPath laboratoryIcon = new SVGPath();
        SVGPath timeIcon = new SVGPath();

        lectureIcon.getStyleClass().add("icon");
        laboratoryIcon.getStyleClass().add("icon");
        timeIcon.getStyleClass().add("icon");

        // Set SVG path content using icon names
        lectureIcon.setContent(iconNames[0]);
        laboratoryIcon.setContent(iconNames[1]);
        timeIcon.setContent(iconNames[2]);

        String hourText = lectureIcon.getContent() + lectureHour + laboratoryIcon.getContent() + laboratoryHour;
        this.hoursWithIcon = new SimpleStringProperty(hourText);

        String unitsText = timeIcon.getContent() + units;
        this.unitsWithIcon = new SimpleStringProperty(unitsText);

        String scheduleText = yearSection + " " + days + " " + startTime + " - " + endTime;
        this.schedule = new SimpleStringProperty(scheduleText);

        String scheduleTextForFaculty = startTime + " - " + endTime;
        this.scheduleForFaculty = new SimpleStringProperty(scheduleTextForFaculty);

        String scheduleTextWithFaculty = yearSection + " " + days + " " + startTime + " - " + endTime + " Faculty: " + facultyName;
        this.scheduleWithFaculty = new SimpleStringProperty(scheduleTextWithFaculty);
    }

    //Setters
    public void setSubCode(String subCode) { this.subCode.set(subCode); }
    public void setSubDesc(String subDesc) { this.subDesc.set(subDesc); }
    public void setFacultyName(String facultyName) { this.facultyName.set(facultyName); }
    public void setFacultyID(String facultyID) { this.facultyID.set(facultyID); }
    public void setYearSection(String yearSection) { this.yearSection.set(yearSection); }
    public void setDays(String days) { this.days.set(days); }
    public void setStartTime(String startTime) { this.startTime.set(startTime); }
    public void setEndTime(String endTime) { this.endTime.set(endTime); }
    public void setRoom(String room) { this.room.set(room); }
    public void setUnits(int units) { this.units.set(units); }
    public void setLectureHour(int lectureHour) { this.lectureHour.set(lectureHour); }
    public void setLaboratoryHour(int laboratoryHour) { this.laboratoryHour.set(laboratoryHour); }


    // Getters
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
    public Button getEditButton() { return editButton; }
    public String getSchedule() { return schedule.get(); }
    public String getScheduleForFaculty() { return scheduleForFaculty.get(); }
    public String getScheduleWithFaculty() { return scheduleWithFaculty.get(); }
    public String getHour() { return hoursWithIcon.get(); }
    public String getUnitsIcon() { return unitsWithIcon.get(); }

    // Property methods
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
    public StringProperty scheduleProperty() { return schedule; }
    public StringProperty scheduleForFacultyProperty() { return scheduleForFaculty; }
    public StringProperty scheduleWithFacultyProperty() { return scheduleWithFaculty; }
    public StringProperty hourProperty() { return hoursWithIcon; }
    public StringProperty unitsIconProperty() { return unitsWithIcon; }
}