package com.example.bikerapp;

import com.google.firebase.Timestamp;

import java.io.Serializable;

public class ReservationModel implements Comparable<ReservationModel>, Serializable {


    private Long rsId;
    private String nameRest;
    private String restId;
    private String addrRest;
    private String addrUser;
    private String nameUser;
    private String infoUser;
    private String userId;
    private String userPhone;
    private Timestamp timestamp;
   //maybe useful for the future
    private String timeRest;
    private String timeUser;

    public ReservationModel(Long rsId, String nameRest, String addrRest, String addrUser, String infoUser, String nameUser, String restId, String userId, String userPhone, Timestamp timestamp) {
       this.rsId=rsId;
        this.nameRest = nameRest;
        this.addrRest = addrRest;
        this.addrUser = addrUser;
        this.infoUser = infoUser;
        this.nameUser=nameUser;
        this.restId = restId;
        this.userId = userId;
        this.userPhone=userPhone;
        this.timestamp=timestamp;
    }

    public String getNameRest() {
        return nameRest;
    }

    public void setNameRest(String nameRest) {
        this.nameRest = nameRest;
    }

    public String getAddrRest() {
        return addrRest;
    }

    public void setAddrRest(String addrRest) {
        this.addrRest = addrRest;
    }

    public String getAddrUser() {
        return addrUser;
    }

    public void setAddrUser(String addrUser) {
        this.addrUser = addrUser;
    }

    public String getInfoUser() {
        return infoUser;
    }

    public void setInfoUser(String infoUser) {
        this.infoUser = infoUser;
    }

    public String getTimeRest() {
        return timeRest;
    }

    public void setTimeRest(String timeRest) {
        this.timeRest = timeRest;
    }

    public String getTimeUser() {
        return timeUser;
    }

    public void setTimeUser(String timeUser) {
        this.timeUser = timeUser;
    }

    public String getRestId() {
        return restId;
    }

    public void setRestId(String restId) {
        this.restId = restId;
    }

    public String getNameUser() {
        return nameUser;
    }

    public void setNameUser(String nameUser) {
        this.nameUser = nameUser;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserPhone() {
        return userPhone;
    }

    public void setUserPhone(String userPhone) {
        this.userPhone = userPhone;
    }

    public Long getRsId() {
        return rsId;
    }

    public void setRsId(Long rsId) {
        this.rsId = rsId;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public int compareTo(ReservationModel other) {
        return this.timestamp.compareTo(other.getTimestamp());
    }
}
