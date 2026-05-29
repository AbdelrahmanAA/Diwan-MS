package com.diwan.users.dto;
public class RegisterRequest {
    private String fullName; private String email; private String password;
    public String getFullName(){return fullName;} public void setFullName(String v){fullName=v;}
    public String getEmail(){return email;} public void setEmail(String v){email=v;}
    public String getPassword(){return password;} public void setPassword(String v){password=v;}
}