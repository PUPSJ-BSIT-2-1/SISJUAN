package com.example.pupsis_main_dashboard.models;

import javafx.beans.property.*;
import javafx.beans.binding.Bindings;
import java.time.LocalDate;

public class Student {
    private final ObjectProperty<Integer> studentId; // Actual DB primary key
    private final StringProperty studentNo; // Textual student number (e.g., "2021-00001-MN-0")
    private final StringProperty firstName;
    private final StringProperty middleName;
    private final StringProperty lastName;
    private final ReadOnlyStringWrapper studentFullName; // Derived full name
    private final StringProperty email;
    private final ObjectProperty<LocalDate> birthday;
    private final StringProperty address;

    private final ObjectProperty<Integer> studentStatusId;
    private final StringProperty studentStatusName;

    private final ObjectProperty<Integer> departmentId; // Represents program/course
    private final StringProperty departmentName; // Program/course name

    private final ObjectProperty<Integer> yearLevelId;
    private final StringProperty yearLevelName;

    private final ObjectProperty<Integer> scholasticStatusId;
    private final StringProperty scholasticStatusName;

    // Fields for specific views like EditGradesPageController
    private final StringProperty finalGrade; 
    private final StringProperty subjectCodeForGrade; // Added for EditGradesPageController
    private final StringProperty gradeStatusName; // Added for EditGradesPageController
    private final ObjectProperty<Integer> studentLoadId; // Added for EditGradesPageController to link to grade table

    // Default constructor
    public Student() {
        this.studentId = new SimpleObjectProperty<>();
        this.studentNo = new SimpleStringProperty("");
        this.firstName = new SimpleStringProperty("");
        this.middleName = new SimpleStringProperty("");
        this.lastName = new SimpleStringProperty("");
        this.studentFullName = new ReadOnlyStringWrapper();
        this.studentFullName.bind(Bindings.createStringBinding(() -> buildFullName(), firstName, middleName, lastName));
        this.email = new SimpleStringProperty("");
        this.birthday = new SimpleObjectProperty<>();
        this.address = new SimpleStringProperty("");
        this.studentStatusId = new SimpleObjectProperty<>();
        this.studentStatusName = new SimpleStringProperty("");
        this.departmentId = new SimpleObjectProperty<>();
        this.departmentName = new SimpleStringProperty("");
        this.yearLevelId = new SimpleObjectProperty<>();
        this.yearLevelName = new SimpleStringProperty("");
        this.scholasticStatusId = new SimpleObjectProperty<>();
        this.scholasticStatusName = new SimpleStringProperty("");
        this.finalGrade = new SimpleStringProperty("");
        this.subjectCodeForGrade = new SimpleStringProperty("");
        this.gradeStatusName = new SimpleStringProperty("");
        this.studentLoadId = new SimpleObjectProperty<>(); // Initialize new field
    }

    // Full constructor with all new properties
    public Student(Integer studentId, String studentNo, String firstName, String middleName, String lastName,
                   String email, LocalDate birthday, String address,
                   Integer studentStatusId, String studentStatusName,
                   Integer departmentId, String departmentName, // departmentId is for program
                   Integer yearLevelId, String yearLevelName,
                   Integer scholasticStatusId, String scholasticStatusName,
                   String finalGrade, String subjectCodeForGrade, String gradeStatusName, ObjectProperty<Integer> studentLoadId) {
        this.studentId = new SimpleObjectProperty<>(studentId);
        this.studentNo = new SimpleStringProperty(studentNo);
        this.firstName = new SimpleStringProperty(firstName);
        this.middleName = new SimpleStringProperty(middleName != null ? middleName : "");
        this.lastName = new SimpleStringProperty(lastName);
        this.studentLoadId = studentLoadId;
        this.studentFullName = new ReadOnlyStringWrapper();
        this.studentFullName.bind(Bindings.createStringBinding(() -> buildFullName(), this.firstName, this.middleName, this.lastName));
        this.email = new SimpleStringProperty(email);
        this.birthday = new SimpleObjectProperty<>(birthday);
        this.address = new SimpleStringProperty(address);
        this.studentStatusId = new SimpleObjectProperty<>(studentStatusId);
        this.studentStatusName = new SimpleStringProperty(studentStatusName);
        this.departmentId = new SimpleObjectProperty<>(departmentId);
        this.departmentName = new SimpleStringProperty(departmentName);
        this.yearLevelId = new SimpleObjectProperty<>(yearLevelId);
        this.yearLevelName = new SimpleStringProperty(yearLevelName);
        this.scholasticStatusId = new SimpleObjectProperty<>(scholasticStatusId);
        this.scholasticStatusName = new SimpleStringProperty(scholasticStatusName);
        this.finalGrade = new SimpleStringProperty(finalGrade);
        this.subjectCodeForGrade = new SimpleStringProperty(subjectCodeForGrade);
        this.gradeStatusName = new SimpleStringProperty(gradeStatusName);
    }

