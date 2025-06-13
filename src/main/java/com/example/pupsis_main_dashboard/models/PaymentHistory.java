package com.example.pupsis_main_dashboard.models;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.control.Button;

public class PaymentHistory {
    private final StringProperty schoolYear;
    private final Button semesterButton;
    private final StringProperty paymentSource;
    private final StringProperty orDate;
    private final DoubleProperty assessment;
    private final DoubleProperty amount;
    private final DoubleProperty balance;

    public PaymentHistory(String schoolYear, Button semesterButton, String paymentSource, String orDate, Double assessment, Double amount, Double balance) {
        this.schoolYear = new SimpleStringProperty(schoolYear == null ? "" : schoolYear);
        this.semesterButton = semesterButton;
        this.paymentSource = new SimpleStringProperty(paymentSource == null ? "" : paymentSource);
        this.orDate = new SimpleStringProperty(orDate == null ? "" : orDate);
        this.assessment = new SimpleDoubleProperty(assessment == null ? 0.00 : assessment);
        this.amount = new SimpleDoubleProperty(amount == null ? 0.00 : amount);
        this.balance = new SimpleDoubleProperty(balance == null ? 0.00 : balance);
    }

    public String getSchoolYear() {
        return schoolYear.get();
    }

    public StringProperty schoolYearProperty() {
        return schoolYear;
    }

    public Button getSemesterButton() {
        return semesterButton;
    }


    public String getPaymentSource() {
        return paymentSource.get();
    }

    public StringProperty paymentSourceProperty() {
        return paymentSource;
    }

    public String getOrDate() {
        return orDate.get();
    }

    public StringProperty orDateProperty() {
        return orDate;
    }

    public double getAssessment() {
        return assessment.get();
    }

    public DoubleProperty assessmentProperty() {
        return assessment;
    }

    public double getAmount() {
        return amount.get();
    }

    public DoubleProperty amountProperty() {
        return amount;
    }

    public double getBalance() {
        return balance.get();
    }

    public DoubleProperty balanceProperty() {
        return balance;
    }
}