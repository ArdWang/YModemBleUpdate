package com.bw.ydb.data.service;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import com.bw.ydb.utils.config.BroadCast;
import com.bw.ydb.utils.config.Constants;
import com.bw.ydb.utils.config.GattAttributes;
import com.bw.ydb.utils.config.UUIDDataBase;
import com.bw.ydb.utils.tool.BWDataParser;

import java.util.List;
import java.util.UUID;


public class BluetoothService extends Service{
    private final static String TAG = BluetoothService.class.getSimpleName();
    private final IBinder iBinder = new LocalBinder();
    private BluetoothManager bluetoothManager;
    private   BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private String bluetoothDeviceAddress;
    private Context mContext;




    @Override
    public IBinder onBind(Intent intent) {
        return iBinder;
    }

    public class LocalBinder extends Binder {
        public BluetoothService getService() {
            return BluetoothService.this;
        }
    }

    /**
        初始化蓝牙
     */
    public boolean initialize() {
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothManager == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }

    /**
         解除绑定
     */
    @Override
    public boolean onUnbind(Intent intent) {
        //执行解绑的操作
        return super.onUnbind(intent);
    }

    /**
        连接蓝牙
     */
    public boolean connect(final String address,Context context) {
        mContext = context;
        if (bluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        if (address.equals(bluetoothDeviceAddress)
                && bluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (bluetoothGatt.connect()) {
                //mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        bluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        bluetoothDeviceAddress = address;
        return true;
    }

    /**
        利用BluetoothGatt连接蓝牙
        蓝牙传输数据
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //连接成功的时候
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    //连接成功
                    intentAction = BroadCast.ACTION_GATT_CONNECTED;
                    //发送广播
                    broadcastUpdate(intentAction);
                    //打印连接服务
                    Log.i(TAG, "Connected to GATT server.");
                    //立即去执行发现服务
                    gatt.discoverServices();
                }

                //当断开连接的时候
                else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    //断开连接
                    intentAction = BroadCast.ACTION_GATT_DISCONNECTED;
                    //打印断开连接
                    Log.i(TAG, "Disconnected from GATT server.");
                    //发送广播
                    broadcastUpdate(intentAction);
                }
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            //连接成功
            if(status==BluetoothGatt.GATT_SUCCESS){
                //发送发现服务的广播
                broadcastUpdate(BroadCast.ACTION_GATT_SERVICES_DISCOVERED);
                Log.i(TAG,"discoverServiced is ok");
            }else {
                Log.w(TAG, "onServicesDiscovered received: " + status);   //发现设备的时候 发送广播
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.i("读取出来的值",gatt+" "+characteristic+" "+status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //读取到里面的数据时候发送广播
                broadcastUpdate(BroadCast.ACTION_DATA_AVAILABLE, characteristic);
                Log.i(TAG,"Read Data");
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(BroadCast.ACTION_DATA_AVAILABLE, characteristic);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            if(status == BluetoothGatt.GATT_SUCCESS){
                BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
                broadcastUpdate(BroadCast.ACTION_DATA_AVAILABLE, characteristic);
                Log.i(TAG,"success is data");
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG,"write success data");
            }else{
                Intent intent = new Intent(BroadCast.ACTION_GATT_CHARACTERISTIC_ERROR);
                intent.putExtra(Constants.EXTRA_CHARACTERISTIC_ERROR_MESSAGE, "" + status);
                mContext.sendBroadcast(intent);
            }
        }
    };

    /**
         更新的时候发送广播
     */
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    /**
        广播更新数据2
     */
    private void broadcastUpdate(final String action,final BluetoothGattCharacteristic characteristic){
        final Intent intent = new Intent(action);
        // case for OTA characteristic received
        if (characteristic.getUuid().equals(UUIDDataBase.UUID_OTA_UPDATE_CHARACTERISTIC)) {
            String ota = BWDataParser.getOtaData(characteristic);
            intent.putExtra(BroadCast.ACTION_OTA_DATA,ota);
        }
        sendBroadcast(intent);
    }

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.readCharacteristic(characteristic);
    }


    public void setCharacteristicIndication(
            BluetoothGattCharacteristic characteristic, boolean enabled) {
        String serviceUUID = characteristic.getService().getUuid().toString();
        String characteristicUUID = characteristic.getUuid().toString();
        Log.i("==TAG==",serviceUUID+"   "+characteristicUUID);
        if (bluetoothAdapter == null) {
            return;
        }
        if (characteristic.getDescriptor(UUID.fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG)) != null) {
            if (enabled) {
                BluetoothGattDescriptor descriptor = characteristic
                        .getDescriptor(UUID
                                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor
                        .setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                bluetoothGatt.writeDescriptor(descriptor);

            } else {
                BluetoothGattDescriptor descriptor = characteristic
                        .getDescriptor(UUID
                                .fromString(GattAttributes.CLIENT_CHARACTERISTIC_CONFIG));
                descriptor
                        .setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                bluetoothGatt.writeDescriptor(descriptor);

            }
        }
        bluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic charac,String message){
        //check mBluetoothGatt is available
        if (bluetoothGatt == null) {
            Log.e(TAG, "lost connection");
            return false;
        }
        if(charac!=null&&!message.equals(null)&&!message.equals("")){
            //int a = Integer.parseInt(message);
            byte []a = convertingTobyteArray(message);
            charac.setValue(a);
            boolean status = bluetoothGatt.writeCharacteristic(charac);
            return status;
        }else{
            return false;
        }
    }

    /**
     * Method to convert hex to byteArray
     */
    private static byte[] convertingTobyteArray(String result) {
        String[] splited = result.split("\\s+");
        byte[] valueByte = new byte[splited.length];
        for (int i = 0; i < splited.length; i++) {
            if (splited[i].length() > 2) {
                String trimmedByte = splited[i].split("x")[1];
                valueByte[i] = (byte) convertstringtobyte(trimmedByte);
            }
        }
        return valueByte;
    }

    /**
     * Convert the string to byte
     *
     * @param string
     * @return
     */
    private static int convertstringtobyte(String string) {
        return Integer.parseInt(string, 16);
    }



    /**
        写入蓝牙数据
     */
    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic,byte[] data) {
        if (bluetoothGatt == null) {
            Log.e(TAG, "lost connection");
            return false;
        }
        if (characteristic != null && (data.length > 0)) {
            characteristic.setValue(data);
            boolean status = bluetoothGatt.writeCharacteristic(characteristic);
            return status;
        }else{
            return false;
        }
    }


    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) {
            return null;
        }
        return bluetoothGatt.getServices();
    }

    public void close() {
        if(bluetoothGatt==null){
            return;
        }else{
            bluetoothGatt.close();
            bluetoothGatt = null;
        }
    }

    public void disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.disconnect();
    }




}
