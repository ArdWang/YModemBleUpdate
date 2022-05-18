package com.bw.ydb.model;



import android.os.Parcel;
import android.os.Parcelable;
import com.clj.fastble.data.BleDevice;

import java.util.Date;


public class BleModel implements Parcelable {

    public BleModel(){

    }

    protected BleModel(Parcel in) {
        sendCharacter = in.readString();
        name = in.readString();
        mac = in.readString();
        if (in.readByte() == 0) {
            rss = null;
        } else {
            rss = in.readInt();
        }
        byte tmpConnect = in.readByte();
        connect = tmpConnect == 0 ? null : tmpConnect == 1;
        service = in.readString();
        bleDevice = in.readParcelable(BleDevice.class.getClassLoader());
        otaByte = in.createByteArray();
    }

    public static final Creator<BleModel> CREATOR = new Creator<BleModel>() {
        @Override
        public BleModel createFromParcel(Parcel in) {
            return new BleModel(in);
        }

        @Override
        public BleModel[] newArray(int size) {
            return new BleModel[size];
        }
    };

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

    public byte[] getOtaByte() {
        return otaByte;
    }

    public void setOtaByte(byte[] otaByte) {
        this.otaByte = otaByte;
    }

    private byte[] otaByte;


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(sendCharacter);
        dest.writeString(name);
        dest.writeString(mac);
        if (rss == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(rss);
        }
        dest.writeByte((byte) (connect == null ? 0 : connect ? 1 : 2));
        dest.writeString(service);
        dest.writeParcelable(bleDevice, flags);
        dest.writeByteArray(otaByte);
    }
}
