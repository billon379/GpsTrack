package com.example.gpstrack;

import java.util.Iterator;

import android.location.GpsSatellite;
import android.location.GpsStatus;

public class GpsResultUnit {

    public static final int SESSION_NO = 2;
    public static final int TFTT = 6;
    public static final int ACCU = 12;
    public static final int ITEM_COUNT = 18;

    public String mStartTime;
    public String mEndTime;
    public int mSessionNO;
    public double mLat;
    public double mLong;
    public double mAltitude;
    public int mTTFF;
    public float mSpeed;
    public String mTime;
    public int mSvUsed;
    public float mRefLat;
    public float mRefLong;
    public float mAccu;
    public String mPrn;
    public String mSnr;
    public String mElev;
    public String mAzim;
    public String mStatus;
    public GpsStatus mGpsStatus;

    @Override
    public String toString() {
        parseGpsStatus();
        return mStartTime + "," + mEndTime + "," + mSessionNO + "," + mLat
                + "," + mLong + "," + mAltitude + "," + mTTFF + "," + mSpeed
                + "," + mTime + "," + mSvUsed + "," + mRefLat + "," + mRefLong
                + "," + mAccu + "," + mPrn + "," + mSnr + "," + mElev + ","
                + mAzim + "," + mStatus + "\n";
    }

    // parse GpsStatus to mPrn,mSnr,mElev,mAzim,each of them may have more than one item
    // which should be divided by ":"
    private void parseGpsStatus() {
        StringBuilder sbPrn = new StringBuilder();
        StringBuilder sbSnr = new StringBuilder();
        StringBuilder sbElev = new StringBuilder();
        StringBuilder sbAzim = new StringBuilder();
        if (mGpsStatus != null) {
            Iterable<GpsSatellite> satellites = mGpsStatus.getSatellites();
            Iterator<GpsSatellite> iter = satellites.iterator();
            while (iter.hasNext()) {
                GpsSatellite satellite = iter.next();
                sbPrn.append(satellite.getPrn()).append(":");
                sbSnr.append(satellite.getSnr()).append(":");
                sbElev.append(satellite.getElevation()).append(":");
                sbAzim.append(satellite.getAzimuth()).append(":");
            }
        }
        mPrn = sbPrn.toString();
        mSnr = sbSnr.toString();
        mElev = sbElev.toString();
        mAzim = sbAzim.toString();
    }

}
