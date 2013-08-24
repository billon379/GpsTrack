package com.example.gpstrack;

import android.os.Parcel;
import android.os.Parcelable;

public class GpsConfigModel implements Parcelable {

    // gps mode
    public static final int MODE_MSB = 0;
    public static final int MODE_MSA = 1;
    public static final int MODE_STAND_ALONE = 2;
    public static final int MODE_TRACKING = 3;

    // gps start mode
    public static final int START_MODE_COLD = 0;
    public static final int START_MODE_WARM = 1;
    public static final int START_MODE_HOT = 2;

    // storage path
    public static final int STORAGE_PATH_INTERNAL = 0;
    public static final int STORAGE_PATH_EXTERNAL = 1;

    // the variable to be serialized
    public int mSessions;
    public int mSessionTime;
    public int mOutTime;
    public float mRefLong;
    public float mRefLat;
    public int mMode;
    public int mStartMode;
    public boolean mTime;
    public boolean mPosition;
    public boolean mEph;
    public boolean mAlm;
    public int mStoragePath;
    public boolean mNMEA;

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<GpsConfigModel> CREATOR = new Parcelable.Creator<GpsConfigModel>() {

        // read the model object
        @Override
        public GpsConfigModel createFromParcel(Parcel source) {
            GpsConfigModel model = new GpsConfigModel();
            model.mSessions = source.readInt();
            model.mSessionTime = source.readInt();
            model.mOutTime = source.readInt();
            model.mRefLong = source.readFloat();
            model.mRefLat = source.readFloat();
            model.mMode = source.readInt();
            model.mStartMode = source.readInt();
            model.mStoragePath = source.readInt();
            boolean[] array = new boolean[5];
            source.readBooleanArray(array);
            model.mTime = array[0];
            model.mPosition = array[1];
            model.mEph = array[2];
            model.mAlm = array[3];
            model.mNMEA = array[4];
            return model;
        }

        @Override
        public GpsConfigModel[] newArray(int size) {
            return new GpsConfigModel[size];
        }
    };

    // serialize the model object
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mSessions);
        dest.writeInt(mSessionTime);
        dest.writeInt(mOutTime);
        dest.writeFloat(mRefLong);
        dest.writeFloat(mRefLat);
        dest.writeInt(mMode);
        dest.writeInt(mStartMode);
        dest.writeInt(mStoragePath);
        boolean[] array = { mTime, mPosition, mEph, mAlm, mNMEA };
        dest.writeBooleanArray(array);
    }

    @Override
    public String toString() {
        return "GpsConfigModel [mSessions=" + mSessions + ", mSessionTime="
                + mSessionTime + ", mOutTime=" + mOutTime + ", mRefLong="
                + mRefLong + ", mRefLat=" + mRefLat + ", mMode=" + mMode
                + ", mStartMode=" + mStartMode + ", mTime=" + mTime
                + ", mPosition=" + mPosition + ", mEph=" + mEph + ", mAlm="
                + mAlm + ", mStoragePath=" + mStoragePath + ", mNMEA=" + mNMEA
                + "]";
    }

}
