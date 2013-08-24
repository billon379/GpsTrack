package com.example.gpstrack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import com.example.gpstrack.R;
import com.example.gpstrack.LoadReport.LoadReportListener;

public class GpsReportActivity extends Activity implements LoadReportListener{

    private TextView mTtff_68;
    private TextView mTtff_95;
    private TextView mAccu_68;
    private TextView mAccu_95;
    private ListView mList;

    private static final String[] FROM = { "session", "ttff" };
    private static final int[] TO = { R.id.statistic_adapter_session,
            R.id.statistic_adapter_ttff };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gps_report_layout);

        mTtff_68 = (TextView) findViewById(R.id.ttff_68);
        mTtff_95 = (TextView) findViewById(R.id.ttff_95);
        mAccu_68 = (TextView) findViewById(R.id.accu_68);
        mAccu_95 = (TextView) findViewById(R.id.accu_95);
        mList = (ListView) findViewById(R.id.list);

        Intent intent = getIntent();
        String path = intent.getStringExtra("path");
        // load the report in asynctask
        new LoadReport(this, path, this).execute();

    }

    // this method will be called when the report file load complete
    @Override
    public void onFinished(ArrayList<Integer> ttff, ArrayList<Float> accu,
            ArrayList<Map<String, Integer>> data) {
        if (ttff.size() > 0) {
            Collections.sort(ttff);
            mTtff_68.setText("" + ttff.get((int)(0.68 * ttff.size())));
            mTtff_95.setText("" + ttff.get((int)(0.95 * ttff.size())));
        }
        if (accu.size() > 0) {
            Collections.sort(accu);
            mAccu_68.setText("" + accu.get((int)(0.68 * accu.size())));
            mAccu_95.setText("" + accu.get((int)(0.95 * accu.size())));
        }
        SimpleAdapter adapter = new SimpleAdapter(this, data, R.layout.report_adapter_layout, FROM, TO);
        mList.setAdapter(adapter);
    }

}
