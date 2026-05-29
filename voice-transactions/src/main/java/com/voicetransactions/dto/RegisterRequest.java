package com.voicetransactions.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    public String getFullName() { return fullName; }
    public void   setFullName(String v) { this.fullName = v; }
    public String getEmail()    { return email; }
    public void   setEmail(String v)    { this.email = v; }
    public String getPassword() { return password; }
    public void   setPassword(String v) { this.password = v; }
}
