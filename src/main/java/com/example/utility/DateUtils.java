package com.example.utility;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.Year;

public class DateUtils {
    public static int getDaysInMonth(String month, int year) {
        int monthNumber = getMonthNumber(month); // Convert month to number if needed (1 = Jan, 2 = Feb, etc.)
        switch (monthNumber) {
            case 1: case 3: case 5: case 7: case 8: case 10: case 12: return 31; // Months with 31 days
            case 4: case 6: case 9: case 11: return 30; // Months with 30 days
            case 2: // Handle February
                return (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) ? 29 : 28;
            default:
                throw new IllegalArgumentException("Invalid month: " + month);
        }
    }

    public static int getMonthNumber(String month) {
        return switch (month) {
            case "January" -> 1;
            case "February" -> 2;
            case "March" -> 3;
            case "April" -> 4;
            case "May" -> 5;
            case "June" -> 6;
            case "July" -> 7;
            case "August" -> 8;
            case "September" -> 9;
            case "October" -> 10;
            case "November" -> 11;
            case "December" -> 12;
            default -> 0;
        };
    }
}
