package com.bw.ydb.event;

import com.bw.ydb.model.BleModel;

import java.util.List;

public class ConnectEvent {

    public ConnectEvent(List<BleModel> list){
        this.list = list;
    }

    public List<BleModel> getList() {
        return list;
    }

    public void setList(List<BleModel> list) {
        this.list = list;
    }

    private List<BleModel> list;
}
