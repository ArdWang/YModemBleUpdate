package com.bw.ydb.model;

import com.clj.fastble.data.BleDevice;

import java.util.Date;

public class BleModel {

    public String getSendCharacter() {
        return sendCharacter;
    }

    public void setSendCharacter(String sendCharacter) {
        this.sendCharacter = sendCharacter;
    }

    public String sendCharacter;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public Integer getRss() {
        return rss;
    }

    public void setRss(Integer rss) {
        this.rss = rss;
    }

    public Boolean getConnect() {
        return connect;
    }

    public void setConnect(Boolean connect) {
        this.connect = connect;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public BleDevice getBleDevice() {
        return bleDevice;
    }

    public void setBleDevice(BleDevice bleDevice) {
        this.bleDevice = bleDevice;
    }

    private String name;

    private String mac;

    private Integer rss;

    private Boolean connect;

    private Date date;

    private String service;

    private BleDevice bleDevice;

}
