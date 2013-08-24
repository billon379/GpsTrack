package com.example.gpstrack;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.example.gpstrack.R;


import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

public class LoadReport extends AsyncTask<Void, Void, Void> {

    private static final String TAG = "LoadReport";

    interface LoadReportListener {
        public void onFinished(ArrayList<Integer> ttff, ArrayList<Float> accu, ArrayList<Map<String, Integer>> data);
    }

    private Context mContext;
    private String mPath;
    private LoadReportListener mListener;

    private ProgressDialog mDialog;
    private ArrayList<Integer> mTtffList = new ArrayList<Integer>();
    private ArrayList<Float> mAccuList = new ArrayList<Float>();
    private ArrayList<Map<String, Integer>> mListData = new ArrayList<Map<String, Integer>>();

    public LoadReport(Context context, String path, LoadReportListener listener) {
        mContext = context;
        mPath = path;
        mListener = listener;
    }

    // show a dialog when loading file
    @Override
    protected void onPreExecute() {
        mDialog = new ProgressDialog(mContext);
        mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mDialog.setCancelable(false);
        mDialog.setIndeterminate(true);
        mDialog.setMessage(mContext.getString(R.string.load_report));
        mDialog.show();
    }

    // loading the file from specified path
    @Override
    protected Void doInBackground(Void... params) {
        File file = new File(mPath);
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            // skip the title
            br.readLine();
            String line = br.readLine();
            while (line != null) {
                // generate the string to a list item
                Log.d(TAG,"line : " + line);
                generateList(line);
                line = br.readLine();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return null;
    }

    // split the string by "," and generate the list from the specify item
    private void generateList(String line) {
        String[] unit = line.split(",");
        if (unit.length == GpsResultUnit.ITEM_COUNT) {
            int session = Integer.parseInt(unit[GpsResultUnit.SESSION_NO]);
            int ttff = Integer.parseInt(unit[GpsResultUnit.TFTT]);
            float accu = Float.parseFloat(unit[GpsResultUnit.ACCU]);
            mTtffList.add(ttff);
            mAccuList.add(accu);
            Map<String, Integer> map = new HashMap<String, Integer>();
            map.put("session", session);
            map.put("ttff", ttff);
            mListData.add(map);
        } else {
            Log.d(TAG,"split result: " + unit.length);
        }
    }

    // dismiss the dialog and call onFinished() method to show the result
    @Override
    protected void onPostExecute(Void result) {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            mDialog = null;
        }
        if (mListener != null) {
            mListener.onFinished(mTtffList, mAccuList, mListData);
        }
    }

}
