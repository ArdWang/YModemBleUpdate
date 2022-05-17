package com.bw.ydb.utils;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.util.Log;

import com.bw.ydb.event.BleEvent;
import com.bw.ydb.event.ConnectEvent;
import com.bw.ydb.model.BleModel;
import com.bw.ydb.utils.config.GattAttributes;
import com.clj.fastble.BleManager;
import com.clj.fastble.callback.BleGattCallback;
import com.clj.fastble.callback.BleMtuChangedCallback;
import com.clj.fastble.callback.BleNotifyCallback;
import com.clj.fastble.callback.BleScanCallback;
import com.clj.fastble.callback.BleWriteCallback;
import com.clj.fastble.data.BleDevice;
import com.clj.fastble.exception.BleException;
import com.clj.fastble.scan.BleScanRuleConfig;

import org.greenrobot.eventbus.EventBus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class BleManage {

    private static BleManage instance = null;

    public static synchronized BleManage getInstance(){
        if(instance == null){
            instance = new BleManage();
        }
        return instance;
    }


    private final List<BleModel> successList = new ArrayList<>();

    // 定义
   public void init(){
       int SCAN_PERIOD = 10000;
       BleManager.getInstance().enableLog(true)
                .setReConnectCount(10,5000)
                .setConnectOverTime(20000)
                .setOperateTimeout(SCAN_PERIOD);
    }

    // 规则
    public void rule(){
        BleScanRuleConfig scanRuleConfig = new BleScanRuleConfig.Builder()
                .setServiceUuids(null)      // 只扫描指定的服务的设备，可选
                .setDeviceName(true)   // 只扫描指定广播名的设备，可选
                .setDeviceMac(null)                  // 只扫描指定mac的设备，可选
                .setAutoConnect(false)      // 连接时的autoConnect参数，可选，默认false
                .setScanTimeOut(10000)              // 扫描超时时间，可选，默认10秒
                .build();
        BleManager.getInstance().initScanRule(scanRuleConfig);
    }

    //扫描
    public void scan(){
        BleManager.getInstance().scan(new BleScanCallback(){

            @Override
            public void onScanStarted(boolean success) {

            }

            @Override
            public void onLeScan(BleDevice bleDevice) {
                super.onLeScan(bleDevice);
                Log.i("Begin...","Start...");
            }

            @Override
            public void onScanning(BleDevice bleDevice) {
                BleModel model = new BleModel();
                model.setBleDevice(bleDevice);
                model.setName(bleDevice.getName());
                model.setMac(bleDevice.getMac());
                model.setRss(bleDevice.getRssi());

                EventBus.getDefault().post(new BleEvent(model));

            }

            @Override
            public void onScanFinished(List<BleDevice> scanResultList) {

            }
        });
    }


    // 取消
    public void cancel(){
        BleManager.getInstance().cancelScan();
    }

    // 连接
    public void connect(BleDevice bleDevice){

        BleManager.getInstance().connect(bleDevice,new BleGattCallback(){

            @Override
            public void onStartConnect() {
                Log.i("====TAG===","开始连接");
            }

            @Override
            public void onConnectFail(BleDevice bleDevice, BleException exception) {
                if(successList.size() > 0){
                   for(int i =0; i< successList.size(); i++){
                       if(Objects.equals(bleDevice.getMac(), successList.get(i).getBleDevice().getMac())){
                           successList.remove(i);
                       }
                   }
                }
                EventBus.getDefault().post(new ConnectEvent(successList));
            }

            @Override
            public void onConnectSuccess(BleDevice bleDevice, BluetoothGatt gatt, int status) {
                if(bleDevice != null){
                    BleModel model = new BleModel();
                    model.setBleDevice(bleDevice);
                    model.setConnect(true);
                    successList.add(model);
                }
                EventBus.getDefault().post(new ConnectEvent(successList));
            }

            @Override
            public void onDisConnected(boolean isActiveDisConnected, BleDevice device, BluetoothGatt gatt, int status) {
                if(successList.size() > 0){
                    for(int i =0; i< successList.size(); i++){
                        if(Objects.equals(bleDevice.getMac(), successList.get(i).getBleDevice().getMac())){
                            successList.remove(i);
                        }
                    }
                }
                EventBus.getDefault().post(new ConnectEvent(successList));
            }
        });

    }

    // 通知
    public void notify(BleDevice bleDevice, String service, String characteristic){
        BleManager.getInstance().notify(bleDevice, service, characteristic, new MyBleNotifyCallback(bleDevice));
    }



    // 监听通知
    class MyBleNotifyCallback extends BleNotifyCallback{

        private BleDevice bleDevice;

        public MyBleNotifyCallback(BleDevice bleDevice) {
            this.bleDevice = bleDevice;
        }

        @Override
        public void onNotifySuccess() {

        }

        @Override
        public void onNotifyFailure(BleException exception) {
            Log.i("Error", exception.toString());
        }

        @Override
        public void onCharacteristicChanged(byte[] data) {
            try {
                // 接收数据
                for (int i = 0; i < successList.size(); i++) {
                    if (successList.get(i).sendCharacter != null) {
                        if (Objects.equals(successList.get(i).getMac(), bleDevice.getMac())) {
                            // 进行ota的数据读取或者交换

                        }
                    }
                }

                EventBus.getDefault().post(new ConnectEvent(successList));

            }catch (Exception e){
                e.printStackTrace();
            }
        }
    }


    // 读取服务
    public void readServer(BleDevice bleDevice){
        BluetoothGatt gatt = BleManager.getInstance().getBluetoothGatt(bleDevice);
        List<BluetoothGattService> serviceList = gatt.getServices();
        for (BluetoothGattService service : serviceList) {
            List<BluetoothGattCharacteristic> characteristicList= service.getCharacteristics();
            for(BluetoothGattCharacteristic characteristic : characteristicList) {
                if(characteristic.getUuid().toString().equals(GattAttributes.BW_PROJECT_OTA_DATA)){
                   for(BleModel model: successList){
                       if(Objects.equals(model.getBleDevice().getMac(), bleDevice.getMac())){
                           model.setSendCharacter(characteristic.toString());
                           model.setService(service.toString());
                           // 发送通知
                           notify(bleDevice, service.toString(), gatt.toString());
                       }
                   }
                }
            }
        }
    }

    // 写入蓝牙数据
    public void write(BleDevice bleDevice, String service, String character, byte[] data){
        BleManager.getInstance().write(
                bleDevice,
                service,
                character,
                data,
                new BleWriteCallback() {
                    @Override
                    public void onWriteSuccess(int current, int total, byte[] justWrite) {
                        Log.i("Success", justWrite.toString());
                    }

                    @Override
                    public void onWriteFailure(BleException exception) {
                        Log.i("Error", exception.toString());
                    }
                }
        );
    }


    // 断开蓝牙
    public void disCon(BleDevice bleDevice){
        if(successList.size() > 0){
            for(int i =0; i< successList.size(); i++){
                if(Objects.equals(bleDevice.getMac(), successList.get(i).getBleDevice().getMac())){
                    successList.remove(i);
                }
            }
        }
        BleManager.getInstance().disconnect(bleDevice);
        EventBus.getDefault().post(new ConnectEvent(successList));
    }


    //断开所有蓝牙
    public void disAll(){
        if(successList.size() > 0){
            successList.clear();
        }
        BleManager.getInstance().disconnectAllDevice();
    }


    // 设置mtu
    public void sendMtu(BleDevice bleDevice){
        BleManager.getInstance().setMtu(bleDevice, 512, new BleMtuChangedCallback() {
            @Override
            public void onSetMTUFailure(BleException exception) {
                Log.i("Send mtu tag is ", exception.toString());
            }

            @Override
            public void onMtuChanged(int mtu) {
                Log.i("Send mtu tag is "+mtu, "mtu success!");
            }
        });
    }






}
