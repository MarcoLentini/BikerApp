package com.example.bikerapp.Information;

import android.net.Uri;

import java.net.URI;

public class UserInformationModel {


    private String name;
    private String mail;
    private String phone;
    private Uri image;

    public UserInformationModel(String name, String mail, String phone) {
        this.name = name;
        this.mail = mail;
        this.phone = phone;
    }

    public UserInformationModel(String name, String mail, String phone, Uri image) {
        this.name = name;
        this.mail = mail;
        this.phone = phone;
        this.image = image;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMail() {
        return mail;
    }

    public void setMail(String mail) {
        this.mail = mail;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Uri getImage() {
        return image;
    }

    public void setImage(Uri image) {
        this.image = image;
    }
}
