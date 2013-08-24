package com.example.gpstrack;

public class GpsReportUnit {

    public float mAccu_50;
    public float mAccu_68;
    public float mAccu_95;
    public float mTTFF_50;
    public float mTTFF_68;
    public float mTTFF_95;
    public int mFixed;
    public int mFailed;

    @Override
    public String toString() {
        return mAccu_50 + "," + mAccu_68 + "," + mAccu_95 + "," + mTTFF_50
                + "," + mTTFF_68 + "," + mTTFF_95 + "," + mFixed + ","
                + mFailed + "\n";
    }

}
