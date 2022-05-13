package com.bw.ydb.ui.activity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bw.ydb.R;
import com.bw.ydb.data.service.BluetoothService;
import com.bw.ydb.utils.config.BroadCast;
import com.bw.ydb.utils.config.Constants;
import com.bw.ydb.utils.config.GattAttributes;
import com.bw.ydb.widgets.TextProgressBar;
import com.bw.yml.YModem;
import com.bw.yml.YModemListener;
import java.util.ArrayList;
import java.util.List;

public class OTAActivity extends AppCompatActivity implements View.OnClickListener{
    /**
     * 静态的非变量成员
     */
    public static final int mApplicationUpgrade = 101;
    public static final int mApplicationAndStackCombined = 201;
    public static final int mApplicationAndStackSeparate = 301;
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    public BluetoothService bluetoothService;

    private Button mOTAUpdate;
    //OTA
    public static BluetoothGattCharacteristic mSendOTACharacteristic;

    //获取得到的设备地址和设备名称
    private String deviceAddre;

    private YModem yModem;

    private String mCurrentFilePath;

    private String mCurrentFileName;

    private boolean sendData;

    private TextProgressBar mUpgradeBar;

    private TextView mUpgradeFilename;

    private Button mOTAStop;

    private static final String TAG = "OTAActivity";



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ota);

        initView();
        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if(mCurrentFileName!=null) {
            mUpgradeFilename.setText(mCurrentFileName);
        }else{
            mUpgradeFilename.setText("选择需要升级的文件");
        }
    }

    /**
     * 发送服务广播
     * @return
     */
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BroadCast.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BroadCast.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BroadCast.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BroadCast.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BroadCast.ACTION_OTA_DATA_AVAILABLE);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        return intentFilter;
    }


    private void initView() {
        mOTAUpdate = findViewById(R.id.mOTAUpdate);
        mUpgradeBar = findViewById(R.id.mUpgradeBar);
        mUpgradeFilename = findViewById(R.id.mUpgradeFilename);
        mOTAStop = findViewById(R.id.mOTAStop);

        mOTAUpdate.setOnClickListener(this);
        mOTAStop.setOnClickListener(this);
    }

    private void initData(){
        deviceAddre = getIntent().getStringExtra(OTAActivity.EXTRAS_DEVICE_ADDRESS);
        //创建service
        Intent gattServiceIntent = new Intent(this, BluetoothService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }

    /**
     * 蓝牙连接
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            //断开的时候作出来判断
        }
        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            bluetoothService = ((BluetoothService.LocalBinder) service)
                    .getService();
            if (!bluetoothService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }

            if(deviceAddre!=null) {
                bluetoothService.connect(deviceAddre, OTAActivity.this);
            }
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Bundle extras = intent.getExtras();
            // 如果已经连接
            if (BroadCast.ACTION_GATT_CONNECTED.equals(action)) {
                /**
                 * 如果连接成功就要存储设备信息
                 */
                //saveDeviceInfo(mDeviceName, mDeviceAddress);

            } else if (BroadCast.ACTION_GATT_DISCONNECTED.equals(action)) {
                // 如果没有连接
                /**
                 * 当蓝牙连接出现断开的时候那么需要把该界面finish()掉
                 * 我这里是清除了所有的界面 除了MainActivity
                 */
            } else if (BroadCast.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // 发现服务
                displayGattServices(bluetoothService.getSupportedGattServices());
            }

            //ota的
            else if (BroadCast.ACTION_DATA_AVAILABLE.equals(action)) {
                if(extras.containsKey(BroadCast.ACTION_OTA_DATA)) {
                    String otadata = intent.getStringExtra(BroadCast.ACTION_OTA_DATA);
                    onDataReceivedFromBLE(strToByteArray(otadata));
                }else{
                    sendData = false;
                }
            }
        }
    };

    private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) {
            return;
        }
        for (BluetoothGattService gattService : gattServices) {
            List<BluetoothGattCharacteristic> gattCharacteristicss = gattService.getCharacteristics();
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristicss) {
                String uuidchara = gattCharacteristic.getUuid().toString();
                Log.i("===特征===",uuidchara);
                //写入OTA的数据
                if(uuidchara.equalsIgnoreCase(GattAttributes.BW_PROJECT_OTA_DATA)){
                    mSendOTACharacteristic = gattCharacteristic;
                    //温度的数据读取
                    prepareBroadcastDataRead(gattCharacteristic);
                    prepareBroadcastDataIndicate(gattCharacteristic);
                }
            }
        }
    }

    void prepareBroadcastDataRead(BluetoothGattCharacteristic gattCharacteristic) {
        if ((gattCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
            bluetoothService.readCharacteristic(gattCharacteristic);
        }
    }

    void prepareBroadcastDataIndicate(
            BluetoothGattCharacteristic gattCharacteristic) {
        if ((gattCharacteristic.getProperties() | BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0) {
            bluetoothService.setCharacteristicIndication(gattCharacteristic,
                    true);
        }
    }


    public void onDataReceivedFromBLE(byte[] data) {
        yModem.onReceiveData(data);
        sendData = true;
    }

    public static byte[] strToByteArray(String str) {
        if (str == null) {
            return null;
        }
        byte[] byteArray = str.getBytes();
        return byteArray;
    }

    private void startTransmission(){
        //String md5 = MD5Util.MD5(mCurrentFilePath);
        yModem = new YModem.Builder()
                .with(this)
                .filePath(mCurrentFilePath)
                .fileName(mCurrentFileName)
                .checkMd5("")
                .callback(new YModemListener() {
                    @Override
                    public void onDataReady(byte[] data) {
                        if(sendData) {
                            if (bluetoothService.writeCharacteristic(mSendOTACharacteristic, data)) {
                                Log.i("==Send Data is:", bytesToHexFun(data));
                            }
                        }
                    }

                    @Override
                    public void onProgress(int currentSent, int total) {
                        float currentPt = (float)currentSent/total;
                        int a = (int)(currentPt*100);
                        mUpgradeBar.setProgress(currentSent);   // Main Progress
                        mUpgradeBar.setMax(total); // Maximum Progress
                        if(a<=100){
                            mUpgradeBar.setProgressText(""+a+"%");
                        }else{
                            mUpgradeBar.setProgressText("100%");
                        }

                    }

                    @Override
                    public void onSuccess() {
                        Toast.makeText(OTAActivity.this,"固件升级完成",Toast.LENGTH_LONG).show();
                        finish();
                    }

                    @Override
                    public void onFailed(String reason) {
                        Toast.makeText(OTAActivity.this,reason,Toast.LENGTH_LONG).show();
                    }
                }).build();
        yModem.start();
    }

    public static String bytesToHexFun(byte[] bytes) {
        StringBuilder buf = new StringBuilder(bytes.length * 2);
        for(byte b : bytes) { // 使用String的format方法进行转换
            buf.append(String.format("%02x", new Integer(b & 0xff)));
        }
        return buf.toString();
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.mOTAUpdate:
                if(mCurrentFileName==null&&mCurrentFilePath==null){
                    mOTAUpdate.setText("点我升级");
                    Intent ApplicationAndStackCombined = new Intent(OTAActivity.this, FileListActivity.class);
                    ApplicationAndStackCombined.putExtra("FilesName", "Files");
                    ApplicationAndStackCombined.putExtra(Constants.REQ_FILE_COUNT, mApplicationAndStackCombined);
                    startActivityForResult(ApplicationAndStackCombined, mApplicationAndStackCombined);
                }else {
                    mOTAUpdate.setVisibility(View.GONE);
                    mOTAStop.setVisibility(View.VISIBLE);
                    boolean isSuccess = bluetoothService.writeCharacteristic(OTAActivity.mSendOTACharacteristic,"0x05");
                    if (isSuccess) {
                        startTransmission();
                    }
                }
                break;

            case R.id.mOTAStop:
                finish();
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            ArrayList<String> selectedFiles = data.
                    getStringArrayListExtra(Constants.ARRAYLIST_SELECTED_FILE_NAMES);
            ArrayList<String> selectedFilesPaths = data.
                    getStringArrayListExtra(Constants.ARRAYLIST_SELECTED_FILE_PATHS);
            if (requestCode == mApplicationUpgrade) {
                mCurrentFileName = selectedFiles.get(0);
                mCurrentFilePath = selectedFilesPaths.get(0);
            } else if (requestCode == mApplicationAndStackCombined) {
                mCurrentFileName = selectedFiles.get(0);
                mCurrentFilePath = selectedFilesPaths.get(0);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        yModem.stop();
        unregisterReceiver(mGattUpdateReceiver);
        //断开服务连接
        unbindService(mServiceConnection);
        //断开蓝牙服务连接
        if(bluetoothService!=null){
            bluetoothService.disconnect();
        }
    }


}
