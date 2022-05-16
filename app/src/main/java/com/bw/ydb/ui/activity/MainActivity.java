package com.bw.ydb.ui.activity;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;

import android.os.Bundle;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bw.ydb.R;
import com.bw.ydb.ui.adapter.LeDeviceListAdapter;
import com.bw.ydb.widgets.CustomsDialog;


import java.io.File;
import java.util.ArrayList;
import java.util.List;



/**
 * mac 代码 格式化 OPTION + CMD + L
 */

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener{

    private ListView mDeviceList;
    private SwipeRefreshLayout mRefresh;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;

    private static final int MY_PERMISSION_REQUEST_CODE = 10000;

    //扫描时间为5秒
    private static final int SCAN_PERIOD = 10000;
    private static final int REQUEST_ENABLE_BT = 1;

    private static final int REQUEST_CODE_PERMISSION_LOCATION = 2;

    private static final int REQUEST_CODE_STORE = 3;


    private CustomsDialog mDialog;
    private Handler mHandler;

    // 蓝牙权限列表的开启
    private final List<String> permissionList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        //openPermission();
        checkBluetooth();

        requestPermissions();
    }

    /**
     * 检查设备是否提供蓝牙
     */
    private void checkBluetooth() {
        // 检查当前手机是否支持ble 蓝牙,如果不支持退出程序
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "没有提供蓝牙", Toast.LENGTH_SHORT).show();
            //finish();
        }

        // 初始化 Bluetooth adapter, 通过蓝牙管理器得到一个参考蓝牙适配器(API必须在以上android4.3或以上和版本)
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // 检查设备上是否支持蓝牙
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "设备不支持蓝牙", Toast.LENGTH_SHORT).show();
            //finish();
            return;
        }
    }


    private void requestPermissions(){

        /**
         * 第 1 步: 检查是否有相应的权限
         */
        boolean isAllGranted = checkPermissionAllGranted(
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.BLUETOOTH
                }
        );
        // 如果这3个权限全都拥有, 则直接执行备份代码
        if (isAllGranted) {
            autoRefresh();
            return;
        }

        /**
         * 第 2 步: 请求权限
         */
        // 一次请求多个权限, 如果其他有权限是已经授予的将会自动忽略掉
        ActivityCompat.requestPermissions(
                this,
                new String[]{
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.BLUETOOTH
                },
                MY_PERMISSION_REQUEST_CODE
        );

    }

    /**
     * 检查是否拥有指定的所有权限
     */
    private boolean checkPermissionAllGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                // 只要有一个权限没有被授予, 则直接返回 false
                return false;
            }
        }
        return true;
    }



    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_PERMISSION_REQUEST_CODE) {
            boolean isAllGranted = true;

            // 判断是否所有的权限都已经授予了
            for (int grant : grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false;
                    break;
                }
            }

            if (isAllGranted) {
                // 如果所有的权限都授予了, 则执行备份代码
                //doBackup();
                autoRefresh();
            } else {
                // 弹出对话框告诉用户需要权限的原因, 并引导用户去应用权限管理中手动打开权限按钮
                openAppDetails();
            }
        }
    }

    /**
     * 打开 APP 的详情设置
     */
    private void openAppDetails() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("蓝牙需要访问 “定位” 和 “外部存储器”，“蓝牙”，请到 “应用信息 -> 权限” 中授予！");
        builder.setPositiveButton("去手动授权", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Intent intent = new Intent();
                intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.addCategory(Intent.CATEGORY_DEFAULT);
                intent.setData(Uri.parse("package:" + getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                startActivity(intent);
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }



    /**
     * 启动的时候要扫描蓝牙设备
     */
    @Override
    protected void onResume() {
        super.onResume();
        // 为了确保设备上蓝牙能使用, 如果当前蓝牙设备没启用,弹出对话框向用户要求授予权限来启用
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(
                        BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        //添加到蓝牙设备
        mDeviceList.setAdapter(mLeDeviceListAdapter);
        //自动扫描
       autoRefresh();
    }

    /**
     * 自动执行刷新
     */
    public void autoRefresh(){
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                scanLeDevice();
                mRefresh.setRefreshing(false);
            }
        }, 1000);
        mRefresh.setRefreshing(true);  //直接调用是没有用的
    }

    private void scanLeDevice() {
        // Stops scanning after a pre-defined scan period.
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                /**
                 * 停止扫描后需要自动连接
                 */
                Log.i("check times", "we end!");
            }
        }, SCAN_PERIOD);
        Log.i("check times", "we starting!");
        mBluetoothAdapter.startLeScan(mLeScanCallback);

    }


    private void initView() {
        mHandler = new Handler(Looper.getMainLooper());
        mDeviceList = findViewById(R.id.mDeviceList);
        mRefresh = findViewById(R.id.mRefresh);
        mRefresh.setOnRefreshListener(this);
        mRefresh.setColorSchemeResources(R.color.colorNav);
        //点击事件
        mDeviceList.setOnItemClickListener(onItemClickListener);

        try {
            if (isok()) {
                String b = "/storage/emulated/0/TESTBLE";
                File file = new File(b);
                if (!file.exists()) {
                    file.mkdirs();
                    Log.i("Create_file", "文件夹不存在创建文件夹");
                } else {
                    Log.i("Create_file", "文件夹存在不需要创建");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private boolean isok() {
        String status = Environment.getExternalStorageState();
        return status.equals(Environment.MEDIA_MOUNTED);
    }



    /**
     * listview点击项目
     */
    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            BluetoothDevice device = mLeDeviceListAdapter
                    .getDevice(position);
            if (device == null) {
                return;
            }
            showDialog(device);
        }
    };

    private void showDialog(final BluetoothDevice device){
        CustomsDialog.Builder builder = new CustomsDialog.Builder(MainActivity.this);
        builder.setTips("蓝牙连接");
        builder.setContent("OTA升级");

        builder.setNegativeButton(R.string.custom_dialog_left, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                Intent intent = new Intent(MainActivity.this,OTAActivity.class);
                intent.putExtra(OTAActivity.EXTRAS_DEVICE_NAME,device.getName());
                intent.putExtra(OTAActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                startActivity(intent);
            }
        }).setPositiveButton(R.string.custom_dialog_right, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        mDialog = builder.create();
        mDialog.show();
        mDialog.setCanceledOnTouchOutside(true);
    }


    // 找到设备回调  处理机制
    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi, byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //要指定这样子的设备才能添加进去
                    Log.i("设备的名称是:",device.getAddress());
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };


    @Override
    public void onRefresh() {
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            @Override
            public void run() {
                // 为了确保设备上蓝牙能使用, 如果当前蓝牙设备没启用,弹出对话框向用户要求授予权限来启用
                if (!mBluetoothAdapter.isEnabled()) {
                    if (!mBluetoothAdapter.isEnabled()) {
                        Intent enableBtIntent = new Intent(
                                BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                    }
                }
                //if(isSet){
                // Initializes list view adapter.
                mLeDeviceListAdapter = new LeDeviceListAdapter(MainActivity.this);

                //添加到蓝牙设备
                mDeviceList.setAdapter(mLeDeviceListAdapter);
                scanLeDevice();
                // 停止刷新
                mRefresh.setRefreshing(false);
            }
        }, 2000); // 5秒后发送消息，停止刷新
    }

    /**
     * 退出程序显示提示
     */
    private long mExitTime;
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN
                && event.getRepeatCount() == 0) {
            if ((System.currentTimeMillis() - mExitTime) > 2000) {
                Toast.makeText(this, "再按一次退出程序", Toast.LENGTH_SHORT).show();
                mExitTime = System.currentTimeMillis();
            }
            else {
                //ActivityCollector.finishAll();
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                Log.d("TAG", "打开蓝牙成功！");
            }

            if (resultCode == RESULT_CANCELED) {
                Log.d("TAG", "放弃打开蓝牙！");
            }

        } else {
            Log.d("TAG", "蓝牙异常！");
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mLeScanCallback!=null){
            mLeScanCallback = null;
        }
    }

}