    // Constructor for EditGradesPageController
    public Student(String studentNo, String studentFullNameDisplay, String subjectCode, String finalGrade, String gradeStatusName, Integer studentLoadId) {
        this(); // Call default constructor to initialize all properties

        this.studentNo.set(studentNo);
        // For studentFullName, which is bound, set its constituent parts.
        // Assuming studentFullNameDisplay is the complete name, we can set firstName to it.
        this.firstName.set(studentFullNameDisplay);
        // Middle and Last name can be empty for this specific constructor if not provided separately
        this.middleName.set(""); 
        this.lastName.set("");

        this.subjectCodeForGrade.set(subjectCode);
        this.finalGrade.set(finalGrade);
        this.gradeStatusName.set(gradeStatusName);
        this.studentLoadId.set(studentLoadId); // Set the new field
    }

    private String buildFullName() {
        StringBuilder fullNameBuilder = new StringBuilder(firstName.get() != null ? firstName.get() : "");
        if (middleName.get() != null && !middleName.get().isEmpty()) {
            fullNameBuilder.append(" ").append(middleName.get());
        }
        if (lastName.get() != null && !lastName.get().isEmpty()) {
            fullNameBuilder.append(" ").append(lastName.get());
        }
        return fullNameBuilder.toString().trim();
    }

    // --- Getters and Setters with Property Methods ---

    // StudentId (Integer - actual PK)
    public Integer getStudentId() { return studentId.get(); }
    public void setStudentId(Integer id) { this.studentId.set(id); }
    public ObjectProperty<Integer> studentIdProperty() { return studentId; }

    // StudentNo (String - textual identifier)
    public String getStudentNo() { return studentNo.get(); }
    public void setStudentNo(String no) { this.studentNo.set(no); }
    public StringProperty studentNoProperty() { return studentNo; }

    // FirstName
    public String getFirstName() { return firstName.get(); }
    public void setFirstName(String name) { this.firstName.set(name); }
    public StringProperty firstNameProperty() { return firstName; }

    // MiddleName
    public String getMiddleName() { return middleName.get(); }
    public void setMiddleName(String name) { this.middleName.set(name); }
    public StringProperty middleNameProperty() { return middleName; }

    // LastName
    public String getLastName() { return lastName.get(); }
    public void setLastName(String name) { this.lastName.set(name); }
    public StringProperty lastNameProperty() { return lastName; }

    // StudentFullName (ReadOnlyStringProperty - derived)
    public String getStudentFullName() { return studentFullName.get(); }
    public ReadOnlyStringProperty studentFullNameProperty() { return studentFullName.getReadOnlyProperty(); }

    // Email
    public String getEmail() { return email.get(); }
    public void setEmail(String mail) { this.email.set(mail); }
    public StringProperty emailProperty() { return email; }

    // Birthday (LocalDate)
    public LocalDate getBirthday() { return birthday.get(); }
    public void setBirthday(LocalDate date) { this.birthday.set(date); }
    public ObjectProperty<LocalDate> birthdayProperty() { return birthday; }

