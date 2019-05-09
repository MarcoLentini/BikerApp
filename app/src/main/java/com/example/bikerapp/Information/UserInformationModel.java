package com.example.bikerapp.Information;

import android.net.Uri;

public class UserInformationModel {


    private String name;
    private String mail;
    private String phone;
    private Uri image;
    private String rest_id;
    private String biker_id;


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

    public UserInformationModel(String name, String mail, String phone, String rest_id, String biker_id) {
        this.name = name;
        this.mail = mail;
        this.phone = phone;
        this.rest_id = rest_id;
        this.biker_id = biker_id;
    }

    public UserInformationModel(String name, String mail, String phone, Uri image, String rest_id, String biker_id) {
        this.name = name;
        this.mail = mail;
        this.phone = phone;
        this.image = image;
        this.rest_id = rest_id;
        this.biker_id = biker_id;
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

    public String getRest_id() {
        return rest_id;
    }

    public void setRest_id(String rest_id) {
        this.rest_id = rest_id;
    }

    public String getBiker_id() {
        return biker_id;
    }

    public void setBiker_id(String user_id) {
        this.biker_id = user_id;
    }
}
