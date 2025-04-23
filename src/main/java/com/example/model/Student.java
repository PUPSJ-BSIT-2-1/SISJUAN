package com.example.model;

public class Student {
    private String studentId;
    private String email;
    private String firstName;
    private String lastName;
    
    public Student(String studentId, String email, String firstName, String lastName) {
        this.studentId = studentId;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
    }

    // Getters
    public String getStudentId() { return studentId; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }

    // Setters
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setEmail(String email) { this.email = email; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    @Override
    public String toString() {
        return "Student{" +
            "studentId='" + studentId + '\'' +
            ", email='" + email + '\'' +
            ", firstName='" + firstName + '\'' +
            ", lastName='" + lastName + '\'' +
            '}';
    }
}
