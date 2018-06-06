package com.bw.ydb.ui.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import com.bw.ydb.R;
import com.bw.ydb.model.OTAFileModel;
import com.bw.ydb.ui.adapter.OTAFileListAdapter;
import com.bw.ydb.utils.config.Constants;
import java.io.File;
import java.util.ArrayList;

public class FileListActivity extends FragmentActivity implements View.OnClickListener{
    private int mFilesCount;
    public static Boolean mApplicationInBackground = false;
    private final ArrayList<OTAFileModel> mArrayListFiles = new ArrayList<>();
    private final ArrayList<String> mArrayListPaths = new ArrayList<>();
    private final ArrayList<String> mArrayListFileNames = new ArrayList<>();

    private OTAFileListAdapter mFirmwareAdapter;
    private ListView mFileListView;
    private Button mUpgrade;
    private Button mNext;
    private TextView mHeading;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_filelist);

        initView();

        initData();
    }

    private void initView() {
        mFileListView = findViewById(R.id.mFileListView);
        mUpgrade = findViewById(R.id.upgrade_button);
        mNext = findViewById(R.id.next_button);
        mHeading = findViewById(R.id.heading_2);

        mUpgrade.setOnClickListener(this);
        mNext.setOnClickListener(this);
    }

    private void initData(){
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            mFilesCount = extras.getInt(Constants.REQ_FILE_COUNT);
        }

        /*
            文件夹可以以随意创建 不需要是 iBlueTool
         */
        File filedir = new File(Environment.getExternalStorageDirectory()
                + File.separator + "iBlueTool");
        mFirmwareAdapter = new OTAFileListAdapter(this,
                mArrayListFiles, mFilesCount);
        mFileListView.setAdapter(mFirmwareAdapter);
        searchRequiredFile(filedir);

        if (mFilesCount == OTAActivity.mApplicationAndStackSeparate) {
            mHeading.setText("Select stack upgrade file");
            mUpgrade.setVisibility(View.GONE);
            mNext.setVisibility(View.VISIBLE);
        } else {
            mUpgrade.setVisibility(View.VISIBLE);
            mNext.setVisibility(View.GONE);
        }

        /**
         * File Selection click event
         */
        mFileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                OTAFileModel model = mArrayListFiles.get(position);
                model.setSelected(!model.isSelected());
                for (int i = 0; i < mArrayListFiles.size(); i++) {
                    if (position != i) {
                        mArrayListFiles.get(i).setSelected(false);
                    }
                }
                mFirmwareAdapter.notifyDataSetChanged();
            }
        });
    }


    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.upgrade_button:
                if (mFilesCount == OTAActivity.mApplicationAndStackSeparate) {
                    for (int count = 0; count < mArrayListFiles.size(); count++) {
                        if (mArrayListFiles.get(count).isSelected()) {
                            mArrayListPaths.add(1, mArrayListFiles.get(count).getFilePath());
                            mArrayListFileNames.add(1, mArrayListFiles.get(count).getFileName());
                        }
                    }
                } else {
                    for (int count = 0; count < mArrayListFiles.size(); count++) {
                        if (mArrayListFiles.get(count).isSelected()) {
                            mArrayListPaths.add(0, mArrayListFiles.get(count).getFilePath());
                            mArrayListFileNames.add(0, mArrayListFiles.get(count).getFileName());
                        }
                    }
                }

                if (mFilesCount == OTAActivity.mApplicationAndStackSeparate) {
                    if (mArrayListPaths.size() == 2) {
                        Intent returnIntent = new Intent();
                        returnIntent.putExtra(Constants.SELECTION_FLAG, true);
                        returnIntent.putExtra(Constants.ARRAYLIST_SELECTED_FILE_PATHS, mArrayListPaths);
                        returnIntent.putExtra(Constants.ARRAYLIST_SELECTED_FILE_NAMES, mArrayListFileNames);
                        setResult(RESULT_OK, returnIntent);
                        finish();
                    } else {
                        alertFileSelection("Select the application upgrade file to be performed！");
                    }
                } else if (mFilesCount != OTAActivity.mApplicationAndStackSeparate
                        && mArrayListPaths.size() == 1) {
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra(Constants.SELECTION_FLAG, true);
                    returnIntent.putExtra(Constants.ARRAYLIST_SELECTED_FILE_PATHS, mArrayListPaths);
                    returnIntent.putExtra(Constants.ARRAYLIST_SELECTED_FILE_NAMES, mArrayListFileNames);
                    setResult(RESULT_OK, returnIntent);
                    finish();
                } else {
                    if (mFilesCount != OTAActivity.mApplicationAndStackCombined) {
                        alertFileSelection("Select file for application upgrade！");
                    } else {
                        alertFileSelection("Select the stack and application upgrade files for the merged file！");
                    }
                }

                break;

            case R.id.next_button:
                for (int count = 0; count < mArrayListFiles.size(); count++) {
                    if (mArrayListFiles.get(count).isSelected()) {
                        mArrayListPaths.add(0, mArrayListFiles.get(count).getFilePath());
                        mArrayListFileNames.add(0, mArrayListFiles.get(count).getFileName());
                        mHeading.setText("Select the application upgrade file");
                        mArrayListFiles.remove(count);
                        mFirmwareAdapter.addFiles(mArrayListFiles);
                        mFirmwareAdapter.notifyDataSetChanged();
                        mUpgrade.setVisibility(View.VISIBLE);
                        mNext.setVisibility(View.GONE);
                    }
                }

                if(mArrayListPaths.size() == 0){
                    alertFileSelection("Select stack upgrade file！");
                }
                break;
        }
    }

    /**
     * Method to search phone/directory for the .bin files
     * 对手机搜索/目录的方法.bin文件
     * @param dir
     */
    private void searchRequiredFile(File dir) {
        if (dir.exists()) {
            String filePattern = "bin";
            File[] allFilesList = dir.listFiles();
            for (int pos = 0; pos < allFilesList.length; pos++) {
                File analyseFile = allFilesList[pos];
                if (analyseFile != null) {
                    if (analyseFile.isDirectory()) {
                        searchRequiredFile(analyseFile);
                    } else {
                        Uri selectedUri = Uri.fromFile(analyseFile);
                        String fileExtension
                                = MimeTypeMap.getFileExtensionFromUrl(selectedUri.toString());
                        if (fileExtension.equalsIgnoreCase(filePattern)) {
                            OTAFileModel fileModel = new OTAFileModel(analyseFile.getName(),
                                    analyseFile.getAbsolutePath(), false, analyseFile.getParent());
                            mArrayListFiles.add(fileModel);
                            mFirmwareAdapter.addFiles(mArrayListFiles);
                            mFirmwareAdapter.notifyDataSetChanged();
                        }
                    }

                }
            }
        } else {
            Toast.makeText(this, "Directory does not exist", Toast.LENGTH_SHORT).show();
        }
    }

    private void alertFileSelection(String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(message)
                .setTitle(R.string.app_name)
                .setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.dismiss();
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    protected void onResume() {
        mApplicationInBackground = false;
        super.onResume();
    }

    @Override
    protected void onPause() {
        mApplicationInBackground = true;
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
