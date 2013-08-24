package com.example.gpstrack;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import com.example.gpstrack.R;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class GpsStatisticsActivity extends Activity implements OnItemClickListener {

    private static final String TAG = "GpsStatisticsActivity";

    // the pattern to filter the file likes "results_20130603111245.csv"
    private static final String PATTERN = "results_\\d{14}.csv";

    private ListView mListView;
    private List<ReportItem> mList = new ArrayList<ReportItem>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gps_statistics_layout);

        mListView = (ListView) findViewById(R.id.list);
        mListView.setOnItemClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReports();

        ArrayAdapter<ReportItem> adapter = new ArrayAdapter<ReportItem>(this, R.layout.array_adapter_text_layout,R.id.text, mList);
        mListView.setAdapter(adapter);
    }

    // when item was clicked,the result of the file will be computed
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        String path = mList.get(position).getItemPath();
        Log.i(TAG,"path: " + path);
        Intent intent = new Intent();
        intent.putExtra("path", path);
        intent.setClass(this, GpsReportActivity.class);
        startActivity(intent);
    }

    // load the results files
    private void loadReports() {
        // clear the list
        mList.clear();

        // load internal files
        String internalPath = getString(R.string.cfg_internal_path);
        File internalDir = new File(internalPath);
        String[] internalFiles = internalDir.list(new FilenameFilter(){
            @Override
            public boolean accept(File dir, String filename) {
                if (filename.matches(PATTERN)) {
                    return true;
                } else {
                    return false;
                }
            }
        });

        // load external files
        String externalPath = getString(R.string.cfg_external_path);
        File externalDir = new File(externalPath);
        String[] externalFiles = externalDir.list(new FilenameFilter(){
            @Override
            public boolean accept(File dir, String filename) {
                if (filename.matches(PATTERN)) {
                    return true;
                } else {
                    return false;
                }
            }
        });

        // add internal files to list
        if (internalFiles != null && internalFiles.length > 0) {
            for (int i=0;i<internalFiles.length;i++) {
                mList.add(new ReportItem(internalPath,internalFiles[i]));
            }
        }

        // add external files to list
        if (externalFiles != null && externalFiles.length > 0) {
            for (int i=0;i<externalFiles.length;i++) {
                mList.add(new ReportItem(externalPath,externalFiles[i]));
            }
        }

    }


    // each item of the list is the object of this class
    private class ReportItem {

        private String itemName;
        private String itemPath;

        public ReportItem(String path, String name) {
            this.itemPath = path;
            this.itemName = name;
        }

        @Override
        public String toString() {
            return itemName;
        }

        public String getItemPath() {
            return itemPath + itemName;
        }
    }

}
