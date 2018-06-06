

package com.bw.ydb.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bw.ydb.R;
import com.bw.ydb.model.OTAFileModel;
import java.util.ArrayList;


public class OTAFileListAdapter extends BaseAdapter {

    ArrayList<OTAFileModel> mFileList;
    LayoutInflater mInflater;
    int mRequiredFilesCount;
    Context mContext;

    public OTAFileListAdapter(Context context, ArrayList<OTAFileModel> fileList,
                              int requiredFilesCount) {
        this.mFileList = fileList;
        this.mContext = context;
        this.mRequiredFilesCount = requiredFilesCount;
        mInflater = LayoutInflater.from(this.mContext);        // only context can also be used
    }


    @Override
    public int getCount() {
        return mFileList.size();
    }

    @Override
    public Object getItem(int position) {
        return mFileList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        MyViewHolder mViewHolder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.firmware_item, null);
            mViewHolder = new MyViewHolder();
            mViewHolder.fileName = convertView.findViewById(R.id.file_name);
            mViewHolder.layout = convertView.findViewById(R.id.itemParent);
            mViewHolder.fileSelect = convertView.findViewById(R.id.file_checkbox);
            convertView.setTag(mViewHolder);
        }
        mViewHolder = (MyViewHolder) convertView.getTag();
        OTAFileModel file = mFileList.get(position);
        mViewHolder.fileName.setText(file.getFileName());
        mViewHolder.fileSelect.setChecked(file.isSelected());
        return convertView;
    }

    public void addFiles(ArrayList<OTAFileModel> fileModels) {
        this.mFileList = fileModels;
    }

    private class MyViewHolder {
        TextView fileName;
        CheckBox fileSelect;
        LinearLayout layout;
    }

}
