package com.example.uu;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

public class recruit_object {
    private String recruitId;
    private String mapUrl;
    private String leader;
    private int currentUserNum;
    private int totalUserNum;
    private String date;
    private String time;
    private String userInfo;
    private String hostId;
    private String runningSpeed;

    private Map<String, Object> users = new HashMap<String, Object>();

    public recruit_object() { }

    public String getRecruitId() {
        return recruitId;
    }

    public void setRecruitId(String recruitId) {
        this.recruitId = recruitId;
    }

    public String getHostId() {
        return hostId;
    }

    public void setHostId(String hostId) {
        this.hostId = hostId;
    }

    public int getCurrentUserNum() {
        return currentUserNum;
    }

    public void setCurrentUserNum(int currentUserNum) {
        this.currentUserNum = currentUserNum;
    }

    public int getTotalUserNum() {
        return totalUserNum;
    }

    public void setTotalUserNum(int totalUserNum) {
        this.totalUserNum = totalUserNum;
    }

    public String getMapUrl() {
        return mapUrl;
    }

    public void setMapUrl(String mapUrl) { this.mapUrl=mapUrl;}

    public String getLeader() {
        return leader;
    }

    public void setLeader(String leader) {
        this.leader = leader;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getUserInfo() {
        return userInfo;
    }

    public void setUserInfo(String userInfo) {
        this.userInfo = userInfo;
    }

    public String getRunningSpeed() {
        return runningSpeed;
    }

    public void setRunningSpeed(String runningSpeed) {
        this.runningSpeed = runningSpeed;
    }

}

