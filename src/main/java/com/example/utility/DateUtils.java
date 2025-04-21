package com.example.utility;

public class DateUtils {
    public static int getDaysInMonth(String month, int year) {
        int monthNumber = getMonthNumber(month); // Convert month to number if needed (1 = Jan, 2 = Feb, etc.)
        return switch (monthNumber) {
            case 1, 3, 5, 7, 8, 10, 12 -> 31; // Months with 31 days
            case 4, 6, 9, 11 -> 30; // Months with 30 days
            case 2 -> // Handle February
                    (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) ? 29 : 28;
            default -> throw new IllegalArgumentException("Invalid month: " + month);
        };
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
