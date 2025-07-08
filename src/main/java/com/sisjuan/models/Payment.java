package com.sisjuan.models;

import javafx.beans.property.*;

public class Payment {

    private final IntegerProperty paymentId;
    private final StringProperty transactionId;
    private final StringProperty studentNumber;
    private final StringProperty firstName;
    private final StringProperty lastName;
    private final StringProperty sectionName;
    private final StringProperty semesterName;
    private final StringProperty academicYearName;
    private final DoubleProperty balance;
    private final DoubleProperty amount;
    private final DoubleProperty assessment;
    private final StringProperty status;
    private final StringProperty paymentSource;
    private final StringProperty createdAt;
    private final StringProperty approvedAt;
    private final StringProperty eligibility;
    private final StringProperty email;

    public Payment(int paymentId, String transactionId, String studentNumber, String firstName, String lastName,
                   String sectionName, String semesterName, String academicYearName, Double balance,
                   Double amount, Double assessment, String status, String paymentSource,
                   String createdAt, String approvedAt, String eligibility, String email) {
        this.paymentId = new SimpleIntegerProperty(paymentId);
        this.transactionId = new SimpleStringProperty(transactionId);
        this.studentNumber = new SimpleStringProperty(studentNumber);
        this.firstName = new SimpleStringProperty(firstName);
        this.lastName = new SimpleStringProperty(lastName);
        this.sectionName = new SimpleStringProperty(sectionName);
        this.semesterName = new SimpleStringProperty(semesterName);
        this.academicYearName = new SimpleStringProperty(academicYearName);
        this.balance = new SimpleDoubleProperty(balance);
        this.amount = new SimpleDoubleProperty(amount);
        this.assessment = new SimpleDoubleProperty(assessment);
        this.status = new SimpleStringProperty(status);
        this.paymentSource = new SimpleStringProperty(paymentSource);
        this.createdAt = new SimpleStringProperty(createdAt);
        this.approvedAt = new SimpleStringProperty(approvedAt);
        this.eligibility = new SimpleStringProperty(eligibility);
        this.email = new SimpleStringProperty(email);
    }

    public String getFullName() {
        return firstName.get() + " " + lastName.get();
    }

    public Integer getPaymentId() {
        return paymentId.get();
    }

    public String getEmail() {
        return email.get();
    }

    public void setEmail(String email) {
        this.email.set(email);
    }

    public StringProperty emailProperty() {
        return email;
    }

    public IntegerProperty paymentIdProperty() {
        return paymentId;
    }

    public String getTransactionId() {
        return transactionId.get();
    }

    public StringProperty transactionIdProperty() {
        return transactionId;
    }

    public String getStudentNumber() {
        return studentNumber.get();
    }

    public StringProperty studentNumberProperty() {
        return studentNumber;
    }

    public String getFirstName() {
        return firstName.get();
    }

    public StringProperty firstNameProperty() {
        return firstName;
    }

    public String getLastName() {
        return lastName.get();
    }

    public StringProperty lastNameProperty() {
        return lastName;
    }

    public String getSectionName() {
        return sectionName.get();
    }

    public StringProperty sectionNameProperty() {
        return sectionName;
    }

    public String getSemesterName() {
        return semesterName.get();
    }

    public StringProperty semesterNameProperty() {
        return semesterName;
    }

    public String getAcademicYearName() {
        return academicYearName.get();
    }

    public StringProperty academicYearNameProperty() {
        return academicYearName;
    }

    public Double getBalance() {
        return balance.get();
    }

    public DoubleProperty balanceProperty() {
        return balance;
    }

    public Double getAmount() {
        return amount.get();
    }

    public DoubleProperty amountProperty() {
        return amount;
    }

    public Double getAssessment() {
        return assessment.get();
    }

    public DoubleProperty assessmentProperty() {
        return assessment;
    }

    public String getStatus() {
        return status.get();
    }

    public StringProperty statusProperty() {
        return status;
    }

    public String getPaymentSource() {
        return paymentSource.get();
    }

    public StringProperty paymentSourceProperty() {
        return paymentSource;
    }

    public String getCreatedAt() {
        return createdAt.get();
    }

    public StringProperty createdAtProperty() {
        return createdAt;
    }

    public String getApprovedAt() {
        return approvedAt.get();
    }

    public StringProperty approvedAtProperty() {
        return approvedAt;
    }

    public String getEligibility() {
        return eligibility.get();
    }

    public StringProperty eligibilityProperty() {
        return eligibility;
    }
}
