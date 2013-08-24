package com.example.gpstrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.example.gpstrack.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Resources;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

/**
 * this is the main Activity of this app,the main logic is also implemented in this class
 */
public class GpsTestActivity extends Activity implements OnClickListener,
        TaskExecuteListener {

    private static final String TAG = "GpsTestActivity";

    // date format
    private static final String DATE_FORMAT = "yyyyMMddHHmmss";

    // the command used in sendExtraCommand()
    private static final String COMMAND_DELETE_AIDING_DATA = "delete_aiding_data";
    private static final String COMMAND_STAND_ALONE = "standalone";

    // the textview should be update every location
    private TextView mLatitude;
    private TextView mCurSession;
    private TextView mLongitude;
    private TextView mFixNO;
    private TextView mTTFF;
    private TextView mAltitude;
    private TextView mSpeed;
    private TextView mTime;
    private TextView mSvUsed;
    private TextView mStatus;

    private ListView mListView;
    private Button mStartStop;
    private Button mClear;

    private GpsConfigModel mConfigModel;
    private int mSessions;
    private int mSessionTime;
    private int mOutTime;
    private int mSessionNO;
    private int mStoragePath;
    private int mMode;
    private int mFixed;
    private int mFailed;

    private static final int REQUEST_CONFIG_CODE = 0x99;

    // update flag
    private static final int CLEAR_UI = 0x100;
    private static final int UPDATE_GPS_INFO = 0x101;
    private static final int UPDATE_SV_INFO = 0x102;

    // show or dismiss dialog flag
    private static final int DIALOG_SHOW = 0x104;
    private static final int DIALOG_DISMISS = 0x105;

    // send command error
    private static final int SEND_COMMAND_ERROR = 0x106;

    // used in svinfo adapter
    private static String[] FROM = { "prn", "snr", "elev", "azim" };
    private static int[] TO = { R.id.adapter_prn, R.id.adapter_snr,
            R.id.adapter_elev, R.id.adapter_azim };

    private LocationManager mLocationManager;
    //private GpsTimerTask mTrackingTask;
    private GpsTimerTask mCurrentTask;
    private Timer mNextTimer;
    private ArrayList<GpsResultUnit> mTestResults = new ArrayList<GpsResultUnit>();
    private ArrayList<Integer> mTTFFReport = new ArrayList<Integer>();
    private ArrayList<Float> mAccuReport = new ArrayList<Float>();
    private File mNMEAFile;
    private Date mStartTime;

    // the flag indicate the test status
    private boolean mStopLocate = true;
    private boolean mIsLocating = false;

    private ProgressDialog mDialog;

    // update the ui by handler
    private Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {
            Log.d(TAG,"msg.what: " + msg.what);
            switch (msg.what) {
            case CLEAR_UI:
                clearUI();
                break;
            case UPDATE_GPS_INFO:
                updateGpsInfo((GpsResultUnit) msg.obj);
                break;
            case UPDATE_SV_INFO:
                updateSvInfo((GpsStatus) msg.obj);
                break;
            case DIALOG_SHOW:
                showDialog();
                break;
            case DIALOG_DISMISS:
                dismissDialog();
                break;
            case SEND_COMMAND_ERROR:
                showErrorMessage();
                break;
            default:
                break;
            }
        }

    };

    // toast error message and stop the test when sendExtraCommand error
    private void showErrorMessage() {
        mStartStop.setText(getString(R.string.start));
        Toast.makeText(this, R.string.send_command_error, Toast.LENGTH_LONG).show();
    }

    // show dialog when generate the test report
    private void showDialog() {
        mDialog = new ProgressDialog(this);
        mDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mDialog.setIndeterminate(true);
        mDialog.setCancelable(false);
        mDialog.setMessage(getResources().getString(R.string.generate_report));
        mDialog.show();
    }

    // dismiss the dialog when the test report generated,toast the report path
    private void dismissDialog() {
        mStartStop.setText(getString(R.string.start));
        mClear.setEnabled(true);
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
            mDialog = null;
            mConfigModel = null;
            Resources res = getResources();
            String formatter = res.getString(R.string.generate_report_complete);
            String savePath = null;
            switch (mStoragePath) {
            case GpsConfigModel.STORAGE_PATH_INTERNAL:
                savePath = res.getString(R.string.cfg_internal_path);
                break;
            case GpsConfigModel.STORAGE_PATH_EXTERNAL:
                savePath = res.getString(R.string.cfg_external_path);
                break;
            default:
                savePath = res.getString(R.string.cfg_internal_path);
                break;
            }
            String msg = String.format(formatter, savePath);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        // initialize the components
        mLatitude = (TextView) findViewById(R.id.latitude);
        mCurSession = (TextView) findViewById(R.id.session_no);
        mLongitude = (TextView) findViewById(R.id.longitude);
        mFixNO = (TextView) findViewById(R.id.fix_no);
        mTTFF = (TextView) findViewById(R.id.ttff);
        mAltitude = (TextView) findViewById(R.id.altitude);
        mSpeed = (TextView) findViewById(R.id.speed);
        mTime = (TextView) findViewById(R.id.time);
        mSvUsed = (TextView) findViewById(R.id.sv_used);
        mStatus = (TextView) findViewById(R.id.status);
        mListView = (ListView) findViewById(R.id.list);

        mStartStop = (Button) findViewById(R.id.start_stop);
        mStartStop.setOnClickListener(this);

        mClear = (Button) findViewById(R.id.clear);
        mClear.setOnClickListener(this);

        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGpsDialog();
        }
    }

    // guide the user to turn on the gps
    private void showGpsDialog() {
        new AlertDialog.Builder(this)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setTitle(R.string.notice)
            .setMessage(R.string.dialog_message)
            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                }
            })
            .setCancelable(false)
            .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.main_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent = new Intent();
        switch (item.getItemId()) {
        case R.id.settings:
            intent.setClass(this, GpsConfigActivity.class);
            startActivityForResult(intent, REQUEST_CONFIG_CODE);
            break;
        case R.id.statistics:
            intent.setClass(this, GpsStatisticsActivity.class);
            startActivity(intent);
            break;
        }
        return super.onOptionsItemSelected(item);
    }

    // get the gps config from GpsConfigActivity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CONFIG_CODE && resultCode == RESULT_OK) {
            mConfigModel = data.getParcelableExtra("config");
            Log.d(TAG, mConfigModel.toString());
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    // stop the test when press BACK
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            SharedPreferences sp = getSharedPreferences(GpsConfigActivity.PREF_FILE_NAME, MODE_PRIVATE);
            Editor editor = sp.edit();
            editor.putBoolean(GpsConfigActivity.PREF_USE_DEFAULT_VALUE, true);
            editor.commit();
            if (mIsLocating) {
                stopTest();
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
        case R.id.start_stop:
            if (mIsLocating) {
                stopTest();
            } else {
                startTest();
            }
            break;
        case R.id.clear:
            clearAidingData();
            break;
        }
    }

    // start gps test
    private void startTest() {
        if (mConfigModel == null) {
            Toast.makeText(this,R.string.config_gps,Toast.LENGTH_SHORT).show();
            return;
        }
        initGpsConfig(mConfigModel);
        mIsLocating = true;
        mStopLocate = false;
        new Timer().schedule(new GpsTimerTask(this), 0);
        mStartStop.setText(getString(R.string.stop));
        mClear.setEnabled(false);
    }

    // stop gps test
    private void stopTest() {
        mStopLocate = true;
        if (mNextTimer != null) {
            mNextTimer.cancel();
        }
        if (mCurrentTask != null) {
            mCurrentTask.removeUpdateAndListeners();
        }
        exportTestReport();
        mConfigModel = null;
        mIsLocating = false;
        mStartStop.setText(getString(R.string.start));
        mClear.setEnabled(true);
    }

    // clear Aiding Data
    private void clearAidingData() {
        boolean status = mLocationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, COMMAND_DELETE_AIDING_DATA, null);
        if (status) {
            Toast.makeText(this, R.string.clear_aiding_data_success, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, R.string.clear_aiding_data_failed, Toast.LENGTH_SHORT).show();
        }
    }

    // init gps config
    private void initGpsConfig(GpsConfigModel gpsConfig) {
        clearUI();
        mTestResults.clear();
        mTTFFReport.clear();
        mAccuReport.clear();
        mSessionNO = 0;
        mFixed = 0;
        mFailed = 0;
        mSessions = gpsConfig.mSessions;
        mSessionTime = gpsConfig.mSessionTime;
        mOutTime = gpsConfig.mOutTime;
        mStoragePath = gpsConfig.mStoragePath;
        mMode = gpsConfig.mMode;
        mStartTime = new Date();
        mNMEAFile = null;
    }

    private void exportTestResult(GpsResultUnit result) {
        String filePath = null;
        switch (mStoragePath) {
        case GpsConfigModel.STORAGE_PATH_INTERNAL:
            filePath = getResources().getString(R.string.cfg_internal_path);
            break;
        case GpsConfigModel.STORAGE_PATH_EXTERNAL:
            filePath = getResources().getString(R.string.cfg_external_path);
            break;
        default:
            filePath = getResources().getString(R.string.cfg_internal_path);
            break;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        String date = sdf.format(mStartTime);
        String resultFormat = getString(R.string.file_result_format);
        String resultFileName = String.format(resultFormat, date);

        File testResult = new File(filePath + resultFileName);
        File parentDir = testResult.getParentFile();
        // if the dir or file does not exists,create them
        boolean isNewFile = false;
        if (!parentDir.exists() || !parentDir.isDirectory()) {
            parentDir.mkdirs();
        }
        if (!testResult.exists() || testResult.isDirectory()) {
            try {
                testResult.createNewFile();
                isNewFile = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.d(TAG,"export result to " + testResult.getAbsolutePath());
        FileOutputStream fosResult = null;
        try {
            // generate the gps_test_result.csv
            fosResult = new FileOutputStream(testResult,true);
            // write title at the first time
            if (isNewFile) {
                fosResult.write(getString(R.string.colume_of_statistics_1).getBytes());
            }
            fosResult.write(result.toString().getBytes());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fosResult != null) {
                try {
                    fosResult.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    // export test Result
    private void exportTestReport() {
        String filePath = null;
        switch (mStoragePath) {
        case GpsConfigModel.STORAGE_PATH_INTERNAL:
            filePath = getResources().getString(R.string.cfg_internal_path);
            break;
        case GpsConfigModel.STORAGE_PATH_EXTERNAL:
            filePath = getResources().getString(R.string.cfg_external_path);
            break;
        default:
            filePath = getResources().getString(R.string.cfg_internal_path);
            break;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        String date = sdf.format(mStartTime);
        String reportFormat = getString(R.string.file_report_format);
        String reportFileName = String.format(reportFormat, date);

        File testReport = new File(filePath + reportFileName);
        File parentDir = testReport.getParentFile();
        // if the dir or file does not exists,create them
        if (!parentDir.exists() || !parentDir.isDirectory()) {
            parentDir.mkdirs();
        }
        if (!testReport.exists() || testReport.isDirectory()) {
            try {
                testReport.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        Log.d(TAG,"begin export test report csv");
        FileOutputStream fosReport = null;
        try {
            // create dialog in case it will cause a long time
            mHandler.sendEmptyMessage(DIALOG_SHOW);
            // generate the gps_test_report.csv
            fosReport = new FileOutputStream(testReport,true);
            fosReport.write(getString(R.string.colume_of_statistics_2).getBytes());
            fosReport.write(generateReport().toString().getBytes());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // dismiss the dialog and close the io stream
            mHandler.sendEmptyMessage(DIALOG_DISMISS);
            if (fosReport != null) {
                try {
                    fosReport.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    // generate the GpsReportUnit
    private GpsReportUnit generateReport() {
        GpsReportUnit unit = new GpsReportUnit();
        if (mAccuReport.size() > 0) {
            Collections.sort(mAccuReport);
            unit.mAccu_50 = mAccuReport.get((int)(mAccuReport.size() * 0.5));
            unit.mAccu_68 = mAccuReport.get((int)(mAccuReport.size() * 0.68));
            unit.mAccu_95 = mAccuReport.get((int)(mAccuReport.size() * 0.95));
        }
        if (mTTFFReport.size() > 0) {
            Collections.sort(mTTFFReport);
            unit.mTTFF_50 = mTTFFReport.get((int)(mTTFFReport.size() * 0.5));
            unit.mTTFF_68 = mTTFFReport.get((int)(mTTFFReport.size() * 0.68));
            unit.mTTFF_95 = mTTFFReport.get((int)(mTTFFReport.size() * 0.95));
        }
        unit.mFixed = mFixed;
        unit.mFailed = mFailed;
        return unit;
    }

    // create nmea file when nmea on
    private void createNmeaFile() {
        String filePath = null;
        switch (mStoragePath) {
        case GpsConfigModel.STORAGE_PATH_INTERNAL:
            filePath = getResources().getString(R.string.cfg_internal_path);
            break;
        case GpsConfigModel.STORAGE_PATH_EXTERNAL:
            filePath = getResources().getString(R.string.cfg_external_path);
            break;
        default:
            filePath = getResources().getString(R.string.cfg_internal_path);
            break;
        }

        // the file format likes "NMEA_201305171635.txt"
        SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
        String format = getString(R.string.file_nmea_format);
        String date = sdf.format(mStartTime);
        String fileName = String.format(format, date);
        mNMEAFile = new File(filePath + fileName);

        File parentDir = mNMEAFile.getParentFile();
        if (!parentDir.exists() || !parentDir.isDirectory()) {
            parentDir.mkdirs();
        }
        if (!mNMEAFile.exists() || mNMEAFile.isDirectory()) {
            try {
                mNMEAFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // it will be called every time the location start
    @Override
    public void onTaskStart() {
        mHandler.sendEmptyMessage(CLEAR_UI);
        mSessionNO++;
    }

    // this method will only be used in tracking mode
    @Override
    public void onUpdate(GpsResultUnit gpsResultUnit) {
        Log.d(TAG, "onUpdate mStopLocate=" + mStopLocate);
        if (!mStopLocate) {
            Message msg = mHandler.obtainMessage(UPDATE_GPS_INFO);
            mTestResults.add(gpsResultUnit);
            msg.obj = gpsResultUnit;
            mHandler.sendMessage(msg);
            exportTestResult(gpsResultUnit);
        }
    }

    // update the svinfo
    public void onUpdateSvInfo(GpsStatus gpsStatus) {
        Message msg = mHandler.obtainMessage(UPDATE_SV_INFO);
        msg.obj = gpsStatus;
        mHandler.sendMessage(msg);
    };

    // it will be called every time the location is succeed or overtime
    @Override
    public void onTaskFinished(GpsResultUnit gpsResultUnit) {
        Log.d(TAG, "mSessionNO: " + mSessionNO + ",mSessions: " + mSessions);
        if (!mStopLocate && mSessionNO <= mSessions) {
            Message msg = mHandler.obtainMessage(UPDATE_GPS_INFO);
            mTestResults.add(gpsResultUnit);
            msg.obj = gpsResultUnit;
            mHandler.sendMessage(msg);
            exportTestResult(gpsResultUnit);
            if (mSessionNO == mSessions) {
                exportTestReport();
                mConfigModel = null;
                mIsLocating = false;
            } else {
                mNextTimer = new Timer();
                mNextTimer.schedule(new GpsTimerTask(this), mSessionTime * 1000);
            }
        }
    }

    // this method will be called only the sendExtraCommand() return false
    @Override
    public void onError() {
        Log.d(TAG, "sendExtraCommand Error");
        mIsLocating = false;
        mConfigModel = null;
        mHandler.sendEmptyMessage(SEND_COMMAND_ERROR);
    }

    private void clearUI() {
        mLatitude.setText("");
        mCurSession.setText("");
        mLongitude.setText("");
        mFixNO.setText("");
        mTTFF.setText("");
        mAltitude.setText("");
        mSpeed.setText("");
        mTime.setText("");
        mSvUsed.setText("");
        mStatus.setText("");
        mListView.setAdapter(null);
    }

    // update gps info
    private void updateGpsInfo(GpsResultUnit result) {
        mLatitude.setText("" + result.mLat);
        mCurSession.setText("" + mSessionNO);
        mLongitude.setText("" + result.mLong);
        mFixNO.setText("" + mFixed);
        mTTFF.setText("" + result.mTTFF);
        mAltitude.setText("" + result.mAltitude);
        mSpeed.setText("" + result.mSpeed);
        mTime.setText(result.mTime);
        mSvUsed.setText("" + result.mSvUsed);
        mStatus.setText(result.mStatus);
    }

    // update Sv Info
    private void updateSvInfo(GpsStatus status) {
        List<Map<String,String>> data = new ArrayList<Map<String,String>>();
        Map<String,String> map = null;
        if (status != null) {
            Iterable<GpsSatellite> satellites = status.getSatellites();
            Iterator<GpsSatellite> iter = satellites.iterator();
            while(iter.hasNext()) {
                GpsSatellite satellite = iter.next();
                map = new HashMap<String,String>();
                map.put("prn", "" + satellite.getPrn());
                map.put("snr", "" + satellite.getSnr());
                map.put("elev", "" + satellite.getElevation());
                map.put("azim", "" + satellite.getAzimuth());
                data.add(map);
            }
        }
        SimpleAdapter adapter = new SimpleAdapter(this, data, R.layout.svinfo_adapter_layout, FROM, TO);
        mListView.setAdapter(adapter);
    }

    /**
     * the class which control once location,including get the gps info and deal
     * with location timeover.The TaskExecuteListener is used to notify the
     * Activity when task begin or finished
     */
    class GpsTimerTask extends TimerTask implements LocationListener,GpsStatus.Listener,GpsStatus.NmeaListener {

        private Timer mOverTimeTimer;
        private TaskExecuteListener mListener;
        private GpsResultUnit mResultUnit = new GpsResultUnit();
        private GpsStatus mGpsStatus;
        private int mTTFF;

        // the TaskExecuteListener is implemented in it's caller
        public GpsTimerTask(TaskExecuteListener listener) {
            mListener = listener;
            mCurrentTask = this;
            mTTFF = 0;
        }

        @Override
        public void run() {
            Looper.prepare();
            Log.d(TAG, "GpsTimerTask run");
            if (sendCommands()) {
                mLocationManager.addGpsStatusListener(this);
                if (mConfigModel.mNMEA) {
                    mLocationManager.addNmeaListener(this);
                    if (mNMEAFile == null) {
                        createNmeaFile();
                    }
                }
                // notify the caller that the task is beginning
                mListener.onTaskStart();
                Date startTime = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
                mResultUnit.mStartTime = sdf.format(startTime);
                // start gps location with different mode
                Log.d(TAG, "GpsTimerTask startTime=" + mResultUnit.mStartTime + ",mMode=" + mMode);
                switch(mMode) {
                case GpsConfigModel.MODE_MSA:
                    mLocationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, this, null);
                    break;
                case GpsConfigModel.MODE_MSB:
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1 * 1000, 0, this);
                    break;
                case GpsConfigModel.MODE_STAND_ALONE:
                    if (sendStandAloneCommand()) {
                        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1 * 1000, 0, this);
                        break;
                    } else {
                        removeUpdateAndListeners();
                        mListener.onError();
                        return;
                    }
                case GpsConfigModel.MODE_TRACKING:
                    mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1 * 1000, 0, this);
                }
                // start the overtime task
                if (mMode != GpsConfigModel.MODE_TRACKING) {
                    mOverTimeTimer = new Timer();
                    mOverTimeTimer.schedule(new StopGpsTimerTask(), mOutTime * 1000);
                }
            } else {
                removeUpdateAndListeners();
                mListener.onError();
            }
            Looper.loop();
        }

        // send command to gps provider
        private boolean sendCommands() {
            if (mConfigModel.mTime || mConfigModel.mPosition
                    || mConfigModel.mEph || mConfigModel.mAlm) {
                Bundle extras = new Bundle();
                extras.putBoolean("time", mConfigModel.mTime);
                extras.putBoolean("position", mConfigModel.mPosition);
                extras.putBoolean("ephemeris", mConfigModel.mEph);
                extras.putBoolean("almanac", mConfigModel.mAlm);
                return mLocationManager.sendExtraCommand(LocationManager.GPS_PROVIDER,
                        COMMAND_DELETE_AIDING_DATA, extras);
            } else {
                // there is no need to send Command if nothing of options was choosed
                return true;
            }
        }

        // this method is only be used in standalone mode
        private boolean sendStandAloneCommand() {
            Bundle extras = new Bundle();
            extras.putString("standalone", "standalone");
            return mLocationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, COMMAND_STAND_ALONE, extras);
        }

        // remove update and listeners when current locate stopped
        public void removeUpdateAndListeners() {
            mLocationManager.removeUpdates(this);
            mLocationManager.removeGpsStatusListener(this);
            mLocationManager.removeNmeaListener(this);
            if (mOverTimeTimer != null) {
                mOverTimeTimer.cancel();
            }
        }

        /**
         * the overtime timertask,when it execute,the gps will stop locating and
         * the next locate will be prepared
         */
        class StopGpsTimerTask extends TimerTask {

            @Override
            public void run() {
                Log.d(TAG, "StopGpsTimerTask run");
                removeUpdateAndListeners();
                mFailed++;
                Date endTime = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
                mResultUnit.mEndTime = sdf.format(endTime);
                mResultUnit.mSessionNO = mSessionNO;
                mResultUnit.mTime = "0";
                if (mGpsStatus != null) {
                    mResultUnit.mRefLat = mConfigModel.mRefLat;
                    mResultUnit.mRefLong = mConfigModel.mRefLong;
                    mResultUnit.mGpsStatus = mGpsStatus;
                }
                mResultUnit.mStatus = getResources().getString(R.string.status_failed);
                mListener.onTaskFinished(mResultUnit);
            }

        }

        /*
         * the gps locate succeed once the location is not null.when it happened
         * the overtime task will be canceled and next locate will be prepared
         */
        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, "onLocationChanged");
            if (location != null) {
                Log.i(TAG,"onLocationChanged TTFF=" + mGpsStatus.getTimeToFirstFix());
                if (mTTFF == 0) {
                    return;
                }
                if (mMode != GpsConfigModel.MODE_TRACKING) {
                    mOverTimeTimer.cancel();
                    removeUpdateAndListeners();
                }
                mFixed++;
                Date endTime = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT);
                mResultUnit.mEndTime = sdf.format(endTime);
                mResultUnit.mSessionNO = mSessionNO;
                mResultUnit.mLat = location.getLatitude();
                mResultUnit.mLong = location.getLongitude();
                mResultUnit.mAltitude = location.getAltitude();
                mResultUnit.mSpeed = location.getSpeed();

                // time
                Date time = new Date(location.getTime());
                mResultUnit.mTime = sdf.format(time);

                // accuracy
                float[] results = new float[1];
                Location.distanceBetween(mConfigModel.mRefLat, mConfigModel.mRefLong, location.getLatitude(), location.getLongitude(), results);
                mResultUnit.mAccu = results[0];

                mResultUnit.mStatus = getResources().getString(R.string.status_success);
                mResultUnit.mGpsStatus = mGpsStatus;
                mResultUnit.mTTFF = mTTFF / 1000;
                mResultUnit.mSvUsed = mListView.getAdapter().getCount();
                mResultUnit.mRefLat = mConfigModel.mRefLat;
                mResultUnit.mRefLong = mConfigModel.mRefLong;
                mTTFFReport.add(mResultUnit.mTTFF);
                mAccuReport.add(mResultUnit.mAccu);
                if (mMode == GpsConfigModel.MODE_TRACKING) {
                    mListener.onUpdate(mResultUnit);
                } else {
                    mListener.onTaskFinished(mResultUnit);
                }
            }
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "onProviderDisabled");
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "onProviderEnabled");
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "onStatusChanged");
        }

        // the ttff,svused,prn,snr,elev,azim are getted from this callback method
        @Override
        public void onGpsStatusChanged(int event) {
            Log.d(TAG, "onGpsStatusChanged event: " + event);
            switch(event){
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                mTTFF = mLocationManager.getGpsStatus(null).getTimeToFirstFix();
                break;
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                mGpsStatus = mLocationManager.getGpsStatus(null);
                Log.i(TAG,"GPS_EVENT_SATELLITE_STATUS TTFF = " + mGpsStatus.getTimeToFirstFix());
                mListener.onUpdateSvInfo(mGpsStatus);
                break;
            }
        }

        // if the nmea on,this callback method will be called
        @Override
        public void onNmeaReceived(long timestamp, String nmea) {
            if (mNMEAFile == null) {
                createNmeaFile();
            }
            FileOutputStream fos = null;
            try {
                 fos = new FileOutputStream(mNMEAFile,true);
                 fos.write(nmea.getBytes());
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (fos != null) {
                    try {
                        fos.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    }

}