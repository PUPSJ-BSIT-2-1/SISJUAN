package com.example.pupsis_main_dashboard.models;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class TransactionHistory {
    private final StringProperty transactionID;
    private final StringProperty dateTime;
    private final StringProperty studentNumber;
    private final StringProperty paymentMethod;
    private final StringProperty amount;
    private final StringProperty assessment;
    private final StringProperty balance;
    private final StringProperty status;
    
    public TransactionHistory(String transactionID, String dateTime, String studentNumber, String paymentMethod, String amount, String assessment, String balance, String status) {
        this.transactionID = new SimpleStringProperty(transactionID);
        this.dateTime = new SimpleStringProperty(dateTime);
        this.studentNumber = new SimpleStringProperty(studentNumber);
        this.paymentMethod = new SimpleStringProperty(paymentMethod);
        this.amount = new SimpleStringProperty(amount);
        this.assessment = new SimpleStringProperty(assessment);
        this.balance = new SimpleStringProperty(balance);
        this.status = new SimpleStringProperty(status);
    }
    
    public String getTransactionID() {
        return transactionID.get();
    }
    
    public String getDateTime() {
        return dateTime.get();
    }
    
    public String getStudentNumber() {
        return studentNumber.get();
    }
    
    public String getPaymentMethod() {
        return paymentMethod.get();
    }
    
    public String getAmount() {
        return amount.get();
    }
    
    public String getAssessment() {
        return assessment.get();
    }
    
    public String getBalance() {
        return balance.get();
    }
    
    public String getStatus() {
        return status.get();
    }
    
    public void setTransactionID(String transactionID) {
        this.transactionID.set(transactionID);
    }
    
    public void setDateTime(String dateTime) {
        this.dateTime.set(dateTime);
    }
    
    public void setStudentNumber(String studentNumber) {
        this.studentNumber.set(studentNumber);
    }
    
    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod.set(paymentMethod);
    }
    
    public void setAmount(String amount) {
        this.amount.set(amount);
    }
    
    public void setAssessment(String assessment) {
        this.assessment.set(assessment);
    }
    
    public void setBalance(String balance) {
        this.balance.set(balance);
    }
    
    public void setStatus(String status) {
        this.status.set(status);
    }
    
    public StringProperty transactionIDProperty() {
        return transactionID;
    }
    
    public StringProperty dateTimeProperty() {
        return dateTime;
    }
    
    public StringProperty studentNumberProperty() {
        return studentNumber;
    }
    
    public StringProperty paymentMethodProperty() {
        return paymentMethod;
    }
    
    public StringProperty amountProperty() {
        return amount;
    }
    
    public StringProperty assessmentProperty() {
        return assessment;
    }
    
    public StringProperty balanceProperty() {
        return balance;
    }
    
    public StringProperty statusProperty() {
        return status;
    }
}