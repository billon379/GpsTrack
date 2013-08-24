package com.example.gpstrack;

import com.example.gpstrack.R;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.Spinner;

public class GpsConfigActivity extends Activity implements OnClickListener {

    public static final String TAG = "GpsConfigActivity";

    public static final String PREF_FILE_NAME = "gps_config.xml";
    public static final String PREF_USE_DEFAULT_VALUE = "pref_use_default_value";
    public static final String PREF_SESSIONS = "sessions";
    public static final String PREF_SESSION_TIME = "session_time";
    public static final String PREF_OUT_TIME = "out_time";
    public static final String PREF_REF_LONG = "ref_long";
    public static final String PREF_REF_LAT = "ref_lat";
    public static final String PREF_MODE = "mode";
    public static final String PREF_START_MODE = "start_mode";
    public static final String PREF_TIME = "time";
    public static final String PREF_POSITION = "position";
    public static final String PREF_EPH = "eph";
    public static final String PREF_ALM = "alm";
    public static final String PREF_STORAGE_PATH = "storage_path";
    public static final String PREF_NEME = "nmea";

    private EditText mSessions;
    private EditText mSessionTime;
    private EditText mOutTime;
    private EditText mRefLong;
    private EditText mRefLat;
    private Spinner mMode;
    private RadioGroup mStartMode;
    private RadioButton mColdMode;
    private RadioButton mWarmMode;
    private RadioButton mHotMode;
    private CheckBox mTime;
    private CheckBox mPosition;
    private CheckBox mEph;
    private CheckBox mAlm;
    private RadioGroup mStoragePath;
    private RadioButton mInternal;
    private RadioButton mExternal;
    private RadioGroup mNMEA;
    private Button mConfirm;
    private Button mCancel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gps_config_layout);

        // initialize the components
        mSessions = (EditText) findViewById(R.id.sessions);
        mSessionTime = (EditText) findViewById(R.id.time_between_sessions);
        mOutTime = (EditText) findViewById(R.id.time_out);
        mRefLong = (EditText) findViewById(R.id.ref_long);
        mRefLat = (EditText) findViewById(R.id.ref_lat);

        //when select tracking item,clear all config data
        mMode = (Spinner) findViewById(R.id.mode);
        mMode.setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Log.d(TAG,"mode position: " + position);
                if (position == GpsConfigModel.MODE_TRACKING) {
                    updateComponentsStatus(false);
                } else {
                    updateComponentsStatus(true);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}

        });

        mStartMode = (RadioGroup) findViewById(R.id.start_mode);
        mStartMode.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG,"start mode position: " + checkedId);
                updateOthersCheckbox(checkedId);
            }
        });

        mColdMode = (RadioButton) findViewById(R.id.cold);
        mWarmMode = (RadioButton) findViewById(R.id.warm);
        mHotMode = (RadioButton) findViewById(R.id.hot);

        mTime = (CheckBox) findViewById(R.id.cfg_time);
        mPosition = (CheckBox) findViewById(R.id.cfg_position);
        mEph = (CheckBox) findViewById(R.id.cfg_eph);
        mAlm = (CheckBox) findViewById(R.id.cfg_alm);

        mStoragePath = (RadioGroup) findViewById(R.id.storage_path);
        mInternal = (RadioButton) findViewById(R.id.internal);
        mExternal = (RadioButton) findViewById(R.id.external);

        mNMEA = (RadioGroup) findViewById(R.id.nmea);

        mConfirm = (Button) findViewById(R.id.confirm);
        mConfirm.setOnClickListener(this);
        mCancel = (Button) findViewById(R.id.cancel);
        mCancel.setOnClickListener(this);

        init();

        SharedPreferences sp = getSharedPreferences(GpsConfigActivity.PREF_FILE_NAME, MODE_PRIVATE);
        boolean useDefaultValue = sp.getBoolean(PREF_USE_DEFAULT_VALUE, true);
        if (!useDefaultValue) {
            restoreSavedValues();
        }
    }

    @Override
    public void onClick(View view) {
        // TODO Auto-generated method stub
        switch (view.getId()) {
        case R.id.confirm:
            completeConfig();
            break;
        case R.id.cancel:
            finish();
            break;
        default:
            break;
        }
    }

    // when select tracking mode,unable the following components
    private void updateComponentsStatus(boolean flag) {
        mSessions.setEnabled(flag);
        mSessionTime.setEnabled(flag);
        mOutTime.setEnabled(flag);
        mRefLong.setEnabled(flag);
        mRefLat.setEnabled(flag);
        mColdMode.setEnabled(flag);
        mWarmMode.setEnabled(flag);
        mHotMode.setEnabled(flag);
        mTime.setEnabled(flag);
        mPosition.setEnabled(flag);
        mEph.setEnabled(flag);
        mAlm.setEnabled(flag);
    }

    // update others checkbox when start mode changed
    private void updateOthersCheckbox(int checkedId) {
        switch(checkedId) {
        case R.id.cold:
            mTime.setChecked(true);
            mTime.setClickable(false);
            mPosition.setChecked(true);
            mPosition.setClickable(false);
            mEph.setChecked(true);
            mEph.setClickable(false);
            mAlm.setChecked(true);
            mAlm.setClickable(false);
            break;
        case R.id.warm:
            mTime.setChecked(false);
            mTime.setClickable(true);
            mPosition.setChecked(false);
            mPosition.setClickable(true);
            mEph.setChecked(true);
            mEph.setClickable(false);
            mAlm.setChecked(false);
            mAlm.setClickable(true);
            break;
        case R.id.hot:
            mTime.setChecked(false);
            mTime.setClickable(true);
            mPosition.setChecked(false);
            mPosition.setClickable(true);
            mEph.setChecked(false);
            mEph.setClickable(true);
            mAlm.setChecked(false);
            mAlm.setClickable(true);
            break;
        }
    }

    // initialize some config
    public void init() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            mExternal.setChecked(true);
        } else {
            mInternal.setChecked(true);
            mExternal.setEnabled(false);
        }
        updateOthersCheckbox(R.id.cold);
    }

    // we should generate the GpsConfigModel object and send to the
    // GpsConfigActivity when the confirm button pressed
    public void completeConfig() {
        GpsConfigModel model = generateConfigModel();
        Intent data = new Intent();
        data.putExtra("config", model);
        setResult(RESULT_OK, data);
        saveConfigData();
        finish();
    }

    // generate the GpsConfigModel Object
    public GpsConfigModel generateConfigModel() {
        GpsConfigModel model = new GpsConfigModel();
        if (!(mMode.getSelectedItemPosition() == GpsConfigModel.MODE_TRACKING)) {
            String sessions = mSessions.getText().toString();
            model.mSessions = Integer.parseInt("".equals(sessions) ? mSessions.getHint().toString() : sessions);

            String sessionTime = mSessionTime.getText().toString();
            model.mSessionTime = Integer.parseInt("".equals(sessionTime) ? mSessionTime.getHint().toString() : sessionTime);

            String outTime = mOutTime.getText().toString();
            model.mOutTime = Integer.parseInt("".equals(outTime) ? mOutTime.getHint().toString() : outTime);

            String refLong = mRefLong.getText().toString();
            model.mRefLong = Float.parseFloat("".equals(refLong) ? mRefLong.getHint().toString() : refLong);

            String refLat = mRefLat.getText().toString();
            model.mRefLat = Float.parseFloat("".equals(refLat) ? mRefLat.getHint().toString() : refLat);
        }
        model.mMode = mMode.getSelectedItemPosition();
        model.mStartMode = getStartMode();
        model.mTime = mTime.isChecked();
        model.mPosition = mPosition.isChecked();
        model.mEph = mEph.isChecked();
        model.mAlm = mAlm.isChecked();
        model.mStoragePath = getStoragePath();
        model.mNMEA = getNMEA();

        Log.d(TAG, model.toString());
        return model;
    }

    // save the config data
    private void saveConfigData() {
        SharedPreferences sp = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
        Editor editor = sp.edit();
        editor.putString(PREF_SESSIONS, mSessions.getText().toString());
        editor.putString(PREF_SESSION_TIME, mSessionTime.getText().toString());
        editor.putString(PREF_OUT_TIME, mOutTime.getText().toString());
        editor.putString(PREF_REF_LONG, mRefLong.getText().toString());
        editor.putString(PREF_REF_LAT, mRefLat.getText().toString());
        editor.putInt(PREF_MODE, mMode.getSelectedItemPosition());
        editor.putInt(PREF_START_MODE, mStartMode.getCheckedRadioButtonId());
        editor.putBoolean(PREF_TIME, mTime.isChecked());
        editor.putBoolean(PREF_POSITION, mPosition.isChecked());
        editor.putBoolean(PREF_EPH, mEph.isChecked());
        editor.putBoolean(PREF_ALM, mAlm.isChecked());
        editor.putInt(PREF_STORAGE_PATH, mStoragePath.getCheckedRadioButtonId());
        editor.putInt(PREF_NEME, mNMEA.getCheckedRadioButtonId());
        editor.putBoolean(PREF_USE_DEFAULT_VALUE, false);
        editor.commit();
    }

    // restore the config data
    private void restoreSavedValues() {
        SharedPreferences sp = getSharedPreferences(PREF_FILE_NAME, MODE_PRIVATE);
        mSessions.setText(sp.getString(PREF_SESSIONS, ""));
        mSessionTime.setText(sp.getString(PREF_SESSION_TIME, ""));
        mOutTime.setText(sp.getString(PREF_OUT_TIME, ""));
        mRefLong.setText(sp.getString(PREF_REF_LONG, ""));
        mRefLat.setText(sp.getString(PREF_REF_LAT, ""));
        mMode.setSelection(sp.getInt(PREF_MODE, GpsConfigModel.MODE_MSB));
        mStartMode.check(sp.getInt(PREF_START_MODE, R.id.cold));
        mTime.setChecked(sp.getBoolean(PREF_TIME, true));
        mPosition.setChecked(sp.getBoolean(PREF_POSITION, true));
        mEph.setChecked(sp.getBoolean(PREF_EPH, true));
        mAlm.setChecked(sp.getBoolean(PREF_ALM, true));
        mStoragePath.check(sp.getInt(PREF_STORAGE_PATH, R.id.external));
        mNMEA.check(sp.getInt(PREF_NEME, R.id.nmea_on));
    }

    // get gps start mode
    public int getStartMode() {
        switch (mStartMode.getCheckedRadioButtonId()) {
        case R.id.cold:
            return GpsConfigModel.START_MODE_COLD;
        case R.id.warm:
            return GpsConfigModel.START_MODE_WARM;
        case R.id.hot:
            return GpsConfigModel.START_MODE_HOT;
        default:
            return -1;
        }
    }

    // get gps storage path
    public int getStoragePath() {
        switch (mStoragePath.getCheckedRadioButtonId()) {
        case R.id.internal:
            return GpsConfigModel.STORAGE_PATH_INTERNAL;
        case R.id.external:
            return GpsConfigModel.STORAGE_PATH_EXTERNAL;
        default:
            return -1;
        }
    }

    // get NMEA status
    public boolean getNMEA() {
        switch (mNMEA.getCheckedRadioButtonId()) {
        case R.id.nmea_on:
            return true;
        case R.id.nmea_off:
            return false;
        default:
            return false;
        }
    }

}
