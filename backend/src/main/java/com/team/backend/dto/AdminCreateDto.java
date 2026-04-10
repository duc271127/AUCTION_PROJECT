package com.team.backend.dto;

public class AdminCreateDto {
    public String username;
    public String password; // plaintext input, sẽ hash trong service
    public boolean superAdmin = false;
}
