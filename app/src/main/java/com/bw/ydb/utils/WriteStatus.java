package com.bw.ydb.utils;

import com.clj.fastble.exception.BleException;

public interface WriteStatus {

    void onSuccess(int current, int total, byte[] justWrite);

    void onFail(BleException exception);

}
