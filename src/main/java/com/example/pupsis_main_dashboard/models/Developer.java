package com.example.pupsis_main_dashboard.models;

public class Developer {
    private String devName;
    private String devRole;
    private String devDesc;
    private String devImage;
    private String devModule;

    @SuppressWarnings("unused")
    public Developer(String devName, String devRole, String devDesc, String devImage, String devModule) {
        this.devName = devName;
        this.devRole = devRole;
        this.devDesc = devDesc;
        this.devImage = devImage;
        this.devModule = devModule;
    }

    public Developer() {}

    public String getDevName() {
        return devName;
    }

    public String getDevRole() {
        return devRole;
    }

    public String getDevDesc() {
        return devDesc;
    }

    public String getDevImage() {
        return devImage;
    }

    public String getDevModule() {
        return devModule;
    }
}