    // Address
    public String getAddress() { return address.get(); }
    public void setAddress(String addr) { this.address.set(addr); }
    public StringProperty addressProperty() { return address; }

    // Student Status ID
    public Integer getStudentStatusId() { return studentStatusId.get(); }
    public void setStudentStatusId(Integer id) { this.studentStatusId.set(id); }
    public ObjectProperty<Integer> studentStatusIdProperty() { return studentStatusId; }

    // Student Status Name
    public String getStudentStatusName() { return studentStatusName.get(); }
    public void setStudentStatusName(String name) { this.studentStatusName.set(name); }
    public StringProperty studentStatusNameProperty() { return studentStatusName; }

    // Department ID (Program ID)
    public Integer getDepartmentId() { return departmentId.get(); }
    public void setDepartmentId(Integer id) { this.departmentId.set(id); }
    public ObjectProperty<Integer> departmentIdProperty() { return departmentId; }

    // Department Name (Program Name)
    public String getDepartmentName() { return departmentName.get(); }
    public void setDepartmentName(String name) { this.departmentName.set(name); }
    public StringProperty departmentNameProperty() { return departmentName; }

    // Year Level ID
    public Integer getYearLevelId() { return yearLevelId.get(); }
    public void setYearLevelId(Integer id) { this.yearLevelId.set(id); }
    public ObjectProperty<Integer> yearLevelIdProperty() { return yearLevelId; }

    // Year Level Name
    public String getYearLevelName() { return yearLevelName.get(); }
    public void setYearLevelName(String name) { this.yearLevelName.set(name); }
    public StringProperty yearLevelNameProperty() { return yearLevelName; }

    // Scholastic Status ID
    public Integer getScholasticStatusId() { return scholasticStatusId.get(); }
    public void setScholasticStatusId(Integer id) { this.scholasticStatusId.set(id); }
    public ObjectProperty<Integer> scholasticStatusIdProperty() { return scholasticStatusId; }

    // Scholastic Status Name
    public String getScholasticStatusName() { return scholasticStatusName.get(); }
    public void setScholasticStatusName(String name) { this.scholasticStatusName.set(name); }
    public StringProperty scholasticStatusNameProperty() { return scholasticStatusName; }

    // Final Grade
    public String getFinalGrade() { return finalGrade.get(); }
    public void setFinalGrade(String grade) { this.finalGrade.set(grade); }
    public StringProperty finalGradeProperty() { return finalGrade; }

    // Subject Code for Grade (for EditGradesPageController)
    public String getSubjectCodeForGrade() { return subjectCodeForGrade.get(); }
    public void setSubjectCodeForGrade(String code) { this.subjectCodeForGrade.set(code); }
    public StringProperty subjectCodeForGradeProperty() { return subjectCodeForGrade; }

    // Grade Status Name (for EditGradesPageController)
    public String getGradeStatusName() { return gradeStatusName.get(); }
    public void setGradeStatusName(String name) { this.gradeStatusName.set(name); }
    public StringProperty gradeStatusNameProperty() { return gradeStatusName; }

    // Student Load ID (for EditGradesPageController)
    public Integer getStudentLoadId() { return studentLoadId.get(); }
    public void setStudentLoadId(Integer id) { this.studentLoadId.set(id); }
    public ObjectProperty<Integer> studentLoadIdProperty() { return studentLoadId; }

    // toString for debugging
    @Override
    public String toString() {
        return String.format("Student[id=%d, no=%s, name=%s, email=%s, statusId=%d (%s), deptId=%d (%s), yearId=%d (%s), scholasticId=%d (%s), gradeSubj=%s, finalGrade=%s, gradeStatus=%s, studentLoadId=%d]",
                getStudentId(), getStudentNo(), getStudentFullName(), getEmail(),
                getStudentStatusId(), getStudentStatusName(),
                getDepartmentId(), getDepartmentName(),
                getYearLevelId(), getYearLevelName(),
                getScholasticStatusId(), getScholasticStatusName(),
                getSubjectCodeForGrade(), getFinalGrade(), getGradeStatusName(), getStudentLoadId());
    }
}