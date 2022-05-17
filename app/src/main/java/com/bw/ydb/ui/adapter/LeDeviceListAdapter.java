package com.bw.ydb.ui.adapter;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.bw.ydb.R;
import com.bw.ydb.model.BleModel;
import java.util.ArrayList;
import java.util.List;

public class LeDeviceListAdapter extends BaseAdapter {
    public static List<BleModel> mLeDevices;
    private LayoutInflater mInflator;
    ViewHolder viewHolder;
    private int postion=-1;

    public LeDeviceListAdapter(Context context) {
        super();
        mLeDevices = new ArrayList<>();
        mInflator =(LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public boolean isNullDervice(){
        if(mLeDevices.size()>0){
            return true;
        }else{
            return false;
        }
    }

    /**
     * contains()是判断是否有相同的字符串
     * @param bleModel
     */
    public void addDevice(BleModel bleModel) {
        if(!mLeDevices.contains(bleModel)) {
            if(bleModel.getName()!=null){
                mLeDevices.add(bleModel);
            }
        }
    }

    public BleModel getDevice(int position) {
        return mLeDevices.get(position);
    }


    public void addPostion(int i){
        postion = i;
    }



    public void clear() {
        mLeDevices.clear();
    }

    @Override
    public int getCount() {
        return mLeDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return mLeDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        // General ListView optimization code.
        if (view == null) {
            view = mInflator.inflate(R.layout.device_item, null);
            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_addre);
            viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
            viewHolder.clickcontent = (TextView) view.findViewById(R.id.click_content);
            //viewHolder.isConnected = (TextView) view.findViewById(R.id.is_connect);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

       // BluetoothDevice device = mLeDevices.get(i);

        BleModel model = mLeDevices.get(i);

        final String deviceName = model.getName();
        final String deviceAddre = model.getBleDevice().getDevice().getAddress();

        if(i==postion){
            viewHolder.clickcontent.setText("Connected");
        }

        if (!deviceName.isEmpty()) {
            viewHolder.deviceName.setText(deviceName);
        } else {
            viewHolder.deviceName.setText("Unkown Dervice");
        }
        if (!deviceAddre.isEmpty()) {
            String array[] = deviceAddre.split(":");
            String address = "S/N:"+array[0]+array[1]+array[2]+array[3]+array[4]+array[5];
            viewHolder.deviceAddress.setText(address);
        } else {
            viewHolder.deviceAddress.setText("Unkown Address");
        }

        return view;
    }

    private static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView clickcontent;
        //TextView isConnected;  //是否 连接
    }

}
