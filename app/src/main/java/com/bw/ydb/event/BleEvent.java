package com.bw.ydb.event;

import com.bw.ydb.model.BleModel;

public class BleEvent {

    public BleEvent(BleModel model){
        this.model = model;
    }

    public BleModel getModel() {
        return model;
    }

    public void setModel(BleModel model) {
        this.model = model;
    }

    private BleModel model;

}
