package com.sisjuan.models;

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Model class representing a student's enrollment record.
 * Used to display enrollment history in the Student Records tab.
 */
public class EnrollmentRecord {
    private final StringProperty semester;
    private final StringProperty schoolYear;
    private final IntegerProperty enrolledUnits;
    private final StringProperty enrollmentDate;
    private final StringProperty paymentStatus;
    
    // Additional fields that might be useful for detailed view
    private final StringProperty studentId;
    private final IntegerProperty enrollmentId;
    
    /**
     * Constructs a new EnrollmentRecord with the specified parameters.
     *
     * @param semester The semester (e.g., "1st Semester", "2nd Semester")
     * @param schoolYear The school year (e.g., "2025-2026")
     * @param enrolledUnits Number of units enrolled
     * @param enrollmentDate Date when enrollment was processed
     * @param paymentStatus Payment status (e.g., "Paid", "Pending", "Unpaid")
     * @param studentId Student's ID
     * @param enrollmentId Unique identifier for this enrollment record
     */
    public EnrollmentRecord(String semester, String schoolYear, int enrolledUnits, 
                           String enrollmentDate, String paymentStatus, 
                           String studentId, int enrollmentId) {
        this.semester = new SimpleStringProperty(semester);
        this.schoolYear = new SimpleStringProperty(schoolYear);
        this.enrolledUnits = new SimpleIntegerProperty(enrolledUnits);
        this.enrollmentDate = new SimpleStringProperty(enrollmentDate);
        this.paymentStatus = new SimpleStringProperty(paymentStatus);
        this.studentId = new SimpleStringProperty(studentId);
        this.enrollmentId = new SimpleIntegerProperty(enrollmentId);
    }
    
    /**
     * Alternative constructor without ID fields for simpler usage.
     */
    public EnrollmentRecord(String semester, String schoolYear, int enrolledUnits, 
                           String enrollmentDate, String paymentStatus) {
        this(semester, schoolYear, enrolledUnits, enrollmentDate, paymentStatus, "", 0);
    }

    // Getters and property accessors
    public String getSemester() {
        return semester.get();
    }

    public StringProperty semesterProperty() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester.set(semester);
    }

    public String getSchoolYear() {
        return schoolYear.get();
    }

    public StringProperty schoolYearProperty() {
        return schoolYear;
    }

    public void setSchoolYear(String schoolYear) {
        this.schoolYear.set(schoolYear);
    }

    public int getEnrolledUnits() {
        return enrolledUnits.get();
    }

    public IntegerProperty enrolledUnitsProperty() {
        return enrolledUnits;
    }

    public void setEnrolledUnits(int enrolledUnits) {
        this.enrolledUnits.set(enrolledUnits);
    }

    public String getEnrollmentDate() {
        return enrollmentDate.get();
    }

    public StringProperty enrollmentDateProperty() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(String enrollmentDate) {
        this.enrollmentDate.set(enrollmentDate);
    }

    public String getPaymentStatus() {
        return paymentStatus.get();
    }

    public StringProperty paymentStatusProperty() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus.set(paymentStatus);
    }

    public String getStudentId() {
        return studentId.get();
    }

    public StringProperty studentIdProperty() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId.set(studentId);
    }

    public int getEnrollmentId() {
        return enrollmentId.get();
    }

    public IntegerProperty enrollmentIdProperty() {
        return enrollmentId;
    }

    public void setEnrollmentId(int enrollmentId) {
        this.enrollmentId.set(enrollmentId);
    }
    
    @Override
    public String toString() {
        return "EnrollmentRecord{" +
                "semester='" + getSemester() + '\'' +
                ", schoolYear='" + getSchoolYear() + '\'' +
                ", enrolledUnits=" + getEnrolledUnits() +
                ", enrollmentDate='" + getEnrollmentDate() + '\'' +
                ", paymentStatus='" + getPaymentStatus() + '\'' +
                '}';
    }
}
