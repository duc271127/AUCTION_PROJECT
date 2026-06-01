package com.auction.client.dto.request;

public class LoginByEmailRequest {
    private String email;
    private String password;

    public LoginByEmailRequest() {
    }

    public LoginByEmailRequest(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}