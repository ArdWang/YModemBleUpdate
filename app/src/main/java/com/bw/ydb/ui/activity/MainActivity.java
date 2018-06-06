package com.bw.ydb.ui.activity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.bw.ydb.R;
import com.bw.ydb.ui.adapter.LeDeviceListAdapter;
import com.bw.ydb.widgets.CustomsDialog;

/**
 * mac 代码 格式化 OPTION + CMD + L
 */

public class MainActivity extends AppCompatActivity implements SwipeRefreshLayout.OnRefreshListener{

    private ListView mDeviceList;
    private SwipeRefreshLayout mRefresh;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    //扫描时间为5秒
    private static final int SCAN_PERIOD = 10000;
    private static final int REQUEST_ENABLE_BT = 1;
    private CustomsDialog mDialog;
    private Handler mHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initView();

        checkBluetooth();
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
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                scanLeDevice(true);
                mRefresh.setRefreshing(false);
            }
        }, 1000);
        mRefresh.setRefreshing(true);  //直接调用是没有用的
    }

    private void scanLeDevice(final boolean enable) {
        if (enable) {
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

        }else{
            //停止扫描
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }


    private void initView() {
        mHandler = new Handler();
        mDeviceList = findViewById(R.id.mDeviceList);
        mRefresh = findViewById(R.id.mRefresh);
        mRefresh.setOnRefreshListener(this);
        mRefresh.setColorSchemeResources(R.color.colorNav);
        //点击事件
        mDeviceList.setOnItemClickListener(onItemClickListener);
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
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };


    @Override
    public void onRefresh() {
        new Handler().postDelayed(new Runnable() {
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
                scanLeDevice(true);
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
    protected void onDestroy() {
        super.onDestroy();
        if(mLeScanCallback!=null){
            mLeScanCallback = null;
        }
    }

}
