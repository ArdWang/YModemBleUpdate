package com.bw.ydb.utils.tool;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

public class BWDataParser {

    public static String getOtaData(BluetoothGattCharacteristic characteristic){
        byte[] data = characteristic.getValue();
        String qq = byteArrayToStr(data);
        Log.i("qq is ",qq);
        return qq;
    }

    //温度数据处理
    public static String getTempData(BluetoothGattCharacteristic characteristic){
        byte[] data = characteristic.getValue();
        String qq = byteArrayToStr(data);
        Log.i("qq is ",qq);
        return qq;
    }

    //数组转为string
    private static String byteArrayToStr(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        String str = new String(byteArray);
        return str;
    }
}
