package com.bw.ydb.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.bw.ydb.R;
import com.bw.ydb.event.ConnectEvent;
import com.bw.ydb.model.BleModel;
import com.bw.ydb.utils.BleManage;
import com.bw.ydb.utils.WriteStatus;
import com.bw.ydb.utils.config.Constants;
import com.bw.ydb.widgets.TextProgressBar;
import com.bw.yml.YModem;
import com.bw.yml.YModemListener;
import com.clj.fastble.exception.BleException;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import java.util.ArrayList;
import java.util.Objects;


public class OTAActivity extends AppCompatActivity implements View.OnClickListener{
    /**
     * 静态的非变量成员
     */
    public static final int mApplicationUpgrade = 101;
    public static final int mApplicationAndStackCombined = 201;
    public static final int mApplicationAndStackSeparate = 301;
    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private Button mOTAUpdate;

    private YModem yModem;

    private String mCurrentFilePath;

    private String mCurrentFileName;

    private boolean sendData;

    private TextProgressBar mUpgradeBar;

    private TextView mUpgradeFilename;

    private Button mOTAStop;

    private static final String TAG = "OTAActivity";

    private BleModel bleModel;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ota);

        EventBus.getDefault().register(this);

        initView();
        initData();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mCurrentFileName!=null) {
            mUpgradeFilename.setText(mCurrentFileName);
        }else{
            mUpgradeFilename.setText("选择需要升级的文件");
        }
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
        // 获取传递归来的BleModel
        bleModel = getIntent().getParcelableExtra(Constants.BLE_MODEL);

        //deviceAddre = getIntent().getStringExtra(OTAActivity.EXTRAS_DEVICE_ADDRESS);
    }


    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onBleEvent(ConnectEvent event){
        if(event.getList().size() > 0){
            for(BleModel model:event.getList()){
                // 只能一致才能读取数据
                if(Objects.equals(model.getBleDevice().getMac(), bleModel.getBleDevice().getMac())){
                    // 必须要有返回数据才能进行操作
                    if(model.getOtaByte() != null) {
                        // 进行数据处理 ota接收数据
                        yModem.onReceiveData(model.getOtaByte());
                        sendData = true;
                    }
                }
            }
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
        return str.getBytes();
    }

    private void startTransmission(){
        //String md5 = MD5Util.MD5(mCurrentFilePath);
        yModem = new YModem.Builder()
                .with(this)
                .filePath(mCurrentFilePath)
                .fileName(mCurrentFileName)
                .checkMd5("")
                .sendSize(1024)
                .callback(new YModemListener() {
                    @Override
                    public void onDataReady(byte[] data) {
                        if(sendData) {
                            BleManage.getInstance().write(bleModel.getBleDevice(), bleModel.getService(), bleModel.getSendCharacter(), data, new WriteStatus() {
                                @Override
                                public void onSuccess(int current, int total, byte[] justWrite) {
                                    Log.i("==Send Data is:", bytesToHexFun(justWrite));
                                }

                                @Override
                                public void onFail(BleException exception) {
                                    Log.e("==Send Data fail!",exception.toString());
                                }
                            });
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
            buf.append(String.format("%02x", b & 0xff));
        }
        return buf.toString();
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.mOTAUpdate:

                if(bleModel.getBleDevice() != null) {
                    // 这一步进行读取服务
                    BleManage.getInstance().readServer(bleModel.getBleDevice());
                }

                if(mCurrentFileName==null&&mCurrentFilePath==null){
                    mOTAUpdate.setText("点我升级");
                    Intent ApplicationAndStackCombined = new Intent(OTAActivity.this, FileListActivity.class);
                    ApplicationAndStackCombined.putExtra("FilesName", "Files");
                    ApplicationAndStackCombined.putExtra(Constants.REQ_FILE_COUNT, mApplicationAndStackCombined);
                    startActivityForResult(ApplicationAndStackCombined, mApplicationAndStackCombined);
                }else {
                    mOTAUpdate.setVisibility(View.GONE);
                    mOTAStop.setVisibility(View.VISIBLE);

                    // 开始协议每个项目都不一样 需要针对底层去协议去沟通，本次0x05只针对本项目

                    BleManage.getInstance().write(bleModel.getBleDevice(), bleModel.getService(), bleModel.getSendCharacter(), "0x05".getBytes(), new WriteStatus() {
                        @Override
                        public void onSuccess(int current, int total, byte[] justWrite) {
                            Log.i("==Send Data is:", bytesToHexFun(justWrite));
                            startTransmission();
                        }

                        @Override
                        public void onFail(BleException exception) {
                            Log.e("==Send Data fail! ",exception.toString());
                        }
                    });
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
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(yModem != null) {
            yModem.stop();
        }
        BleManage.getInstance().disAll();
        EventBus.getDefault().unregister(this);
    }
}
