package com.example.pupsis_main_dashboard.controllers;

import java.time.LocalDate;

public class TransactionRecord {
    private LocalDate date;
    private String description;
    private String orNumber;
    private double debit;
    private double credit;
    private double balance;

    public TransactionRecord(LocalDate date, String description, String orNumber, double debit, double credit, double balance) {
        this.date = date;
        this.description = description;
        this.orNumber = orNumber;
        this.debit = debit;
        this.credit = credit;
        this.balance = balance;
    }

    // Getters (required for TableView PropertyValueFactory)

    public LocalDate getDate() {
        return date;
    }

    public String getDescription() {
        return description;
    }

    public String getOrNumber() {
        return orNumber;
    }

    public double getDebit() {
        return debit;
    }

    public double getCredit() {
        return credit;
    }

    public double getBalance() {
        return balance;
    }
}