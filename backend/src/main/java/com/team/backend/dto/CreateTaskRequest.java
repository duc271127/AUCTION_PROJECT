package com.team.backend.dto;

public class CreateTaskRequest {

    private String title;

    public CreateTaskRequest() {
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}