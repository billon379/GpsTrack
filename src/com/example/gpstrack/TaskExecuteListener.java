package com.example.gpstrack;

import android.location.GpsStatus;

public interface TaskExecuteListener {

    public void onTaskStart();
    public void onUpdate(GpsResultUnit gpsResultUnit);
    public void onUpdateSvInfo(GpsStatus gpsStatus);
    public void onTaskFinished(GpsResultUnit gpsResultUnit);
    public void onError();

}
