package com.example.pupsis_main_dashboard.utilities;

import java.time.LocalDate;

public class SchoolYearAndSemester {

    public static String getCurrentAcademicYear() {
        LocalDate now = LocalDate.now();
        int year = now.getYear();


        if (now.getMonthValue() >= 9) {
            return year + "-" + (year + 1);
        } else {

            return (year - 1) + "-" + year;
        }
    }

    public static String determineCurrentSemester() {
        LocalDate now = LocalDate.now();

        int yearStart;  // academic year start (e.g., 2024)
        if (now.getMonthValue() >= 9) {
            yearStart = now.getYear();
        } else {
            yearStart = now.getYear() - 1;
        }

        LocalDate firstSemStart = LocalDate.of(yearStart, 9, 9);
        LocalDate firstSemEnd = LocalDate.of(yearStart + 1, 1, 28);

        LocalDate secondSemStart = LocalDate.of(yearStart + 1, 2, 17);
        LocalDate secondSemEnd = LocalDate.of(yearStart + 1, 6, 29);

        LocalDate summerStart = LocalDate.of(yearStart + 1, 7, 14);
        LocalDate summerEnd = LocalDate.of(yearStart + 1, 8, 28);

        if (!now.isBefore(firstSemStart) && !now.isAfter(firstSemEnd)) {
            return "1st Semester";
        } else if (!now.isBefore(secondSemStart) && !now.isAfter(secondSemEnd)) {
            return "2nd Semester";
        } else if (!now.isBefore(summerStart) && !now.isAfter(summerEnd)) {
            return "Summer Term";
        } else {
            return "Out of Academic Calendar";
        }
    }
}
